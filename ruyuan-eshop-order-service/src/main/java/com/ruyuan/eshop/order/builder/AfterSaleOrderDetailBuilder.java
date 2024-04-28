package com.ruyuan.eshop.order.builder;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.common.utils.ObjectUtil;
import com.ruyuan.eshop.order.domain.dto.*;
import com.ruyuan.eshop.order.domain.entity.AfterSaleInfoDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleItemDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleLogDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleRefundDO;

import java.util.List;

/**
 * <p>
 *  售后单详情构造器
 * </p>
 *
 * @author zhonghuashishan
 */
public class AfterSaleOrderDetailBuilder {

    private AfterSaleOrderDetailDTO detailDTO = new AfterSaleOrderDetailDTO();

    public AfterSaleOrderDetailBuilder afterSaleInfo(AfterSaleInfoDO afterSaleInfoDO) {
        if(null!= afterSaleInfoDO) {
            detailDTO.setAfterSaleInfo(afterSaleInfoDO.clone(AfterSaleInfoDTO.class));
        }
        return this;
    }

    public AfterSaleOrderDetailBuilder afterSaleItems(List<AfterSaleItemDO> afterSaleItems) {
        if(CollectionUtils.isNotEmpty(afterSaleItems)) {
            detailDTO.setAfterSaleItems(ObjectUtil.convertList(afterSaleItems, AfterSaleItemDTO.class));
        }
        return this;
    }

    public AfterSaleOrderDetailBuilder afterSalePays(List<AfterSaleRefundDO> afterSalePays) {
        if(CollectionUtils.isNotEmpty(afterSalePays)) {
            detailDTO.setAfterSalePays(ObjectUtil.convertList(afterSalePays
                    , AfterSalePayDTO.class));
        }
        return this;
    }

    public AfterSaleOrderDetailBuilder afterSaleLogs(List<AfterSaleLogDO> afterSaleLogs) {
       if(CollectionUtils.isNotEmpty(afterSaleLogs)) {
           detailDTO.setAfterSaleLogs(ObjectUtil.convertList(afterSaleLogs,AfterSaleLogDTO.class));
       }
       return this;
    }



    public AfterSaleOrderDetailDTO build() {
        return detailDTO;
    }
}
