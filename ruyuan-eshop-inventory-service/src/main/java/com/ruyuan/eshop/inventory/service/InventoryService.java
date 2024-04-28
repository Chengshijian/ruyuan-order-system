package com.ruyuan.eshop.inventory.service;

import com.ruyuan.eshop.inventory.domain.request.LockProductStockRequest;
import com.ruyuan.eshop.inventory.domain.request.ReleaseProductStockRequest;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
public interface InventoryService {

    /**
     * 锁定商品库存
     * @param lockProductStockRequest
     * @return
     */
    Boolean lockProductStock(LockProductStockRequest lockProductStockRequest);

    /**
     * 释放商品库存
     */
    Boolean releaseProductStock(ReleaseProductStockRequest releaseProductStockRequest);

}
