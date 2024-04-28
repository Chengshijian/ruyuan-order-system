package com.ruyuan.eshop.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruyuan.eshop.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 订单编号表
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
@TableName("order_auto_no")
public class OrderAutoNoDO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

}
