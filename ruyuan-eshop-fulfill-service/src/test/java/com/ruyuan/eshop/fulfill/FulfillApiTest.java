package com.ruyuan.eshop.fulfill;

import com.ruyuan.eshop.common.enums.OrderStatusChangeEnum;
import com.ruyuan.eshop.fulfill.api.FulfillApi;
import com.ruyuan.eshop.fulfill.domain.event.OrderDeliveredWmsEvent;
import com.ruyuan.eshop.fulfill.domain.event.OrderOutStockWmsEvent;
import com.ruyuan.eshop.fulfill.domain.event.OrderSignedWmsEvent;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

@SpringBootTest(classes = FulfillApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class FulfillApiTest {

    @DubboReference(version = "1.0.0")
    private FulfillApi fulfillApi;


    @Test
    public void triggerOrderWmsShipEvent() throws Exception {

        String orderId = "1011250000000010000";
        OrderStatusChangeEnum orderStatusChange = OrderStatusChangeEnum.ORDER_OUT_STOCKED;
//        OrderOutStockWmsEvent wmsEvent1 = new OrderOutStockWmsEvent();
//        wmsEvent1.setOrderId(orderId);
//        wmsEvent1.setOutStockTime(new Date());
//
//        fulfillApi.triggerOrderWmsShipEvent(orderId,orderStatusChange,wmsEvent1);


//        orderStatusChange = OrderStatusChangeEnum.ORDER_DELIVERED;
//        OrderDeliveredWmsEvent wmsEvent2 = new OrderDeliveredWmsEvent();
//        wmsEvent2.setOrderId(orderId);
//        wmsEvent2.setDelivererNo("rc2019");
//        wmsEvent2.setDelivererName("张三");
//        wmsEvent2.setDelivererPhone("19100012112");
//
//        fulfillApi.triggerOrderWmsShipEvent(orderId,orderStatusChange,wmsEvent2);
//
//
        orderStatusChange = OrderStatusChangeEnum.ORDER_SIGNED;
        OrderSignedWmsEvent wmsEvent3 = new OrderSignedWmsEvent();
        wmsEvent3.setOrderId(orderId);
        wmsEvent3.setSignedTime(new Date());

        fulfillApi.triggerOrderWmsShipEvent(orderId,orderStatusChange,wmsEvent3);
    }

}
