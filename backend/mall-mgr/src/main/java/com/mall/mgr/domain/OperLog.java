package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台操作日志（内存保留最近记录）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperLog extends Entity {

    private Long adminId;

    private String adminName;

    /** 操作模块/动作，如：商户审核 */
    private String action;

    /** 操作对象，如：商户编号 M20260101 */
    private String target;

    /** 操作详情 */
    private String detail;

    /** 客户端 IP */
    private String ip;
}