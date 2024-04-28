package com.ruyuan.eshop.order.domain.dto;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.common.core.AbstractObject;
import com.ruyuan.eshop.common.enums.OrderStatusEnum;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Date;
import java.util.Set;

/**
 * <p>
 *  订单列表查询入参DTO
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
public class OrderListQueryDTO extends AbstractObject {

    /**
     * 业务线
     */
    private Integer businessIdentifier;
    /**
     * 订单类型
     */
    private Set<Integer> orderTypes;
    /**
     * 订单号
     */
    private Set<String> orderIds;
    /**
     * 卖家ID
     */
    private Set<String> sellerIds;
    /**
     * 父订单号
     */
    private Set<String> parentOrderIds;
    /**
     * 用户ID
     */
    private Set<String> userIds;
    /**
     * 订单状态
     */
    private Set<Integer> orderStatus;
    /**
     * 收货人手机号
     */
    private Set<String> receiverPhones;
    /**
     * 收货人姓名
     */
    private Set<String> receiverNames;
    /**
     * 交易流水号
     */
    private Set<String> tradeNos;

    /**
     * sku code
     */
    private Set<String> skuCodes;
    /**
     * sku商品名称
     */
    private Set<String> productNames;
    /**
     * 创建时间-查询区间
     */
    private Pair<Date,Date> createdTimeInterval;
    /**
     * 支付时间-查询区间
     */
    private Pair<Date,Date> payTimeInterval;
    /**
     * 支付金额-查询区间
     */
    private Pair<Integer,Integer> payAmountInterval;


    /**
     * 页码
     */
    private Integer pageNo = 1;
    /**
     * 每页条数
     */
    private Integer pageSize = 20;

    /**
     * 内部构造器
     */
    public static class Builder {

        private OrderListQueryDTO query = null;

        public static Builder builder() {
            return new Builder();
        }

        public Builder copy(OrderQuery orderQuery) {
            query = orderQuery.clone(OrderListQueryDTO.class);
            return this;
        }

        /**
         * 不展示无效订单
         * @return
         */
        public Builder removeInValidStatus() {
            if(CollectionUtils.isEmpty(query.getOrderStatus())) {
                query.setOrderStatus(OrderStatusEnum.validStatus());
            }
            return this;
        }

        /**
         * 设置分页
         * @return
         */
        public Builder setPage(OrderQuery orderQuery) {
            query.setPageNo(orderQuery.getPageNo());
            query.setPageSize(orderQuery.getPageSize());
            return this;
        }

        public OrderListQueryDTO build() {
            return query;
        }
    }

}
