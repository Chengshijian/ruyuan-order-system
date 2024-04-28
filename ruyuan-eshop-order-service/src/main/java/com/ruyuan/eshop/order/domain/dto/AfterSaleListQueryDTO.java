package com.ruyuan.eshop.order.domain.dto;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.common.core.AbstractObject;
import com.ruyuan.eshop.order.domain.query.AfterSaleQuery;
import com.ruyuan.eshop.order.domain.query.OrderQuery;
import com.ruyuan.eshop.order.enums.AfterSaleApplySourceEnum;
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
public class AfterSaleListQueryDTO extends AbstractObject {

    /**
     * 业务线
     */
    private Integer businessIdentifier;

    /**
     * 订单类型
     */
    private Set<Integer> orderTypes;
    /**
     * 售后单状态
     */
    private Set<Integer> afterSaleStatus;
    /**
     * 售后申请来源
     */
    private Set<Integer> applySources;
    /**
     * 售后类型
     */
    private Set<Integer> afterSaleTypes;
    /**
     * 售后单号
     */
    private Set<Long> afterSaleIds;
    /**
     * 订单号
     */
    private Set<String> orderIds;
    /**
     * 用户ID
     */
    private Set<String> userIds;
    /**
     * sku code
     */
    private Set<String> skuCodes;
    /**
     * 创建时间-查询区间
     */
    private Pair<Date,Date> createdTimeInterval;
    /**
     * 售后申请时间-查询区间
     */
    private Pair<Date,Date> applyTimeInterval;
    /**
     * 售后客服审核时间-查询区间
     */
    private Pair<Date,Date> reviewTimeInterval;
    /**
     * 退款支付时间-查询区间
     */
    private Pair<Date,Date> refundPayTimeInterval;
    /**
     * 退款金额-查询区间
     */
    private Pair<Integer,Integer> refundAmountInterval;
    /**
     * 页码；默认为1；
     */
    private Integer pageNo = 1;
    /**
     * 一页的数据量. 默认为20
     */
    private Integer pageSize = 20;

    /**
     * 内部构造器
     */
    public static class Builder {

        private AfterSaleListQueryDTO query = null;

        public static Builder builder() {
            return new Builder();
        }

        public Builder copy(AfterSaleQuery afterSaleQuery) {
            query = afterSaleQuery.clone(AfterSaleListQueryDTO.class);
            return this;
        }

        /**
         * 售后列表默认只展示用户主动发起的售后退款记录，超时自动取消和用户手动取消的售后单默认不展示
         * @return
         */
        public Builder userApplySource() {
            if(CollectionUtils.isEmpty(query.getApplySources())) {
                query.setApplySources(AfterSaleApplySourceEnum.userApply());
            }
            return this;
        }

        /**
         * 设置分页
         * @return
         */
        public Builder setPage(AfterSaleQuery orderQuery) {
            query.setPageNo(orderQuery.getPageNo());
            query.setPageSize(orderQuery.getPageSize());
            return this;
        }

        public AfterSaleListQueryDTO build() {
            return query;
        }
    }

}
