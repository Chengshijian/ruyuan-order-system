package com.ruyuan.eshop.order.api.impl;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.order.api.OrderQueryApi;
import com.ruyuan.eshop.order.domain.dto.OrderDetailDTO;
import com.ruyuan.eshop.order.domain.dto.OrderListDTO;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.service.OrderQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 订单中心-订单查询业务接口
 *
 * @author zhonghuashishan
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = OrderQueryApi.class)
public class OrderQueryApiImpl implements OrderQueryApi {

    @Autowired
    private OrderQueryService orderQueryService;

    /**
     * 查询订单列表
     * @param query
     * @return
     */
    @Override
    public JsonResult<PagingInfo<OrderListDTO>> listOrders(OrderQuery query) {
        try {
            //1、参数校验
            orderQueryService.checkQueryParam(query);

            //2、查询
            return JsonResult.buildSuccess(orderQueryService.executeListQuery(query));

        } catch(OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }

    /**
     * 查询订单详情
     * @param orderId
     * @return
     */
    @Override
    public JsonResult<OrderDetailDTO> orderDetail(String orderId) {
        try {
            //1、参数校验
            ParamCheckUtil.checkStringNonEmpty(orderId,OrderErrorCodeEnum.ORDER_ID_IS_NULL);

            //2、查询
            return JsonResult.buildSuccess(orderQueryService.orderDetail(orderId));

        } catch(OrderBizException e) {
            log.error("biz error", e);
            return JsonResult.buildError(e.getErrorCode(), e.getErrorMsg());
        } catch (Exception e) {
            log.error("error", e);
            return JsonResult.buildError(e.getMessage());
        }
    }
}
