package com.ruyuan.eshop.pay.api;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.pay.domain.dto.PayOrderDTO;
import com.ruyuan.eshop.pay.domain.request.PayRefundRequest;
import com.ruyuan.eshop.pay.domain.request.PayOrderRequest;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
public interface PayApi {

    /**
     * 支付订单
     *
     * @param payOrderRequest
     * @return
     */
    JsonResult<PayOrderDTO> payOrder(PayOrderRequest payOrderRequest);

    /**
     * 查询支付交易流水号
     */
    Boolean getTradeNoByRealTime(String orderId, Integer businessIdentifier);

    /**
     * 调用支付接口执行退款
     */
    Boolean executeRefund(PayRefundRequest payRefundRequest);

}