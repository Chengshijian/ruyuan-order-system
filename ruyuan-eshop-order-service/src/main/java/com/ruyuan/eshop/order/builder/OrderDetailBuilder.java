package com.ruyuan.eshop.order.builder;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.common.utils.ObjectUtil;
import com.ruyuan.eshop.order.domain.dto.*;
import com.ruyuan.eshop.order.domain.entity.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  订单详情构造器
 * </p>
 *
 * @author zhonghuashishan
 */
public class OrderDetailBuilder {

    private OrderDetailDTO orderDetail = new OrderDetailDTO();

    public OrderDetailBuilder orderInfo(OrderInfoDO orderInfoDO) {
        if(null!= orderInfoDO) {
            orderDetail.setOrderInfo(orderInfoDO.clone(OrderInfoDTO.class));
        }
        return this;
    }

    public OrderDetailBuilder orderItems(List<OrderItemDO> orderItems) {
        if(CollectionUtils.isNotEmpty(orderItems)) {
            orderDetail.setOrderItems(ObjectUtil.convertList(orderItems, OrderItemDTO.class));
        }
        return this;
    }

    public OrderDetailBuilder orderAmountDetails(List<OrderAmountDetailDO> orderAmountDetails) {
        if(CollectionUtils.isNotEmpty(orderAmountDetails)) {
            orderDetail.setOrderAmountDetails(ObjectUtil.convertList(orderAmountDetails
                    , OrderAmountDetailDTO.class));
        }
        return this;
    }

    public OrderDetailBuilder orderDeliveryDetail(OrderDeliveryDetailDO orderDeliveryDetail) {
       if(null != orderDeliveryDetail) {
           orderDetail.setOrderDeliveryDetail(orderDeliveryDetail.clone(OrderDeliveryDetailDTO.class));
       }
       return this;
    }

    public OrderDetailBuilder orderPaymentDetails(List<OrderPaymentDetailDO> orderPaymentDetails) {
        if(CollectionUtils.isNotEmpty(orderPaymentDetails)) {
            orderDetail.setOrderPaymentDetails(ObjectUtil.convertList(orderPaymentDetails
                    , OrderPaymentDetailDTO.class));
        }
        return this;
    }

    public OrderDetailBuilder orderAmounts(List<OrderAmountDO> orderAmounts) {
        if(CollectionUtils.isNotEmpty(orderAmounts)) {
            orderDetail.setOrderAmounts(orderAmounts.stream().collect(
                    Collectors.toMap(OrderAmountDO::getAmountType, OrderAmountDO::getAmount, (v1, v2) -> v1)));
        }
        return this;
    }

    public OrderDetailBuilder orderOperateLogs(List<OrderOperateLogDO> orderOperateLogs) {
        if(CollectionUtils.isNotEmpty(orderOperateLogs)) {
            orderDetail.setOrderOperateLogs(ObjectUtil.convertList(orderOperateLogs
                    , OrderOperateLogDTO.class));
        }
        return this;
    }

    public OrderDetailBuilder orderSnapshots(List<OrderSnapshotDO> orderSnapshots) {
        if(CollectionUtils.isNotEmpty(orderSnapshots)) {
            orderDetail.setOrderSnapshots(ObjectUtil.convertList(orderSnapshots
                    , OrderSnapshotDTO.class));
        }
        return this;
    }

    public OrderDetailBuilder lackItems(List<OrderLackItemDTO> lackItems) {
        orderDetail.setLackItems(lackItems);
        return this;
    }

    public OrderDetailDTO build() {
        return orderDetail;
    }
}
