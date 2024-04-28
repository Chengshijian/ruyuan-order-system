package com.ruyuan.eshop.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruyuan.eshop.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 订单价格表
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
@TableName("order_amount")
public class OrderAmountDO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单编号
     */
    private String orderId;

    /**
     * 收费类型
     */
    private Integer amountType;

    /**
     * 收费金额
     */
    private Integer amount;
}
