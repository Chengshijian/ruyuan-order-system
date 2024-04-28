package com.ruyuan.eshop.inventory.api.impl;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.inventory.api.InventoryApi;
import com.ruyuan.eshop.inventory.domain.request.CancelOrderReleaseProductStockRequest;
import com.ruyuan.eshop.inventory.domain.request.LockProductStockRequest;
import com.ruyuan.eshop.inventory.exception.InventoryBizException;
import com.ruyuan.eshop.inventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@DubboService(version = "1.0.0", interfaceClass = InventoryApi.class, retries = 0)
@Slf4j
public class InventoryApiImpl implements InventoryApi {

    @Autowired
    private InventoryService inventoryService;

    /**
     * 锁定商品库存
     *
     * @param lockProductStockRequest
     * @return
     */
    @Override
    public JsonResult<Boolean> lockProductStock(LockProductStockRequest lockProductStockRequest) {
        try {
            Boolean result = inventoryService.lockProductStock(lockProductStockRequest);
            return JsonResult.buildSuccess(result);
        } catch (InventoryBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    /**
     * 回滚库存
     */
    @Override
    public JsonResult<Boolean> cancelOrderReleaseProductStock(CancelOrderReleaseProductStockRequest cancelOrderReleaseProductStockRequest) {
        log.info("回滚库存,orderId:{}",cancelOrderReleaseProductStockRequest.getOrderId());
        return JsonResult.buildSuccess(true);
    }

}