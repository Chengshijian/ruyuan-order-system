CREATE database if NOT EXISTS `ruyuan_eshop_inventory` default character set utf8 collate utf8_general_ci;
use `ruyuan_eshop_inventory`;


SET NAMES utf8;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for inventory_product_stock
-- ----------------------------
DROP TABLE IF EXISTS `inventory_product_stock`;
CREATE TABLE `inventory_product_stock` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_code` bigint(20) NOT NULL COMMENT '商品sku编号',
  `sale_stock_quantity` bigint(20) NOT NULL COMMENT '销售库存',
  `locked_stock_quantity` bigint(20) NOT NULL COMMENT '锁定库存',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='库存中心的商品库存表';

-- ----------------------------
-- Records of inventory_product_stock
-- ----------------------------
BEGIN;
INSERT INTO `inventory_product_stock` VALUES (1, 10101010, 90, 10, '2021-11-26 13:57:12', '2021-11-26 13:57:14');
INSERT INTO `inventory_product_stock` VALUES (2, 10101011, 49, 1, '2021-11-26 13:57:32', '2021-11-26 13:57:34');
COMMIT;

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
