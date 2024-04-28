package com.ruyuan.eshop.order.schedule;


import com.ruyuan.eshop.order.config.OrderProperties;
import com.ruyuan.eshop.order.dao.OrderInfoDAO;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.domain.request.CancelOrderRequest;
import com.ruyuan.eshop.order.enums.OrderCancelTypeEnum;
import com.ruyuan.eshop.order.service.OrderAfterSaleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自动取消超时订单任务
 * @author zhonghuashishan
 *
 */
@Component
@Slf4j
public class AutoCancelExpiredOrderTask {

    /**
     * 订单管理DAO组件
     */
    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderAfterSaleService orderAfterSaleService;

    public static final Integer ORDER_EXPIRE_TIME = 30 * 60 * 1000;

    @Autowired
    private OrderProperties orderProperties;

    /**
     * 执行任务逻辑
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void execute() {
        // 扫描当前时间 - 订单超时时间 -> 前的一小段时间范围(时间范围用配置中心配置)
        // 比如当前时间11:40，订单超时时间是30分钟，扫描11:09:00 -> 11:10:00这一分钟的未支付订单，
        // 缺点：有一个订单超过了30 + 1 = 31分钟，都没有被处理(取消)，这笔订单就永久待支付
        for(OrderInfoDO order : orderInfoDAO.listAllUnPaid()) {
            if(new Date().getTime() -
                    order.getExpireTime().getTime() >= orderProperties.getExpireTime()) {
                // 超过30min未支付
                CancelOrderRequest request = new CancelOrderRequest();
                request.setOrderId(order.getOrderId());
                request.setUserId(order.getUserId());
                request.setBusinessIdentifier(order.getBusinessIdentifier());
                request.setOrderType(order.getOrderType());
                request.setCancelType(OrderCancelTypeEnum.TIMEOUT_CANCELED.getCode());
                request.setOrderStatus(order.getOrderStatus());
                try {
                    orderAfterSaleService.cancelOrder(request);
                } catch (Exception e) {
                    log.error("AutoCancelExpiredOrderTask execute error:", e);
                }
            }
        }
    }

}
