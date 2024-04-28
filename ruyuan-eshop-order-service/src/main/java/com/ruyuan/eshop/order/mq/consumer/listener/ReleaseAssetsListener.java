package com.ruyuan.eshop.order.mq.consumer.listener;

import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.constants.RocketMqConstant;
import com.ruyuan.eshop.inventory.domain.request.CancelOrderReleaseProductStockRequest;
import com.ruyuan.eshop.market.domain.request.CancelOrderReleaseUserCouponRequest;
import com.ruyuan.eshop.order.dao.OrderItemDAO;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.entity.OrderItemDO;
import com.ruyuan.eshop.order.domain.request.CancelOrderAssembleRequest;
import com.ruyuan.eshop.order.mq.producer.DefaultProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监听 释放资产消息
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class ReleaseAssetsListener implements MessageListenerConcurrently {

    @Autowired
    private DefaultProducer defaultProducer;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        try {
            for(MessageExt messageExt : list) {
                String message = new String(messageExt.getBody());
                log.info("ReleaseAssetsConsumer message:{}", message);
                CancelOrderAssembleRequest cancelOrderAssembleRequest = JSONObject.parseObject(message,
                        CancelOrderAssembleRequest.class);

                //  发送取消订单退款请求MQ
                defaultProducer.sendMessage(RocketMqConstant.CANCEL_REFUND_REQUEST_TOPIC,
                        JSONObject.toJSONString(cancelOrderAssembleRequest), "取消订单退款");

                //  发送释放库存MQ
                OrderInfoDTO orderInfoDTO = cancelOrderAssembleRequest.getOrderInfoDTO();
                CancelOrderReleaseProductStockRequest cancelOrderReleaseProductStockRequest =
                        buildSkuList(orderInfoDTO, orderItemDAO);
                defaultProducer.sendMessage(RocketMqConstant.CANCEL_RELEASE_INVENTORY_TOPIC,
                        JSONObject.toJSONString(cancelOrderReleaseProductStockRequest), "取消订单释放库存");

                //  发送释放优惠券MQ
                CancelOrderReleaseUserCouponRequest cancelOrderReleaseUserCouponRequest =
                        orderInfoDTO.clone(CancelOrderReleaseUserCouponRequest.class);
                defaultProducer.sendMessage(RocketMqConstant.CANCEL_RELEASE_PROPERTY_TOPIC,
                        JSONObject.toJSONString(cancelOrderReleaseUserCouponRequest), "取消订单释放优惠券");
            }

            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        } catch (Exception e) {
            log.error("consumer error", e);
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }

    private CancelOrderReleaseProductStockRequest buildSkuList(OrderInfoDTO orderInfoDTO, OrderItemDAO orderItemDAO) {
        List<OrderItemDO> orderItemDOList = orderItemDAO.listByOrderId(orderInfoDTO.getOrderId());
        List skuList = orderItemDOList.stream().map(OrderItemDO::getSkuCode).collect(Collectors.toList());

        CancelOrderReleaseProductStockRequest cancelOrderReleaseProductStockRequest = new CancelOrderReleaseProductStockRequest();
        cancelOrderReleaseProductStockRequest.setSkuCodeList(skuList);

        return cancelOrderReleaseProductStockRequest;
    }

}