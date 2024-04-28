package com.ruyuan.eshop.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.ruyuan.eshop.address.api.AddressApi;
import com.ruyuan.eshop.address.domain.dto.AddressDTO;
import com.ruyuan.eshop.address.domain.query.AddressQuery;
import com.ruyuan.eshop.common.constants.RedisLockKeyConstants;
import com.ruyuan.eshop.common.constants.RocketDelayedLevel;
import com.ruyuan.eshop.common.constants.RocketMqConstant;
import com.ruyuan.eshop.common.core.CloneDirection;
import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.enums.*;
import com.ruyuan.eshop.common.message.PaidOrderSuccessMessage;
import com.ruyuan.eshop.common.message.PayOrderTimeoutDelayMessage;
import com.ruyuan.eshop.common.redis.RedisLock;
import com.ruyuan.eshop.common.utils.JsonUtil;
import com.ruyuan.eshop.common.utils.ObjectUtil;
import com.ruyuan.eshop.common.utils.ParamCheckUtil;
import com.ruyuan.eshop.inventory.api.InventoryApi;
import com.ruyuan.eshop.inventory.domain.request.LockProductStockRequest;
import com.ruyuan.eshop.market.api.MarketApi;
import com.ruyuan.eshop.market.domain.dto.CalculateOrderAmountDTO;
import com.ruyuan.eshop.market.domain.dto.UserCouponDTO;
import com.ruyuan.eshop.market.domain.query.UserCouponQuery;
import com.ruyuan.eshop.market.domain.request.CalculateOrderAmountRequest;
import com.ruyuan.eshop.market.domain.request.LockUserCouponRequest;
import com.ruyuan.eshop.order.builder.NewOrderBuilder;
import com.ruyuan.eshop.order.builder.FullOrderData;
import com.ruyuan.eshop.order.config.OrderProperties;
import com.ruyuan.eshop.order.dao.*;
import com.ruyuan.eshop.order.domain.dto.*;
import com.ruyuan.eshop.order.domain.entity.*;
import com.ruyuan.eshop.order.domain.request.*;
import com.ruyuan.eshop.order.enums.*;
import com.ruyuan.eshop.order.exception.OrderBizException;
import com.ruyuan.eshop.order.exception.OrderErrorCodeEnum;
import com.ruyuan.eshop.order.manager.OrderNoManager;
import com.ruyuan.eshop.order.mq.producer.DefaultProducer;
import com.ruyuan.eshop.order.service.OrderService;
import com.ruyuan.eshop.pay.api.PayApi;
import com.ruyuan.eshop.pay.domain.dto.PayOrderDTO;
import com.ruyuan.eshop.pay.domain.request.PayOrderRequest;
import com.ruyuan.eshop.pay.domain.request.PayRefundRequest;
import com.ruyuan.eshop.product.api.ProductApi;
import com.ruyuan.eshop.product.domain.dto.ProductSkuDTO;
import com.ruyuan.eshop.product.domain.query.ProductSkuQuery;
import com.ruyuan.eshop.risk.api.RiskApi;
import com.ruyuan.eshop.risk.domain.dto.CheckOrderRiskDTO;
import com.ruyuan.eshop.risk.domain.request.CheckOrderRiskRequest;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private OrderItemDAO orderItemDAO;

    @Autowired
    private OrderDeliveryDetailDAO orderDeliveryDetailDAO;

    @Autowired
    private OrderPaymentDetailDAO orderPaymentDetailDAO;

    @Autowired
    private OrderAmountDAO orderAmountDAO;

    @Autowired
    private OrderAmountDetailDAO orderAmountDetailDAO;

    @Autowired
    private OrderOperateLogDAO orderOperateLogDAO;

    @Autowired
    private OrderSnapshotDAO orderSnapshotDAO;

    @Autowired
    private OrderNoManager orderNoManager;

    @Autowired
    private DefaultProducer defaultProducer;

    @Autowired
    private OrderProperties orderProperties;

    @Autowired
    private RedisLock redisLock;

    /**
     * 商品服务
     */
    @DubboReference(version = "1.0.0")
    private ProductApi productApi;

    /**
     * 库存服务
     */
    @DubboReference(version = "1.0.0", retries = 0)
    private InventoryApi inventoryApi;

    /**
     * 营销服务
     */
    @DubboReference(version = "1.0.0", retries = 0)
    private MarketApi marketApi;

    /**
     * 风控服务
     */
    @DubboReference(version = "1.0.0", retries = 0)
    private RiskApi riskApi;

    /**
     * 支付服务
     */
    @DubboReference(version = "1.0.0", retries = 0)
    private PayApi payApi;

    /**
     * 地址服务
     */
    @DubboReference(version = "1.0.0")
    private AddressApi addressApi;

    /**
     * 生成订单号接口
     *
     * @param genOrderIdRequest 生成订单号入参
     * @return 订单号
     */
    @Override
    public GenOrderIdDTO genOrderId(GenOrderIdRequest genOrderIdRequest) {
        // 参数检查
        String userId = genOrderIdRequest.getUserId();
        ParamCheckUtil.checkStringNonEmpty(userId);
        Integer businessIdentifier = genOrderIdRequest.getBusinessIdentifier();
        ParamCheckUtil.checkObjectNonNull(businessIdentifier);

        String orderId = orderNoManager.genOrderId(OrderNoTypeEnum.SALE_ORDER.getCode(), userId);
        GenOrderIdDTO genOrderIdDTO = new GenOrderIdDTO();
        genOrderIdDTO.setOrderId(orderId);
        return genOrderIdDTO;
    }

    /**
     * 提交订单/生成订单接口
     *
     * @param createOrderRequest 提交订单请求入参
     * @return 订单号
     */
    @GlobalTransactional(rollbackFor = Exception.class)
    @Override
    public CreateOrderDTO createOrder(CreateOrderRequest createOrderRequest) {
        // 1、入参检查
        checkCreateOrderRequestParam(createOrderRequest);

        // 2、风控检查
        checkRisk(createOrderRequest);

        // 3、获取商品信息
        List<ProductSkuDTO> productSkuList = listProductSkus(createOrderRequest);

        // 4、计算订单价格
        CalculateOrderAmountDTO calculateOrderAmountDTO = calculateOrderAmount(createOrderRequest, productSkuList);

        /**
         * 5,6,7是最关键的三个步骤
         */
        // 5、验证订单实付金额
        checkRealPayAmount(createOrderRequest, calculateOrderAmountDTO);

        // 6、锁定优惠券
        lockUserCoupon(createOrderRequest);

        // 7、锁定商品库存
        lockProductStock(createOrderRequest);

        // 8、生成订单到数据库
        addNewOrder(createOrderRequest, productSkuList, calculateOrderAmountDTO);

        // 9、发送订单延迟消息用于支付超时自动关单
        sendPayOrderTimeoutDelayMessage(createOrderRequest);

        // 返回订单信息
        CreateOrderDTO createOrderDTO = new CreateOrderDTO();
        createOrderDTO.setOrderId(createOrderRequest.getOrderId());
        return createOrderDTO;
    }

    /**
     * 检查创建订单请求参数
     */
    private void checkCreateOrderRequestParam(CreateOrderRequest createOrderRequest) {
        ParamCheckUtil.checkObjectNonNull(createOrderRequest);

        // 订单ID
        String orderId = createOrderRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId, OrderErrorCodeEnum.ORDER_ID_IS_NULL);

        // 业务线标识
        Integer businessIdentifier = createOrderRequest.getBusinessIdentifier();
        ParamCheckUtil.checkObjectNonNull(businessIdentifier, OrderErrorCodeEnum.BUSINESS_IDENTIFIER_IS_NULL);
        if (BusinessIdentifierEnum.getByCode(businessIdentifier) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.BUSINESS_IDENTIFIER_ERROR);
        }

        // 用户ID
        String userId = createOrderRequest.getUserId();
        ParamCheckUtil.checkStringNonEmpty(userId, OrderErrorCodeEnum.USER_ID_IS_NULL);

        // 订单类型
        Integer orderType = createOrderRequest.getOrderType();
        ParamCheckUtil.checkObjectNonNull(businessIdentifier, OrderErrorCodeEnum.ORDER_TYPE_IS_NULL);
        if (OrderTypeEnum.getByCode(orderType) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_TYPE_ERROR);
        }

        // 卖家ID
        String sellerId = createOrderRequest.getSellerId();
        ParamCheckUtil.checkStringNonEmpty(sellerId, OrderErrorCodeEnum.SELLER_ID_IS_NULL);

        // 配送类型
        Integer deliveryType = createOrderRequest.getDeliveryType();
        ParamCheckUtil.checkObjectNonNull(deliveryType, OrderErrorCodeEnum.USER_ADDRESS_ERROR);
        if (DeliveryTypeEnum.getByCode(deliveryType) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.DELIVERY_TYPE_ERROR);
        }

        // 地址信息
        String province = createOrderRequest.getProvince();
        String city = createOrderRequest.getCity();
        String area = createOrderRequest.getArea();
        String streetAddress = createOrderRequest.getStreet();
        ParamCheckUtil.checkStringNonEmpty(province, OrderErrorCodeEnum.USER_ADDRESS_ERROR);
        ParamCheckUtil.checkStringNonEmpty(city, OrderErrorCodeEnum.USER_ADDRESS_ERROR);
        ParamCheckUtil.checkStringNonEmpty(area, OrderErrorCodeEnum.USER_ADDRESS_ERROR);
        ParamCheckUtil.checkStringNonEmpty(streetAddress, OrderErrorCodeEnum.USER_ADDRESS_ERROR);

        // 区域ID
        String regionId = createOrderRequest.getRegionId();
        ParamCheckUtil.checkStringNonEmpty(regionId, OrderErrorCodeEnum.REGION_ID_IS_NULL);

        // 经纬度
        BigDecimal lon = createOrderRequest.getLon();
        BigDecimal lat = createOrderRequest.getLat();
        ParamCheckUtil.checkObjectNonNull(lon, OrderErrorCodeEnum.USER_LOCATION_IS_NULL);
        ParamCheckUtil.checkObjectNonNull(lat, OrderErrorCodeEnum.USER_LOCATION_IS_NULL);

        // 收货人信息
        String receiverName = createOrderRequest.getReceiverName();
        String receiverPhone = createOrderRequest.getReceiverPhone();
        ParamCheckUtil.checkStringNonEmpty(receiverName, OrderErrorCodeEnum.ORDER_RECEIVER_IS_NULL);
        ParamCheckUtil.checkStringNonEmpty(receiverPhone, OrderErrorCodeEnum.ORDER_RECEIVER_IS_NULL);

        // 客户端设备信息
        String clientIp = createOrderRequest.getClientIp();
        ParamCheckUtil.checkStringNonEmpty(clientIp, OrderErrorCodeEnum.CLIENT_IP_IS_NULL);

        // 商品条目信息
        List<CreateOrderRequest.OrderItemRequest> orderItemRequestList = createOrderRequest.getOrderItemRequestList();
        ParamCheckUtil.checkCollectionNonEmpty(orderItemRequestList, OrderErrorCodeEnum.ORDER_ITEM_IS_NULL);

        for (CreateOrderRequest.OrderItemRequest orderItemRequest : orderItemRequestList) {
            Integer productType = orderItemRequest.getProductType();
            Integer saleQuantity = orderItemRequest.getSaleQuantity();
            String skuCode = orderItemRequest.getSkuCode();
            ParamCheckUtil.checkObjectNonNull(productType, OrderErrorCodeEnum.ORDER_ITEM_PARAM_ERROR);
            ParamCheckUtil.checkObjectNonNull(saleQuantity, OrderErrorCodeEnum.ORDER_ITEM_PARAM_ERROR);
            ParamCheckUtil.checkStringNonEmpty(skuCode, OrderErrorCodeEnum.ORDER_ITEM_PARAM_ERROR);
        }

        // 订单费用信息
        List<CreateOrderRequest.OrderAmountRequest> orderAmountRequestList = createOrderRequest.getOrderAmountRequestList();
        ParamCheckUtil.checkCollectionNonEmpty(orderAmountRequestList, OrderErrorCodeEnum.ORDER_AMOUNT_IS_NULL);

        for (CreateOrderRequest.OrderAmountRequest orderAmountRequest : orderAmountRequestList) {
            Integer amountType = orderAmountRequest.getAmountType();
            ParamCheckUtil.checkObjectNonNull(amountType, OrderErrorCodeEnum.ORDER_AMOUNT_TYPE_IS_NULL);

            if (AmountTypeEnum.getByCode(amountType) == null) {
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_AMOUNT_TYPE_PARAM_ERROR);
            }
        }
        Map<Integer, Integer> orderAmountMap = orderAmountRequestList.stream()
                .collect(Collectors.toMap(CreateOrderRequest.OrderAmountRequest::getAmountType,
                        CreateOrderRequest.OrderAmountRequest::getAmount));

        // 订单支付原价不能为空
        if (orderAmountMap.get(AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode()) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_ORIGIN_PAY_AMOUNT_IS_NULL);
        }
        // 订单运费不能为空
        if (orderAmountMap.get(AmountTypeEnum.SHIPPING_AMOUNT.getCode()) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_SHIPPING_AMOUNT_IS_NULL);
        }
        // 订单实付金额不能为空
        if (orderAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode()) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_REAL_PAY_AMOUNT_IS_NULL);
        }
        if (StringUtils.isNotEmpty(createOrderRequest.getCouponId())) {
            // 订单优惠券抵扣金额不能为空
            if (orderAmountMap.get(AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode()) == null) {
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_DISCOUNT_AMOUNT_IS_NULL);
            }
        }

        // 订单支付信息
        List<CreateOrderRequest.PaymentRequest> paymentRequestList = createOrderRequest.getPaymentRequestList();
        ParamCheckUtil.checkCollectionNonEmpty(paymentRequestList, OrderErrorCodeEnum.ORDER_PAYMENT_IS_NULL);

        for (CreateOrderRequest.PaymentRequest paymentRequest : paymentRequestList) {
            Integer payType = paymentRequest.getPayType();
            Integer accountType = paymentRequest.getAccountType();
            if (payType == null || PayTypeEnum.getByCode(payType) == null) {
                throw new OrderBizException(OrderErrorCodeEnum.PAY_TYPE_PARAM_ERROR);
            }
            if (accountType == null || AccountTypeEnum.getByCode(accountType) == null) {
                throw new OrderBizException(OrderErrorCodeEnum.ACCOUNT_TYPE_PARAM_ERROR);
            }
        }

    }

    /**
     * 风控检查
     */
    private void checkRisk(CreateOrderRequest createOrderRequest) {
        // 调用风控服务进行风控检查
        CheckOrderRiskRequest checkOrderRiskRequest = createOrderRequest.clone(CheckOrderRiskRequest.class);
        // 代理对象调用
        JsonResult<CheckOrderRiskDTO> jsonResult = riskApi.checkOrderRisk(checkOrderRiskRequest);
        if (!jsonResult.getSuccess()) {
            throw new OrderBizException(jsonResult.getErrorCode(), jsonResult.getErrorMessage());
        }
    }

    /**
     * 锁定用户优惠券
     */
    private void lockUserCoupon(CreateOrderRequest createOrderRequest) {
        String couponId = createOrderRequest.getCouponId();
        if (StringUtils.isEmpty(couponId)) {
            return;
        }
        LockUserCouponRequest lockUserCouponRequest = createOrderRequest.clone(LockUserCouponRequest.class);
        // 调用营销服务锁定用户优惠券
        JsonResult<Boolean> jsonResult = marketApi.lockUserCoupon(lockUserCouponRequest);
        // 检查锁定用户优惠券结果
        if (!jsonResult.getSuccess()) {
            throw new OrderBizException(jsonResult.getErrorCode(), jsonResult.getErrorMessage());
        }
    }

    /**
     * 锁定商品库存
     *
     * @param createOrderRequest 订单信息
     */
    private void lockProductStock(CreateOrderRequest createOrderRequest) {
        String orderId = createOrderRequest.getOrderId();
        List<LockProductStockRequest.OrderItemRequest> orderItemRequestList = ObjectUtil.convertList(
                createOrderRequest.getOrderItemRequestList(), LockProductStockRequest.OrderItemRequest.class);

        LockProductStockRequest lockProductStockRequest = new LockProductStockRequest();
        lockProductStockRequest.setOrderId(orderId);
        lockProductStockRequest.setOrderItemRequestList(orderItemRequestList);
        JsonResult<Boolean> jsonResult = inventoryApi.lockProductStock(lockProductStockRequest);
        // 检查锁定商品库存结果
        if (!jsonResult.getSuccess()) {
            throw new OrderBizException(jsonResult.getErrorCode(), jsonResult.getErrorMessage());
        }
    }

    /**
     * 获取订单条目商品信息
     */
    private List<ProductSkuDTO> listProductSkus(CreateOrderRequest createOrderRequest) {
        List<CreateOrderRequest.OrderItemRequest> orderItemRequestList = createOrderRequest.getOrderItemRequestList();
        List<ProductSkuDTO> productSkuList = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest orderItemRequest : orderItemRequestList) {
            String skuCode = orderItemRequest.getSkuCode();

            ProductSkuQuery productSkuQuery = new ProductSkuQuery();
            productSkuQuery.setSkuCode(skuCode);
            productSkuQuery.setSellerId(createOrderRequest.getSellerId());
            JsonResult<ProductSkuDTO> jsonResult = productApi.getProductSku(productSkuQuery);
            if (!jsonResult.getSuccess()) {
                throw new OrderBizException(jsonResult.getErrorCode(), jsonResult.getErrorMessage());
            }
            ProductSkuDTO productSkuDTO = jsonResult.getData();
            // sku不存在
            if (productSkuDTO == null) {
                throw new OrderBizException(OrderErrorCodeEnum.PRODUCT_SKU_CODE_ERROR, skuCode);
            }
            productSkuList.add(productSkuDTO);
        }
        return productSkuList;
    }

    /**
     * 计算订单价格
     * 如果使用了优惠券、红包、积分等，会一并进行扣减
     *
     * @param createOrderRequest 订单信息
     * @param productSkuList     商品信息
     */
    private CalculateOrderAmountDTO calculateOrderAmount(CreateOrderRequest createOrderRequest, List<ProductSkuDTO> productSkuList) {
        CalculateOrderAmountRequest calculateOrderPriceRequest = createOrderRequest.clone(CalculateOrderAmountRequest.class, CloneDirection.FORWARD);

        // 订单条目补充商品信息
        Map<String, ProductSkuDTO> productSkuDTOMap = productSkuList.stream().collect(Collectors.toMap(ProductSkuDTO::getSkuCode, Function.identity()));
        calculateOrderPriceRequest.getOrderItemRequestList().forEach(item -> {
            String skuCode = item.getSkuCode();
            ProductSkuDTO productSkuDTO = productSkuDTOMap.get(skuCode);
            item.setProductId(productSkuDTO.getProductId());
            item.setSalePrice(productSkuDTO.getSalePrice());
        });

        // 调用营销服务计算订单价格
        JsonResult<CalculateOrderAmountDTO> jsonResult = marketApi.calculateOrderAmount(calculateOrderPriceRequest);

        // 检查价格计算结果
        if (!jsonResult.getSuccess()) {
            throw new OrderBizException(jsonResult.getErrorCode(), jsonResult.getErrorMessage());
        }
        CalculateOrderAmountDTO calculateOrderAmountDTO = jsonResult.getData();
        if (calculateOrderAmountDTO == null) {
            throw new OrderBizException(OrderErrorCodeEnum.CALCULATE_ORDER_AMOUNT_ERROR);
        }
        // 订单费用信息
        List<OrderAmountDTO> orderAmountList = ObjectUtil.convertList(calculateOrderAmountDTO.getOrderAmountList(), OrderAmountDTO.class);
        if (orderAmountList == null || orderAmountList.isEmpty()) {
            throw new OrderBizException(OrderErrorCodeEnum.CALCULATE_ORDER_AMOUNT_ERROR);
        }

        // 订单条目费用明细
        List<OrderAmountDetailDTO> orderItemAmountList = ObjectUtil.convertList(calculateOrderAmountDTO.getOrderAmountDetail(), OrderAmountDetailDTO.class);
        if (orderItemAmountList == null || orderItemAmountList.isEmpty()) {
            throw new OrderBizException(OrderErrorCodeEnum.CALCULATE_ORDER_AMOUNT_ERROR);
        }
        return calculateOrderAmountDTO;
    }

    /**
     * 验证订单实付金额
     */
    private void checkRealPayAmount(CreateOrderRequest createOrderRequest, CalculateOrderAmountDTO calculateOrderAmountDTO) {
        List<CreateOrderRequest.OrderAmountRequest> originOrderAmountRequestList = createOrderRequest.getOrderAmountRequestList();
        Map<Integer, CreateOrderRequest.OrderAmountRequest> originOrderAmountMap =
                originOrderAmountRequestList.stream().collect(Collectors.toMap(
                        CreateOrderRequest.OrderAmountRequest::getAmountType, Function.identity()));
        // 前端给的实付金额
        Integer originRealPayAmount = originOrderAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode()).getAmount();


        List<CalculateOrderAmountDTO.OrderAmountDTO> orderAmountDTOList = calculateOrderAmountDTO.getOrderAmountList();
        Map<Integer, CalculateOrderAmountDTO.OrderAmountDTO> orderAmountMap =
                orderAmountDTOList.stream().collect(Collectors.toMap(CalculateOrderAmountDTO.OrderAmountDTO::getAmountType, Function.identity()));
        // 营销计算出来的实付金额
        Integer realPayAmount = orderAmountMap.get(AmountTypeEnum.REAL_PAY_AMOUNT.getCode()).getAmount();

        if (!originRealPayAmount.equals(realPayAmount)) {
            // 订单验价失败
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_CHECK_REAL_PAY_AMOUNT_FAIL);
        }
    }

    /**
     * 新增订单数据到数据库
     */
    private void addNewOrder(CreateOrderRequest createOrderRequest, List<ProductSkuDTO> productSkuList, CalculateOrderAmountDTO calculateOrderAmountDTO) {

        // 封装新订单数据
        NewOrderDataHolder newOrderDataHolder = new NewOrderDataHolder();

        // 生成主订单
        FullOrderData fullMasterOrderData = addNewMasterOrder(createOrderRequest, productSkuList, calculateOrderAmountDTO);

        // 封装主订单数据到NewOrderData对象中
        newOrderDataHolder.appendOrderData(fullMasterOrderData);


        // 如果存在多种商品类型，需要按商品类型进行拆单
        Map<Integer, List<ProductSkuDTO>> productTypeMap = productSkuList.stream().collect(Collectors.groupingBy(ProductSkuDTO::getProductType));
        if (productTypeMap.keySet().size() > 1) {
            for (Integer productType : productTypeMap.keySet()) {
                // 生成子订单
                FullOrderData fullSubOrderData = addNewSubOrder(fullMasterOrderData, productType);

                // 封装子订单数据到NewOrderData对象中
                newOrderDataHolder.appendOrderData(fullSubOrderData);
            }
        }

        // 保存订单到数据库
        // 订单信息
        List<OrderInfoDO> orderInfoDOList = newOrderDataHolder.getOrderInfoDOList();
        if (!orderInfoDOList.isEmpty()) {
            orderInfoDAO.saveBatch(orderInfoDOList);
        }

        // 订单条目
        List<OrderItemDO> orderItemDOList = newOrderDataHolder.getOrderItemDOList();
        if (!orderItemDOList.isEmpty()) {
            orderItemDAO.saveBatch(orderItemDOList);
        }

        // 订单配送信息
        List<OrderDeliveryDetailDO> orderDeliveryDetailDOList = newOrderDataHolder.getOrderDeliveryDetailDOList();
        if (!orderDeliveryDetailDOList.isEmpty()) {
            orderDeliveryDetailDAO.saveBatch(orderDeliveryDetailDOList);
        }

        // 订单支付信息
        List<OrderPaymentDetailDO> orderPaymentDetailDOList = newOrderDataHolder.getOrderPaymentDetailDOList();
        if (!orderPaymentDetailDOList.isEmpty()) {
            orderPaymentDetailDAO.saveBatch(orderPaymentDetailDOList);
        }

        // 订单费用信息
        List<OrderAmountDO> orderAmountDOList = newOrderDataHolder.getOrderAmountDOList();
        if (!orderAmountDOList.isEmpty()) {
            orderAmountDAO.saveBatch(orderAmountDOList);
        }

        // 订单费用明细
        List<OrderAmountDetailDO> orderAmountDetailDOList = newOrderDataHolder.getOrderAmountDetailDOList();
        if (!orderAmountDetailDOList.isEmpty()) {
            orderAmountDetailDAO.saveBatch(orderAmountDetailDOList);
        }

        // 订单状态变更日志信息
        List<OrderOperateLogDO> orderOperateLogDOList = newOrderDataHolder.getOrderOperateLogDOList();
        if (!orderOperateLogDOList.isEmpty()) {
            orderOperateLogDAO.saveBatch(orderOperateLogDOList);
        }

        // 订单快照数据
        List<OrderSnapshotDO> orderSnapshotDOList = newOrderDataHolder.getOrderSnapshotDOList();
        if (!orderSnapshotDOList.isEmpty()) {
            orderSnapshotDAO.saveBatch(orderSnapshotDOList);
        }
    }

    /**
     * 新增主订单信息订单
     */
    private FullOrderData addNewMasterOrder(CreateOrderRequest createOrderRequest, List<ProductSkuDTO> productSkuList, CalculateOrderAmountDTO calculateOrderAmountDTO) {

        NewOrderBuilder newOrderBuilder = new NewOrderBuilder(createOrderRequest, productSkuList, calculateOrderAmountDTO, orderProperties);
        FullOrderData fullOrderData = newOrderBuilder.buildOrder()
                .buildOrderItems()
                .buildOrderDeliveryDetail()
                .buildOrderPaymentDetail()
                .buildOrderAmount()
                .buildOrderAmountDetail()
                .buildOperateLog()
                .buildOrderSnapshot()
                .build();

        // 订单信息
        OrderInfoDO orderInfoDO = fullOrderData.getOrderInfoDO();

        // 订单条目信息
        List<OrderItemDO> orderItemDOList = fullOrderData.getOrderItemDOList();

        // 订单费用信息
        List<OrderAmountDO> orderAmountDOList = fullOrderData.getOrderAmountDOList();

        // 补全地址信息
        OrderDeliveryDetailDO orderDeliveryDetailDO = fullOrderData.getOrderDeliveryDetailDO();
        String detailAddress = getDetailAddress(orderDeliveryDetailDO);
        orderDeliveryDetailDO.setDetailAddress(detailAddress);

        // 补全订单状态变更日志
        OrderOperateLogDO orderOperateLogDO = fullOrderData.getOrderOperateLogDO();
        String remark = "创建订单操作0-10";
        orderOperateLogDO.setRemark(remark);

        // 补全订单商品快照信息
        List<OrderSnapshotDO> orderSnapshotDOList = fullOrderData.getOrderSnapshotDOList();
        for (OrderSnapshotDO orderSnapshotDO : orderSnapshotDOList) {
            // 优惠券信息
            if (orderSnapshotDO.getSnapshotType().equals(SnapshotTypeEnum.ORDER_COUPON.getCode())) {
                String couponId = orderInfoDO.getCouponId();
                String userId = orderInfoDO.getUserId();
                UserCouponQuery userCouponQuery = new UserCouponQuery();
                userCouponQuery.setCouponId(couponId);
                userCouponQuery.setUserId(userId);
                JsonResult<UserCouponDTO> jsonResult = marketApi.getUserCoupon(userCouponQuery);
                if (jsonResult.getSuccess()) {
                    UserCouponDTO userCouponDTO = jsonResult.getData();
                    if (userCouponDTO != null) {
                        orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(userCouponDTO));
                    }
                } else {
                    orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(couponId));
                }
            }
            // 订单费用信息
            else if (orderSnapshotDO.getSnapshotType().equals(SnapshotTypeEnum.ORDER_AMOUNT.getCode())) {
                orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(orderAmountDOList));
            }
            // 订单条目信息
            else if (orderSnapshotDO.getSnapshotType().equals(SnapshotTypeEnum.ORDER_ITEM.getCode())) {
                orderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(orderItemDOList));
            }
        }

        return fullOrderData;
    }

    /**
     * 获取用户收货详细地址
     */
    private String getDetailAddress(OrderDeliveryDetailDO orderDeliveryDetailDO) {
        String provinceCode = orderDeliveryDetailDO.getProvince();
        String cityCode = orderDeliveryDetailDO.getCity();
        String areaCode = orderDeliveryDetailDO.getArea();
        String streetCode = orderDeliveryDetailDO.getStreet();
        AddressQuery query = new AddressQuery();
        query.setProvinceCode(provinceCode);
        query.setCityCode(cityCode);
        query.setAreaCode(areaCode);
        query.setStreetCode(streetCode);
        JsonResult<AddressDTO> jsonResult = addressApi.queryAddress(query);
        if (!jsonResult.getSuccess() || jsonResult.getData() == null) {
            return orderDeliveryDetailDO.getDetailAddress();
        }

        AddressDTO addressDTO = jsonResult.getData();
        StringBuilder detailAddress = new StringBuilder();
        if (StringUtils.isNotEmpty(addressDTO.getProvince())) {
            detailAddress.append(addressDTO.getProvince());
        }
        if (StringUtils.isNotEmpty(addressDTO.getCity())) {
            detailAddress.append(addressDTO.getCity());
        }
        if (StringUtils.isNotEmpty(addressDTO.getArea())) {
            detailAddress.append(addressDTO.getArea());
        }
        if (StringUtils.isNotEmpty(addressDTO.getStreet())) {
            detailAddress.append(addressDTO.getStreet());
        }
        if (StringUtils.isNotEmpty(orderDeliveryDetailDO.getDetailAddress())) {
            detailAddress.append(orderDeliveryDetailDO.getDetailAddress());
        }
        return detailAddress.toString();
    }

    /**
     * 生成子单
     *
     * @param fullOrderData 主单数据
     * @param productType   商品类型
     */
    private FullOrderData addNewSubOrder(FullOrderData fullOrderData, Integer productType) {

        // 主单信息
        OrderInfoDO orderInfoDO = fullOrderData.getOrderInfoDO();
        // 主订单条目
        List<OrderItemDO> orderItemDOList = fullOrderData.getOrderItemDOList();
        // 主订单配送信息
        OrderDeliveryDetailDO orderDeliveryDetailDO = fullOrderData.getOrderDeliveryDetailDO();
        // 主订单支付信息
        List<OrderPaymentDetailDO> orderPaymentDetailDOList = fullOrderData.getOrderPaymentDetailDOList();
        // 主订单费用信息
        List<OrderAmountDO> orderAmountDOList = fullOrderData.getOrderAmountDOList();
        // 主订单费用明细
        List<OrderAmountDetailDO> orderAmountDetailDOList = fullOrderData.getOrderAmountDetailDOList();
        // 主订单状态变更日志信息
        OrderOperateLogDO orderOperateLogDO = fullOrderData.getOrderOperateLogDO();
        // 主订单快照数据
        List<OrderSnapshotDO> orderSnapshotDOList = fullOrderData.getOrderSnapshotDOList();


        // 父订单号
        String orderId = orderInfoDO.getOrderId();
        // 用户ID
        String userId = orderInfoDO.getUserId();

        // 生成新的子订单的订单号
        String subOrderId = orderNoManager.genOrderId(OrderNoTypeEnum.SALE_ORDER.getCode(), userId);

        // 子订单全量的数据
        FullOrderData subFullOrderData = new FullOrderData();

        // 过滤出当前商品类型的订单条目信息
        List<OrderItemDO> subOrderItemDOList = orderItemDOList.stream()
                .filter(orderItemDO -> productType.equals(orderItemDO.getProductType()))
                .collect(Collectors.toList());

        // 统计子单总金额
        Integer subTotalAmount = 0;
        Integer subRealPayAmount = 0;
        for (OrderItemDO subOrderItemDO : subOrderItemDOList) {
            subTotalAmount += subOrderItemDO.getOriginAmount();
            subRealPayAmount += subOrderItemDO.getPayAmount();
        }

        // 订单主信息
        OrderInfoDO newSubOrderInfo = orderInfoDO.clone(OrderInfoDO.class);
        newSubOrderInfo.setId(null);
        newSubOrderInfo.setOrderId(subOrderId);
        newSubOrderInfo.setParentOrderId(orderId);
        newSubOrderInfo.setOrderStatus(OrderStatusEnum.INVALID.getCode());
        newSubOrderInfo.setTotalAmount(subTotalAmount);
        newSubOrderInfo.setPayAmount(subRealPayAmount);
        subFullOrderData.setOrderInfoDO(newSubOrderInfo);

        // 订单条目
        List<OrderItemDO> newSubOrderItemList = new ArrayList<>();
        for (OrderItemDO orderItemDO : subOrderItemDOList) {
            OrderItemDO newSubOrderItem = orderItemDO.clone(OrderItemDO.class);
            newSubOrderItem.setId(null);
            newSubOrderItem.setOrderId(subOrderId);
            String subOrderItemId = getSubOrderItemId(orderItemDO.getOrderItemId(), subOrderId);
            newSubOrderItem.setOrderItemId(subOrderItemId);
            newSubOrderItemList.add(newSubOrderItem);
        }
        subFullOrderData.setOrderItemDOList(newSubOrderItemList);

        // 订单配送地址信息
        OrderDeliveryDetailDO newSubOrderDeliveryDetail = orderDeliveryDetailDO.clone(OrderDeliveryDetailDO.class);
        newSubOrderDeliveryDetail.setId(null);
        newSubOrderDeliveryDetail.setOrderId(subOrderId);
        subFullOrderData.setOrderDeliveryDetailDO(newSubOrderDeliveryDetail);


        Map<String, OrderItemDO> subOrderItemMap = subOrderItemDOList.stream()
                .collect(Collectors.toMap(OrderItemDO::getOrderItemId, Function.identity()));

        // 统计子订单费用信息
        Integer subTotalOriginPayAmount = 0;
        Integer subTotalCouponDiscountAmount = 0;
        Integer subTotalRealPayAmount = 0;

        // 订单费用明细
        List<OrderAmountDetailDO> subOrderAmountDetailList = new ArrayList<>();
        for (OrderAmountDetailDO orderAmountDetailDO : orderAmountDetailDOList) {
            String orderItemId = orderAmountDetailDO.getOrderItemId();
            if (!subOrderItemMap.containsKey(orderItemId)) {
                continue;
            }
            OrderAmountDetailDO subOrderAmountDetail = orderAmountDetailDO.clone(OrderAmountDetailDO.class);
            subOrderAmountDetail.setId(null);
            subOrderAmountDetail.setOrderId(subOrderId);
            String subOrderItemId = getSubOrderItemId(orderItemId, subOrderId);
            subOrderAmountDetail.setOrderItemId(subOrderItemId);
            subOrderAmountDetailList.add(subOrderAmountDetail);

            Integer amountType = orderAmountDetailDO.getAmountType();
            Integer amount = orderAmountDetailDO.getAmount();
            if (AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode().equals(amountType)) {
                subTotalOriginPayAmount += amount;
            }
            if (AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode().equals(amountType)) {
                subTotalCouponDiscountAmount += amount;
            }
            if (AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amountType)) {
                subTotalRealPayAmount += amount;
            }
        }
        subFullOrderData.setOrderAmountDetailDOList(subOrderAmountDetailList);

        // 订单费用信息
        List<OrderAmountDO> subOrderAmountList = new ArrayList<>();
        for (OrderAmountDO orderAmountDO : orderAmountDOList) {
            Integer amountType = orderAmountDO.getAmountType();
            OrderAmountDO subOrderAmount = orderAmountDO.clone(OrderAmountDO.class);
            subOrderAmount.setId(null);
            subOrderAmount.setOrderId(subOrderId);
            if (AmountTypeEnum.ORIGIN_PAY_AMOUNT.getCode().equals(amountType)) {
                subOrderAmount.setAmount(subTotalOriginPayAmount);
                subOrderAmountList.add(subOrderAmount);
            }
            if (AmountTypeEnum.COUPON_DISCOUNT_AMOUNT.getCode().equals(amountType)) {
                subOrderAmount.setAmount(subTotalCouponDiscountAmount);
                subOrderAmountList.add(subOrderAmount);
            }
            if (AmountTypeEnum.REAL_PAY_AMOUNT.getCode().equals(amountType)) {
                subOrderAmount.setAmount(subTotalRealPayAmount);
                subOrderAmountList.add(subOrderAmount);
            }
        }
        subFullOrderData.setOrderAmountDOList(subOrderAmountList);

        // 订单支付信息
        List<OrderPaymentDetailDO> subOrderPaymentDetailDOList = new ArrayList<>();
        for (OrderPaymentDetailDO orderPaymentDetailDO : orderPaymentDetailDOList) {
            OrderPaymentDetailDO subOrderPaymentDetail = orderPaymentDetailDO.clone(OrderPaymentDetailDO.class);
            subOrderPaymentDetail.setId(null);
            subOrderPaymentDetail.setOrderId(subOrderId);
            subOrderPaymentDetail.setPayAmount(subTotalRealPayAmount);
            subOrderPaymentDetailDOList.add(subOrderPaymentDetail);
        }
        subFullOrderData.setOrderPaymentDetailDOList(subOrderPaymentDetailDOList);

        // 订单状态变更日志信息
        OrderOperateLogDO subOrderOperateLogDO = orderOperateLogDO.clone(OrderOperateLogDO.class);
        subOrderOperateLogDO.setId(null);
        subOrderOperateLogDO.setOrderId(subOrderId);
        subFullOrderData.setOrderOperateLogDO(subOrderOperateLogDO);

        // 订单商品快照信息
        List<OrderSnapshotDO> subOrderSnapshotDOList = new ArrayList<>();
        for (OrderSnapshotDO orderSnapshotDO : orderSnapshotDOList) {
            OrderSnapshotDO subOrderSnapshotDO = orderSnapshotDO.clone(OrderSnapshotDO.class);
            subOrderSnapshotDO.setId(null);
            subOrderSnapshotDO.setOrderId(subOrderId);
            if (SnapshotTypeEnum.ORDER_AMOUNT.getCode().equals(orderSnapshotDO.getSnapshotType())) {
                subOrderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(subOrderAmountList));
            } else if (SnapshotTypeEnum.ORDER_ITEM.getCode().equals(orderSnapshotDO.getSnapshotType())) {
                subOrderSnapshotDO.setSnapshotJson(JsonUtil.object2Json(subOrderItemDOList));
            }
            subOrderSnapshotDOList.add(subOrderSnapshotDO);
        }
        subFullOrderData.setOrderSnapshotDOList(subOrderSnapshotDOList);
        return subFullOrderData;
    }

    /**
     * 获取子订单的orderItemId值
     */
    private String getSubOrderItemId(String orderItemId, String subOrderId) {
        String postfix = orderItemId.substring(orderItemId.indexOf("_"));
        return subOrderId + postfix;
    }

    /**
     * 发送支付订单超时延迟消息，用于支付超时自动关单
     */
    private void sendPayOrderTimeoutDelayMessage(CreateOrderRequest createOrderRequest) {
        PayOrderTimeoutDelayMessage message = new PayOrderTimeoutDelayMessage();

        message.setOrderId(createOrderRequest.getOrderId());
        message.setBusinessIdentifier(createOrderRequest.getBusinessIdentifier());
        message.setCancelType(OrderCancelTypeEnum.TIMEOUT_CANCELED.getCode());
        message.setUserId(createOrderRequest.getUserId());
        message.setOrderType(createOrderRequest.getOrderType());
        message.setOrderStatus(OrderStatusEnum.CREATED.getCode());

        String msgJson = JsonUtil.object2Json(message);
        defaultProducer.sendMessage(RocketMqConstant.PAY_ORDER_TIMEOUT_DELAY_TOPIC, msgJson,
                RocketDelayedLevel.DELAYED_30m, "支付订单超时延迟消息");
    }

    /**
     * 预支付订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrePayOrderDTO prePayOrder(PrePayOrderRequest prePayOrderRequest) {
        // 入参检查
        checkPrePayOrderRequestParam(prePayOrderRequest);

        String orderId = prePayOrderRequest.getOrderId();
        Integer payAmount = prePayOrderRequest.getPayAmount();

        // 加分布式锁（与订单支付回调时加的是同一把锁）
        String key = RedisLockKeyConstants.ORDER_PAY_KEY + orderId;
        boolean lock = redisLock.lock(key);
        if (!lock) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PRE_PAY_ERROR);
        }
        try {
            // 预支付订单前的检查
            checkPrePayOrderInfo(orderId, payAmount);

            // 调用支付系统进行预支付
            PayOrderRequest payOrderRequest = prePayOrderRequest.clone(PayOrderRequest.class);
            JsonResult<PayOrderDTO> jsonResult = payApi.payOrder(payOrderRequest);
            if (!jsonResult.getSuccess()) {
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_PRE_PAY_ERROR);
            }
            PayOrderDTO payOrderDTO = jsonResult.getData();

            // 更新订单表与支付信息表
            updateOrderPaymentInfo(payOrderDTO);

            return payOrderDTO.clone(PrePayOrderDTO.class);
        } finally {
            // 释放分布式锁
            redisLock.unlock(key);
        }
    }

    /**
     * 预支付订单的前置检查
     * @param orderId
     * @param payAmount
     */
    private void checkPrePayOrderInfo(String orderId, Integer payAmount) {
        // 查询订单信息
        OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);
        OrderPaymentDetailDO orderPaymentDetailDO = orderPaymentDetailDAO.getPaymentDetailByOrderId(orderId);
        if (orderInfoDO == null || orderPaymentDetailDO == null) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_INFO_IS_NULL);
        }

        // 检查订单支付金额
        if (!payAmount.equals(orderInfoDO.getPayAmount())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PAY_AMOUNT_ERROR);
        }

        // 判断一下订单状态
        if (!OrderStatusEnum.CREATED.getCode().equals(orderInfoDO.getOrderStatus())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 判断一下支付状态
        if (PayStatusEnum.PAID.getCode().equals(orderPaymentDetailDO.getPayStatus())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PAY_STATUS_IS_PAID);
        }

        // 判断是否超过了支付超时时间
        Date curDate = new Date();
        if(curDate.after(orderInfoDO.getExpireTime())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PRE_PAY_EXPIRE_ERROR);
        }
    }

    /**
     * 检查预支付接口入参
     */
    private void checkPrePayOrderRequestParam(PrePayOrderRequest prePayOrderRequest) {

        String userId = prePayOrderRequest.getUserId();
        ParamCheckUtil.checkStringNonEmpty(userId, OrderErrorCodeEnum.USER_ID_IS_NULL);

        String businessIdentifier = prePayOrderRequest.getBusinessIdentifier();
        ParamCheckUtil.checkStringNonEmpty(businessIdentifier, OrderErrorCodeEnum.BUSINESS_IDENTIFIER_ERROR);

        Integer payType = prePayOrderRequest.getPayType();
        ParamCheckUtil.checkObjectNonNull(payType, OrderErrorCodeEnum.PAY_TYPE_PARAM_ERROR);
        if (PayTypeEnum.getByCode(payType) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.PAY_TYPE_PARAM_ERROR);
        }

        String orderId = prePayOrderRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId, OrderErrorCodeEnum.ORDER_ID_IS_NULL);

        Integer payAmount = prePayOrderRequest.getPayAmount();
        ParamCheckUtil.checkObjectNonNull(payAmount, OrderErrorCodeEnum.PAY_TYPE_PARAM_ERROR);
    }

    /**
     * 预支付更新订单支付信息
     */
    private void updateOrderPaymentInfo(PayOrderDTO payOrderDTO) {
        String orderId = payOrderDTO.getOrderId();
        Integer payType = payOrderDTO.getPayType();
        String outTradeNo = payOrderDTO.getOutTradeNo();
        Date payTime = new Date();

        // 订单表支付信息
        OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);
        orderInfoDO.setPayType(payType);
        orderInfoDO.setPayTime(payTime);
        orderInfoDAO.updateById(orderInfoDO);

        // 支付明细信息
        OrderPaymentDetailDO orderPaymentDetailDO = orderPaymentDetailDAO.getPaymentDetailByOrderId(orderId);
        orderPaymentDetailDO.setPayTime(payTime);
        orderPaymentDetailDO.setPayType(payType);
        orderPaymentDetailDO.setOutTradeNo(outTradeNo);
        orderPaymentDetailDAO.updateById(orderPaymentDetailDO);

        // 判断是否存在子订单
        List<OrderInfoDO> subOrderInfoList = orderInfoDAO.listByParentOrderId(orderId);
        if (subOrderInfoList != null && !subOrderInfoList.isEmpty()) {
            for (OrderInfoDO subOrderInfoDO : subOrderInfoList) {

                // 更新子订单支付信息
                subOrderInfoDO.setPayType(payType);
                subOrderInfoDO.setPayTime(payTime);
                orderInfoDAO.updateById(subOrderInfoDO);

                // 更新子订单支付明细信息
                OrderPaymentDetailDO subOrderPaymentDetailDO =
                        orderPaymentDetailDAO.getPaymentDetailByOrderId(subOrderInfoDO.getOrderId());
                subOrderPaymentDetailDO.setPayTime(payTime);
                subOrderPaymentDetailDO.setPayType(payType);
                subOrderPaymentDetailDO.setOutTradeNo(outTradeNo);
                orderPaymentDetailDAO.updateById(subOrderPaymentDetailDO);
            }
        }

    }

    /**
     * 支付回调
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void payCallback(PayCallbackRequest payCallbackRequest) {
        // 入参检查
        checkPayCallbackRequestParam(payCallbackRequest);

        String orderId = payCallbackRequest.getOrderId();
        Integer payAmount = payCallbackRequest.getPayAmount();
        Integer payType = payCallbackRequest.getPayType();

        // 加支付分布式锁避免支付系统并发回调
        String key = RedisLockKeyConstants.ORDER_PAY_KEY + orderId;
        boolean lock = redisLock.lock(key);
        if (!lock) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_PAY_CALLBACK_ERROR);
        }

        try {
            // 从数据库中查询出当前订单信息
            OrderInfoDO orderInfoDO = orderInfoDAO.getByOrderId(orderId);
            OrderPaymentDetailDO orderPaymentDetailDO = orderPaymentDetailDAO.getPaymentDetailByOrderId(orderId);

            // 校验参数
            if (orderInfoDO == null || orderPaymentDetailDO == null) {
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_INFO_IS_NULL);
            }
            if (!payAmount.equals(orderInfoDO.getPayAmount())) {
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_CALLBACK_PAY_AMOUNT_ERROR);
            }

            // 异常场景判断
            Integer orderStatus = orderInfoDO.getOrderStatus();
            if (OrderStatusEnum.CREATED.getCode().equals(orderStatus)) {
                // 如果订单状态是 "已创建"，直接更新订单状态为已支付
                updateOrderStatusPaid(payCallbackRequest, orderInfoDO, orderPaymentDetailDO);

                // 发送 "订单已完成支付" 消息
                sendPaidOrderSuccessMessage(orderInfoDO);
            } else {
                // 如果订单状态不是 "已创建"
                if (OrderStatusEnum.CANCELED.getCode().equals(orderStatus)) {
                    // 如果订单那状态是取消状态
                    Integer payStatus = orderPaymentDetailDO.getPayStatus();
                    if (PayStatusEnum.UNPAID.getCode().equals(payStatus)) {
                        // 调用退款
                        executeOrderRefund(orderInfoDO, orderPaymentDetailDO);
                        throw new OrderBizException(OrderErrorCodeEnum.ORDER_CANCEL_PAY_CALLBACK_ERROR);
                    } else if (PayStatusEnum.PAID.getCode().equals(payStatus)) {
                        if (payType.equals(orderPaymentDetailDO.getPayType())) {
                            throw new OrderBizException(OrderErrorCodeEnum.ORDER_CANCEL_PAY_CALLBACK_PAY_TYPE_SAME_ERROR);
                        } else {
                            throw new OrderBizException(OrderErrorCodeEnum.ORDER_CANCEL_PAY_CALLBACK_PAY_TYPE_NO_SAME_ERROR);
                        }
                    }
                } else {
                    // 如果订单状态不是取消状态
                    if (PayStatusEnum.PAID.getCode().equals(orderPaymentDetailDO.getPayStatus())) {
                        if (payType.equals(orderPaymentDetailDO.getPayType())) {
                            return;
                        }
                        // 调用退款
                        executeOrderRefund(orderInfoDO, orderPaymentDetailDO);
                        throw new OrderBizException(OrderErrorCodeEnum.ORDER_CANCEL_PAY_CALLBACK_REPEAT_ERROR);
                    }
                }
            }

        } finally {
            // 释放分布式锁
            redisLock.unlock(key);
        }
    }

    /**
     * 执行订单退款
     */
    private void executeOrderRefund(OrderInfoDO orderInfoDO, OrderPaymentDetailDO orderPaymentDetailDO) {
        PayRefundRequest payRefundRequest = new PayRefundRequest();
        payRefundRequest.setOrderId(orderInfoDO.getOrderId());
        payRefundRequest.setRefundAmount(orderPaymentDetailDO.getPayAmount());
        payRefundRequest.setOutTradeNo(orderPaymentDetailDO.getOutTradeNo());
        payApi.executeRefund(payRefundRequest);
    }

    /**
     * 检查订单支付回调接口入参
     */
    private void checkPayCallbackRequestParam(PayCallbackRequest payCallbackRequest) {
        ParamCheckUtil.checkObjectNonNull(payCallbackRequest);

        // 订单号
        String orderId = payCallbackRequest.getOrderId();
        ParamCheckUtil.checkStringNonEmpty(orderId);

        // 支付金额
        Integer payAmount = payCallbackRequest.getPayAmount();
        ParamCheckUtil.checkObjectNonNull(payAmount);

        // 支付系统交易流水号
        String outTradeNo = payCallbackRequest.getOutTradeNo();
        ParamCheckUtil.checkStringNonEmpty(outTradeNo);

        // 支付类型
        Integer payType = payCallbackRequest.getPayType();
        ParamCheckUtil.checkObjectNonNull(payType);
        if (PayTypeEnum.getByCode(payType) == null) {
            throw new OrderBizException(OrderErrorCodeEnum.PAY_TYPE_PARAM_ERROR);
        }

        // 商户ID
        String merchantId = payCallbackRequest.getMerchantId();
        ParamCheckUtil.checkStringNonEmpty(merchantId);
    }

    /**
     * 更新订单状态为 已支付
     */
    private void updateOrderStatusPaid(PayCallbackRequest payCallbackRequest,
                                       OrderInfoDO orderInfoDO,
                                       OrderPaymentDetailDO orderPaymentDetailDO) {
        // 主单信息
        String orderId = payCallbackRequest.getOrderId();
        Integer preOrderStatus = orderInfoDO.getOrderStatus();
        orderInfoDO.setOrderStatus(OrderStatusEnum.PAID.getCode());
        orderInfoDAO.updateById(orderInfoDO);

        // 主单支付信息
        orderPaymentDetailDO.setPayStatus(PayStatusEnum.PAID.getCode());
        orderPaymentDetailDAO.updateById(orderPaymentDetailDO);

        // 新增订单状态变更日志
        OrderOperateLogDO orderOperateLogDO = new OrderOperateLogDO();
        orderOperateLogDO.setOrderId(orderId);
        orderOperateLogDO.setOperateType(OrderOperateTypeEnum.PAID_ORDER.getCode());
        orderOperateLogDO.setPreStatus(preOrderStatus);
        orderOperateLogDO.setCurrentStatus(orderInfoDO.getOrderStatus());
        orderOperateLogDO.setRemark("订单支付回调操作"
                + orderOperateLogDO.getPreStatus() + "-"
                + orderOperateLogDO.getCurrentStatus());
        orderOperateLogDAO.save(orderOperateLogDO);

        // 判断是否存在子订单
        List<OrderInfoDO> subOrderInfoDOList = orderInfoDAO.listByParentOrderId(orderId);
        if (subOrderInfoDOList != null && !subOrderInfoDOList.isEmpty()) {
            // 先将主订单状态设置为无效订单
            Integer newPreOrderStatus = orderInfoDO.getOrderStatus();
            orderInfoDO.setOrderStatus(OrderStatusEnum.INVALID.getCode());
            orderInfoDAO.updateById(orderInfoDO);

            // 新增订单状态变更日志
            OrderOperateLogDO newOrderOperateLogDO = new OrderOperateLogDO();
            newOrderOperateLogDO.setOrderId(orderId);
            newOrderOperateLogDO.setOperateType(OrderOperateTypeEnum.PAID_ORDER.getCode());
            newOrderOperateLogDO.setPreStatus(newPreOrderStatus);
            newOrderOperateLogDO.setCurrentStatus(OrderStatusEnum.INVALID.getCode());
            orderOperateLogDO.setRemark("订单支付回调操作，主订单状态变更"
                    + newOrderOperateLogDO.getPreStatus() + "-"
                    + newOrderOperateLogDO.getCurrentStatus());
            orderOperateLogDAO.save(newOrderOperateLogDO);

            // 再更新子订单的状态
            for (OrderInfoDO subOrderInfo : subOrderInfoDOList) {
                Integer subPreOrderStatus = subOrderInfo.getOrderStatus();
                subOrderInfo.setOrderStatus(OrderStatusEnum.PAID.getCode());
                orderInfoDAO.updateById(subOrderInfo);

                // 更新子订单的支付明细状态
                String subOrderId = subOrderInfo.getOrderId();
                OrderPaymentDetailDO subOrderPaymentDetailDO =
                        orderPaymentDetailDAO.getPaymentDetailByOrderId(subOrderId);
                if (subOrderPaymentDetailDO != null) {
                    subOrderPaymentDetailDO.setPayStatus(PayStatusEnum.PAID.getCode());
                    orderPaymentDetailDAO.updateById(subOrderPaymentDetailDO);
                }

                // 新增订单状态变更日志
                OrderOperateLogDO subOrderOperateLogDO = new OrderOperateLogDO();
                subOrderOperateLogDO.setOrderId(subOrderId);
                subOrderOperateLogDO.setOperateType(OrderOperateTypeEnum.PAID_ORDER.getCode());
                subOrderOperateLogDO.setPreStatus(subPreOrderStatus);
                subOrderOperateLogDO.setCurrentStatus(OrderStatusEnum.PAID.getCode());
                orderOperateLogDO.setRemark("订单支付回调操作，子订单状态变更"
                        + subOrderOperateLogDO.getPreStatus() + "-"
                        + subOrderOperateLogDO.getCurrentStatus());
                orderOperateLogDAO.save(subOrderOperateLogDO);
            }
        }

    }

    /**
     * 发送订单已完成支付消息，触发订单进行履约
     */
    private void sendPaidOrderSuccessMessage(OrderInfoDO orderInfoDO) {
        String orderId = orderInfoDO.getOrderId();
        PaidOrderSuccessMessage message = new PaidOrderSuccessMessage();
        message.setOrderId(orderId);
        String msgJson = JSON.toJSONString(message);
        defaultProducer.sendMessage(RocketMqConstant.PAID_ORDER_SUCCESS_TOPIC, msgJson, "订单已完成支付");
    }

    @Override
    public boolean removeOrders(List<String> orderIds) {
        //1、根据id查询订单
        List<OrderInfoDO> orders = orderInfoDAO.listByOrderIds(orderIds);
        if (CollectionUtils.isEmpty(orders)) {
            return true;
        }

        //2、校验订单是否可以移除
        orders.forEach(order -> {
            if (!canRemove(order)) {
                throw new OrderBizException(OrderErrorCodeEnum.ORDER_CANNOT_REMOVE);
            }
        });

        //3、对订单进行软删除
        List<Long> ids = orders.stream().map(OrderInfoDO::getId).collect(Collectors.toList());
        orderInfoDAO.softRemoveOrders(ids);

        return true;
    }

    private boolean canRemove(OrderInfoDO order) {
        return OrderStatusEnum.canRemoveStatus().contains(order.getOrderStatus()) &&
                DeleteStatusEnum.NO.getCode().equals(order.getDeleteStatus());
    }

    @Override
    public boolean adjustDeliveryAddress(AdjustDeliveryAddressRequest request) {
        //1、根据id查询订单
        OrderInfoDO order = orderInfoDAO.getByOrderId(request.getOrderId());
        ParamCheckUtil.checkObjectNonNull(order, OrderErrorCodeEnum.ORDER_NOT_FOUND);

        //2、校验订单是否未出库
        if (!OrderStatusEnum.unOutStockStatus().contains(order.getOrderStatus())) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_NOT_ALLOW_TO_ADJUST_ADDRESS);
        }

        //3、查询订单配送信息
        OrderDeliveryDetailDO orderDeliveryDetail = orderDeliveryDetailDAO.getByOrderId(request.getOrderId());
        if (null == orderDeliveryDetail) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_DELIVERY_NOT_FOUND);
        }

        //4、校验配送信息是否已经被修改过一次
        if (orderDeliveryDetail.getModifyAddressCount() > 0) {
            throw new OrderBizException(OrderErrorCodeEnum.ORDER_DELIVERY_ADDRESS_HAS_BEEN_ADJUSTED);
        }

        //5、更新配送地址信息
        orderDeliveryDetailDAO.updateDeliveryAddress(orderDeliveryDetail.getId()
                , orderDeliveryDetail.getModifyAddressCount(), request);

        return true;
    }
}