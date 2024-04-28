package com.ruyuan.eshop.market.domain.request;

import com.ruyuan.eshop.common.core.AbstractObject;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 取消订单 释放优惠券入参
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
public class CancelOrderReleaseUserCouponRequest extends AbstractObject implements Serializable {

    private static final long serialVersionUID = -1002367548096870904L;
    /**
     * 接入方业务线标识  1, "自营商城"
     */
    private Integer businessIdentifier;

    /**
     * 订单编号
     */
    private String orderId;

    /**
     * 使用的优惠券编号
     */
    private String couponId;
}
