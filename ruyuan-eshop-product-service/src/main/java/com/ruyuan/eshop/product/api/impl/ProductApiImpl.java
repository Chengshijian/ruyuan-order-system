package com.ruyuan.eshop.product.api.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.product.api.ProductApi;
import com.ruyuan.eshop.product.domain.dto.ProductSkuDTO;
import com.ruyuan.eshop.product.domain.query.ProductSkuQuery;
import com.ruyuan.eshop.product.exception.ProductBizException;
import com.ruyuan.eshop.product.service.ProductSkuService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 商品中心-商品信息
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = ProductApi.class)
public class ProductApiImpl implements ProductApi {

    @Autowired
    private ProductSkuService productSkuService;

    @Override
    public JsonResult<ProductSkuDTO> getProductSku(ProductSkuQuery productSkuQuery) {
        try {
            ParamCheckUtil.checkObjectNonNull(productSkuQuery);
            String skuCode = productSkuQuery.getSkuCode();

            ProductSkuDTO productSkuDTO = productSkuService.getProductSkuByCode(skuCode);
            log.info("productSkuDTO={},productSkuQuery={}"
                    , JSONObject.toJSONString(productSkuDTO),JSONObject.toJSONString(productSkuQuery));
            return JsonResult.buildSuccess(productSkuDTO);
        } catch(ProductBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch(Exception e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

}