package com.ruyuan.eshop.product.service;

import com.ruyuan.eshop.product.domain.dto.ProductSkuDTO;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
public interface ProductSkuService {

    /**
     * 根据skuCode获取商品sku信息
     * @param skuCode
     * @return
     */
    ProductSkuDTO getProductSkuByCode(String skuCode);

}