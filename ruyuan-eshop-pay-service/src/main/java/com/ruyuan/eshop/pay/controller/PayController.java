package com.ruyuan.eshop.pay.controller;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.order.api.AfterSaleApi;
import com.ruyuan.eshop.order.domain.request.RefundCallbackRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@RestController
@RequestMapping("/pay")
@Slf4j
public class PayController {

    @DubboReference(version = "1.0.0")
    private AfterSaleApi afterSaleApi;

    /**
     * 测试取消订单支付退款回调
     */
    @PostMapping("/refundCallback")
    public JsonResult<Boolean> refundCallback(@RequestBody RefundCallbackRequest refundCallbackRequest) {
        RefundCallbackRequest payRefundCallbackRequest = new RefundCallbackRequest();
        //  模拟数据
        payRefundCallbackRequest.setOrderId(refundCallbackRequest.getOrderId());
        payRefundCallbackRequest.setAfterSaleId(refundCallbackRequest.getAfterSaleId());
        payRefundCallbackRequest.setBatchNo(refundCallbackRequest.getBatchNo());
        payRefundCallbackRequest.setRefundStatus(refundCallbackRequest.getRefundStatus());
        payRefundCallbackRequest.setRefundFee(refundCallbackRequest.getRefundFee());
        payRefundCallbackRequest.setTotalFee(refundCallbackRequest.getTotalFee());
        payRefundCallbackRequest.setSign(refundCallbackRequest.getSign());
        payRefundCallbackRequest.setTradeNo(refundCallbackRequest.getTradeNo());
        payRefundCallbackRequest.setRefundTime(refundCallbackRequest.getRefundTime());
        payRefundCallbackRequest.setRefundFee(refundCallbackRequest.getRefundFee());
        payRefundCallbackRequest.setTotalFee(refundCallbackRequest.getTotalFee());

        return afterSaleApi.refundCallback(payRefundCallbackRequest);
    }
}