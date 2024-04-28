package com.ruyuan.eshop.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyuan.eshop.inventory.domain.entity.ProductStockDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 库存中心的商品库存表 Mapper 接口
 * </p>
 *
 * @author zhonghuashishan
 */
@Mapper
public interface ProductStockMapper extends BaseMapper<ProductStockDO> {

    /**
     * 锁定商品库存
     * @param skuCode
     * @param saleQuantity
     * @return
     */
    int lockProductStock(@Param("skuCode") String skuCode, @Param("saleQuantity") Integer saleQuantity);

    /**
     * 释放商品库存
     * @param skuCode
     * @param saleQuantity
     * @return
     */
    int releaseProductStock(@Param("skuCode") String skuCode, @Param("saleQuantity") Integer saleQuantity);

}
