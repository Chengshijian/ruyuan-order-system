package com.ruyuan.eshop.inventory.api;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.inventory.domain.request.CancelOrderReleaseProductStockRequest;
import com.ruyuan.eshop.inventory.domain.request.LockProductStockRequest;
import com.ruyuan.eshop.inventory.domain.request.ReleaseProductStockRequest;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
public interface InventoryApi {

    /**
     * 锁定商品库存
     *
     * @param lockProductStockRequest
     * @return
     */
    JsonResult<Boolean> lockProductStock(LockProductStockRequest lockProductStockRequest);

    /**
     * 取消订单 释放商品库存
     */
    JsonResult<Boolean> cancelOrderReleaseProductStock(CancelOrderReleaseProductStockRequest cancelOrderReleaseProductStockRequest);
}