package com.ruyuan.eshop.order.domain.dto;

import com.ruyuan.eshop.common.core.AbstractObject;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单返回结果
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
public class CreateOrderDTO extends AbstractObject implements Serializable {

    /**
     * 订单ID
     */
    private String orderId;

    // 库存不足的商品列表
}