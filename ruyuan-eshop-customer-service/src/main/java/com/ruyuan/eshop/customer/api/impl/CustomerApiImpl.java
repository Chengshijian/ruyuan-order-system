package com.ruyuan.eshop.customer.api.impl;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.customer.api.CustomerApi;
import com.ruyuan.eshop.customer.domain.request.CustomerReviewReturnGoodsRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = CustomerApi.class, retries = 0)
public class CustomerApiImpl implements CustomerApi {

    @Override
    public JsonResult<Boolean> customerAudit(CustomerReviewReturnGoodsRequest customerReviewReturnGoodsRequest) {
        log.info("收到提交的审核申请");
        return JsonResult.buildSuccess(true);
    }
}