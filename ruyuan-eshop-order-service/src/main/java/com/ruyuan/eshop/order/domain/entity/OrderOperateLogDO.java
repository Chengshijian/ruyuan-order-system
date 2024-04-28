package com.ruyuan.eshop.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruyuan.eshop.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 订单操作日志表
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
@TableName("order_operate_log")
public class OrderOperateLogDO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderId;

    /**
     * 操作类型
     */
    private Integer operateType;

    /**
     * 前置状态
     */
    private Integer preStatus;

    /**
     * 当前状态
     */
    private Integer currentStatus;

    /**
     * 备注说明
     */
    private String remark;

}
