package com.ruyuan.eshop.order.api;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.order.domain.dto.*;
import com.ruyuan.eshop.order.domain.query.OrderQuery;

/**
 * 订单中心-订单查询业务接口
 *
 * @author zhonghuashishan
 */
public interface OrderQueryApi {

    /**
     * 查询订单列表
     * @param query
     * @return
     */
    JsonResult<PagingInfo<OrderListDTO>> listOrders(OrderQuery query);

    /**
     * 查询订单详情
     * @param orderId
     * @return
     */
    JsonResult<OrderDetailDTO> orderDetail(String orderId);

}
