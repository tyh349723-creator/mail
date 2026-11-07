package com.mall.alipay.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.alipay.domain.*;
import com.mall.alipay.mapper.WalletMapper;
import com.mall.alipay.mapper.WalletRecordMapper;
import com.mall.alipay.mapper.WithdrawMapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import com.mall.common.util.CurrentUser;
import com.mall.common.util.NoGen;
import com.mall.common.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 商户钱包服务：入账（交易净额）、退款支出、提现申请/驳回退回、流水查询。
 * 钱包按商户号懒创建（getOrCreate），保证新入驻商户可立即收款。
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletMapper walletMapper;
    private final WalletRecordMapper walletRecordMapper;
    private final WithdrawMapper withdrawMapper;

    /** 交易入账：balance += 净额（金额-手续费），累计入账与手续费同步累加；流水存 营收 + 手续费 两列（金额明细可查） */
    public synchronized void credit(String merchantNo, BigDecimal money, BigDecimal fee, String orderNo) {
        Wallet w = getOrCreate(merchantNo);
        BigDecimal net = money.subtract(fee);
        w.setBalance(w.getBalance().add(net));
        w.setTotalIncome(w.getTotalIncome().add(net));
        w.setTotalFee(w.getTotalFee().add(fee));
        walletMapper.updateById(w);
        record(merchantNo, "pay_in", money, fee, "订单入账 " + orderNo);
    }

    /** 退款支出：从钱包余额扣回（退还给买家） */
    public synchronized void refundOut(String merchantNo, BigDecimal money, String orderNo) {
        Wallet w = getOrCreate(merchantNo);
        if (w.getBalance().compareTo(money) < 0) {
            throw new ApiException("商户钱包余额不足，无法退款");
        }
        w.setBalance(w.getBalance().subtract(money));
        walletMapper.updateById(w);
        record(merchantNo, "refund_out", money, BigDecimal.ZERO, "售后退款 " + orderNo);
    }

    /** 提现扣减：申请时即扣余额，审核驳回时退回 */
    public synchronized void withdrawOut(String merchantNo, BigDecimal money, String cardNo) {
        Wallet w = getOrCreate(merchantNo);
        if (w.getBalance().compareTo(money) < 0) {
            throw new ApiException("可提现余额不足");
        }
        w.setBalance(w.getBalance().subtract(money));
        walletMapper.updateById(w);
        record(merchantNo, "withdraw_out", money, BigDecimal.ZERO, "提现打款 " + cardNo);
    }

    public synchronized void withdrawBack(String merchantNo, BigDecimal money, String cardNo) {
        Wallet w = getOrCreate(merchantNo);
        w.setBalance(w.getBalance().add(money));
        walletMapper.updateById(w);
        record(merchantNo, "withdraw_back", money, BigDecimal.ZERO, "提现驳回退回 " + cardNo);
    }

    public Wallet getOrCreate(String merchantNo) {
        Wallet w = walletMapper.selectOne(new QueryWrapper<Wallet>().eq("merchant_no", merchantNo).last("LIMIT 1"));
        if (w != null) {
            return w;
        }
        w = new Wallet();
        w.setMerchantNo(merchantNo);
        w.setBalance(BigDecimal.ZERO);
        w.setTotalIncome(BigDecimal.ZERO);
        w.setTotalFee(BigDecimal.ZERO);
        walletMapper.insert(w);
        return w;
    }

    private void record(String merchantNo, String type, BigDecimal money, BigDecimal fee, String remark) {
        WalletRecord r = new WalletRecord();
        r.setMerchantNo(merchantNo);
        r.setTradeType(type);
        r.setMoney(money);
        r.setFee(fee == null ? BigDecimal.ZERO : fee);
        r.setRemark(remark);
        walletRecordMapper.insert(r);
    }

    // ---------- 对外查询 ----------

    /** 当前商户钱包余额 */
    public Result balance() {
        Wallet w = getOrCreate(currentShopNum());
        Result r = Result.ok();
        r.add("balance", w.getBalance());
        r.add("totalIncome", w.getTotalIncome());
        r.add("totalFee", w.getTotalFee());
        return r;
    }

    /**
     * 钱包流水（分页，可按类型筛选）。
     * 返回每条记录的 金额 + 手续费（pay_in 时 money=营收、fee=手续费、实收=money-fee），
     * 并附带筛选口径下的汇总 moneyTotal/feeTotal（手续费明细汇总，供「店铺资金管理」统计）。
     */
    public Result records(Map<String, Object> body) {
        String shopNum = currentShopNum();
        String tradeType = str(body, "tradeType");
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        List<WalletRecord> matched = walletRecordMapper.selectList(new QueryWrapper<WalletRecord>()
                .eq("merchant_no", shopNum)
                .eq(!tradeType.isBlank(), "trade_type", tradeType)
                .orderByDesc("create_time"));
        BigDecimal moneyTotal = matched.stream()
                .map(r -> r.getMoney() == null ? BigDecimal.ZERO : r.getMoney())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal feeTotal = matched.stream()
                .map(r -> r.getFee() == null ? BigDecimal.ZERO : r.getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        matched.sort(Comparator.comparing(a -> a.getCreateTime(), Comparator.reverseOrder()));
        long total = matched.size();
        List<Map<String, Object>> records = matched.stream().skip((long) (current - 1) * size).limit(size).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("tradeType", r.getTradeType());
            m.put("tradeTypeText", typeText(r.getTradeType()));
            m.put("money", r.getMoney());
            m.put("fee", r.getFee() == null ? BigDecimal.ZERO : r.getFee());
            m.put("remark", r.getRemark());
            m.put("createTime", r.getCreateTime());
            return m;
        }).toList();
        return Result.ok().page(total, current, size, records)
                .add("moneyTotal", moneyTotal)
                .add("feeTotal", feeTotal);
    }

    /** 提现申请：{money, cardNo, bankName} */
    public Result withdrawApply(Map<String, Object> body) {
        String shopNum = currentShopNum();
        BigDecimal money = new BigDecimal(String.valueOf(body.get("money")));
        String cardNo = body.get("cardNo") == null ? "" : String.valueOf(body.get("cardNo"));
        if (money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("提现金额不合法");
        }
        if (cardNo.isBlank()) {
            throw new ApiException("请填写收款卡号");
        }
        withdrawOut(shopNum, money, cardNo);
        Withdraw w = new Withdraw();
        w.setWithdrawNo(NoGen.next("W"));
        w.setMerchantNo(shopNum);
        w.setMoney(money);
        w.setBankName(body.get("bankName") == null ? "" : String.valueOf(body.get("bankName")));
        w.setCardNo(cardNo);
        w.setStatus(StatusConst.WITHDRAW_PENDING);
        withdrawMapper.insert(w);
        return Result.ok("提现申请已提交").add("withdrawNo", w.getWithdrawNo());
    }

    /** 当前商户提现记录 */
    public Result withdrawList(Map<String, Object> body) {
        String shopNum = currentShopNum();
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        QueryWrapper<Withdraw> w = new QueryWrapper<Withdraw>()
                .eq("merchant_no", shopNum)
                .orderByDesc("create_time");
        long total = withdrawMapper.selectCount(w);
        List<Withdraw> list = withdrawMapper.selectList(w.last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::withdrawView).toList();
        return Result.ok().page(total, current, size, records);
    }

    /** 平台侧提现记录（内部） */
    public Result withdrawListPlatform(Map<String, Object> body) {
        String merchantNo = str(body, "merchantNo");
        int status = intv(body, "status", -99);
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        QueryWrapper<Withdraw> w = new QueryWrapper<Withdraw>()
                .eq(!merchantNo.isBlank(), "merchant_no", merchantNo)
                .eq(status != -99, "status", status)
                .orderByDesc("create_time");
        long total = withdrawMapper.selectCount(w);
        List<Withdraw> list = withdrawMapper.selectList(w.last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::withdrawView).toList();
        return Result.ok().page(total, current, size, records);
    }

    /** 提现审核（内部）：同意→打款；驳回→余额退回 */
    public Result withdrawAudit(long id, boolean approve) {
        Withdraw w = withdrawMapper.selectById(id);
        if (w == null) {
            throw new ApiException("提现单不存在");
        }
        if (w.getStatus() != StatusConst.WITHDRAW_PENDING) {
            throw new ApiException("提现单已处理");
        }
        if (approve) {
            w.setStatus(StatusConst.WITHDRAW_DONE);
            w.setAuditRemark("打款成功");
        } else {
            w.setStatus(StatusConst.WITHDRAW_REJECTED);
            w.setAuditRemark("材料不完整或异常，已驳回");
            withdrawBack(w.getMerchantNo(), w.getMoney(), w.getCardNo());
        }
        w.setAuditTime(TimeUtils.now());
        withdrawMapper.updateById(w);
        return Result.ok(approve ? "打款成功" : "已驳回，余额退回");
    }

    private Map<String, Object> withdrawView(Withdraw w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("withdrawNo", w.getWithdrawNo());
        m.put("merchantNo", w.getMerchantNo());
        m.put("money", w.getMoney());
        m.put("bankName", w.getBankName());
        m.put("cardNo", w.getCardNo());
        m.put("status", w.getStatus());
        m.put("statusText", withdrawStatusText(w.getStatus()));
        m.put("createTime", w.getCreateTime());
        m.put("auditTime", w.getAuditTime() == null ? "" : w.getAuditTime());
        return m;
    }

    private String withdrawStatusText(int status) {
        return switch (status) {
            case StatusConst.WITHDRAW_PENDING -> "待审核";
            case StatusConst.WITHDRAW_DONE -> "已打款";
            case StatusConst.WITHDRAW_REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    private String typeText(String t) {
        return switch (t) {
            case "pay_in" -> "交易收入";
            case "refund_out" -> "售后退款";
            case "withdraw_out" -> "提现支出";
            case "withdraw_back" -> "提现退回";
            default -> t;
        };
    }

    private String currentShopNum() {
        if (!CurrentUser.isMerchant()) {
            throw ApiException.unauthorized();
        }
        return CurrentUser.userNum();
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private int intv(Map<String, Object> body, String key, int def) {
        Object v = body.get(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}