package com.mall.alipay.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.alipay.domain.PayOrder;
import com.mall.alipay.mapper.PayOrderMapper;
import com.mall.alipay.service.AlipayGatewayService;
import com.mall.alipay.service.PayService;
import com.mall.common.constant.StatusConst;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * 支付宝支付入口：
 * <ul>
 *   <li>GET /pay/alipay/redirect/{payOrderId}：返回手机网站支付自动提交表单；凭证未配置时回退模拟收银台</li>
 *   <li>POST /pay/alipay/notify：支付宝异步通知（RSA2 验签 → 幂等入账），返回 success/failure 纯文本</li>
 *   <li>GET /pay/alipay/return：浏览器同步跳回，验签兜底入账后跳支付结果页</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/pay/alipay")
@RequiredArgsConstructor
public class AlipayController {

    private final PayOrderMapper payOrderMapper;
    private final PayService payService;
    private final AlipayGatewayService alipayGateway;

    @Value("${mall.cashier.base-url}")
    private String cashierBaseUrl;

    @Value("${mall.frontend.base-url}")
    private String frontendBaseUrl;

    /** 收银台入口：真实模式输出支付宝表单，未配置凭证则 302 到本地模拟收银台 */
    @GetMapping("/redirect/{payOrderId}")
    public void redirect(@PathVariable String payOrderId, HttpServletResponse response) throws IOException {
        PayOrder po = payOrderMapper.selectOne(new QueryWrapper<PayOrder>().eq("pay_order_id", payOrderId));
        if (po == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "支付单不存在");
            return;
        }
        if (po.getStatus() == StatusConst.PAY_SUCCESS) {
            response.sendRedirect(cashierBaseUrl + "/pay/result/" + payOrderId);
            return;
        }
        String form = alipayGateway.enabled()
                ? alipayGateway.createWapPayForm(payOrderId, po.getMoney(), "商城订单 " + po.getOrderNo(),
                frontendBaseUrl + "/orders")
                : null;
        if (form == null) {
            // 未配置凭证或支付宝下单失败：回退本地模拟收银台，保证演示链路可用
            response.sendRedirect(cashierBaseUrl + "/pay/cashier/" + payOrderId);
            return;
        }
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(form);
    }

    /** 支付宝异步通知：验签通过后按支付单号幂等入账 */
    @PostMapping("/notify")
    public String notify(@RequestParam Map<String, String> params) {
        if (!alipayGateway.verify(params)) {
            log.warn("支付宝异步通知验签失败 outTradeNo={}", params.get("out_trade_no"));
            return "failure";
        }
        if (!alipayGateway.appId().equals(params.get("app_id"))) {
            log.warn("支付宝异步通知 app_id 不匹配: {}", params.get("app_id"));
            return "failure";
        }
        String tradeStatus = params.get("trade_status");
        String payOrderId = params.get("out_trade_no");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            // 非成功状态（如等待付款）直接确认收到，避免支付宝重复推送
            return "success";
        }
        try {
            payService.onPaySuccess(payOrderId);
        } catch (Exception e) {
            log.error("支付宝回调入账失败 payOrderId={}", payOrderId, e);
            return "failure";
        }
        return "success";
    }

    /** 浏览器同步跳回：验签成功则兜底入账（异步通知丢失时也能确认），随后跳转支付结果页 */
    @GetMapping("/return")
    public void syncReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        String payOrderId = params.getOrDefault("out_trade_no", "");
        try {
            String tradeStatus = params.get("trade_status");
            if (alipayGateway.verify(params)
                    && alipayGateway.appId().equals(params.get("app_id"))
                    && ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))) {
                payService.onPaySuccess(payOrderId);
            }
        } catch (Exception e) {
            log.warn("支付宝同步跳回处理失败 payOrderId={}: {}", payOrderId, e.getMessage());
        }
        if (payOrderId.isBlank()) {
            response.sendRedirect(frontendBaseUrl);
            return;
        }
        response.sendRedirect(cashierBaseUrl + "/pay/result/" + payOrderId);
    }
}
