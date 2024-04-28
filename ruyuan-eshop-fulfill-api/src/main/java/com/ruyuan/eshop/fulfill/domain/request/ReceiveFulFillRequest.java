package com.ruyuan.eshop.fulfill.domain.request;

import lombok.*;
import lombok.experimental.Tolerate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 *  接受订单履约请求
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
@Builder
public class ReceiveFulFillRequest implements Serializable {

    /**
     * 订单号
     */
    private String orderId;

    /**
     * 商家id
     */
    private String sellerId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 配送类型，默认是自配送
     */
    private Integer deliveryType;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人电话
     */
    private String receiverPhone;

    /**
     * 省
     */
    private String receiverProvince;

    /**
     * 市
     */
    private String receiverCity;

    /**
     * 区
     */
    private String receiverArea;

    /**
     * 街道地址
     */
    private String receiverStreetAddress;

    /**
     * 详细地址
     */
    private String receiverDetailAddress;

    /**
     * 经度 六位小数点
     */
    private BigDecimal receiverLon;

    /**
     * 纬度 六位小数点
     */
    private BigDecimal receiverLat;

    /**
     * 商家备注
     */
    private String shopRemark;

    /**
     * 用户备注
     */
    private String userRemark;

    /**
     * 支付方式
     */
    private Integer payType;

    /**
     *  付款总金额
     */
    private Integer payAmount;

    /**
     * 交易总金额
     */
    private Integer totalAmount;

    /**
     * 运费
     */
    private Integer deliveryAmount;

    /**
     * 订单商品明细
     */
    private List<ReceiveOrderItemRequest> receiveOrderItems;

    @Tolerate
    public ReceiveFulFillRequest() {
    }
}
