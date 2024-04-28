package com.ruyuan.eshop.market.api.impl;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.market.api.MarketApi;
import com.ruyuan.eshop.market.domain.dto.CalculateOrderAmountDTO;
import com.ruyuan.eshop.market.domain.dto.UserCouponDTO;
import com.ruyuan.eshop.market.domain.query.UserCouponQuery;
import com.ruyuan.eshop.market.domain.request.CalculateOrderAmountRequest;
import com.ruyuan.eshop.market.domain.request.CancelOrderReleaseUserCouponRequest;
import com.ruyuan.eshop.market.domain.request.LockUserCouponRequest;
import com.ruyuan.eshop.market.domain.request.ReleaseUserCouponRequest;
import com.ruyuan.eshop.market.exception.MarketBizException;
import com.ruyuan.eshop.market.service.CouponService;
import com.ruyuan.eshop.market.service.MarketService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = MarketApi.class, retries = 0)
public class MarketApiImpl implements MarketApi {

    @Autowired
    private CouponService couponService;

    @Autowired
    private MarketService marketService;

    @Override
    public JsonResult<UserCouponDTO> getUserCoupon(UserCouponQuery userCouponQuery) {
        try {
            UserCouponDTO userCouponDTO = couponService.getUserCoupon(userCouponQuery);
            return JsonResult.buildSuccess(userCouponDTO);
        } catch (MarketBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public JsonResult<Boolean> lockUserCoupon(LockUserCouponRequest lockUserCouponRequest) {
        try {
            Boolean result = couponService.lockUserCoupon(lockUserCouponRequest);
            return JsonResult.buildSuccess(result);
        } catch (MarketBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }


    /**
     * 回退用户使用的优惠券
     */
    @Override
    public JsonResult<Boolean> releaseUserCoupon(ReleaseUserCouponRequest releaseUserCouponRequest) {
        try {
            Boolean result = couponService.releaseUserCoupon(releaseUserCouponRequest);
            return JsonResult.buildSuccess(result);
        } catch (MarketBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    /**
     * 计算订单费用
     *
     * @param calculateOrderAmountRequest
     * @return
     */
    @Override
    public JsonResult<CalculateOrderAmountDTO> calculateOrderAmount(CalculateOrderAmountRequest calculateOrderAmountRequest) {
        try {
            CalculateOrderAmountDTO calculateOrderAmountDTO = marketService.calculateOrderAmount(calculateOrderAmountRequest);
            return JsonResult.buildSuccess(calculateOrderAmountDTO);
        } catch (MarketBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    @Override
    public JsonResult<Boolean> cancelOrderReleaseCoupon(CancelOrderReleaseUserCouponRequest cancelOrderReleaseUserCouponRequest) {
        log.info("回退优惠券,orderId:{}",cancelOrderReleaseUserCouponRequest.getOrderId());
        return JsonResult.buildSuccess(true);
    }
}