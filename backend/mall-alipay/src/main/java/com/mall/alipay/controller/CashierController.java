package com.mall.alipay.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.alipay.domain.PayChannel;
import com.mall.alipay.domain.PayOrder;
import com.mall.alipay.mapper.PayChannelMapper;
import com.mall.alipay.mapper.PayOrderMapper;
import com.mall.alipay.service.PayService;
import com.mall.common.constant.StatusConst;
import com.mall.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 模拟渠道收银台：GET /pay/cashier/{payOrderId} 展示收银页，
 * 页面点击「模拟支付成功」→ POST /pay/local-callback/{payOrderId}（同源，模拟渠道服务器异步回调）
 * → 幂等处理并通知业务侧 → 跳转 /pay/result/{payOrderId} 结果页。
 * <p>生产环境该页面由真实渠道（微信/支付宝收银台）替代，异步通知为渠道服务器回调到 /pay/callback（验签）。</p>
 */
@RestController
@RequiredArgsConstructor
public class CashierController {

    private final PayOrderMapper payOrderMapper;
    private final PayChannelMapper payChannelMapper;
    private final PayService payService;

    /** 商城前端地址（收银台/支付结果页「返回商城」回退链接），默认本地 vite 端口 */
    @Value("${mall.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    /** 收银台页面 */
    @GetMapping("/pay/cashier/{payOrderId}")
    public String cashier(@PathVariable String payOrderId) {
        PayOrder po = payOrderMapper.selectOne(new QueryWrapper<PayOrder>().eq("pay_order_id", payOrderId));
        if (po == null) {
            throw new com.mall.common.exception.ApiException("支付单不存在");
        }
        PayChannel ch = payChannelMapper.selectOne(new QueryWrapper<PayChannel>()
                .eq("channel_code", po.getChannelCode()).last("LIMIT 1"));
        String channelName = ch == null ? po.getChannelCode() : ch.getChannelName();
        String html = """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>模拟收银台</title>
                  <style>
                    body{font-family:-apple-system,"PingFang SC",sans-serif;background:#f5f6f8;margin:0;padding:48px 16px}
                    .card{max-width:400px;margin:0 auto;background:#fff;border-radius:16px;box-shadow:0 8px 24px rgba(0,0,0,.06);padding:32px 28px;text-align:center}
                    .logo{width:56px;height:56px;border-radius:50%%;background:#1677ff;color:#fff;font-size:28px;line-height:56px;margin:0 auto 12px}
                    h2{margin:8px 0 4px;font-size:18px}
                    .mch{color:#999;font-size:12px;margin-bottom:20px}
                    .amount{font-size:32px;font-weight:700;color:#e4393c;margin:16px 0 4px}
                    .amount small{font-size:14px;color:#999;font-weight:400}
                    .meta{color:#666;font-size:13px;line-height:1.9}
                    button{margin-top:24px;width:100%%;padding:13px;border:0;border-radius:8px;background:#1677ff;color:#fff;font-size:16px;cursor:pointer}
                    button:disabled{background:#ccc}
                    .tip{margin-top:14px;color:#bbb;font-size:12px}
                    .back{display:inline-block;margin-top:16px;color:#93a1b5;text-decoration:none;font-size:13px}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="logo">%s</div>
                    <h2>%s · 模拟收款</h2>
                    <div class="mch">收款方：%s | 订单号 %s</div>
                    <div class="amount"><small>¥</small> %s</div>
                    <div class="meta">支付单号：%s<br/>渠道：%s<br/>回调地址：（本页即模拟渠道服务器通知）</div>
                    <button id="pay">模拟支付成功</button>
                    <div class="tip">用于演示支付成功异步回调与幂等处理</div>
                    <a class="back" href="%s">暂不支付，返回商城</a>
                  </div>
                  <script>
                    document.getElementById('pay').onclick = function(){
                      var btn = this; btn.disabled = true; btn.textContent = '支付处理中...';
                      fetch('/pay/local-callback/%s', {method:'POST', headers:{'Content-Type':'application/json'}, body:'{}'})
                        .then(function(r){ return r.json(); })
                        .then(function(res){
                          if (res.id === '0') { location.href = '/pay/result/%s?ok=1'; }
                          else { alert(res.message || '支付失败'); btn.disabled=false; btn.textContent='模拟支付成功'; }
                        })
                        .catch(function(e){ alert('网络异常：' + e.message); btn.disabled=false; btn.textContent='模拟支付成功'; });
                    };
                  </script>
                </body>
                </html>
                """;
        String svc = po.getChannelCode().startsWith("ALI") ? "支" : po.getChannelCode().startsWith("WX") ? "微" : "银";
        // 占位符顺序：...支付单号、渠道、【返回商城链接】、【回调地址】、【支付结果跳转】
        return html.formatted(svc, channelName, po.getMerchantNo(), po.getOrderNo(), po.getMoney().toPlainString(),
                po.getPayOrderId(), channelName, frontendBaseUrl, payOrderId, payOrderId);
    }

    /**
     * 模拟渠道服务器异步回调（同源，收银台页面触发）。
     * 与正式渠道回调共用同一幂等支付处理逻辑；生产环境由渠道服务器直连 /pay/callback（验签）替代。
     */
    @PostMapping("/pay/local-callback/{payOrderId}")
    public Result localCallback(@PathVariable String payOrderId) {
        if (payOrderId.startsWith("PO-")) {
            payOrderId = payOrderId.substring(3);
        }
        boolean done = payService.onPaySuccess(payOrderId);
        return Result.ok(done ? "支付成功" : "重复回调，已忽略");
    }

    /** 支付结果页（支付成功后提供「查看订单 / 返回订单列表」回退按钮，跳回商城前端） */
    @GetMapping("/pay/result/{payOrderId}")
    public String result(@PathVariable String payOrderId) {
        PayOrder po = payOrderMapper.selectOne(new QueryWrapper<PayOrder>().eq("pay_order_id", payOrderId));
        boolean ok = po != null && (po.getStatus() == StatusConst.PAY_SUCCESS || po.getStatus() == StatusConst.PAY_REFUNDED);
        String title = ok ? "支付成功" : "支付未完成";
        String color = ok ? "#00b578" : "#fa5151";
        String orderNo = po == null ? "-" : po.getOrderNo();
        String detailUrl = frontendBaseUrl + "/user/order-detail/" + orderNo;
        String listUrl = frontendBaseUrl + "/user/orders";
        String hint = ok ? "请返回商城查看订单状态" : "请返回商城重新发起支付";
        String actions = ok
                ? """
                   <a class="btn" href="%s">查看订单</a>
                   <a class="btn ghost" href="%s">返回订单列表</a>
                   """.formatted(detailUrl, listUrl)
                : """
                   <a class="btn" href="%s">返回商城</a>
                   """.formatted(listUrl);
        return """
                <!DOCTYPE html>
                <html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title><style>
                body{background:#f5f6f8;font-family:-apple-system,"PingFang SC",sans-serif;padding:80px 16px;text-align:center}
                .ic{width:72px;height:72px;border-radius:50%%;background:%s;color:#fff;font-size:40px;line-height:72px;margin:0 auto}
                h2{color:%s}
                .sub{color:#999;font-size:13px;margin-top:6px}
                .acts{margin-top:30px;display:flex;flex-direction:column;gap:10px}
                .btn{display:block;max-width:320px;margin:0 auto;box-sizing:border-box;padding:12px;border-radius:8px;background:#1677ff;color:#fff;text-decoration:none;font-size:15px}
                .btn.ghost{background:#fff;color:#1677ff;border:1px solid #1677ff}
                </style></head><body>
                <div class="ic">%s</div>
                <h2>%s</h2>
                <div class="sub">订单号 %s · %s</div>
                <div class="acts">%s</div>
                </body></html>
                """.formatted(title, color, color, ok ? "✓" : "!", title, orderNo, hint, actions);
    }
}