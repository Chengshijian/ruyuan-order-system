package com.ruyuan.eshop.order.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruyuan.eshop.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 订单快照表
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
@TableName("order_snapshot")
public class OrderSnapshotDO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    private String orderId;

    /**
     * 快照类型
     */
    private Integer snapshotType;

    /**
     * 订单快照内容
     */
    private String snapshotJson;

}
