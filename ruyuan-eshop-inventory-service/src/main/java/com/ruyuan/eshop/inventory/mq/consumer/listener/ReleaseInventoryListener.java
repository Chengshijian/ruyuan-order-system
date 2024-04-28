package com.ruyuan.eshop.inventory.mq.consumer.listener;

import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.constants.RocketMqConstant;
import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.inventory.api.InventoryApi;
import com.ruyuan.eshop.inventory.domain.request.CancelOrderReleaseProductStockRequest;
import com.ruyuan.eshop.inventory.exception.InventoryBizException;
import com.ruyuan.eshop.inventory.exception.InventoryErrorCodeEnum;
import com.ruyuan.eshop.inventory.mq.config.RocketMQProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 监听释放库存消息
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Component
public class ReleaseInventoryListener implements MessageListenerConcurrently {

    @DubboReference(version = "1.0.0")
    private InventoryApi inventoryApi;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        try {
            for (MessageExt msg : list) {
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                log.info("ReleaseInventoryConsumer message:{}", content);
                CancelOrderReleaseProductStockRequest cancelOrderReleaseProductStockRequest
                        = JSONObject.parseObject(content, CancelOrderReleaseProductStockRequest.class);

                JsonResult<Boolean> jsonResult = inventoryApi.cancelOrderReleaseProductStock(cancelOrderReleaseProductStockRequest);
                if (!jsonResult.getSuccess()) {
                    throw new InventoryBizException(InventoryErrorCodeEnum.CONSUME_MQ_FAILED);
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (Exception e) {
            log.error("consumer error", e);
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }
}