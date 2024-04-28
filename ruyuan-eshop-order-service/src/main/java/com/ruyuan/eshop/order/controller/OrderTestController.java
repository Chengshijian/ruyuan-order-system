package com.ruyuan.eshop.order.controller;

import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.fulfill.api.FulfillApi;
import com.ruyuan.eshop.fulfill.domain.event.OrderDeliveredWmsEvent;
import com.ruyuan.eshop.fulfill.domain.event.OrderOutStockWmsEvent;
import com.ruyuan.eshop.fulfill.domain.event.OrderSignedWmsEvent;
import com.ruyuan.eshop.order.api.OrderApi;
import com.ruyuan.eshop.order.api.OrderQueryApi;
import com.ruyuan.eshop.order.domain.dto.*;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.domain.request.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;


/**
 * 正向下单流程接口冒烟测试
 * @author zhonghuashishan
 * @version 1.0
 */
@RestController
@Slf4j
@RequestMapping("/order/test")
public class OrderTestController {

    /**
     * 订单服务
     */
    @DubboReference(version = "1.0.0", retries = 0)
    private OrderApi orderApi;

    @DubboReference(version = "1.0.0")
    private OrderQueryApi queryApi;

    @DubboReference(version = "1.0.0", retries = 0)
    private FulfillApi fulfillApi;

    /**
     * 测试生成新的订单号
     * @param genOrderIdRequest
     * @return
     */
    @PostMapping("/genOrderId")
    public JsonResult<GenOrderIdDTO> genOrderId(@RequestBody GenOrderIdRequest genOrderIdRequest) {
        JsonResult<GenOrderIdDTO> genOrderIdDTO = orderApi.genOrderId(genOrderIdRequest);
        return genOrderIdDTO;
    }

    /**
     * 测试提交订单
     * @param createOrderRequest
     * @return
     */
    @PostMapping("/createOrder")
    public JsonResult<CreateOrderDTO> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        JsonResult<CreateOrderDTO> createOrderDTO = orderApi.createOrder(createOrderRequest);
        return createOrderDTO;
    }

    /**
     * 测试预支付订单
     * @param prePayOrderRequest
     * @return
     */
    @PostMapping("/prePayOrder")
    public JsonResult<PrePayOrderDTO> prePayOrder(@RequestBody PrePayOrderRequest prePayOrderRequest) {
        JsonResult<PrePayOrderDTO> prePayOrderDTO = orderApi.prePayOrder(prePayOrderRequest);
        return prePayOrderDTO;
    }

    /**
     * 测试支付回调
     * @param payCallbackRequest
     * @return
     */
    @PostMapping("/payCallback")
    public JsonResult<Boolean> payCallback(@RequestBody PayCallbackRequest payCallbackRequest) {
        JsonResult<Boolean> result = orderApi.payCallback(payCallbackRequest);
        return result;
    }

    /**
     * 移除订单
     * @param request
     * @return
     */
    @PostMapping("/removeOrders")
    public JsonResult<RemoveOrderDTO> removeOrders(@RequestBody RemoveOrderRequest request) {
        JsonResult<RemoveOrderDTO> result = orderApi.removeOrders(request);
        return result;
    }

    /**
     * 调整订单配置地址
     * @param request
     * @return
     */
    @PostMapping("/adjustDeliveryAddress")
    public JsonResult<AdjustDeliveryAddressDTO> adjustDeliveryAddress(@RequestBody AdjustDeliveryAddressRequest request) {
        JsonResult<AdjustDeliveryAddressDTO> result = orderApi.adjustDeliveryAddress(request);
        return result;
    }

    /**
     * 订单列表查询
     * @param orderQuery
     * @return
     */
    @PostMapping("/listOrders")
    public JsonResult<PagingInfo<OrderListDTO>> listOrders(@RequestBody OrderQuery orderQuery) {
        JsonResult<PagingInfo<OrderListDTO>> result = queryApi.listOrders(orderQuery);
        return result;
    }

    /**
     * 订单详情
     * @param orderId
     * @return
     */
    @GetMapping("/orderDetail")
    public JsonResult<OrderDetailDTO> orderDetail(String orderId) {
        JsonResult<OrderDetailDTO> result = queryApi.orderDetail(orderId);
        return result;
    }

    /**
     * 触发订单发货出库事件
     * @param event
     * @return
     */
    @PostMapping("/triggerOutStockEvent")
    public JsonResult<Boolean> triggerOrderOutStockWmsEvent(@RequestBody OrderOutStockWmsEvent event) {
        log.info("orderId={},event={}",event.getOrderId(), JSONObject.toJSONString(event));
        JsonResult<Boolean> result = fulfillApi.triggerOrderWmsShipEvent(event.getOrderId()
                , OrderStatusChangeEnum.ORDER_OUT_STOCKED,event);
        return result;
    }

    /**
     * 触发订单配送事件
     * @param event
     * @return
     */
    @PostMapping("/triggerDeliveredWmsEvent")
    public JsonResult<Boolean> triggerOrderDeliveredWmsEvent(@RequestBody OrderDeliveredWmsEvent event) {
        log.info("orderId={},event={}",event.getOrderId(), JSONObject.toJSONString(event));
        JsonResult<Boolean> result = fulfillApi.triggerOrderWmsShipEvent(event.getOrderId()
                , OrderStatusChangeEnum.ORDER_DELIVERED,event);
        return result;
    }

    /**
     * 触发订单签收事件
     * @param event
     * @return
     */
    @PostMapping("/triggerSignedWmsEvent")
    public JsonResult<Boolean> triggerOrderSignedWmsEvent(@RequestBody OrderSignedWmsEvent event) {
        log.info("orderId={},event={}",event.getOrderId(), JSONObject.toJSONString(event));
        JsonResult<Boolean> result = fulfillApi.triggerOrderWmsShipEvent(event.getOrderId()
                , OrderStatusChangeEnum.ORDER_SIGNED,event);
        return result;
    }

}