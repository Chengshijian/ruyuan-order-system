package com.ruyuan.eshop.product.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruyuan.eshop.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 商品sku记录表
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
@TableName("product_sku")
public class ProductSkuDO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品编号
     */
    private String productId;

    /**
     * 商品类型 1:普通商品,2:预售商品
     */
    private Integer productType;

    /**
     * 商品SKU编码
     */
    private String skuCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片
     */
    private String productImg;

    /**
     * 商品单位
     */
    private String productUnit;

    /**
     * 商品销售价格
     */
    private Integer salePrice;

    /**
     * 商品采购价格
     */
    private Integer purchasePrice;
}
