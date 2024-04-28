package com.ruyuan.eshop.inventory.exception;

import com.ruyuan.eshop.common.exception.BaseErrorCodeEnum;

/**
 * 异常错误码枚举值
 * 前三位代表服务，后三位代表功能错误码
 * @author zhonghuashishan
 * @version 1.0
 */
public enum InventoryErrorCodeEnum implements BaseErrorCodeEnum {

    PRODUCT_SKU_STOCK_ERROR("100001", "商品库存记录不存在"),
    LOCK_PRODUCT_SKU_STOCK_ERROR("100002", "锁定商品库存失败"),
    RELEASE_PRODUCT_SKU_STOCK_ERROR("100003", "释放商品库存失败"),
    CONSUME_MQ_FAILED("100004", "消费MQ消息失败"),
    ;

    private String errorCode;

    private String errorMsg;

    InventoryErrorCodeEnum(String errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

}