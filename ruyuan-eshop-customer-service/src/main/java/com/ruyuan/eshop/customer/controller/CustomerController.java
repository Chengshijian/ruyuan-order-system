package com.ruyuan.eshop.customer.controller;

import com.ruyuan.eshop.common.constants.RedisLockKeyConstants;
import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.redis.RedisLock;
import com.ruyuan.eshop.customer.domain.request.CustomerReviewReturnGoodsRequest;
import com.ruyuan.eshop.customer.exception.CustomerBizException;
import com.ruyuan.eshop.customer.exception.CustomerErrorCodeEnum;
import com.ruyuan.eshop.customer.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单售后流程controller
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@RestController
@RequestMapping("/customer")
@Slf4j
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RedisLock redisLock;

    /**
     * 客服售后审核
     */
    @PostMapping("/audit")
    public JsonResult<Boolean> audit(@RequestBody CustomerReviewReturnGoodsRequest customerReviewReturnGoodsRequest) {
        String orderId = customerReviewReturnGoodsRequest.getOrderId();
        //  分布式锁
        String key = RedisLockKeyConstants.REFUND_KEY + orderId;
        boolean lock = redisLock.lock(key);
        if (!lock) {
            throw new CustomerBizException(CustomerErrorCodeEnum.CUSTOMER_AUDIT_REPEAT);
        }
        try {
            //  模拟客服数据
            customerService.customerAudit(customerReviewReturnGoodsRequest);
            return JsonResult.buildSuccess(true);
        } catch (Exception e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());
        } finally {
            redisLock.unlock(key);
        }
    }
}
