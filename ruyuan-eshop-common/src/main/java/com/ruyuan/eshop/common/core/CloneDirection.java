package com.ruyuan.eshop.common.core;

/**
 * 克隆方向枚举
 *
 * @author zhonghuashishan
 * @version 1.0
 */
public enum CloneDirection {

    /**
     * 正向克隆：从VO->DTO，DTO->DO
     */
    FORWARD(1),
    /**
     * 反向克隆：从DO->DTO，DTO->VO
     */
    OPPOSITE(2);

    private Integer code;

    CloneDirection(Integer code) {
        this.code = code;
    }

}