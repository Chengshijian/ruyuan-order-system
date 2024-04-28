package com.ruyuan.eshop.order.domain.request;

import com.ruyuan.eshop.common.core.AbstractObject;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
public class GenOrderIdRequest extends AbstractObject implements Serializable {

    private static final long serialVersionUID = -3918194989507931383L;

    /**
     * 业务线标识
     */
    private Integer businessIdentifier;

    /**
     * 用户ID
     */
    private String userId;

}