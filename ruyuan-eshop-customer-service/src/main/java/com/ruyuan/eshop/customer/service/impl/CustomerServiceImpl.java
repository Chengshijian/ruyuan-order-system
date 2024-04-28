package com.ruyuan.eshop.customer.service.impl;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.customer.domain.request.CustomerReviewReturnGoodsRequest;
import com.ruyuan.eshop.customer.service.CustomerService;
import com.ruyuan.eshop.order.api.AfterSaleApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;


/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    @DubboReference(version = "1.0.0")
    private AfterSaleApi afterSaleApi;

    @Override
    public JsonResult<Boolean> customerAudit(CustomerReviewReturnGoodsRequest customerReviewReturnGoodsRequest) {
        return afterSaleApi.receiveCustomerAuditResult(customerReviewReturnGoodsRequest);

    }
}
