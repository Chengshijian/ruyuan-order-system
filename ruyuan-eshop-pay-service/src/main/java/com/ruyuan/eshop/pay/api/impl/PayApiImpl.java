package com.ruyuan.eshop.pay.api.impl;

import com.ruyuan.eshop.common.core.JsonResult;
import com.ruyuan.eshop.common.enums.PayTypeEnum;
import com.ruyuan.eshop.common.utils.RandomUtil;
import com.ruyuan.eshop.pay.api.PayApi;
import com.ruyuan.eshop.pay.domain.dto.PayOrderDTO;
import com.ruyuan.eshop.pay.domain.request.PayOrderRequest;
import com.ruyuan.eshop.pay.domain.request.PayRefundRequest;
import com.ruyuan.eshop.pay.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = PayApi.class, retries = 0)
public class PayApiImpl implements PayApi {
    /**
     * 支付交易流水管理service组件
     */
    @Autowired
    private PayService payService;

    @Override
    public JsonResult<PayOrderDTO> payOrder(PayOrderRequest payOrderRequest) {

        String orderId = payOrderRequest.getOrderId();
        Integer payAmount = payOrderRequest.getPayAmount();
        String outTradeNo = RandomUtil.genRandomNumber(19);

        // 模拟调用了第三方支付平台的支付接口

        // 组装返回数据
        PayOrderDTO payOrderDTO = new PayOrderDTO();
        payOrderDTO.setOrderId(orderId);
        payOrderDTO.setOutTradeNo(outTradeNo);
        payOrderDTO.setPayType(PayTypeEnum.WECHAT_PAY.getCode());

        Map<String, Object> payData = new HashMap<>();
        payData.put("appid", "wx207d34495e688e0c");
        payData.put("prepayId", RandomUtil.genRandomNumber(11));
        payData.put("payAmount", payAmount);
        payData.put("webUrl", "http://xxx/payurl");
        payOrderDTO.setPayData(payData);

        return JsonResult.buildSuccess(payOrderDTO);
    }

    @Override
    public Boolean getTradeNoByRealTime(String orderId, Integer businessIdentifier) {
        return payService.getRealTimeTradeNo(orderId, businessIdentifier);
    }

    @Override
    public Boolean executeRefund(PayRefundRequest payRefundRequest) {
        log.info("调用支付接口执行退款");
        return true;
    }

}