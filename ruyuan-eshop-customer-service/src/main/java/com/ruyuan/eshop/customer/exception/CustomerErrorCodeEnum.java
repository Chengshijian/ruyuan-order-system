package com.ruyuan.eshop.customer.exception;

import com.ruyuan.eshop.common.exception.BaseErrorCodeEnum;

/**
 * 异常错误码枚举值
 * 前三位代表服务，后三位代表功能错误码
 *
 * @author zhonghuashishan
 * @version 1.0
 */
public enum CustomerErrorCodeEnum implements BaseErrorCodeEnum {

    CUSTOMER_AUDIT_REPEAT("200000", "客服审核重复"),
    ;

    private String errorCode;

    private String errorMsg;

    CustomerErrorCodeEnum(String errorCode, String errorMsg) {
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