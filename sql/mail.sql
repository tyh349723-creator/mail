-- =====================================================================
-- Mall-X 商城项目数据库建表脚本
-- 数据库：mail    字符集：utf8mb4（支持中文/emoji）
-- 覆盖模块：
--   一、业务侧（mall-mgr）  ：16 张表
--   二、支付侧（mall-alipay）： 8 张表
-- 说明：所有表均含公共字段 id / create_time / update_time（对应实体基类 Entity）
-- =====================================================================

-- 客户端连接字符集显式声明为 utf8mb4：
-- 防止 mysql 客户端默认 latin1 将本文件（UTF-8 编码）中的中文按字节误解码，
-- 导致写入数据库的汉字变成双重编码乱码（如 张三 → å¼ ä¸‰）
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS mail DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE mail;

-- =====================================================================
-- 一、业务侧（mall-mgr）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 用户账号表（C 端用户）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS mall_user;
CREATE TABLE mall_user (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_num    VARCHAR(32)  NOT NULL COMMENT '用户唯一编号 userNum',
    username    VARCHAR(64)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(128) NOT NULL COMMENT '登录密码',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    avatar      VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 正常 0 禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_num (user_num),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户账号表';

-- ---------------------------------------------------------------------
-- 2. 用户收货地址表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS address;
CREATE TABLE address (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    consignee    VARCHAR(64)  NOT NULL COMMENT '收货人',
    phone        VARCHAR(20)  NOT NULL COMMENT '联系电话',
    province     VARCHAR(32)  DEFAULT NULL COMMENT '省',
    city         VARCHAR(32)  DEFAULT NULL COMMENT '市',
    district     VARCHAR(32)  DEFAULT NULL COMMENT '区/县',
    detail       VARCHAR(255) NOT NULL COMMENT '详细地址',
    default_flag TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否 1-是',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户收货地址表';

-- ---------------------------------------------------------------------
-- 3. 入驻商户（店铺）表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS merchant;
CREATE TABLE merchant (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    merchant_no   VARCHAR(32)  NOT NULL COMMENT '商户唯一编号 shopNum',
    username      VARCHAR(64)  NOT NULL COMMENT '登录账号',
    password      VARCHAR(128) NOT NULL COMMENT '登录密码',
    shop_name     VARCHAR(128) NOT NULL COMMENT '店铺名称',
    shop_phone    VARCHAR(20)  DEFAULT NULL COMMENT '联系手机号',
    shop_address  VARCHAR(255) DEFAULT NULL COMMENT '经营地址',
    contact_name  VARCHAR(64)  DEFAULT NULL COMMENT '联系人姓名',
    category_name VARCHAR(64)  DEFAULT NULL COMMENT '经营类目名称',
    status        INT          NOT NULL DEFAULT 0 COMMENT '状态：0-待审核 1-正常 2-已驳回 3-已禁用',
    reject_reason VARCHAR(255) DEFAULT NULL COMMENT '审核备注（驳回原因等）',
    auditor       VARCHAR(64)  DEFAULT NULL COMMENT '审核操作管理员名',
    audit_time    DATETIME     DEFAULT NULL COMMENT '审核时间',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_merchant_no (merchant_no),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='入驻商户表';

-- ---------------------------------------------------------------------
-- 4. 商户资质文件表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS qualification;
CREATE TABLE qualification (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    merchant_id  BIGINT UNSIGNED NOT NULL COMMENT '商户ID',
    merchant_no  VARCHAR(32)  NOT NULL COMMENT '商户编号',
    type         VARCHAR(32)  NOT NULL COMMENT '资质类型：business_license营业执照/id_card法人身份证/bank银行账户',
    name         VARCHAR(128) NOT NULL COMMENT '资质名称',
    url          VARCHAR(512) DEFAULT NULL COMMENT '文件地址',
    audit_status INT          NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审 1-通过 2-驳回',
    audit_remark VARCHAR(255) DEFAULT NULL COMMENT '审核意见',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_merchant_id (merchant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商户资质文件表';

-- ---------------------------------------------------------------------
-- 5. 商品分类表（两级：level=1 顶级，level=2 子级）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS category;
CREATE TABLE category (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '分类名称',
    parent_id   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0-顶级，否则父分类id',
    level       INT          NOT NULL DEFAULT 1 COMMENT '分类层级 1/2',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    icon        VARCHAR(255) DEFAULT NULL COMMENT '图标（emoji或图片地址）',
    status      INT          NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商品分类表';

-- ---------------------------------------------------------------------
-- 6. 商品表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    product_no     VARCHAR(32)   NOT NULL COMMENT '商品编号 productNo',
    merchant_id    BIGINT UNSIGNED NOT NULL COMMENT '所属商户ID',
    merchant_no    VARCHAR(32)   NOT NULL COMMENT '所属商户编号',
    shop_name      VARCHAR(128)  DEFAULT NULL COMMENT '店铺名称快照',
    title          VARCHAR(128)  NOT NULL COMMENT '商品标题',
    sub_title      VARCHAR(255)  DEFAULT NULL COMMENT '副标题/卖点',
    category_id    BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
    main_image     VARCHAR(512)  DEFAULT NULL COMMENT '主图地址',
    images         TEXT          DEFAULT NULL COMMENT '轮播图片（JSON数组）',
    price          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '现价（元）',
    original_price DECIMAL(10,2) DEFAULT NULL COMMENT '原价（元）',
    stock          INT           NOT NULL DEFAULT 0 COMMENT '库存（各SKU库存聚合值）',
    sales          INT           NOT NULL DEFAULT 0 COMMENT '已售',
    status         INT           NOT NULL DEFAULT 0 COMMENT '状态：0-下架 1-上架 2-删除',
    is_recommend   INT           NOT NULL DEFAULT 0 COMMENT '是否推荐：0-否 1-是',
    detail         TEXT          DEFAULT NULL COMMENT '详情图文（HTML）',
    create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_no (product_no),
    KEY idx_merchant_id (merchant_id),
    KEY idx_category_id (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商品表';

-- ---------------------------------------------------------------------
-- 7. 商品 SKU 规格表（库存按 SKU 独立维护）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS product_sku;
CREATE TABLE product_sku (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    product_id  BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    spec_name   VARCHAR(64)   NOT NULL DEFAULT '默认' COMMENT '规格名，如：颜色/包装/默认',
    spec_value  VARCHAR(64)   NOT NULL DEFAULT '默认规格' COMMENT '规格值，如：红色/单件装',
    price       DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'SKU价格（元）',
    stock       INT           NOT NULL DEFAULT 0 COMMENT 'SKU库存',
    image       VARCHAR(512)  DEFAULT NULL COMMENT 'SKU图片（为空沿用商品主图）',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_product_id (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商品SKU规格表';

-- ---------------------------------------------------------------------
-- 7.1 库存流水表（记录每次库存变动，便于对账与排查库存对不上）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS inventory_log;
CREATE TABLE inventory_log (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    sku_id       BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    product_id   BIGINT UNSIGNED DEFAULT NULL COMMENT '商品ID',
    order_no     VARCHAR(32)   DEFAULT NULL COMMENT '关联业务订单号',
    change_type  VARCHAR(16)   NOT NULL COMMENT '变动类型：下单扣减/超时释放/用户取消/平台关闭',
    change_qty   INT           NOT NULL COMMENT '变动数量（正数入库，负数出库）',
    before_stock INT           DEFAULT NULL COMMENT '变动前SKU库存',
    after_stock  INT           DEFAULT NULL COMMENT '变动后SKU库存',
    remark       VARCHAR(255)  DEFAULT NULL COMMENT '备注',
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_sku_id (sku_id),
    KEY idx_order_no (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='库存流水表';

-- ---------------------------------------------------------------------
-- 8. 购物车条目表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS cart_item;
CREATE TABLE cart_item (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    product_id    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    sku_id        BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    product_title VARCHAR(128)  DEFAULT NULL COMMENT '商品标题快照',
    product_image VARCHAR(512)  DEFAULT NULL COMMENT '商品图片快照',
    spec_info     VARCHAR(128)  DEFAULT NULL COMMENT '规格信息，如：颜色:红色',
    price         DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价（元）',
    quantity      INT           NOT NULL DEFAULT 1 COMMENT '数量',
    checked       TINYINT       NOT NULL DEFAULT 1 COMMENT '勾选状态：0-未勾选 1-已勾选',
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='购物车条目表';

-- ---------------------------------------------------------------------
-- 9. 业务订单表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_no     VARCHAR(32)   NOT NULL COMMENT '业务订单号 mchOrderNo',
    user_id      BIGINT UNSIGNED NOT NULL COMMENT '下单用户ID',
    user_num     VARCHAR(32)   DEFAULT NULL COMMENT '下单用户编号',
    buyer_name   VARCHAR(64)   DEFAULT NULL COMMENT '买家昵称快照',
    merchant_id  BIGINT UNSIGNED NOT NULL COMMENT '商户ID',
    merchant_no  VARCHAR(32)   NOT NULL COMMENT '商户编号 shopNum',
    shop_name    VARCHAR(128)  DEFAULT NULL COMMENT '店铺名称快照',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品总额（元）',
    pay_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '应付金额（元）',
    status       INT           NOT NULL DEFAULT -1 COMMENT '状态：-1待支付 0已支付 1支付失败 2已退款 3订单关闭 4已发货 5已完成',
    channel      VARCHAR(16)   DEFAULT NULL COMMENT '支付渠道编码：WECHAT/ALIPAY/UNIONPAY',
    pay_time     DATETIME      DEFAULT NULL COMMENT '支付时间',
    consignee    VARCHAR(64)   DEFAULT NULL COMMENT '收货人',
    phone        VARCHAR(20)   DEFAULT NULL COMMENT '联系电话',
    address      VARCHAR(255)  DEFAULT NULL COMMENT '收货地址（省市区+详细）',
    remark       VARCHAR(255)  DEFAULT NULL COMMENT '买家备注',
    close_time   DATETIME      DEFAULT NULL COMMENT '关闭时间',
    close_reason VARCHAR(64)   DEFAULT NULL COMMENT '关闭原因：超时未支付/用户取消/平台关闭',
    pay_timeout_minutes INT    DEFAULT NULL COMMENT '支付超时时长（分钟），下单时固化；为空则用全局配置',
    finish_time  DATETIME      DEFAULT NULL COMMENT '确认收货时间',
    freight      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费（元，本项目包邮）',
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_merchant_no (merchant_no),
    KEY idx_status_create (status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='业务订单表';

-- ---------------------------------------------------------------------
-- 10. 订单商品快照表（下单时固化价格与名称）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_id      BIGINT UNSIGNED NOT NULL COMMENT '所属订单ID',
    product_id    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    sku_id        BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    product_title VARCHAR(128)  DEFAULT NULL COMMENT '商品标题快照',
    product_image VARCHAR(512)  DEFAULT NULL COMMENT '商品图片快照',
    spec_info     VARCHAR(128)  DEFAULT NULL COMMENT '规格信息',
    price         DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '成交单价（元）',
    quantity      INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_order_id (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单商品快照表';

-- ---------------------------------------------------------------------
-- 11. 平台管理员账号表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS admin_user;
CREATE TABLE admin_user (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(128) NOT NULL COMMENT '登录密码',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    roles       VARCHAR(255) DEFAULT NULL COMMENT '角色编码（逗号分隔），如 super_admin,operator',
    status      INT          NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='平台管理员账号表';

-- ---------------------------------------------------------------------
-- 12. 管理员角色表（RBAC）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS role;
CREATE TABLE role (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    role_name   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(64)  NOT NULL COMMENT '角色编码',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    status      INT          NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    perms       TEXT         DEFAULT NULL COMMENT '权限码集合（逗号分隔），如 merchant:review,order:list',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='管理员角色表';

-- ---------------------------------------------------------------------
-- 13. 后台操作日志表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS oper_log;
CREATE TABLE oper_log (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    admin_id    BIGINT UNSIGNED DEFAULT NULL COMMENT '操作管理员ID',
    admin_name  VARCHAR(64)  DEFAULT NULL COMMENT '操作管理员名',
    action      VARCHAR(64)  DEFAULT NULL COMMENT '操作模块/动作，如：商户审核',
    target      VARCHAR(128) DEFAULT NULL COMMENT '操作对象，如：商户编号 M20260101',
    detail      VARCHAR(512) DEFAULT NULL COMMENT '操作详情',
    ip          VARCHAR(64)  DEFAULT NULL COMMENT '客户端IP',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_admin_id (admin_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='后台操作日志表';

-- ---------------------------------------------------------------------
-- 14. 广告位表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS advert_slot;
CREATE TABLE advert_slot (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    slot_name   VARCHAR(64) NOT NULL COMMENT '广告位名称',
    slot_code   VARCHAR(64) NOT NULL COMMENT '编码：home_banner/category_banner/detail_banner',
    position    VARCHAR(64) DEFAULT NULL COMMENT '位置标识：首页轮播/分类页/详情页',
    status      INT         NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_slot_code (slot_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='广告位表';

-- ---------------------------------------------------------------------
-- 15. 广告内容表（挂载到广告位下）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS advert;
CREATE TABLE advert (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    slot_id     BIGINT UNSIGNED NOT NULL COMMENT '所属广告位ID',
    title       VARCHAR(128) DEFAULT NULL COMMENT '广告标题',
    image_url   VARCHAR(512) DEFAULT NULL COMMENT '广告图片地址',
    link_url    VARCHAR(512) DEFAULT NULL COMMENT '跳转地址（product商品ID/category分类ID/home首页）',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    start_time  DATETIME     DEFAULT NULL COMMENT '投放开始时间',
    end_time    DATETIME     DEFAULT NULL COMMENT '投放结束时间',
    status      INT          NOT NULL DEFAULT 1 COMMENT '0-下架 1-上架',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_slot_id (slot_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='广告内容表';

-- ---------------------------------------------------------------------
-- 16. 平台系统参数表（名值对存储）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    config_key   VARCHAR(64)  NOT NULL COMMENT '配置键（唯一），如 order.timeout',
    config_value VARCHAR(512) DEFAULT NULL COMMENT '配置值',
    remark       VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='平台系统参数表';

-- =====================================================================
-- 二、支付侧（mall-alipay）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 17. 支付单表（对应一笔业务订单的一次支付）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS pay_order;
CREATE TABLE pay_order (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    pay_order_id VARCHAR(32)   NOT NULL COMMENT '支付单号 payOrderId（对外唯一标识）',
    order_no     VARCHAR(32)   NOT NULL COMMENT '业务订单号',
    merchant_no  VARCHAR(32)   NOT NULL COMMENT '商户编号',
    channel_code VARCHAR(16)   NOT NULL COMMENT '支付渠道编码',
    money        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '交易金额（元）',
    fee          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '手续费（元）',
    status       INT           NOT NULL DEFAULT -1 COMMENT '状态：-1待支付 0成功 1失败 2已退款 3已关闭',
    pay_time     DATETIME      DEFAULT NULL COMMENT '支付时间',
    close_time   DATETIME      DEFAULT NULL COMMENT '关闭时间',
    close_reason VARCHAR(64)   DEFAULT NULL COMMENT '关闭原因：超时未支付/用户取消/平台关闭',
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pay_order_id (pay_order_id),
    KEY idx_order_no (order_no),
    KEY idx_merchant_no (merchant_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付单表';

-- ---------------------------------------------------------------------
-- 18. 退款单表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS refund;
CREATE TABLE refund (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    refund_order_id VARCHAR(32)   NOT NULL COMMENT '退款单号',
    order_no        VARCHAR(32)   NOT NULL COMMENT '原业务订单号',
    merchant_no     VARCHAR(32)   NOT NULL COMMENT '商户编号',
    money           DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额（元）',
    reason          VARCHAR(255)  DEFAULT NULL COMMENT '退款原因',
    status          INT           NOT NULL DEFAULT 0 COMMENT '状态：0审核中 1退款成功 2退款失败（驳回）',
    audit_remark    VARCHAR(255)  DEFAULT NULL COMMENT '审核意见',
    audit_time      DATETIME      DEFAULT NULL COMMENT '审核时间',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_refund_order_id (refund_order_id),
    KEY idx_order_no (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='退款单表';

-- ---------------------------------------------------------------------
-- 19. 支付流水日志表（下单/回调/关单等事件，供对账与排查）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS pay_log;
CREATE TABLE pay_log (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    pay_order_id VARCHAR(32)  DEFAULT NULL COMMENT '支付单号',
    order_no     VARCHAR(32)  DEFAULT NULL COMMENT '业务订单号',
    channel_code VARCHAR(16)  DEFAULT NULL COMMENT '渠道编码',
    event        VARCHAR(32)  DEFAULT NULL COMMENT '事件：create/callback/close/refund',
    remark       VARCHAR(512) DEFAULT NULL COMMENT '备注',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_order_no (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付流水日志表';

-- ---------------------------------------------------------------------
-- 20. 支付渠道配置表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS pay_channel;
CREATE TABLE pay_channel (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    channel_code VARCHAR(16)   NOT NULL COMMENT '渠道编码：WECHAT/ALIPAY/UNIONPAY',
    channel_name VARCHAR(32)   NOT NULL COMMENT '渠道名称',
    base_rate    DECIMAL(6,4)  NOT NULL DEFAULT 0.0060 COMMENT '平台默认费率（如0.006=千分之六）',
    status       INT           NOT NULL DEFAULT 1 COMMENT '渠道开关：1-开启 0-关闭',
    config_json  TEXT          DEFAULT NULL COMMENT '渠道参数（JSON，商户号/appId等）',
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_channel_code (channel_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付渠道配置表';

-- ---------------------------------------------------------------------
-- 21. 商户差异化费率表（覆盖渠道基础费率）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS merchant_rate;
CREATE TABLE merchant_rate (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    merchant_no VARCHAR(32)  NOT NULL COMMENT '商户编号',
    way_code    VARCHAR(16)  NOT NULL COMMENT '渠道编码',
    mch_rate    DECIMAL(6,4) NOT NULL DEFAULT 0.0030 COMMENT '给该商户的费率（如0.003）',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_mch_way (merchant_no, way_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商户差异化费率表';

-- ---------------------------------------------------------------------
-- 22. 商户钱包表（交易实收，扣除手续费后入账）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS wallet;
CREATE TABLE wallet (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    merchant_no  VARCHAR(32)   NOT NULL COMMENT '商户编号（唯一）',
    balance      DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '可提现余额（元）',
    total_income DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计入账净额（元）',
    total_fee    DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计手续费（元）',
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_merchant_no (merchant_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商户钱包表';

-- ---------------------------------------------------------------------
-- 23. 钱包流水表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS wallet_record;
CREATE TABLE wallet_record (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    merchant_no VARCHAR(32)   NOT NULL COMMENT '商户编号',
    trade_type  VARCHAR(32)   NOT NULL COMMENT '类型：pay_in交易入账/refund_out退款支出/withdraw_out提现支出/withdraw_back提现驳回退回',
    money       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '变动金额（pay_in为交易额）',
    fee         DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '手续费（仅pay_in有值）',
    remark      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_merchant_no (merchant_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='钱包流水表';

-- ---------------------------------------------------------------------
-- 24. 商户提现申请表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS withdraw;
CREATE TABLE withdraw (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    withdraw_no  VARCHAR(32)   NOT NULL COMMENT '提现单号',
    merchant_no  VARCHAR(32)   NOT NULL COMMENT '商户编号',
    money        DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '提现金额（元）',
    bank_name    VARCHAR(64)   DEFAULT NULL COMMENT '收款账户（银行/开户名）',
    card_no      VARCHAR(32)   DEFAULT NULL COMMENT '收款卡号',
    status       INT           NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已打款 2已驳回',
    audit_time   DATETIME      DEFAULT NULL COMMENT '审核时间',
    audit_remark VARCHAR(255)  DEFAULT NULL COMMENT '审核意见',
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_withdraw_no (withdraw_no),
    KEY idx_merchant_no (merchant_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商户提现申请表';

-- =====================================================================
-- 三、示例数据（INSERT）
-- =====================================================================

-- ---------- 角色（必须：后台登录与权限校验依赖 super_admin/operator） ----------
INSERT INTO role (role_name, role_code, description, status, perms) VALUES
('超级管理员', 'super_admin', '拥有全部权限', 1, '*'),
('运营人员',   'operator',    '日常运营：商户审核、订单查看、报表查看', 1, 'merchant:review,order:list,report:view');

-- ---------- 管理员账号（必须：否则后台无法登录） ----------
INSERT INTO admin_user (username, password, nickname, roles, status) VALUES
('admin', '123456', '平台管理员', 'super_admin', 1),
('op001', '123456', '运营小王',   'operator',    1);

-- ---------- 用户 ----------
INSERT INTO mall_user (user_num, username, password, nickname, avatar, phone) VALUES
('U20260001', 'zhangsan', '123456', '张三', 'https://example.com/avatar/u1.png', '13800000001'),
('U20260002', 'lisi',     '123456', '李四', 'https://example.com/avatar/u2.png', '13800000002');

-- ---------- 收货地址 ----------
INSERT INTO address (user_id, consignee, phone, province, city, district, detail, default_flag) VALUES
(1, '张三', '13800000001', '广东省', '深圳市', '南山区', '科技园南路100号A座801', 1),
(1, '张三', '13800000001', '广东省', '广州市', '天河区', '体育西路50号', 0);

-- ---------- 分类（顶级 + 子级） ----------
INSERT INTO category (id, name, parent_id, level, sort, icon, status) VALUES
(1, '食品生鲜', 0, 1, 1, '🍎', 1),
(2, '水果蔬菜', 0, 1, 2, '🥬', 1),
(3, '手机数码', 0, 1, 3, '📱', 1),
(4, '男装',     0, 1, 4, '👔', 1),
(5, '休闲零食', 1, 2, 1, '🍪', 1),
(6, '时令水果', 2, 2, 1, '🍊', 1),
(7, '家用电器', 0, 1, 5, '🔌', 1);

-- ---------- 商户 ----------
INSERT INTO merchant (merchant_no, username, password, shop_name, shop_phone, shop_address, contact_name, category_name, status, auditor, audit_time) VALUES
('M20260001', 'shop001', '123456', '鲜味水果店', '13900000001', '广东省深圳市南山区果园路1号', '王老板', '水果蔬菜', 1, 'admin', NOW()),
('M20260002', 'shop002', '123456', '数码优选店', '13900000002', '广东省深圳市福田区华强北100号', '李老板', '手机数码', 1, 'admin', NOW()),
('M20260003', 'shop003', '123456', '潮流服饰店', '13800000003', NULL, '王老板', '男装',     1, NULL, NULL),
('M20260005', 'shop005', '123456', '智联数码旗舰店', '13800000005', NULL, '李老板', '手机数码', 1, NULL, NULL),
('M20260006', 'shop006', '123456', '品质家电馆', '13800000006', NULL, '赵老板', '家用电器', 1, NULL, NULL);

-- ---------- 商户资质 ----------
INSERT INTO qualification (merchant_id, merchant_no, type, name, url, audit_status, audit_remark) VALUES
(1, 'M20260001', 'business_license', '鲜味水果店营业执照', 'https://example.com/file/license1.pdf', 1, '审核通过'),
(1, 'M20260001', 'id_card',          '法人身份证',       'https://example.com/file/idcard1.pdf',  1, '审核通过');

-- ---------- 商品 ----------
INSERT INTO product (product_no, merchant_id, merchant_no, shop_name, title, sub_title, category_id, main_image, images, price, original_price, stock, sales, status, is_recommend, detail) VALUES
('P20260001', 1, 'M20260001', '鲜味水果店', '烟台红富士苹果 5斤装', '脆甜多汁 产地直发', 6, 'https://tse2-mm.cn.bing.net/th/id/OIP-C.owUjUdRhtPxKPXev6aIAuwHaHa?w=512&h=512&c=7&r=0&o=5&pid=1.7', '["https://tse2-mm.cn.bing.net/th/id/OIP-C.owUjUdRhtPxKPXev6aIAuwHaHa?w=512&h=512&c=7&r=0&o=5&pid=1.7"]', 29.90, 39.90, 100, 12, 1, 1, '<p>山东烟台产地直发，新鲜采摘。</p>'),
('P20260002', 2, 'M20260002', '数码优选店', '无线蓝牙耳机',       '降噪长续航',       3, 'https://example.com/img/earphone.jpg', '["https://example.com/img/earphone1.jpg"]', 99.00, 129.00, 50, 5, 1, 0, '<p>蓝牙5.3，30小时续航。</p>'),
('P20260004', 1, 'M20260001', '鲜味水果店', '每日坚果混合装 30袋', '香脆果仁 每日营养', 5, 'https://tse2-mm.cn.bing.net/th/id/OIP-C.u13clJcCfDhLr_LeEheyPgHaE7?w=512&h=512&c=7&r=0&o=5&pid=1.7', '["https://tse2-mm.cn.bing.net/th/id/OIP-C.u13clJcCfDhLr_LeEheyPgHaE7?w=512&h=512&c=7&r=0&o=5&pid=1.7"]', 59.90, 79.90, 200, 8, 1, 1, '<p>多种坚果混合装，每日一袋，营养均衡。</p>'),
('P20260005', 1, 'M20260001', '鲜味水果店', '赣南脐橙 5斤装',     '皮薄多汁 产地直发', 6, 'https://tse3-mm.cn.bing.net/th/id/OIP-C.EM2O8ALJUGS_z1FyXG1DrwHaFj?w=512&h=512&c=7&r=0&o=5&pid=1.7', '["https://tse3-mm.cn.bing.net/th/id/OIP-C.EM2O8ALJUGS_z1FyXG1DrwHaFj?w=512&h=512&c=7&r=0&o=5&pid=1.7"]', 19.90, 29.90, 150, 20, 1, 1, '<p>江西赣南脐橙，新鲜采摘，酸甜多汁。</p>'),
('P20260006', 3, 'M20260003', '潮流服饰店', '男士纯棉休闲T恤',   '亲肤透气 简约百搭', 4, 'https://tse2-mm.cn.bing.net/th/id/OIP-C.hFRP3rZ3gl-KrHKveDrlFQHaLH?w=512&h=640&c=7&r=0&o=5&pid=1.7', '["https://tse2-mm.cn.bing.net/th/id/OIP-C.hFRP3rZ3gl-KrHKveDrlFQHaLH?w=512&h=640&c=7&r=0&o=5&pid=1.7"]', 79.00, 119.00, 300, 15, 1, 1, '<p>100%纯棉面料，舒适透气，多色可选。</p>'),
('P20260007', 4, 'M20260005', '智联数码旗舰店', '5G全面屏智能手机 256GB', '高清大屏 超长续航', 3, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.aJtqhZVLUkluU3EhyqVOXgHaE7?w=512&h=512&c=7&r=0&o=5&pid=1.7', '["https://tse4-mm.cn.bing.net/th/id/OIP-C.aJtqhZVLUkluU3EhyqVOXgHaE7?w=512&h=512&c=7&r=0&o=5&pid=1.7"]', 2999.00, 3299.00, 100, 25, 1, 1, '<p>5G旗舰处理器，全面屏设计，续航持久。</p>'),
('P20260008', 4, 'M20260005', '智联数码旗舰店', '10.1英寸高清平板电脑', '影音娱乐 学习办公', 3, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.WjgHbItw4FfcdvSceFyRKgHaE0?w=512&h=512&c=7&r=0&o=5&pid=1.7', '["https://tse4-mm.cn.bing.net/th/id/OIP-C.WjgHbItw4FfcdvSceFyRKgHaE0?w=512&h=512&c=7&r=0&o=5&pid=1.7"]', 1899.00, 2099.00, 80, 18, 1, 1, '<p>2K护眼屏，四扬声器，轻薄便携。</p>'),
('P20260009', 4, 'M20260005', '智联数码旗舰店', '智能运动手表 蓝牙通话', '心率监测 血氧检测', 3, 'https://tse2-mm.cn.bing.net/th/id/OIP-C.6dV3RwXPcZ3DIjmJ8zgJWgHaHU?w=512&h=512&c=7&r=0&o=5&pid=1.7', '["https://tse2-mm.cn.bing.net/th/id/OIP-C.6dV3RwXPcZ3DIjmJ8zgJWgHaHU?w=512&h=512&c=7&r=0&o=5&pid=1.7"]', 699.00, 899.00, 200, 40, 1, 1, '<p>全天候健康监测，多种运动模式。</p>'),
('P20260010', 5, 'M20260006', '品质家电馆', '智能电饭煲 4L 预约', '柴火饭 口感升级', 7, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.IM2a0YOu4J4QW0Ry3J1sBgHaHa?w=512&h=512&c=7&r=0&o=5&pid=1.7', '["https://tse4-mm.cn.bing.net/th/id/OIP-C.IM2a0YOu4J4QW0Ry3J1sBgHaHa?w=512&h=512&c=7&r=0&o=5&pid=1.7"]', 399.00, 499.00, 150, 60, 1, 1, '<p>24小时预约，IH电磁加热，米饭更香。</p>'),
('P20260011', 5, 'M20260006', '品质家电馆', '新一级能效变频空调 3匹', '冷暖两用 快速制冷', 7, 'https://tse2-mm.cn.bing.net/th/id/OIP-C.VYUSwKE71nGftfiByhxiLAHaGP?w=512&h=512&c=7&r=0&o=5&pid=1.7', '["https://tse2-mm.cn.bing.net/th/id/OIP-C.VYUSwKE71nGftfiByhxiLAHaGP?w=512&h=512&c=7&r=0&o=5&pid=1.7"]', 4599.00, 5199.00, 50, 12, 1, 1, '<p>新一级能效，静音运行，冷暖双模。</p>'),
('P20260012', 5, 'M20260006', '品质家电馆', '双开门变频冰箱 一级能效', '大容量 风冷无霜', 7, 'https://tse1-mm.cn.bing.net/th/id/OIP-C.wUoOCFhPen8lXOGg63bjAAHaE8?w=512&h=512&c=7&r=0&o=5&pid=1.7', '["https://tse1-mm.cn.bing.net/th/id/OIP-C.wUoOCFhPen8lXOGg63bjAAHaE8?w=512&h=512&c=7&r=0&o=5&pid=1.7"]', 3299.00, 3999.00, 60, 15, 1, 1, '<p>风冷无霜，双门大容量，一级能效更省电。</p>');

-- ---------- 商品 SKU ----------
INSERT INTO product_sku (product_id, spec_name, spec_value, price, stock, image) VALUES
(1, '默认', '默认规格', 29.90, 100, NULL),
(2, '颜色', '黑色',     99.00, 30, NULL),
(2, '颜色', '白色',     99.00, 20, NULL),
(3, '默认', '默认规格', 59.90,    200, NULL),
(4, '默认', '默认规格', 19.90,    150, NULL),
(5, '默认', '默认规格', 79.00,    300, NULL),
(6, '默认', '默认规格', 2999.00,  100, NULL),
(7, '默认', '默认规格', 1899.00,  80,  NULL),
(8, '默认', '默认规格', 699.00,   200, NULL),
(9, '默认', '默认规格', 399.00,   150, NULL),
(10, '默认', '默认规格', 4599.00, 50,  NULL),
(11, '默认', '默认规格', 3299.00, 60,  NULL);

-- ---------- 购物车 ----------
INSERT INTO cart_item (user_id, product_id, sku_id, product_title, product_image, spec_info, price, quantity, checked) VALUES
(1, 1, 1, '烟台红富士苹果 5斤装', 'https://tse2-mm.cn.bing.net/th/id/OIP-C.owUjUdRhtPxKPXev6aIAuwHaHa?w=512&h=512&c=7&r=0&o=5&pid=1.7', '默认规格', 29.90, 2, 1),
(1, 2, 2, '无线蓝牙耳机',         'https://example.com/img/earphone.jpg', '黑色', 99.00, 1, 0);

-- ---------- 订单 + 订单明细 ----------
INSERT INTO orders (order_no, user_id, user_num, buyer_name, merchant_id, merchant_no, shop_name, total_amount, pay_amount, status, channel, pay_time, consignee, phone, address, remark, freight) VALUES
('O20260001', 1, 'U20260001', '张三', 1, 'M20260001', '鲜味水果店', 59.80, 59.80, 0, 'ALIPAY', NOW(), '张三', '13800000001', '广东省深圳市南山区科技园南路100号A座801', '尽快发货', 0.00);

INSERT INTO order_item (order_id, product_id, sku_id, product_title, product_image, spec_info, price, quantity) VALUES
(1, 1, 1, '烟台红富士苹果 5斤装', 'https://tse2-mm.cn.bing.net/th/id/OIP-C.owUjUdRhtPxKPXev6aIAuwHaHa?w=512&h=512&c=7&r=0&o=5&pid=1.7', '默认规格', 29.90, 2);

-- ---------- 广告位 + 广告 ----------
INSERT INTO advert_slot (slot_name, slot_code, position, status) VALUES
('首页轮播', 'home_banner',     '首页顶部轮播区', 1),
('分类页横幅', 'category_banner', '分类页顶部',     1);

INSERT INTO advert (slot_id, title, image_url, link_url, sort, start_time, end_time, status) VALUES
(1, '苹果特惠', 'https://example.com/banner/apple.jpg', 'product:1', 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1),
(1, '数码节',   'https://example.com/banner/digital.jpg', 'category:3', 2, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1);

-- ---------- 系统参数 ----------
INSERT INTO sys_config (config_key, config_value, remark) VALUES
('order.timeout',           '10',   '订单超时未支付自动关闭时间（分钟）'),
('refund.large-threshold',  '500',  '大额退款人工审核阈值（元）'),
('withdraw.min-amount',     '100',  '商户最低提现金额（元）');

-- ---------- 支付渠道 ----------
INSERT INTO pay_channel (channel_code, channel_name, base_rate, status, config_json) VALUES
('ALIPAY',    '支付宝',   0.0060, 1, '{"appId":"demo-alipay"}'),
('WECHAT',    '微信支付', 0.0060, 1, '{"mchId":"demo-wechat"}'),
('UNIONPAY',  '云闪付',   0.0050, 1, '{"merId":"demo-unionpay"}');

-- ---------- 商户差异化费率 ----------
INSERT INTO merchant_rate (merchant_no, way_code, mch_rate) VALUES
('M20260001', 'ALIPAY', 0.0030);

-- ---------- 支付单 ----------
INSERT INTO pay_order (pay_order_id, order_no, merchant_no, channel_code, money, fee, status, pay_time) VALUES
('PAY20260001', 'O20260001', 'M20260001', 'ALIPAY', 59.80, 0.18, 0, NOW());

-- ---------- 退款单 ----------
INSERT INTO refund (refund_order_id, order_no, merchant_no, money, reason, status, audit_remark, audit_time) VALUES
('R20260001', 'O20260001', 'M20260001', 59.80, '不想要了', 0, NULL, NULL);

-- ---------- 支付日志 ----------
INSERT INTO pay_log (pay_order_id, order_no, channel_code, event, remark) VALUES
('PAY20260001', 'O20260001', 'ALIPAY', 'create',   '创建支付单'),
('PAY20260001', 'O20260001', 'ALIPAY', 'callback', '支付回调成功');

-- ---------- 商户钱包 ----------
INSERT INTO wallet (merchant_no, balance, total_income, total_fee) VALUES
('M20260001', 59.62, 59.62, 0.18),
('M20260002', 0.00,  0.00,  0.00);

-- ---------- 钱包流水 ----------
INSERT INTO wallet_record (merchant_no, trade_type, money, fee, remark) VALUES
('M20260001', 'pay_in', 59.80, 0.18, '订单 O20260001 交易入账');

-- ---------- 提现申请 ----------
INSERT INTO withdraw (withdraw_no, merchant_no, money, bank_name, card_no, status) VALUES
('W20260001', 'M20260001', 50.00, '中国工商银行 王老板', '6222020000000001', 0);

-- ---------- 操作日志 ----------
INSERT INTO oper_log (admin_id, admin_name, action, target, detail, ip) VALUES
(1, 'admin', '商户审核', '商户编号 M20260001', '审核通过：鲜味水果店', '127.0.0.1');
