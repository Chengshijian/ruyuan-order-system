package com.ruyuan.eshop.product.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyuan.eshop.common.dao.BaseDAO;
import com.ruyuan.eshop.product.domain.entity.ProductSkuDO;
import com.ruyuan.eshop.product.mapper.ProductSkuMapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 商品sku记录表 Mapper 接口
 * </p>
 *
 * @author zhonghuashishan
 */
@Repository
public class ProductSkuDAO extends BaseDAO<ProductSkuMapper, ProductSkuDO> {

    /**
     * 根据skuCode获取商品信息
     * @param skuCode
     * @return
     */
    public ProductSkuDO getProductSkuByCode(String skuCode) {
        QueryWrapper<ProductSkuDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sku_code", skuCode);
        return getOne(queryWrapper);
    }

}
