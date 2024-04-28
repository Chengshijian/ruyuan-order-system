package com.ruyuan.eshop.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.common.page.PagingInfo;
import com.ruyuan.eshop.common.utils.ExtJsonUtil;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.order.builder.OrderDetailBuilder;
import com.ruyuan.eshop.order.constants.OrderConstants;
import com.ruyuan.eshop.order.dao.*;
import com.ruyuan.eshop.order.domain.dto.*;
import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.enums.*;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.service.AfterSaleQueryService;
import com.ruyuan.eshop.order.service.OrderQueryService;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private OrderAmountDetailDAO orderAmountDetailDAO;

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderSnapshotDAO orderSnapshotDAO;

    @Autowired
    private OrderAmountDAO orderAmountDAO;

    @Autowired
    private OrderOperateLogDAO orderOperateLogDAO;

    @Autowired
    private AfterSaleQueryService afterSaleQueryService;


    @Override
    public void checkQueryParam(OrderQuery query) {

        ParamCheckUtil.checkObjectNonNull(query.getBusinessIdentifier(),OrderErrorCodeEnum.BUSINESS_IDENTIFIER_IS_NULL);
        checkIntAllowableValues(query.getBusinessIdentifier(),BusinessIdentifierEnum.allowableValues(),"businessIdentifier");
        checkIntSetAllowableValues(query.getOrderTypes(),OrderTypeEnum.allowableValues(),"orderTypes");
        checkIntSetAllowableValues(query.getOrderStatus(),OrderStatusEnum.allowableValues(),"orderStatus");


        Integer maxSize = OrderQuery.MAX_PAGE_SIZE;
        checkSetMaxSize(query.getOrderIds(),maxSize,"orderIds");
        checkSetMaxSize(query.getSellerIds(),maxSize,"sellerIds");
        checkSetMaxSize(query.getParentOrderIds(),maxSize,"parentOrderIds");
        checkSetMaxSize(query.getReceiverNames(),maxSize,"receiverNames");
        checkSetMaxSize(query.getReceiverPhones(),maxSize,"receiverPhones");
        checkSetMaxSize(query.getTradeNos(),maxSize,"tradeNos");
        checkSetMaxSize(query.getUserIds(),maxSize,"userIds");
        checkSetMaxSize(query.getSkuCodes(),maxSize,"skuCodes");
        checkSetMaxSize(query.getProductNames(),maxSize,"productNames");
    }

    @Override
    public PagingInfo<OrderListDTO> executeListQuery(OrderQuery query) {

        //第一阶段采用很low的连表查询，连接5张表，即使加索引，只要数据量稍微大一点查询性能就很低了
        //第二阶段会接入es，优化这块的查询性能

        //1、组装业务查询规则
        OrderListQueryDTO queryDTO = OrderListQueryDTO.Builder.builder()
                .copy(query)
                //不展示无效订单
                .removeInValidStatus()
                .setPage(query)
                .build();

        //2、查询
        Page<OrderListDTO> page = orderInfoDAO.listByPage(queryDTO);

        //3、转化
        return PagingInfo.toResponse(page.getRecords()
                , page.getTotal(), (int)page.getCurrent(), (int) page.getSize());
    }

    private void checkIntAllowableValues(Integer i, Set<Integer> allowableValues,String paramName) {
        OrderErrorCodeEnum orderErrorCodeEnum = OrderErrorCodeEnum.ENUM_PARAM_MUST_BE_IN_ALLOWABLE_VALUE;
        ParamCheckUtil.checkIntAllowableValues(i
                , allowableValues,
                orderErrorCodeEnum,paramName,allowableValues);
    }

    private void checkIntSetAllowableValues(Set<Integer> set, Set<Integer> allowableValues, String paramName) {
        OrderErrorCodeEnum orderErrorCodeEnum = OrderErrorCodeEnum.ENUM_PARAM_MUST_BE_IN_ALLOWABLE_VALUE;
        ParamCheckUtil.checkIntSetAllowableValues(set
                , allowableValues,
                orderErrorCodeEnum,paramName,allowableValues);
    }

    private void checkSetMaxSize(Set setParam,Integer maxSize,String paramName) {
        OrderErrorCodeEnum orderErrorCodeEnum = OrderErrorCodeEnum.COLLECTION_PARAM_CANNOT_BEYOND_MAX_SIZE;
        ParamCheckUtil.checkSetMaxSize(setParam, maxSize,
                orderErrorCodeEnum,paramName
                ,maxSize);

    }

    @Override
    public OrderDetailDTO orderDetail(String orderId) {
        //1、查询订单
        OrderInfoDO orderInfo = orderInfoDAO.getByOrderId(orderId);
        if(null == orderInfo) {
            return null;
        }

        //2、查询订单条目
        List<OrderItemDO> orderItems = orderItemDAO.listByOrderId(orderId);

        //3、查询订单费用明细
        List<OrderAmountDetailDO> orderAmountDetails = orderAmountDetailDAO.listByOrderId(orderId);

        //4、查询订单配送信息
        OrderDeliveryDetailDO orderAmountDetail = orderDeliveryDetailDAO.getByOrderId(orderId);

        //5、查询订单支付明细
        List<OrderPaymentDetailDO> orderPaymentDetails = orderPaymentDetailDAO.listByOrderId(orderId);

        //6、查询订单费用类型
        List<OrderAmountDO> orderAmounts = orderAmountDAO.listByOrderId(orderId);

        //7、查询订单操作日志
        List<OrderOperateLogDO> orderOperateLogs = orderOperateLogDAO.listByOrderId(orderId);

        //8、查询订单快照
        List<OrderSnapshotDO> orderSnapshots = orderSnapshotDAO.listByOrderId(orderId);

        //9、查询缺品退款信息
        List<OrderLackItemDTO> lackItems = null;
        if(isLack(orderInfo)) {
            lackItems = afterSaleQueryService.getOrderLackItemInfo(orderId);
        }

        //10、构造返参
        return new OrderDetailBuilder()
                .orderInfo(orderInfo)
                .orderItems(orderItems)
                .orderAmountDetails(orderAmountDetails)
                .orderDeliveryDetail(orderAmountDetail)
                .orderPaymentDetails(orderPaymentDetails)
                .orderAmounts(orderAmounts)
                .orderOperateLogs(orderOperateLogs)
                .orderSnapshots(orderSnapshots)
                .lackItems(lackItems)
                .build();
    }

    /**
     * 是否缺品
     * @param order
     * @return
     */
    private boolean isLack(OrderInfoDO order) {
        OrderExtJsonDTO orderExtJson = ExtJsonUtil.parseExtJson(order.getExtJson(),OrderExtJsonDTO.class);
        if(null != orderExtJson) {
            return orderExtJson.getLackFlag();
        }
        return false;
    }

}
