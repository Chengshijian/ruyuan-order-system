CREATE database if NOT EXISTS `ruyuan_eshop_order` default character set utf8 collate utf8_general_ci;
use `ruyuan_eshop_order`;


SET NAMES utf8;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for after_sale_info
-- ----------------------------
DROP TABLE IF EXISTS `after_sale_info`;
CREATE TABLE `after_sale_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `business_identifier` tinyint(4) DEFAULT NULL COMMENT '接入方业务线标识  1, "自营商城"',
  `after_sale_id` bigint(20) NOT NULL COMMENT '售后id',
  `order_id` varchar(50) NOT NULL DEFAULT '' COMMENT '订单号',
  `order_source_channel` tinyint(4) NOT NULL COMMENT '订单来源渠道',
  `user_id` varchar(20) NOT NULL COMMENT '购买用户id',
  `after_sale_status` tinyint(4) NOT NULL COMMENT '售后单状态',
  `order_type` tinyint(4) NOT NULL COMMENT '订单类型',
  `apply_source` tinyint(4) DEFAULT NULL COMMENT '申请售后来源',
  `apply_time` datetime DEFAULT NULL COMMENT '申请售后时间',
  `apply_reason_code` tinyint(4) DEFAULT NULL COMMENT '申请原因编码',
  `apply_reason` varchar(1024) DEFAULT NULL COMMENT '申请原因',
  `review_time` datetime DEFAULT NULL COMMENT '客服审核时间',
  `review_source` tinyint(4) DEFAULT NULL COMMENT '客服审核来源',
  `review_reason_code` tinyint(4) DEFAULT NULL COMMENT '客服审核结果编码',
  `review_reason` varchar(1024) DEFAULT NULL COMMENT '客服审核结果',
  `after_sale_type` tinyint(4) NOT NULL COMMENT '售后类型',
  `after_sale_type_detail` tinyint(4) DEFAULT NULL COMMENT '售后类型详情枚举',
  `apply_refund_amount` int(11) DEFAULT NULL COMMENT '申请退款金额',
  `real_refund_amount` int(11) DEFAULT NULL COMMENT '实际退款金额',
  `remark` varchar(1024) DEFAULT NULL COMMENT '备注',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_after_sale_type` (`after_sale_type`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8 COMMENT='订单售后表';

-- ----------------------------
-- Table structure for after_sale_item
-- ----------------------------
DROP TABLE IF EXISTS `after_sale_item`;
CREATE TABLE `after_sale_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `after_sale_id` bigint(20) NOT NULL COMMENT '售后id',
  `order_id` varchar(50) NOT NULL DEFAULT '' COMMENT '订单id',
  `sku_code` varchar(20) NOT NULL DEFAULT '' COMMENT 'sku id',
  `product_name` varchar(1024) NOT NULL COMMENT '商品名',
  `return_quantity` tinyint(4) NOT NULL COMMENT '商品退货数量',
  `product_img` varchar(1024) NOT NULL COMMENT '商品图片地址',
  `origin_amount` int(11) NOT NULL COMMENT '商品总金额',
  `apply_refund_amount` int(11) NOT NULL COMMENT '申请退款金额',
  `real_refund_amount` int(11) NOT NULL COMMENT '实际退款金额',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='订单售后条目表';

-- ----------------------------
-- Table structure for after_sale_log
-- ----------------------------
DROP TABLE IF EXISTS `after_sale_log`;
CREATE TABLE `after_sale_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `after_sale_id` varchar(50) NOT NULL COMMENT '售后单号',
  `pre_status` tinyint(4) NOT NULL COMMENT '前一个状态',
  `current_status` tinyint(4) NOT NULL COMMENT '当前状态',
  `remark` varchar(1024) NOT NULL COMMENT '备注',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8 COMMENT='售后单变更表';

-- ----------------------------
-- Table structure for after_sale_refund
-- ----------------------------
DROP TABLE IF EXISTS `after_sale_refund`;
CREATE TABLE `after_sale_refund` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `after_sale_id` varchar(64) NOT NULL COMMENT '售后单号',
  `order_id` varchar(64) NOT NULL COMMENT '订单号',
  `after_sale_batch_no` varchar(64) DEFAULT NULL COMMENT '售后批次编号',
  `account_type` tinyint(4) NOT NULL COMMENT '账户类型',
  `pay_type` tinyint(4) NOT NULL COMMENT '支付类型',
  `refund_status` tinyint(4) NOT NULL COMMENT '退款状态',
  `refund_amount` int(11) NOT NULL COMMENT '退款金额',
  `refund_pay_time` datetime DEFAULT NULL COMMENT '退款支付时间',
  `out_trade_no` varchar(64) DEFAULT NULL COMMENT '交易单号',
  `remark` varchar(1024) DEFAULT NULL COMMENT '备注',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='售后支付表';

-- ----------------------------
-- Table structure for order_amount
-- ----------------------------
DROP TABLE IF EXISTS `order_amount`;
CREATE TABLE `order_amount` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单编号',
  `amount_type` int(11) NOT NULL COMMENT '收费类型',
  `amount` int(11) NOT NULL COMMENT '收费金额',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8 COMMENT='订单费用表';

-- ----------------------------
-- Table structure for order_amount_detail
-- ----------------------------
DROP TABLE IF EXISTS `order_amount_detail`;
CREATE TABLE `order_amount_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单编号',
  `product_type` tinyint(4) NOT NULL COMMENT '产品类型',
  `order_item_id` varchar(50) NOT NULL COMMENT '订单明细编号',
  `product_id` varchar(50) NOT NULL COMMENT '商品编号',
  `sku_code` varchar(50) NOT NULL COMMENT 'sku编码',
  `sale_quantity` int(11) NOT NULL COMMENT '销售数量',
  `sale_price` int(11) NOT NULL COMMENT '销售单价',
  `amount_type` tinyint(4) NOT NULL COMMENT '收费类型',
  `amount` int(11) NOT NULL COMMENT '收费金额',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8 COMMENT='订单费用明细表';

-- ----------------------------
-- Table structure for order_auto_no
-- ----------------------------
DROP TABLE IF EXISTS `order_auto_no`;
CREATE TABLE `order_auto_no` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8 COMMENT='订单编号表';

-- ----------------------------
-- Table structure for order_delivery_detail
-- ----------------------------
DROP TABLE IF EXISTS `order_delivery_detail`;
CREATE TABLE `order_delivery_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单编号',
  `delivery_type` tinyint(4) DEFAULT NULL COMMENT '配送类型',
  `province` varchar(50) DEFAULT NULL COMMENT '省',
  `city` varchar(50) DEFAULT NULL COMMENT '市',
  `area` varchar(50) DEFAULT NULL COMMENT '区',
  `street` varchar(50) DEFAULT NULL COMMENT '街道',
  `detail_address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `lon` decimal(20,10) DEFAULT NULL COMMENT '经度',
  `lat` decimal(20,10) DEFAULT NULL COMMENT '维度',
  `receiver_name` varchar(50) DEFAULT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(50) DEFAULT NULL COMMENT '收货人电话',
  `modify_address_count` tinyint(4) NOT NULL DEFAULT '0' COMMENT '调整地址次数',
  `deliverer_no` varchar(50) DEFAULT NULL COMMENT '配送员编号',
  `deliverer_name` varchar(50) DEFAULT NULL COMMENT '配送员姓名',
  `deliverer_phone` varchar(50) DEFAULT NULL COMMENT '配送员手机号',
  `out_stock_time` datetime DEFAULT NULL COMMENT '出库时间',
  `signed_time` datetime DEFAULT NULL COMMENT '签收时间',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 COMMENT='订单配送信息表';

-- ----------------------------
-- Table structure for order_info
-- ----------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_identifier` tinyint(4) NOT NULL COMMENT '接入方业务线标识  1, "自营商城"',
  `order_id` varchar(50) NOT NULL COMMENT '订单编号',
  `parent_order_id` varchar(50) DEFAULT NULL COMMENT '父订单编号',
  `business_order_id` varchar(50) DEFAULT NULL COMMENT '接入方订单号',
  `order_type` tinyint(4) NOT NULL COMMENT '订单类型 1:一般订单  255:其它',
  `order_status` tinyint(4) NOT NULL COMMENT '订单状态 10:已创建, 30:已履约, 40:出库, 50:配送中, 60:已签收, 70:已取消, 100:已拒收, 255:无效订单',
  `cancel_type` varchar(255) DEFAULT NULL COMMENT '订单取消类型',
  `cancel_time` datetime DEFAULT NULL COMMENT '订单取消时间',
  `seller_id` varchar(50) DEFAULT NULL COMMENT '卖家编号',
  `user_id` varchar(50) DEFAULT NULL COMMENT '买家编号',
  `total_amount` int(11) DEFAULT NULL COMMENT '交易总金额（以分为单位存储）',
  `pay_amount` int(11) DEFAULT NULL COMMENT '交易支付金额',
  `pay_type` tinyint(4) DEFAULT NULL COMMENT '支付方式 10:微信支付 20:支付宝支付',
  `coupon_id` varchar(50) DEFAULT NULL COMMENT '使用的优惠券编号',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `expire_time` datetime NOT NULL COMMENT '支付订单截止时间',
  `user_remark` varchar(255) DEFAULT NULL COMMENT '用户备注',
  `delete_status` tinyint(4) NOT NULL COMMENT '订单删除状态 0:未删除  1:已删除',
  `comment_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '订单评论状态 0:未发表评论  1:已发表评论',
  `ext_json` varchar(1024) DEFAULT NULL COMMENT '扩展信息',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COMMENT='订单表';

-- ----------------------------
-- Table structure for order_item
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单编号',
  `order_item_id` varchar(50) NOT NULL COMMENT '订单明细编号',
  `product_type` tinyint(4) NOT NULL COMMENT '商品类型 1:普通商品,2:预售商品',
  `product_id` varchar(50) NOT NULL COMMENT '商品编号',
  `product_img` varchar(255) DEFAULT NULL COMMENT '商品图片',
  `product_name` varchar(50) NOT NULL COMMENT '商品名称',
  `sku_code` varchar(50) NOT NULL COMMENT 'sku编码',
  `sale_quantity` int(11) NOT NULL COMMENT '销售数量',
  `sale_price` int(11) NOT NULL COMMENT '销售单价',
  `origin_amount` int(11) NOT NULL COMMENT '当前商品支付原总价',
  `pay_amount` int(11) NOT NULL COMMENT '交易支付金额',
  `product_unit` varchar(10) NOT NULL COMMENT '商品单位',
  `purchase_price` int(11) DEFAULT NULL COMMENT '采购成本价',
  `seller_id` varchar(50) DEFAULT NULL COMMENT '卖家编号',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8 COMMENT='订单条目表';

-- ----------------------------
-- Table structure for order_operate_log
-- ----------------------------
DROP TABLE IF EXISTS `order_operate_log`;
CREATE TABLE `order_operate_log` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单编号',
  `operate_type` tinyint(4) DEFAULT NULL COMMENT '操作类型',
  `pre_status` tinyint(4) DEFAULT NULL COMMENT '前置状态',
  `current_status` tinyint(4) DEFAULT NULL COMMENT '当前状态',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注说明',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='订单操作日志表';

-- ----------------------------
-- Table structure for order_payment_detail
-- ----------------------------
DROP TABLE IF EXISTS `order_payment_detail`;
CREATE TABLE `order_payment_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单编号',
  `account_type` tinyint(4) NOT NULL COMMENT '账户类型',
  `pay_type` tinyint(4) NOT NULL COMMENT '支付类型  10:微信支付, 20:支付宝支付',
  `pay_status` tinyint(4) NOT NULL COMMENT '支付状态 10:未支付,20:已支付',
  `pay_amount` int(11) NOT NULL DEFAULT '0' COMMENT '支付金额',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `out_trade_no` varchar(50) DEFAULT NULL COMMENT '支付系统交易流水号',
  `pay_remark` varchar(255) DEFAULT NULL COMMENT '支付备注信息',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='订单支付明细表';

-- ----------------------------
-- Table structure for order_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `order_snapshot`;
CREATE TABLE `order_snapshot` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单号',
  `snapshot_type` tinyint(4) unsigned NOT NULL COMMENT '快照类型',
  `snapshot_json` varchar(2000) NOT NULL COMMENT '订单快照内容',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='订单快照表';

-- ----------------------------
-- Table structure for undo_log
-- ----------------------------
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
