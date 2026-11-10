package com.mall.common.constant;

/**
 * 全局业务状态常量（对齐《行业工程实践》实验指导书枚举）。
 * <p>订单状态统一说明：0-支付成功 1-支付失败 2-已退款 3-订单关闭；
 * 本平台业务侧在此基础上扩展：-1 待支付、4 已发货、5 已完成（详见各字段注释）。</p>
 */
public final class StatusConst {

    private StatusConst() {
    }

    /** 商户状态：0-待审核 1-正常 2-已驳回 3-已禁用 */
    public static final int MCH_PENDING = 0;
    public static final int MCH_NORMAL = 1;
    public static final int MCH_REJECTED = 2;
    public static final int MCH_DISABLED = 3;

    /** 是否可经营（仅状态=正常） */
    public static boolean merchantActive(int status) {
        return status == MCH_NORMAL;
    }

    /** 业务订单状态（引导书 0/1/2/3 + 扩展 -1/4/5） */
    public static final int ORDER_WAIT_PAY = -1;    // 待支付（扩展）
    public static final int ORDER_PAID = 0;          // 支付成功 / 已支付待发货（指导书 0）
    public static final int ORDER_PAY_FAILED = 1;    // 支付失败（指导书 1）
    public static final int ORDER_REFUNDED = 2;      // 已退款（指导书 2）
    public static final int ORDER_CLOSED = 3;        // 订单关闭：超时未支付/取消（指导书 3）
    public static final int ORDER_SHIPPED = 4;       // 已发货 / 待收货（扩展）
    public static final int ORDER_FINISHED = 5;      // 已完成：确认收货（扩展）

    /** 支付单状态：0-支付成功 1-支付失败 2-已退款 3-订单关闭；-1 待支付（支付中） */
    public static final int PAY_WAIT = -1;
    public static final int PAY_SUCCESS = 0;
    public static final int PAY_FAILED = 1;
    public static final int PAY_REFUNDED = 2;
    public static final int PAY_CLOSED = 3;

    /** 退款单状态：0-待处理 1-已处理（退款成功） 2-退款失败（商户/平台驳回） 3-已撤销（买家主动撤销待处理申请） */
    public static final int REFUND_PENDING = 0;
    public static final int REFUND_SUCCESS = 1;
    public static final int REFUND_FAILED = 2;
    public static final int REFUND_CANCELED = 3;

    /** 提现状态：0-待审核 1-已打款 2-已驳回 */
    public static final int WITHDRAW_PENDING = 0;
    public static final int WITHDRAW_DONE = 1;
    public static final int WITHDRAW_REJECTED = 2;

    /** 支付渠道编码 */
    public static final String CHANNEL_WECHAT = "WECHAT";
    public static final String CHANNEL_ALIPAY = "ALIPAY";
    public static final String CHANNEL_UNIONPAY = "UNIONPAY";

    /** 商品状态：0-下架 1-上架 2-删除 */
    public static final int PRODUCT_OFF = 0;
    public static final int PRODUCT_ON = 1;
    public static final int PRODUCT_DELETED = 2;

    /** 资质类型：business_license 营业执照 / id_card 法人身份证 / bank 银行账户 */
    public static final String QUALIFY_LICENSE = "business_license";
    public static final String QUALIFY_ID_CARD = "id_card";
    public static final String QUALIFY_BANK = "bank";

    /*
     * 注意：验签密钥（signSecret）与内部互调令牌（internalToken）不在此处以常量形式定义，
     * 它们属于运行期机密，统一通过配置项 mall.sign.secret / mall.internal.token 从环境变量注入
     * （见各服务的 application.yml 与根目录 .env.example 的 MALL_SIGN_SECRET / MALL_INTERNAL_TOKEN）。
     * 严禁把真实密钥写回代码或配置文件，否则一旦公开到仓库即可被用于伪造签名绕过网关验签。
     */
}