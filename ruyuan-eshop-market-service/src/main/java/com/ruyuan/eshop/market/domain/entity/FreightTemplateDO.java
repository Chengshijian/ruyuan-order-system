package com.ruyuan.eshop.market.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ruyuan.eshop.common.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 运费模板
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
@TableName("market_freight_template")
public class FreightTemplateDO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 区域ID
     */
    private String regionId;

    /**
     * 标准运费
     */
    private Integer shippingAmount;

    /**
     * 订单满多少钱则免运费
     */
    private Integer conditionAmount;

}
