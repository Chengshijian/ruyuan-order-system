package com.ruyuan.eshop.inventory.domain.request;

import com.ruyuan.eshop.common.core.AbstractObject;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 取消订单 释放商品库存入参
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
public class CancelOrderReleaseProductStockRequest extends AbstractObject implements Serializable {
    private static final long serialVersionUID = 8108365293403422149L;

    /**
     * 订单包含的sku list
     */
    private List<String> skuCodeList;

    /**
     * 订单编号
     */
    private String orderId;

    /**
     * 接入方业务线标识  1, "自营商城"
     */
    private Integer businessIdentifier;

}