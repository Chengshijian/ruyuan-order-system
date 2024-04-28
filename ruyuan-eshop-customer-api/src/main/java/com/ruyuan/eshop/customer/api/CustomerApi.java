package com.ruyuan.eshop.customer.api;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.customer.domain.request.CustomerReviewReturnGoodsRequest;

/**
 * 客服中心业务接口
 *
 * @author zhonghuashishan
 * @version 1.0
 */
public interface CustomerApi {

    /**
     * 客服接收审核售后申请
     */
    JsonResult<Boolean> customerAudit(CustomerReviewReturnGoodsRequest customerReviewReturnGoodsRequest);
}