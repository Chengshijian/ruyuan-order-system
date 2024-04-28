package com.ruyuan.eshop.inventory.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyuan.eshop.common.dao.BaseDAO;
import com.ruyuan.eshop.inventory.domain.entity.ProductStockDO;
import com.ruyuan.eshop.inventory.mapper.ProductStockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 库存中心的商品库存表 Mapper 接口
 * </p>
 *
 * @author zhonghuashishan
 */
@Repository
public class ProductStockDAO extends BaseDAO<ProductStockMapper, ProductStockDO> {

    @Autowired
    private ProductStockMapper productStockMapper;

    /**
     * 根据skuCode查询商品库存记录
     * @param skuCode
     * @return
     */
    public ProductStockDO getBySkuCode(String skuCode) {
        QueryWrapper<ProductStockDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sku_code", skuCode);
        return getOne(queryWrapper);
    }

    /**
     * 锁定商品库存
     * @param skuCode
     * @param saleQuantity
     * @return
     */
    public int lockProductStock(String skuCode, Integer saleQuantity) {
        return productStockMapper.lockProductStock(skuCode, saleQuantity);
    }

    /**
     * 释放商品库存
     * @param skuCode
     * @param saleQuantity
     * @return
     */
    public int releaseProductStock(String skuCode, Integer saleQuantity) {
        return productStockMapper.releaseProductStock(skuCode, saleQuantity);
    }

}
