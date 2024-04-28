package com.ruyuan.eshop.common.constants;

/**
 * <p>
 * redis 分布式锁key
 * </p>
 *
 * @author zhonghuashishan
 */
public class RedisLockKeyConstants {

    /**
     * 订单支付
     */
    public static final String ORDER_PAY_KEY = "#ORDER_PAY_KEY:";

    public static final String ORDER_FULFILL_KEY = "#ORDER_FULFILL_KEY:";

    public static final String ORDER_WMS_RESULT_KEY = "#ORDER_WMS_RESULT_KEY:";

    public static final String CANCEL_REFUND_KEY = "#CANCEL_REFUND_KEY:";

    public static final String AFTER_SALE_ORDER_KEY = "#AFTER_SALE_ORDER_KEY:";

    public static final String REFUND_KEY = "#REFUND_KEY:";
    public static final String CANCEL_KEY = "#CANCEL_KEY:";
}
