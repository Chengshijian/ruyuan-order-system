package com.ruyuan.eshop.inventory.service.impl;

import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.inventory.dao.ProductStockDAO;
import com.ruyuan.eshop.inventory.domain.entity.ProductStockDO;
import com.ruyuan.eshop.inventory.domain.request.LockProductStockRequest;
import com.ruyuan.eshop.inventory.domain.request.ReleaseProductStockRequest;
import com.ruyuan.eshop.inventory.exception.InventoryBizException;
import com.ruyuan.eshop.inventory.exception.InventoryErrorCodeEnum;
import com.ruyuan.eshop.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private ProductStockDAO productStockDAO;

    /**
     * 锁定商品库存
     * @param lockProductStockRequest
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean lockProductStock(LockProductStockRequest lockProductStockRequest) {
        // 检查入参
        checkLockProductStockRequest(lockProductStockRequest);

        List<LockProductStockRequest.OrderItemRequest> orderItemRequestList =
                lockProductStockRequest.getOrderItemRequestList();
        for(LockProductStockRequest.OrderItemRequest orderItemRequest : orderItemRequestList) {
            String skuCode = orderItemRequest.getSkuCode();
            ProductStockDO productStockDO = productStockDAO.getBySkuCode(skuCode);
            if(productStockDO == null) {
                throw new InventoryBizException(InventoryErrorCodeEnum.PRODUCT_SKU_STOCK_ERROR);
            }
            Integer saleQuantity = orderItemRequest.getSaleQuantity();
            // 执行库存扣减，并需要解决防止超卖的问题
            int nums = productStockDAO.lockProductStock(skuCode, saleQuantity);
            if(nums <= 0) {
                throw new InventoryBizException(InventoryErrorCodeEnum.LOCK_PRODUCT_SKU_STOCK_ERROR);
            }
        }
        return true;
    }

    /**
     * 检查锁定商品库存入参
     * @param lockProductStockRequest
     */
    private void checkLockProductStockRequest(LockProductStockRequest lockProductStockRequest) {
        String orderId = lockProductStockRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId);
        List<LockProductStockRequest.OrderItemRequest> orderItemRequestList =
                lockProductStockRequest.getOrderItemRequestList();
        ParamCheckUtil.checkCollectionNonEmpty(orderItemRequestList);
    }

    /**
     * 释放商品库存
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean releaseProductStock(ReleaseProductStockRequest releaseProductStockRequest) {
        // 检查入参
        checkReleaseProductStockRequest(releaseProductStockRequest);

        List<ReleaseProductStockRequest.OrderItemRequest> orderItemRequestList =
                releaseProductStockRequest.getOrderItemRequestList();
        for(ReleaseProductStockRequest.OrderItemRequest orderItemRequest : orderItemRequestList) {
            String skuCode = orderItemRequest.getSkuCode();
            ProductStockDO productStockDO = productStockDAO.getBySkuCode(skuCode);
            if(productStockDO == null) {
                throw new InventoryBizException(InventoryErrorCodeEnum.PRODUCT_SKU_STOCK_ERROR);
            }
            Integer saleQuantity = orderItemRequest.getSaleQuantity();
            // 释放商品库存
            int nums = productStockDAO.releaseProductStock(skuCode, saleQuantity);
            if(nums <= 0) {
                throw new InventoryBizException(InventoryErrorCodeEnum.RELEASE_PRODUCT_SKU_STOCK_ERROR);
            }
        }
        return true;
    }

    /**
     * 检查释放商品库存入参
     * @param releaseProductStockRequest
     */
    private void checkReleaseProductStockRequest(ReleaseProductStockRequest releaseProductStockRequest) {
        String orderId = releaseProductStockRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId);

        List<ReleaseProductStockRequest.OrderItemRequest> orderItemRequestList =
                releaseProductStockRequest.getOrderItemRequestList();
        ParamCheckUtil.checkCollectionNonEmpty(orderItemRequestList);
    }

}
