package com.ruyuan.eshop.order.api.impl;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.customer.domain.request.CustomerReviewReturnGoodsRequest;
import com.ruyuan.eshop.order.api.AfterSaleApi;
import com.ruyuan.eshop.order.domain.dto.CheckLackDTO;
import com.ruyuan.eshop.order.domain.dto.LackDTO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleRefundDO;
import com.ruyuan.eshop.order.domain.request.*;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.service.OrderAfterSaleService;
import com.ruyuan.eshop.order.service.OrderLackService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 订单中心-逆向售后业务接口
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = AfterSaleApi.class, retries = 0)
public class AfterSaleApiImpl implements AfterSaleApi {

    @Autowired
    private OrderLackService orderLackItemService;

    @Autowired
    private OrderAfterSaleService orderAfterSaleService;

    /**
     * 取消订单/超时未支付取消
     */
    @Override
    public JsonResult<Boolean> cancelOrder(CancelOrderRequest cancelOrderRequest) {
        try {
            return orderAfterSaleService.cancelOrder(cancelOrderRequest);
        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public JsonResult<LackDTO> lockItem(LackRequest request) {
        try {
            //1、参数校验
            CheckLackDTO checkResult = orderLackItemService.checkRequest(request);

            //2、缺品处理
            return JsonResult.buildSuccess(orderLackItemService.executeLackRequest(request, checkResult));
        } catch (OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public JsonResult<Boolean> refundCallback(RefundCallbackRequest payRefundCallbackRequest) {
        String orderId = payRefundCallbackRequest.getOrderId();
        log.info("接收到取消订单支付退款回调,orderId:{}", orderId);
        return orderAfterSaleService.receivePaymentRefundCallback(payRefundCallbackRequest);
    }

    @Override
    public JsonResult<Boolean> receiveCustomerAuditResult(CustomerReviewReturnGoodsRequest customerReviewReturnGoodsRequest) {
        return orderAfterSaleService.receiveCustomerAuditResult(customerReviewReturnGoodsRequest);
    }

    @Override
    public JsonResult<Boolean> revokeAfterSale(RevokeAfterSaleRequest request) {
        //1、参数校验
        ParamCheckUtil.checkObjectNonNull(request.getAfterSaleId(), OrderErrorCodeEnum.AFTER_SALE_ID_IS_NULL);

        //2、撤销申请
        orderAfterSaleService.revokeAfterSale(request);
        return JsonResult.buildSuccess(true);
    }

}