package com.ruyuan.eshop.order.domain.request;

import com.ruyuan.eshop.common.core.AbstractObject;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 订单缺品请求
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
public class LackRequest extends AbstractObject implements Serializable {

    /**
     * 订单号
     */
    private String orderId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 具体的缺品项
     */
    private Set<LackItemRequest> lackItems;

}
