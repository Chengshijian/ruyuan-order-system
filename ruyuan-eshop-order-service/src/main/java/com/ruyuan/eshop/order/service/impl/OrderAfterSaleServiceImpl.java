package com.ruyuan.eshop.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.ruyuan.eshop.common.constants.RedisLockKeyConstants;
import com.ruyuan.eshop.common.constants.RocketMqConstant;
import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.enums.*;
import com.ruyuan.eshop.common.exception.BaseBizException;
import com.ruyuan.eshop.common.message.ActualRefundMessage;
import com.ruyuan.eshop.common.redis.RedisLock;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.common.utils.RandomUtil;
import com.ruyuan.eshop.customer.api.CustomerApi;
import com.ruyuan.eshop.customer.domain.request.CustomerReviewReturnGoodsRequest;
import com.ruyuan.eshop.fulfill.api.FulfillApi;
import com.ruyuan.eshop.fulfill.domain.request.CancelFulfillRequest;
import com.ruyuan.eshop.market.domain.request.CancelOrderReleaseUserCouponRequest;
import com.ruyuan.eshop.order.dao.*;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderItemDTO;
import com.ruyuan.eshop.order.domain.dto.CancelOrderRefundAmountDTO;
import com.ruyuan.eshop.order.domain.dto.OrderInfoDTO;
import com.ruyuan.eshop.order.domain.dto.OrderItemDTO;
import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.domain.request.*;
import com.ruyuan.eshop.order.enums.*;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.manager.OrderNoManager;
import com.ruyuan.eshop.order.mq.producer.DefaultProducer;
import com.ruyuan.eshop.order.service.OrderAfterSaleService;
import com.ruyuan.eshop.pay.api.PayApi;
import com.ruyuan.eshop.pay.domain.request.PayRefundRequest;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Service
@Slf4j
public class OrderAfterSaleServiceImpl implements OrderAfterSaleService {

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private RedisLock redisLock;

    @DubboReference(version = "1.0.0")
    private PayApi payApi;

    @DubboReference(version = "1.0.0")
    private FulfillApi fulfillApi;

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderOperateLogDAO orderOperateLogDAO;

    @Autowired
    private OrderNoManager orderNoManager;

    @Autowired
    private AfterSaleInfoDAO afterSaleInfoDAO;

    @DubboReference(version = "1.0.0")
    private CustomerApi customerApi;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private AfterSaleLogDAO afterSaleLogDAO;

    @Autowired
    private AfterSaleRefundDAO afterSaleRefundDAO;

    @Autowired
    private DefaultProducer defaultProducer;

    @Autowired
    private OrderAfterSaleService orderAfterSaleService;

    @Autowired
    private AfterSaleItemDAO afterSaleItemDAO;

    @Autowired
    private OrderAmountDAO orderAmountDAO;

    @Autowired
    private AfterSaleOperateLogFactory afterSaleOperateLogFactory;

    /**
     * 取消订单/超时未支付取消
     */
    @Override
    public JsonResult<Boolean> cancelOrder(CancelOrderRequest cancelOrderRequest) {
        //  入参检查
        checkCancelOrderRequestParam(cancelOrderRequest);
        //  分布式锁
        String orderId = cancelOrderRequest.getOrderId();
        String key = RedisLockKeyConstants.CANCEL_KEY + orderId;
        try {
            boolean lock = redisLock.lock(key);
            if (!lock) {
                throw new OrderBizException(OrderErrorCodeEnum.CANCEL_ORDER_REPEAT);
            }
            //  执行取消订单
            executeCancelOrder(cancelOrderRequest, orderId);
            return JsonResult.buildSuccess(true);
        } catch (Exception e) {
            throw new OrderBizException(e.getMessage());
        } finally {
            redisLock.unlock(key);
        }
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void executeCancelOrder(CancelOrderRequest cancelOrderRequest, String orderId) {
        // 1、组装数据
        CancelOrderAssembleRequest cancelOrderAssembleRequest = buildAssembleRequest(orderId, cancelOrderRequest);
        //  幂等校验：防止多个线程同时操作取消同一笔订单
        if (OrderStatusEnum.CANCELED.getCode().equals(cancelOrderAssembleRequest.getOrderInfoDTO().getOrderStatus())) {
            return;
        }

        // 2、检验订单支付状态
        checkOrderPayStatus(cancelOrderAssembleRequest);

        // 3、更新订单状态和记录订单操作日志
        updateOrderStatusAndSaveOperationLog(cancelOrderAssembleRequest);
        // 超时未支付的订单不用继续再往下执行取消履约和释放资产
        if (OrderStatusEnum.PAID.getCode() > cancelOrderAssembleRequest.getOrderInfoDTO().getOrderStatus()) {
            return;
        }
        // 4、履约取消
        cancelFulfill(cancelOrderAssembleRequest);

        // 5、释放资产
        defaultProducer.sendMessage(RocketMqConstant.RELEASE_ASSETS_TOPIC,
                JSONObject.toJSONString(cancelOrderAssembleRequest), "释放资产");
    }

    /**
     * 组装 取消订单 数据
     */
    private CancelOrderAssembleRequest buildAssembleRequest(String orderId, CancelOrderRequest cancelOrderRequest) {
        Integer cancelType = cancelOrderRequest.getCancelType();

        OrderInfoDTO orderInfoDTO = findOrderInfo(orderId, cancelType);
        List<OrderItemDTO> orderItemDTOList = findOrderItemInfo(orderId);

        CancelOrderAssembleRequest cancelOrderAssembleRequest = cancelOrderRequest.clone(CancelOrderAssembleRequest.class);

        cancelOrderAssembleRequest.setOrderId(orderId);
        cancelOrderAssembleRequest.setOrderInfoDTO(orderInfoDTO);
        cancelOrderAssembleRequest.setOrderItemDTOList(orderItemDTOList);
        cancelOrderAssembleRequest.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());

        return cancelOrderAssembleRequest;
    }


    /**
     * 获取订单条目
     */
    private List<OrderItemDTO> findOrderItemInfo(String orderId) {
        List<OrderItemDO> orderItemDOList = orderItemDAO.listByOrderId(orderId);
        if (orderItemDOList == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_ITEM_IS_NULL);
        }

        List<OrderItemDTO> orderItemDTOList = Lists.newArrayList();
        for (OrderItemDO orderItemDO : orderItemDOList) {
            OrderItemDTO orderItemDTO = orderItemDO.clone(OrderItemDTO.class);
            orderItemDTOList.add(orderItemDTO);
        }
        return orderItemDTOList;
    }

    /**
     * 获取订单信息
     */
    private OrderInfoDTO findOrderInfo(String orderId, Integer cancelType) {
        OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);
        if (orderInfoDO == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_NOT_FOUND);
        }

        OrderInfoDTO orderInfoDTO = orderInfoDO.clone(OrderInfoDTO.class);
        orderInfoDTO.setCancelType(String.valueOf(cancelType));
        return orderInfoDTO;
    }

    /**
     * 新增售后信息
     */
    private void updateOrderStatusAndSaveOperationLog(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        //  更新订单表
        OrderInfoDO orderInfoDO = cancelOrderAssembleRequest.getOrderInfoDTO().clone(OrderInfoDO.class);
        orderInfoDO.setCancelType(cancelOrderAssembleRequest.getCancelType().toString());
        orderInfoDO.setOrderStatus(OrderStatusEnum.CANCELED.getCode());
        orderInfoDO.setCancelTime(new Date());
        orderInfoDAO.updateOrderInfo(orderInfoDO);
        log.info("更新订单信息OrderInfo状态: orderId:{},status:{}", orderInfoDO.getOrderId(), orderInfoDO.getOrderStatus());

        //  新增订单操作操作日志表
        Integer cancelType = Integer.valueOf(orderInfoDO.getCancelType());
        String orderId = orderInfoDO.getOrderId();
        OrderOperateLogDO orderOperateLogDO = new OrderOperateLogDO();
        orderOperateLogDO.setOrderId(orderId);
        orderOperateLogDO.setPreStatus(cancelOrderAssembleRequest.getOrderInfoDTO().getOrderStatus());
        orderOperateLogDO.setCurrentStatus(OrderStatusEnum.CANCELED.getCode());
        orderOperateLogDO.setOperateType(OrderOperateTypeEnum.AUTO_CANCEL_ORDER.getCode());

        if (OrderCancelTypeEnum.USER_CANCELED.getCode().equals(cancelType)) {
            orderOperateLogDO.setOperateType(OrderOperateTypeEnum.MANUAL_CANCEL_ORDER.getCode());
            orderOperateLogDO.setRemark(OrderOperateTypeEnum.MANUAL_CANCEL_ORDER.getMsg()
                    + orderOperateLogDO.getPreStatus() + "-" + orderOperateLogDO.getCurrentStatus());
        }
        if (OrderCancelTypeEnum.TIMEOUT_CANCELED.getCode().equals(cancelType)) {
            orderOperateLogDO.setOperateType(OrderOperateTypeEnum.AUTO_CANCEL_ORDER.getCode());
            orderOperateLogDO.setRemark(OrderOperateTypeEnum.AUTO_CANCEL_ORDER.getMsg()
                    + orderOperateLogDO.getPreStatus() + "-" + orderOperateLogDO.getCurrentStatus());
        }


        orderOperateLogDAO.save(orderOperateLogDO);
        log.info("新增订单操作日志OrderOperateLog状态,orderId:{}, PreStatus:{},CurrentStatus:{}", orderInfoDO.getOrderId(),
                orderOperateLogDO.getPreStatus(), orderOperateLogDO.getCurrentStatus());
    }

    /**
     * 更新支付退款回调售后信息
     */
    public void updatePaymentRefundCallbackAfterSale(RefundCallbackRequest payRefundCallbackRequest,
                                                     Integer afterSaleStatus, Integer refundStatus, String refundStatusMsg) {
        Long afterSaleId = Long.valueOf(payRefundCallbackRequest.getAfterSaleId());
        //  更新 订单售后表
        afterSaleInfoDAO.updateStatus(afterSaleId, AfterSaleStatusEnum.REFUNDING.getCode(), afterSaleStatus);

        //  新增 售后单变更表
        AfterSaleInfoDO afterSaleInfoDO = afterSaleInfoDAO.getOneByAfterSaleId(afterSaleId);
        AfterSaleLogDO afterSaleLogDO = afterSaleOperateLogFactory.get(afterSaleInfoDO,
                AfterSaleStatusChangeEnum.getBy(AfterSaleStatusEnum.REFUNDING.getCode(), afterSaleStatus));

        afterSaleLogDAO.save(afterSaleLogDO);

        //  更新 售后退款单表
        AfterSaleRefundDO afterSaleRefundDO = new AfterSaleRefundDO();
        afterSaleRefundDO.setAfterSaleId(payRefundCallbackRequest.getAfterSaleId());
        afterSaleRefundDO.setRefundStatus(refundStatus);
        afterSaleRefundDO.setRefundPayTime(payRefundCallbackRequest.getRefundTime());
        afterSaleRefundDO.setRemark(refundStatusMsg);

        afterSaleRefundDAO.updateAfterSaleRefundStatus(afterSaleRefundDO);
    }

    private void insertReturnGoodsAfterSaleLogTable(String afterSaleId, Integer preAfterSaleStatus, Integer currentAfterSaleStatus) {

        AfterSaleLogDO afterSaleLogDO = new AfterSaleLogDO();
        afterSaleLogDO.setAfterSaleId(afterSaleId);
        afterSaleLogDO.setPreStatus(preAfterSaleStatus);
        afterSaleLogDO.setCurrentStatus(currentAfterSaleStatus);
        //  售后退货的业务值
        afterSaleLogDO.setRemark(ReturnGoodsTypeEnum.AFTER_SALE_RETURN_GOODS.getMsg());

        afterSaleLogDAO.save(afterSaleLogDO);
        log.info("新增售后单变更信息, 售后单号:{},状态:PreStatus{},CurrentStatus:{}", afterSaleLogDO.getAfterSaleId(),
                afterSaleLogDO.getPreStatus(), afterSaleLogDO.getCurrentStatus());
    }

    private void insertCancelOrderAfterSaleLogTable(String afterSaleId, OrderInfoDTO orderInfoDTO,
                                                    Integer preAfterSaleStatus, Integer currentAfterSaleStatus) {

        AfterSaleLogDO afterSaleLogDO = new AfterSaleLogDO();
        afterSaleLogDO.setAfterSaleId(afterSaleId);
        afterSaleLogDO.setPreStatus(preAfterSaleStatus);
        afterSaleLogDO.setCurrentStatus(currentAfterSaleStatus);

        //  取消订单的业务值
        Integer cancelType = Integer.valueOf(orderInfoDTO.getCancelType());
        if (OrderCancelTypeEnum.USER_CANCELED.getCode().equals(cancelType)) {
            afterSaleLogDO.setRemark(OrderCancelTypeEnum.USER_CANCELED.getMsg());
        }
        if (OrderCancelTypeEnum.TIMEOUT_CANCELED.getCode().equals(cancelType)) {
            afterSaleLogDO.setRemark(OrderCancelTypeEnum.TIMEOUT_CANCELED.getMsg());
        }

        afterSaleLogDAO.save(afterSaleLogDO);
        log.info("新增售后单变更信息, 订单号:{},售后单号:{},状态:PreStatus{},CurrentStatus:{}", orderInfoDTO.getOrderId(),
                afterSaleId, afterSaleLogDO.getPreStatus(), afterSaleLogDO.getCurrentStatus());
    }

    /**
     * 更新售后单状态
     */
    public void updateAfterSaleStatus(AfterSaleInfoDO afterSaleInfoDO, Integer fromStatus, Integer toStatus) {
        //  更新 订单售后表
        afterSaleInfoDAO.updateStatus(afterSaleInfoDO.getAfterSaleId(), fromStatus, toStatus);

        //  新增 售后单变更表
        AfterSaleLogDO afterSaleLogDO = afterSaleOperateLogFactory.get(afterSaleInfoDO, AfterSaleStatusChangeEnum.getBy(fromStatus, toStatus));
        log.info("保存售后变更记录,售后单号:{},fromStatus:{}, toStatus:{}", afterSaleInfoDO.getAfterSaleId(), fromStatus, toStatus);

        afterSaleLogDAO.save(afterSaleLogDO);
    }

    private String insertCancelOrderAfterSaleInfoTable(OrderInfoDO orderInfoDO, Integer afterSaleType,
                                                       Integer cancelOrderAfterSaleStatus, AfterSaleInfoDO afterSaleInfoDO) {
        //  生成售后订单号
        String afterSaleId = orderNoManager.genOrderId(OrderNoTypeEnum.AFTER_SALE.getCode(), orderInfoDO.getUserId());
        afterSaleInfoDO.setAfterSaleId(Long.valueOf(afterSaleId));
        afterSaleInfoDO.setBusinessIdentifier(BusinessIdentifierEnum.SELF_MALL.getCode());
        afterSaleInfoDO.setOrderId(orderInfoDO.getOrderId());
        afterSaleInfoDO.setOrderSourceChannel(BusinessIdentifierEnum.SELF_MALL.getCode());
        afterSaleInfoDO.setUserId(orderInfoDO.getUserId());
        afterSaleInfoDO.setOrderType(OrderTypeEnum.NORMAL.getCode());
        afterSaleInfoDO.setApplyTime(new Date());
        afterSaleInfoDO.setAfterSaleStatus(cancelOrderAfterSaleStatus);

        afterSaleInfoDO.setRealRefundAmount(afterSaleInfoDO.getRealRefundAmount());
        afterSaleInfoDO.setApplyRefundAmount(afterSaleInfoDO.getApplyRefundAmount());
        //  取消订单 整笔退款
        afterSaleInfoDO.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());

        Integer cancelType = Integer.valueOf(orderInfoDO.getCancelType());
        if (OrderCancelTypeEnum.TIMEOUT_CANCELED.getCode().equals(cancelType)) {
            afterSaleInfoDO.setAfterSaleTypeDetail(AfterSaleTypeDetailEnum.TIMEOUT_NO_PAY.getCode());
            afterSaleInfoDO.setRemark("超时未支付自动取消");
        }
        if (OrderCancelTypeEnum.USER_CANCELED.getCode().equals(cancelType)) {
            afterSaleInfoDO.setAfterSaleTypeDetail(AfterSaleTypeDetailEnum.USER_CANCEL.getCode());
            afterSaleInfoDO.setRemark("用户手动取消");
        }
        afterSaleInfoDO.setApplyReasonCode(AfterSaleReasonEnum.CANCEL.getCode());
        afterSaleInfoDO.setApplyReason(AfterSaleReasonEnum.CANCEL.getMsg());
        afterSaleInfoDO.setApplySource(AfterSaleApplySourceEnum.SYSTEM.getCode());
        afterSaleInfoDO.setReviewTime(new Date());

        afterSaleInfoDAO.save(afterSaleInfoDO);

        log.info("新增订单售后记录,订单号:{},售后单号:{},订单售后状态:{}", afterSaleInfoDO.getOrderId(),
                afterSaleInfoDO.getAfterSaleId(), afterSaleInfoDO.getAfterSaleStatus());
        return afterSaleId;
    }

    /**
     * 售后退货流程 插入订单销售表
     */
    private String insertReturnGoodsAfterSaleInfoTable(OrderInfoDO orderInfoDO, Integer afterSaleType,
                                                       Integer cancelOrderAfterSaleStatus, AfterSaleInfoDO afterSaleInfoDO) {
        //  生成售后订单号
        String afterSaleId = orderNoManager.genOrderId(OrderNoTypeEnum.AFTER_SALE.getCode(), orderInfoDO.getUserId());
        afterSaleInfoDO.setAfterSaleId(Long.valueOf(afterSaleId));
        afterSaleInfoDO.setBusinessIdentifier(BusinessIdentifierEnum.SELF_MALL.getCode());
        afterSaleInfoDO.setOrderId(orderInfoDO.getOrderId());
        afterSaleInfoDO.setOrderSourceChannel(BusinessIdentifierEnum.SELF_MALL.getCode());
        afterSaleInfoDO.setUserId(orderInfoDO.getUserId());
        afterSaleInfoDO.setOrderType(OrderTypeEnum.NORMAL.getCode());
        afterSaleInfoDO.setApplyTime(new Date());
        afterSaleInfoDO.setAfterSaleStatus(cancelOrderAfterSaleStatus);
        //  用户售后退货的业务值
        afterSaleInfoDO.setApplySource(AfterSaleApplySourceEnum.USER_RETURN_GOODS.getCode());
        afterSaleInfoDO.setRemark(ReturnGoodsTypeEnum.AFTER_SALE_RETURN_GOODS.getMsg());
        afterSaleInfoDO.setApplyReasonCode(AfterSaleReasonEnum.USER.getCode());
        afterSaleInfoDO.setApplyReason(AfterSaleReasonEnum.USER.getMsg());
        afterSaleInfoDO.setAfterSaleTypeDetail(AfterSaleTypeDetailEnum.PART_REFUND.getCode());

        //  退货流程 只退订单的一笔条目
        if (AfterSaleTypeEnum.RETURN_GOODS.getCode().equals(afterSaleType)) {
            afterSaleInfoDO.setAfterSaleType(AfterSaleTypeEnum.RETURN_GOODS.getCode());
        }
        //  退货流程 退订单的全部条目 后续按照整笔退款逻辑处理
        if (AfterSaleTypeEnum.RETURN_MONEY.getCode().equals(afterSaleType)) {
            afterSaleInfoDO.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());
        }

        afterSaleInfoDAO.save(afterSaleInfoDO);

        log.info("新增订单售后记录,订单号:{},售后单号:{},订单售后状态:{}", orderInfoDO.getOrderId(), afterSaleId,
                afterSaleInfoDO.getAfterSaleStatus());
        return afterSaleId;
    }

    /**
     * 调用履约拦截订单
     */
    private void cancelFulfill(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        OrderInfoDTO orderInfoDTO = cancelOrderAssembleRequest.getOrderInfoDTO();
        CancelFulfillRequest cancelFulfillRequest = orderInfoDTO.clone(CancelFulfillRequest.class);
        JsonResult<Boolean> jsonResult = fulfillApi.cancelFulfill(cancelFulfillRequest);
        if (!jsonResult.getSuccess()) {
            throw new OrderBizException(OrderErrorCodeEnum.CANCEL_ORDER_FULFILL_ERROR);
        }
    }

    /**
     * 入参检查
     *
     * @param cancelOrderRequest 取消订单入参
     */
    private void checkCancelOrderRequestParam(CancelOrderRequest cancelOrderRequest) {
        ParamCheckUtil.checkObjectNonNull(cancelOrderRequest);

        //  订单状态
        Integer orderStatus = cancelOrderRequest.getOrderStatus();
        ParamCheckUtil.checkObjectNonNull(orderStatus, OrderErrorCodeEnum.ORDER_STATUS_IS_NULL);

        if (orderStatus.equals(OrderStatusEnum.CANCELED.getCode())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_STATUS_CANCELED);
        }

        if (orderStatus >= OrderStatusEnum.OUT_STOCK.getCode()) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_STATUS_CHANGED);
        }

        //  订单ID
        String orderId = cancelOrderRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId, OrderErrorCodeEnum.CANCEL_ORDER_ID_IS_NULL);

        //  业务线标识
        Integer businessIdentifier = cancelOrderRequest.getBusinessIdentifier();
        ParamCheckUtil.checkObjectNonNull(businessIdentifier, OrderErrorCodeEnum.BUSINESS_IDENTIFIER_IS_NULL);

        //  订单取消类型
        Integer cancelType = cancelOrderRequest.getCancelType();
        ParamCheckUtil.checkObjectNonNull(cancelType, OrderErrorCodeEnum.CANCEL_TYPE_IS_NULL);

        //  用户ID
        String userId = cancelOrderRequest.getUserId();
        ParamCheckUtil.checkStringNonEmpty(userId, OrderErrorCodeEnum.USER_ID_IS_NULL);

        //  订单类型
        Integer orderType = cancelOrderRequest.getOrderType();
        ParamCheckUtil.checkObjectNonNull(orderType, OrderErrorCodeEnum.ORDER_TYPE_IS_NULL);

    }

    public CancelOrderRefundAmountDTO calculatingCancelOrderRefundAmount(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        OrderInfoDTO orderInfoDTO = cancelOrderAssembleRequest.getOrderInfoDTO();
        CancelOrderRefundAmountDTO cancelOrderRefundAmountDTO = new CancelOrderRefundAmountDTO();

        //  退整笔订单的实际支付金额
        cancelOrderRefundAmountDTO.setOrderId(orderInfoDTO.getOrderId());
        cancelOrderRefundAmountDTO.setTotalAmount(orderInfoDTO.getTotalAmount());
        cancelOrderRefundAmountDTO.setReturnGoodAmount(orderInfoDTO.getPayAmount());

        return cancelOrderRefundAmountDTO;
    }

    /**
     * 缺品退款
     * todo 调用
     *
     * @return
     */
    private JsonResult<BigDecimal> lackRefund(String orderId, Long lackAfterSaleId) {
        AfterSaleInfoDO lackAfterSaleInfo = afterSaleInfoDAO.getOneByAfterSaleId(lackAfterSaleId);
        return JsonResult.buildSuccess(new BigDecimal(lackAfterSaleInfo.getRealRefundAmount()));
    }


    /**
     * 验证订单是否状态是未支付，但是实际已经支付
     */
    public void checkOrderPayStatus(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        OrderInfoDTO orderInfoDTO = cancelOrderAssembleRequest.getOrderInfoDTO();
        if (!OrderStatusEnum.CREATED.getCode().equals(cancelOrderAssembleRequest.getOrderInfoDTO().getOrderStatus())) {
            return;
        }
        // 查询出来的订单状态是未支付，去支付中心查交易流水再次确认是否已支付，如果已支付，则不允许取消，需要重新发起。
        String orderId = orderInfoDTO.getOrderId();
        Integer businessIdentifier = orderInfoDTO.getBusinessIdentifier();
        Boolean realTimeOrderResult = payApi.getTradeNoByRealTime(orderId, businessIdentifier);
        if (realTimeOrderResult) {
            //  本地测试时注释掉
            // throw new OrderBizException(OrderErrorCodeEnum.ORDER_STATUS_CHANGED);
        }

    }

    @Override
    public JsonResult<Boolean> processCancelOrder(CancelOrderAssembleRequest cancelOrderAssembleRequest) {
        String orderId = cancelOrderAssembleRequest.getOrderId();
        //  分布式锁
        String key = RedisLockKeyConstants.REFUND_KEY + orderId;
        try {
            boolean lock = redisLock.lock(key);
            if (!lock) {
                throw new OrderBizException(OrderErrorCodeEnum.PROCESS_REFUND_REPEAT);
            }
            //  执行退款前的准备工作
            //  1、计算 取消订单 退款金额
            CancelOrderRefundAmountDTO cancelOrderRefundAmountDTO = calculatingCancelOrderRefundAmount(cancelOrderAssembleRequest);
            cancelOrderAssembleRequest.setCancelOrderRefundAmountDTO(cancelOrderRefundAmountDTO);

            //  2、取消订单操作 记录售后信息
            insertCancelOrderAfterSale(cancelOrderAssembleRequest, AfterSaleStatusEnum.REVIEW_PASS.getCode());

            //  3、发送退款MQ
            ActualRefundMessage actualRefundMessage = new ActualRefundMessage();
            actualRefundMessage.setAfterSaleRefundId(cancelOrderAssembleRequest.getAfterSaleRefundId());
            actualRefundMessage.setOrderId(cancelOrderAssembleRequest.getOrderId());
            actualRefundMessage.setLastReturnGoods(cancelOrderAssembleRequest.isLastReturnGoods());
            actualRefundMessage.setAfterSaleId(Long.valueOf(cancelOrderAssembleRequest.getAfterSaleId()));

            defaultProducer.sendMessage(RocketMqConstant.ACTUAL_REFUND_TOPIC, JSONObject.toJSONString(actualRefundMessage), "实际退款");

            return JsonResult.buildSuccess(true);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new OrderBizException(OrderErrorCodeEnum.PROCESS_REFUND_FAILED);
        } finally {
            redisLock.unlock(key);
        }
    }


    @Override
    public JsonResult<Boolean> sendRefundMobileMessage(String orderId) {
        log.info("发退款通知短信,订单号:{}", orderId);
        return JsonResult.buildSuccess();
    }

    @Override
    public JsonResult<Boolean> sendRefundAppMessage(String orderId) {
        log.info("发退款通知APP信息,订单号:{}", orderId);
        return JsonResult.buildSuccess();
    }

    @Override
    public JsonResult<Boolean> refundMoney(ActualRefundMessage actualRefundMessage) {
        Long afterSaleId = actualRefundMessage.getAfterSaleId();
        String key = RedisLockKeyConstants.REFUND_KEY + afterSaleId;
        try {
            boolean lock = redisLock.lock(key);
            if (!lock) {
                throw new OrderBizException(OrderErrorCodeEnum.REFUND_MONEY_REPEAT);
            }
            AfterSaleInfoDO afterSaleInfoDO = afterSaleInfoDAO.getOneByAfterSaleId(actualRefundMessage.getAfterSaleId());
            Long afterSaleRefundId = actualRefundMessage.getAfterSaleRefundId();
            AfterSaleRefundDO afterSaleRefundDO = afterSaleRefundDAO.getById(afterSaleRefundId);

            //  1、封装调用支付退款接口的数据
            PayRefundRequest payRefundRequest = buildPayRefundRequest(actualRefundMessage, afterSaleRefundDO);

            //  2、执行退款
            if (!payApi.executeRefund(payRefundRequest)) {
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_REFUND_AMOUNT_FAILED);
            }

            //  3、售后记录状态
            //  执行退款更新售后信息
            updateAfterSaleStatus(afterSaleInfoDO, AfterSaleStatusEnum.REVIEW_PASS.getCode(), AfterSaleStatusEnum.REFUNDING.getCode());

            //  4、退货的最后一笔退优惠券
            if (actualRefundMessage.isLastReturnGoods()) {
                //  释放优惠券权益
                String orderId = actualRefundMessage.getOrderId();
                OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);
                CancelOrderReleaseUserCouponRequest cancelOrderReleaseUserCouponRequest = orderInfoDO.clone(CancelOrderReleaseUserCouponRequest.class);

                defaultProducer.sendMessage(RocketMqConstant.CANCEL_RELEASE_PROPERTY_TOPIC,
                        JSONObject.toJSONString(cancelOrderReleaseUserCouponRequest), "释放优惠券权益");
            }
            return JsonResult.buildSuccess(true);

        } catch (OrderBizException e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());

        } finally {
            redisLock.unlock(key);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> processApplyAfterSale(ReturnGoodsOrderRequest returnGoodsOrderRequest) {
        //  参数校验
        checkAfterSaleRequestParam(returnGoodsOrderRequest);
        //  分布式锁
        String orderId = returnGoodsOrderRequest.getOrderId();
        String key = RedisLockKeyConstants.REFUND_KEY + orderId;
        boolean lock = redisLock.lock(key);
        if (!lock) {
            throw new OrderBizException(OrderErrorCodeEnum.PROCESS_AFTER_SALE_RETURN_GOODS);
        }
        try {
            //  1、售后单状态验证
            //  用order id和sku code查到售后id
            String skuCode = returnGoodsOrderRequest.getSkuCode();
            List<AfterSaleItemDO> orderIdAndSkuCodeList = afterSaleItemDAO.getOrderIdAndSkuCode(orderId, skuCode);
            if (!orderIdAndSkuCodeList.isEmpty()) {
                Long afterSaleId = orderIdAndSkuCodeList.get(0).getAfterSaleId();
                //  用售后id查售后支付表
                AfterSaleRefundDO afterSaleRefundDO = afterSaleRefundDAO.findOrderAfterSaleStatus(String.valueOf(afterSaleId));
                //  幂等校验：售后支付表里存在当前这笔未退款的记录 不能重复发起售后
                if (orderId.equals(afterSaleRefundDO.getOrderId())
                        && RefundStatusEnum.UN_REFUND.getCode().equals(afterSaleRefundDO.getRefundStatus())) {
                    throw new OrderBizException(OrderErrorCodeEnum.PROCESS_APPLY_AFTER_SALE_CANNOT_REPEAT);
                }
                //  业务校验：已完成支付退款的订单不能再次重复发起手动售后
                AfterSaleInfoDO afterSaleInfoDO = afterSaleInfoDAO.getOneByAfterSaleId(afterSaleId);
                if (afterSaleInfoDO.getAfterSaleStatus() > AfterSaleStatusEnum.REFUNDING.getCode()) {
                    throw new OrderBizException(OrderErrorCodeEnum.PROCESS_APPLY_AFTER_SALE_CANNOT_REPEAT);
                }
            }

            // 2、封装数据
            ReturnGoodsAssembleRequest returnGoodsAssembleRequest = buildReturnGoodsData(returnGoodsOrderRequest);

            // 3、计算退货金额
            returnGoodsAssembleRequest = calculateReturnGoodsAmount(returnGoodsAssembleRequest);

            // 4、售后数据落库
            insertReturnGoodsAfterSale(returnGoodsAssembleRequest, AfterSaleStatusEnum.COMMITED.getCode());

            // 5、发起客服审核
            CustomerReviewReturnGoodsRequest customerReviewReturnGoodsRequest
                    = returnGoodsAssembleRequest.clone(new CustomerReviewReturnGoodsRequest());
            customerApi.customerAudit(customerReviewReturnGoodsRequest);

        } catch (BaseBizException e) {
            log.error("system error", e);
            return JsonResult.buildError(e.getMessage());
        } finally {
            redisLock.unlock(key);
        }
        return JsonResult.buildSuccess(true);
    }


    @Override
    public JsonResult<Boolean> receiveCustomerAuditResult(CustomerReviewReturnGoodsRequest customerReviewReturnGoodsRequest) {
        //  组装售后数据
        CustomerAuditAssembleRequest customerAuditAssembleRequest = new CustomerAuditAssembleRequest();
        Long afterSaleId = customerReviewReturnGoodsRequest.getAfterSaleId();
        customerAuditAssembleRequest.setAfterSaleId(afterSaleId);
        customerAuditAssembleRequest.setAfterSaleRefundId(customerReviewReturnGoodsRequest.getAfterSaleRefundId());

        Integer auditResult = customerReviewReturnGoodsRequest.getAuditResult();
        customerAuditAssembleRequest.setReviewTime(new Date());
        customerAuditAssembleRequest.setReviewSource(CustomerAuditSourceEnum.SELF_MALL.getCode());
        customerAuditAssembleRequest.setReviewReasonCode(auditResult);
        customerAuditAssembleRequest.setAuditResultDesc(customerReviewReturnGoodsRequest.getAuditResultDesc());

        AfterSaleInfoDO afterSaleInfoDO = afterSaleInfoDAO.getOneByAfterSaleId(customerReviewReturnGoodsRequest.getAfterSaleId());

        //  幂等校验：防止客服重复审核订单
        if (afterSaleInfoDO.getAfterSaleStatus() > AfterSaleStatusEnum.COMMITED.getCode()) {
            throw new OrderBizException(OrderErrorCodeEnum.CUSTOMER_AUDIT_CANNOT_REPEAT);
        }

        if (CustomerAuditResult.ACCEPT.getCode().equals(auditResult)) {
            //  客服审核通过
            //  参数验证
            String orderId = customerReviewReturnGoodsRequest.getOrderId();
            AfterSaleItemDO afterSaleItemDO = afterSaleItemDAO.getOrderIdAndAfterSaleId(orderId, afterSaleId);
            if (afterSaleItemDO == null) {
                throw new OrderBizException(OrderErrorCodeEnum.AFTER_SALE_ITEM_CANNOT_NULL);
            }

            //  更新售后信息
            customerAuditAssembleRequest.setReviewReason(CustomerAuditResult.ACCEPT.getMsg());
            afterSaleInfoDAO.updateCustomerAuditAfterSaleResult(AfterSaleStatusEnum.REVIEW_PASS.getCode(), customerAuditAssembleRequest);
            AfterSaleLogDO afterSaleLogDO = afterSaleOperateLogFactory.get(afterSaleInfoDO, AfterSaleStatusChangeEnum.AFTER_SALE_CUSTOMER_AUDIT_PASS);
            afterSaleLogDAO.save(afterSaleLogDO);

            //  执行退款
            ActualRefundMessage actualRefundMessage = new ActualRefundMessage();
            actualRefundMessage.setAfterSaleRefundId(customerAuditAssembleRequest.getAfterSaleRefundId());
            actualRefundMessage.setOrderId(customerAuditAssembleRequest.getOrderId());
            actualRefundMessage.setLastReturnGoods(customerAuditAssembleRequest.isLastReturnGoods());
            actualRefundMessage.setAfterSaleId(customerAuditAssembleRequest.getAfterSaleId());

            defaultProducer.sendMessage(RocketMqConstant.ACTUAL_REFUND_TOPIC, JSONObject.toJSONString(actualRefundMessage), "实际退款");

        } else if (CustomerAuditResult.REJECT.getCode().equals(auditResult)) {
            customerAuditAssembleRequest.setReviewReason(CustomerAuditResult.REJECT.getMsg());
            //  审核拒绝更新售后信息
            afterSaleInfoDAO.updateCustomerAuditAfterSaleResult(AfterSaleStatusEnum.REVIEW_REJECTED.getCode(), customerAuditAssembleRequest);
            AfterSaleLogDO afterSaleLogDO = afterSaleOperateLogFactory.get(afterSaleInfoDO, AfterSaleStatusChangeEnum.AFTER_SALE_CUSTOMER_AUDIT_REJECT);
            afterSaleLogDAO.save(afterSaleLogDO);

        }
        return JsonResult.buildSuccess(true);
    }

    private void checkAfterSaleRequestParam(ReturnGoodsOrderRequest returnGoodsOrderRequest) {
        ParamCheckUtil.checkObjectNonNull(returnGoodsOrderRequest);

        String orderId = returnGoodsOrderRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId, OrderErrorCodeEnum.ORDER_ID_IS_NULL);

        String userId = returnGoodsOrderRequest.getUserId();
        ParamCheckUtil.checkStringNonEmpty(userId, OrderErrorCodeEnum.USER_ID_IS_NULL);

        Integer businessIdentifier = returnGoodsOrderRequest.getBusinessIdentifier();
        ParamCheckUtil.checkObjectNonNull(businessIdentifier, OrderErrorCodeEnum.BUSINESS_IDENTIFIER_IS_NULL);

        Integer returnGoodsCode = returnGoodsOrderRequest.getReturnGoodsCode();
        ParamCheckUtil.checkObjectNonNull(returnGoodsCode, OrderErrorCodeEnum.RETURN_GOODS_CODE_IS_NULL);

        String skuCode = returnGoodsOrderRequest.getSkuCode();
        ParamCheckUtil.checkStringNonEmpty(skuCode, OrderErrorCodeEnum.SKU_IS_NULL);

    }

    private void insertReturnGoodsAfterSale(ReturnGoodsAssembleRequest returnGoodsAssembleRequest, Integer afterSaleStatus) {
        OrderInfoDTO orderInfoDTO = returnGoodsAssembleRequest.getOrderInfoDTO();
        OrderInfoDO orderInfoDO = orderInfoDTO.clone(OrderInfoDO.class);
        Integer afterSaleType = returnGoodsAssembleRequest.getAfterSaleType();

        //  售后退货过程中的 申请退款金额 和 实际退款金额 是计算出来的，金额有可能不同
        AfterSaleInfoDO afterSaleInfoDO = new AfterSaleInfoDO();
        Integer applyRefundAmount = returnGoodsAssembleRequest.getApplyRefundAmount();
        afterSaleInfoDO.setApplyRefundAmount(applyRefundAmount);
        Integer returnGoodAmount = returnGoodsAssembleRequest.getReturnGoodAmount();
        afterSaleInfoDO.setRealRefundAmount(returnGoodAmount);

        //  1、新增售后订单表
        Integer cancelOrderAfterSaleStatus = AfterSaleStatusEnum.COMMITED.getCode();
        String afterSaleId = insertReturnGoodsAfterSaleInfoTable(orderInfoDO, afterSaleType, cancelOrderAfterSaleStatus, afterSaleInfoDO);
        returnGoodsAssembleRequest.setAfterSaleId(afterSaleId);

        //  2、新增售后条目表
        insertAfterSaleItemTable(orderInfoDO.getOrderId(), returnGoodsAssembleRequest.getRefundOrderItemDTO(), afterSaleId);

        //  3、新增售后变更表
        insertReturnGoodsAfterSaleLogTable(afterSaleId, AfterSaleStatusEnum.UN_CREATED.getCode(), afterSaleStatus);

        //  4、新增售后支付表
        AfterSaleRefundDO afterSaleRefundDO = insertAfterSaleRefundTable(orderInfoDTO, afterSaleId, afterSaleInfoDO);
        returnGoodsAssembleRequest.setAfterSaleRefundId(afterSaleRefundDO.getId());
    }

    /**
     * 取消订单操作 记录售后信息
     */
    private void insertCancelOrderAfterSale(CancelOrderAssembleRequest cancelOrderAssembleRequest, Integer afterSaleStatus) {
        OrderInfoDTO orderInfoDTO = cancelOrderAssembleRequest.getOrderInfoDTO();
        OrderInfoDO orderInfoDO = orderInfoDTO.clone(OrderInfoDO.class);
        Integer afterSaleType = cancelOrderAssembleRequest.getAfterSaleType();
        Integer cancelOrderAfterSaleStatus = AfterSaleStatusEnum.REVIEW_PASS.getCode();

        //  取消订单过程中的 申请退款金额 和 实际退款金额 都是实付退款金额 金额相同
        AfterSaleInfoDO afterSaleInfoDO = new AfterSaleInfoDO();
        afterSaleInfoDO.setApplyRefundAmount(orderInfoDO.getPayAmount());
        afterSaleInfoDO.setRealRefundAmount(orderInfoDO.getPayAmount());

        //  1、新增售后订单表
        String afterSaleId = insertCancelOrderAfterSaleInfoTable(orderInfoDO, afterSaleType, cancelOrderAfterSaleStatus, afterSaleInfoDO);
        cancelOrderAssembleRequest.setAfterSaleId(afterSaleId);

        //  2、新增售后条目表
        String orderId = cancelOrderAssembleRequest.getOrderId();
        List<OrderItemDTO> orderItemDTOList = cancelOrderAssembleRequest.getOrderItemDTOList();
        insertAfterSaleItemTable(orderId, orderItemDTOList, afterSaleId);

        //  3、新增售后变更表
        insertCancelOrderAfterSaleLogTable(afterSaleId, orderInfoDTO, AfterSaleStatusEnum.UN_CREATED.getCode(), afterSaleStatus);

        //  4、新增售后支付表
        AfterSaleRefundDO afterSaleRefundDO = insertAfterSaleRefundTable(orderInfoDTO, afterSaleId, afterSaleInfoDO);
        cancelOrderAssembleRequest.setAfterSaleRefundId(afterSaleRefundDO.getId());
    }


    private ReturnGoodsAssembleRequest buildReturnGoodsData(ReturnGoodsOrderRequest returnGoodsOrderRequest) {
        ReturnGoodsAssembleRequest returnGoodsAssembleRequest = returnGoodsOrderRequest.clone(ReturnGoodsAssembleRequest.class);
        String orderId = returnGoodsAssembleRequest.getOrderId();

        //  封装 订单信息
        OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);
        OrderInfoDTO orderInfoDTO = orderInfoDO.clone(OrderInfoDTO.class);
        returnGoodsAssembleRequest.setOrderInfoDTO(orderInfoDTO);

        //  封装 订单条目
        List<OrderItemDO> orderItemDOList = orderItemDAO.listByOrderId(orderId);
        List<OrderItemDTO> orderItemDTOList = Lists.newArrayList();
        for (OrderItemDO orderItemDO : orderItemDOList) {
            OrderItemDTO orderItemDTO = orderItemDO.clone(new OrderItemDTO());
            orderItemDTOList.add(orderItemDTO);
        }
        returnGoodsAssembleRequest.setOrderItemDTOList(orderItemDTOList);

        //  封装 订单售后条目
        List<AfterSaleItemDO> afterSaleItemDOList = afterSaleItemDAO.listByOrderId(Long.valueOf(orderId));
        List<AfterSaleOrderItemDTO> afterSaleOrderItemRequestList = Lists.newArrayList();
        for (AfterSaleItemDO afterSaleItemDO : afterSaleItemDOList) {
            AfterSaleOrderItemDTO afterSaleOrderItemDTO = afterSaleItemDO.clone(AfterSaleOrderItemDTO.class);
            afterSaleOrderItemRequestList.add(afterSaleOrderItemDTO);
        }
        returnGoodsAssembleRequest.setAfterSaleOrderItemDTOList(afterSaleOrderItemRequestList);

        return returnGoodsAssembleRequest;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JsonResult<Boolean> receivePaymentRefundCallback(RefundCallbackRequest payRefundCallbackRequest) {
        String afterSaleId = payRefundCallbackRequest.getAfterSaleId();
        String key = RedisLockKeyConstants.REFUND_KEY + afterSaleId;
        try {
            boolean lock = redisLock.lock(key);
            if (!lock) {
                throw new OrderBizException(OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_REPEAT);
            }
            //  1、入参校验
            checkRefundCallbackParam(payRefundCallbackRequest);

            //  2、获取三方支付退款的返回结果
            Integer afterSaleStatus;
            Integer refundStatus;
            String refundStatusMsg;
            if (RefundStatusEnum.REFUND_SUCCESS.getCode().equals(payRefundCallbackRequest.getRefundStatus())) {
                afterSaleStatus = AfterSaleStatusEnum.REFUNDED.getCode();
                refundStatus = RefundStatusEnum.REFUND_SUCCESS.getCode();
                refundStatusMsg = RefundStatusEnum.REFUND_SUCCESS.getMsg();
            } else {
                afterSaleStatus = AfterSaleStatusEnum.FAILED.getCode();
                refundStatus = RefundStatusEnum.REFUND_FAIL.getCode();
                refundStatusMsg = RefundStatusEnum.REFUND_FAIL.getMsg();
            }

            //  3、更新售后记录，支付退款回调更新售后信息
            updatePaymentRefundCallbackAfterSale(payRefundCallbackRequest, afterSaleStatus, refundStatus, refundStatusMsg);

            //  4、发短信
            orderAfterSaleService.sendRefundMobileMessage(afterSaleId);

            //  5、发APP通知
            orderAfterSaleService.sendRefundAppMessage(afterSaleId);

            return JsonResult.buildSuccess();

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new OrderBizException(OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_FAILED);
        } finally {
            redisLock.unlock(key);
        }
    }

    private RefundCallbackAssembleRequest buildRefundCallbackAfterSaleData(RefundCallbackRequest payRefundCallbackRequest) {
        RefundCallbackAssembleRequest refundCallbackAssembleRequest = payRefundCallbackRequest.clone(RefundCallbackAssembleRequest.class);

        String afterSaleId = payRefundCallbackRequest.getAfterSaleId();

        AfterSaleInfoDO afterSaleInfoDO = afterSaleInfoDAO.getOneByOrderId(Long.valueOf(afterSaleId));
        refundCallbackAssembleRequest.setAfterSaleId(afterSaleInfoDO.getAfterSaleId().toString());

        return refundCallbackAssembleRequest;
    }


    private void checkRefundCallbackParam(RefundCallbackRequest payRefundCallbackRequest) {
        ParamCheckUtil.checkObjectNonNull(payRefundCallbackRequest);

        String orderId = payRefundCallbackRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId, OrderErrorCodeEnum.CANCEL_ORDER_ID_IS_NULL);

        String batchNo = payRefundCallbackRequest.getBatchNo();
        ParamCheckUtil.checkStringNonEmpty(batchNo, OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_BATCH_NO_IS_NULL);

        Integer refundStatus = payRefundCallbackRequest.getRefundStatus();
        ParamCheckUtil.checkObjectNonNull(refundStatus, OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_STATUS_NO_IS_NUL);

        Integer refundFee = payRefundCallbackRequest.getRefundFee();
        ParamCheckUtil.checkObjectNonNull(refundFee, OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_FEE_NO_IS_NUL);

        Integer totalFee = payRefundCallbackRequest.getTotalFee();
        ParamCheckUtil.checkObjectNonNull(totalFee, OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_TOTAL_FEE_NO_IS_NUL);

        String sign = payRefundCallbackRequest.getSign();
        ParamCheckUtil.checkStringNonEmpty(sign, OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_SIGN_NO_IS_NUL);

        String tradeNo = payRefundCallbackRequest.getTradeNo();
        ParamCheckUtil.checkStringNonEmpty(tradeNo, OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_TRADE_NO_IS_NUL);

        String afterSaleId = payRefundCallbackRequest.getAfterSaleId();
        ParamCheckUtil.checkStringNonEmpty(afterSaleId, OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_AFTER_SALE_ID_IS_NULL);

        Date refundTime = payRefundCallbackRequest.getRefundTime();
        ParamCheckUtil.checkObjectNonNull(refundTime, OrderErrorCodeEnum.PROCESS_PAY_REFUND_CALLBACK_AFTER_SALE_REFUND_TIME_IS_NULL);

        //  数据库中当前售后单不是未退款状态，表示已经退款成功 or 失败，那么本次就不能再执行支付回调退款
        AfterSaleRefundDO afterSaleByDatabase = afterSaleRefundDAO.findOrderAfterSaleStatus(afterSaleId);
        if (!RefundStatusEnum.UN_REFUND.getCode().equals(afterSaleByDatabase.getRefundStatus())) {
            throw new OrderBizException(OrderErrorCodeEnum.REPEAT_CALLBACK);
        }
    }

    /**
     * 一笔订单只有一个条目：整笔退
     * 一笔订单有多个条目，每次退一条，退完最后一条补退运费和优惠券
     */
    public ReturnGoodsAssembleRequest calculateReturnGoodsAmount(ReturnGoodsAssembleRequest returnGoodsAssembleRequest) {
        String skuCode = returnGoodsAssembleRequest.getSkuCode();
        String orderId = returnGoodsAssembleRequest.getOrderId();
        //  为便于以后扩展，这里封装成list
        List<OrderItemDTO> refundOrderItemDTOList = Lists.newArrayList();
        List<OrderItemDTO> orderItemDTOList = returnGoodsAssembleRequest.getOrderItemDTOList();
        List<AfterSaleOrderItemDTO> afterSaleOrderItemDTOList = returnGoodsAssembleRequest.getAfterSaleOrderItemDTOList();
        //  订单条目数
        int orderItemNum = orderItemDTOList.size();
        //  售后订单条目数
        int afterSaleOrderItemNum = afterSaleOrderItemDTOList.size();
        //  订单条目数，只有一条，退整笔 ，本次售后类型：全部退款
        if (orderItemNum == 1) {
            OrderItemDTO orderItemDTO = orderItemDTOList.get(0);
            returnGoodsAssembleRequest.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());
            return calculateWholeOrderFefundAmount(
                    orderId,
                    orderItemDTO.getPayAmount(),
                    orderItemDTO.getOriginAmount(),
                    returnGoodsAssembleRequest
            );
        }
        //  该笔订单还有其他条目，本次售后类型：退货
        returnGoodsAssembleRequest.setAfterSaleType(AfterSaleTypeEnum.RETURN_GOODS.getCode());
        //  skuCode和orderId查询本次要退的条目
        OrderItemDO orderItemDO = orderItemDAO.getOrderItemBySkuIdAndOrderId(orderId, skuCode);
        //  该笔订单条目数 = 已存的售后订单条目数 + 本次退货的条目 (每次退 1 条)
        if (orderItemNum == afterSaleOrderItemNum + 1) {
            //  当前条目是订单的最后一笔
            returnGoodsAssembleRequest = calculateWholeOrderFefundAmount(
                    orderId,
                    orderItemDO.getPayAmount(),
                    orderItemDO.getOriginAmount(),
                    returnGoodsAssembleRequest
            );
        } else {
            //  只退当前条目
            returnGoodsAssembleRequest.setReturnGoodAmount(orderItemDO.getPayAmount());
            returnGoodsAssembleRequest.setApplyRefundAmount(orderItemDO.getOriginAmount());
            returnGoodsAssembleRequest.setLastReturnGoods(false);
        }
        refundOrderItemDTOList.add(orderItemDO.clone(OrderItemDTO.class));
        returnGoodsAssembleRequest.setRefundOrderItemDTO(refundOrderItemDTOList);

        return returnGoodsAssembleRequest;
    }

    private ReturnGoodsAssembleRequest calculateWholeOrderFefundAmount(String orderId, Integer payAmount,
                                                                       Integer originAmount,
                                                                       ReturnGoodsAssembleRequest returnGoodsAssembleRequest) {
        //  模拟取运费
        OrderAmountDO deliveryAmount = orderAmountDAO.getOne(orderId, AmountTypeEnum.SHIPPING_AMOUNT.getCode());
        Integer freightAmount = (deliveryAmount == null || deliveryAmount.getAmount() == null) ? 0 : deliveryAmount.getAmount();
        //  最终退款金额 = 实际退款金额 + 运费
        Integer returnGoodAmount = payAmount + freightAmount;
        returnGoodsAssembleRequest.setReturnGoodAmount(returnGoodAmount);
        returnGoodsAssembleRequest.setApplyRefundAmount(originAmount);
        returnGoodsAssembleRequest.setAfterSaleType(AfterSaleTypeEnum.RETURN_MONEY.getCode());
        returnGoodsAssembleRequest.setLastReturnGoods(true);

        return returnGoodsAssembleRequest;

    }

    private PayRefundRequest buildPayRefundRequest(ActualRefundMessage actualRefundMessage, AfterSaleRefundDO afterSaleRefundDO) {
        String orderId = actualRefundMessage.getOrderId();
        PayRefundRequest payRefundRequest = new PayRefundRequest();
        payRefundRequest.setOrderId(orderId);
        payRefundRequest.setAfterSaleId(actualRefundMessage.getAfterSaleId());
        payRefundRequest.setRefundAmount(afterSaleRefundDO.getRefundAmount());

        return payRefundRequest;
    }

    private void insertAfterSaleItemTable(String orderId, List<OrderItemDTO> orderItemDTOList, String afterSaleId) {

        for (OrderItemDTO orderItem : orderItemDTOList) {
            AfterSaleItemDO afterSaleItemDO = new AfterSaleItemDO();
            afterSaleItemDO.setAfterSaleId(Long.valueOf(afterSaleId));
            afterSaleItemDO.setOrderId(orderId);
            afterSaleItemDO.setSkuCode(orderItem.getSkuCode());
            afterSaleItemDO.setProductName(orderItem.getProductName());
            afterSaleItemDO.setProductImg(orderItem.getProductImg());
            afterSaleItemDO.setReturnQuantity(orderItem.getSaleQuantity());
            afterSaleItemDO.setOriginAmount(orderItem.getOriginAmount());
            afterSaleItemDO.setApplyRefundAmount(orderItem.getOriginAmount());
            afterSaleItemDO.setRealRefundAmount(orderItem.getPayAmount());

            afterSaleItemDAO.save(afterSaleItemDO);
        }
    }

    private AfterSaleRefundDO insertAfterSaleRefundTable(OrderInfoDTO orderInfoDTO, String afterSaleId, AfterSaleInfoDO afterSaleInfoDO) {
        String orderId = orderInfoDTO.getOrderId();
        OrderPaymentDetailDO paymentDetail = orderPaymentDetailDAO.getPaymentDetailByOrderId(orderId);

        AfterSaleRefundDO afterSaleRefundDO = new AfterSaleRefundDO();
        afterSaleRefundDO.setAfterSaleId(afterSaleId);
        afterSaleRefundDO.setOrderId(orderId);
        afterSaleRefundDO.setAccountType(AccountTypeEnum.THIRD.getCode());
        afterSaleRefundDO.setRefundStatus(RefundStatusEnum.UN_REFUND.getCode());
        afterSaleRefundDO.setRemark(RefundStatusEnum.UN_REFUND.getMsg());
        afterSaleRefundDO.setRefundAmount(afterSaleInfoDO.getRealRefundAmount());
        afterSaleRefundDO.setAfterSaleBatchNo(orderId + RandomUtil.genRandomNumber(10));

        if (paymentDetail != null) {
            afterSaleRefundDO.setOutTradeNo(paymentDetail.getOutTradeNo());
            afterSaleRefundDO.setPayType(paymentDetail.getPayType());
        }
        afterSaleRefundDAO.save(afterSaleRefundDO);
        log.info("新增售后支付信息,订单号:{},售后单号:{},状态:{}", orderId, afterSaleId, afterSaleRefundDO.getRefundStatus());
        return afterSaleRefundDO;
    }


    @Override
    public void revokeAfterSale(RevokeAfterSaleRequest request) {
        //1、查询售后单
        Long afterSaleId = request.getAfterSaleId();
        AfterSaleInfoDO afterSaleInfo = afterSaleInfoDAO.getOneByAfterSaleId(afterSaleId);
        ParamCheckUtil.checkObjectNonNull(afterSaleInfo, OrderErrorCodeEnum.AFTER_SALE_ID_IS_NULL);

        //2、校验售后单是否可以撤销：只有提交申请状态才可以撤销
        if (!AfterSaleStatusEnum.COMMITED.getCode().equals(afterSaleInfo.getAfterSaleStatus())) {
            throw new OrderBizException(OrderErrorCodeEnum.AFTER_SALE_CANNOT_REVOKE);
        }

        //3、加锁,锁整个售后单
        // 只要涉及售后表的更新，就需要加锁，锁整个售后表，否则算钱的时候，
        // 就会由于突然撤销，导致钱多算了
        String lockKey = RedisLockKeyConstants.REFUND_KEY + afterSaleId;
        try {
            boolean isLock = redisLock.lock(lockKey);
            if (!isLock) {
                throw new OrderBizException(OrderErrorCodeEnum.AFTER_SALE_CANNOT_REVOKE);
            }

            //4、更新售后单状态为："已撤销"
            afterSaleInfoDAO.updateStatus(afterSaleId, AfterSaleStatusEnum.COMMITED.getCode(),
                    AfterSaleStatusEnum.REVOKE.getCode());

            //5、增加一条售后单操作日志
            afterSaleLogDAO.save(afterSaleOperateLogFactory.get(afterSaleInfo, AfterSaleStatusChangeEnum.AFTER_SALE_REVOKE));
        } finally {
            //6、释放锁
            redisLock.unlock(lockKey);
        }
    }

}
