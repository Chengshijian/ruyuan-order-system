package com.ruyuan.eshop.fulfill.api.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.constants.RocketMqConstant;
import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.common.message.OrderEvent;
import com.ruyuan.eshop.fulfill.api.FulfillApi;
import com.ruyuan.eshop.fulfill.domain.event.BaseWmsShipEvent;
import com.ruyuan.eshop.fulfill.domain.event.OrderDeliveredWmsEvent;
import com.ruyuan.eshop.fulfill.domain.event.OrderOutStockWmsEvent;
import com.ruyuan.eshop.fulfill.domain.event.OrderSignedWmsEvent;
import com.ruyuan.eshop.fulfill.domain.request.CancelFulfillRequest;
import com.ruyuan.eshop.fulfill.domain.request.ReceiveFulFillRequest;
import com.ruyuan.eshop.fulfill.mq.producer.DefaultProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = FulfillApi.class, retries = 0)
public class FulfillApiImpl implements FulfillApi {

    @Autowired
    private DefaultProducer defaultProducer;


    @Override
    public JsonResult<Boolean> receiveOrderFulFill(ReceiveFulFillRequest request) {
        log.info("接受订单履约成功，request={}", JSONObject.toJSONString(request));
        return JsonResult.buildSuccess(true);
    }

    @Override
    public JsonResult<Boolean> triggerOrderWmsShipEvent(String orderId, OrderStatusChangeEnum orderStatusChange, BaseWmsShipEvent wmsEvent) {
        log.info("触发订单物流配送结果事件，orderId={},orderStatusChange={},wmsEvent={}", orderId, orderStatusChange, JSONObject.toJSONString(wmsEvent));

        Message message = null;
        String body = null;
        if (OrderStatusChangeEnum.ORDER_OUT_STOCKED.equals(orderStatusChange)) {
            message = new Message();
            //订单已出库事件
            OrderOutStockWmsEvent outStockEvent = (OrderOutStockWmsEvent) wmsEvent;
            outStockEvent.setOrderId(orderId);

            //构建订单已出库消息体
            OrderEvent<OrderOutStockWmsEvent> orderEvent = buildOrderEvent(orderId, OrderStatusChangeEnum.ORDER_OUT_STOCKED,
                    outStockEvent, OrderOutStockWmsEvent.class);

            body = JSONObject.toJSONString(orderEvent);
        } else if (OrderStatusChangeEnum.ORDER_DELIVERED.equals(orderStatusChange)) {
            message = new Message();
            //订单已配送事件
            OrderDeliveredWmsEvent deliveredWmsEvent = (OrderDeliveredWmsEvent) wmsEvent;
            deliveredWmsEvent.setOrderId(orderId);

            //构建订单已配送消息体
            OrderEvent<OrderDeliveredWmsEvent> orderEvent = buildOrderEvent(orderId, OrderStatusChangeEnum.ORDER_DELIVERED,
                    deliveredWmsEvent, OrderDeliveredWmsEvent.class);

            body = JSONObject.toJSONString(orderEvent);

        } else if (OrderStatusChangeEnum.ORDER_SIGNED.equals(orderStatusChange)) {
            message = new Message();
            //订单已签收事件
            OrderSignedWmsEvent signedWmsEvent = (OrderSignedWmsEvent) wmsEvent;
            signedWmsEvent.setOrderId(orderId);

            //构建订单已签收消息体
            OrderEvent<OrderSignedWmsEvent> orderEvent = buildOrderEvent(orderId, OrderStatusChangeEnum.ORDER_SIGNED,
                    signedWmsEvent, OrderSignedWmsEvent.class);

            body = JSONObject.toJSONString(orderEvent);
        }


        if (null != message) {
            message.setTopic(RocketMqConstant.ORDER_WMS_SHIP_RESULT_TOPIC);
            message.setBody(body.getBytes(StandardCharsets.UTF_8));
            try {
                DefaultMQProducer defaultMQProducer = defaultProducer.getProducer();
                SendResult sendResult = defaultMQProducer.send(message, new MessageQueueSelector() {
                    @Override
                    public MessageQueue select(List<MessageQueue> mqs, Message message, Object arg) {
                        //根据订单id选择发送queue
                        String orderId = (String) arg;
                        long index = hash(orderId) % mqs.size();
                        return mqs.get((int) index);
                    }
                }, orderId);

                log.info("send order wms ship result message finished，SendResult status:%s, queueId:%d, body:%s", sendResult.getSendStatus(),
                        sendResult.getMessageQueue().getQueueId(), body);
            } catch (Exception e) {
                log.error("send order wms ship result message error,orderId={},err={}", orderId, e.getMessage(), e);
            }
        }

        return JsonResult.buildSuccess(true);
    }


    @Override
    public JsonResult<Boolean> cancelFulfill(CancelFulfillRequest cancelFulfillRequest) {
        log.info("告知仓储不要配货、物流不要取货");
        return JsonResult.buildSuccess(true);
    }

    private <T> OrderEvent buildOrderEvent(String orderId, OrderStatusChangeEnum orderStatusChange, T messaheContent, Class<T> clazz) {
        OrderEvent<T> orderEvent = new OrderEvent<>();

        orderEvent.setOrderId(orderId);
        orderEvent.setBusinessIdentifier(1);
        orderEvent.setOrderType(1);
        orderEvent.setOrderStatusChange(orderStatusChange);
        orderEvent.setMessageContent(messaheContent);

        return orderEvent;
    }

    private int hash(String orderId) {
        //解决取模可能为负数的情况
        return orderId.hashCode() & Integer.MAX_VALUE;
    }

}
