package com.ruyuan.eshop.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruyuan.eshop.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 订单价格明细表
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
@TableName("order_amount_detail")
public class OrderAmountDetailDO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单编号
     */
    private String orderId;

    /**
     * 产品类型
     */
    private Integer productType;

    /**
     * 订单明细编号
     */
    private String orderItemId;

    /**
     * 商品编号
     */
    private String productId;

    /**
     * sku编码
     */
    private String skuCode;

    /**
     * 销售数量
     */
    private Integer saleQuantity;

    /**
     * 销售单价
     */
    private Integer salePrice;

    /**
     * 收费类型
     */
    private Integer amountType;

    /**
     * 收费金额
     */
    private Integer amount;
}
