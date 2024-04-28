package com.ruyuan.eshop.product.domain.query;

import com.ruyuan.eshop.common.core.AbstractObject;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
public class ProductSkuQuery extends AbstractObject implements Serializable {

    private static final long serialVersionUID = 4788741095015777932L;

    /**
     * 卖家ID
     */
    private String sellerId;

    /**
     * 商品skuCode
     */
    private String skuCode;
}