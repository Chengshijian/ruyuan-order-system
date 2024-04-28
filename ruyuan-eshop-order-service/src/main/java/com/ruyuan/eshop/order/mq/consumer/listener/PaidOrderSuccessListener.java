package com.ruyuan.eshop.order.mq.consumer.listener;

import com.alibaba.fastjson.JSON;
import com.ruyuan.eshop.common.constants.RedisLockKeyConstants;
import com.ruyuan.eshop.common.exception.BaseBizException;
import com.ruyuan.eshop.common.message.PaidOrderSuccessMessage;
import com.ruyuan.eshop.common.redis.RedisLock;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.service.OrderFulFillService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 监听订单支付成功后的消息
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class PaidOrderSuccessListener implements MessageListenerConcurrently {

    @Autowired
    private OrderFulFillService orderFulFillService;

    @Autowired
    RedisLock redisLock;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        try {
            for(MessageExt messageExt : list) {
                String message = new String(messageExt.getBody());
                PaidOrderSuccessMessage paidOrderSuccessMessage =
                        JSON.parseObject(message, PaidOrderSuccessMessage.class);
                String orderId = paidOrderSuccessMessage.getOrderId();
                log.info("触发订单履约，orderId:{}", orderId);

                //1、加分布式锁+里面的履约前置状态校验防止消息重复消费
                String key = RedisLockKeyConstants.ORDER_FULFILL_KEY + orderId;
                boolean lock = redisLock.lock(key);
                if(!lock) {
                    log.error("order has not acquired lock，cannot fulfill, orderId={}",orderId);
                    throw new BaseBizException(OrderErrorCodeEnum.ORDER_FULFILL_ERROR);
                }

                try {
                    //2、触发订单履约逻辑
                    // 注意这里分布式锁加锁放在了本地事务外面
                    orderFulFillService.triggerOrderFulFill(orderId);
                }finally {
                    if(lock) {
                        redisLock.unlock(key);
                    }
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (Exception e) {
            log.error("consumer error", e);
            //本地业务逻辑执行失败，触发消息重新消费
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }
}