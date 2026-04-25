-- ============================================
-- 多仓维护功能 - 数据库表结构 SQL (MySQL)
-- 执行顺序：按依赖关系排列
-- ============================================

-- 1. 仓库主表 (warehouse_table)
DROP TABLE IF EXISTS `warehouse_table`;
CREATE TABLE `warehouse_table` (
  `warehouse_id` varchar(50) NOT NULL COMMENT '仓库ID（主键）',
  `warehouse_code` varchar(50) DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(100) DEFAULT NULL COMMENT '仓库名称',
  `warehouse_type` varchar(20) DEFAULT NULL COMMENT '仓库类型',
  `region_code` varchar(50) DEFAULT NULL COMMENT '行政区划代码',
  `address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `warehouse_status` tinyint DEFAULT NULL COMMENT '仓库状态(0停用1正常)',
  `is_default` tinyint DEFAULT NULL COMMENT '是否默认仓(0否1是)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库主表';

-- 2. 仓库供应商表 (warehouse_supplier_table)
DROP TABLE IF EXISTS `warehouse_supplier_table`;
CREATE TABLE `warehouse_supplier_table` (
  `supplier_id` varchar(50) NOT NULL COMMENT '供应商ID（主键）',
  `supplier_name` varchar(100) DEFAULT NULL COMMENT '供应商名称',
  `category_name` varchar(100) DEFAULT NULL COMMENT '供应商分类',
  `manager_name` varchar(50) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '电话',
  `address` varchar(255) DEFAULT NULL COMMENT '联系地址',
  `payment_method` varchar(50) DEFAULT NULL COMMENT '付款方式',
  `payment_days` int DEFAULT NULL COMMENT '账期天数',
  `supplier_status` tinyint DEFAULT NULL COMMENT '供应商状态(0停用1正常)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库供应商表';

-- 3. 仓库商品属性表 (warehouse_product_table)
DROP TABLE IF EXISTS `warehouse_product_table`;
CREATE TABLE `warehouse_product_table` (
  `warehouse_product_id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库商品ID（主键）',
  `warehouse_id` varchar(50) DEFAULT NULL COMMENT '仓库ID',
  `product_sku_id` bigint DEFAULT NULL COMMENT '商品SKU ID',
  `warehouse_product_cost_price` decimal(12,2) DEFAULT NULL COMMENT '该仓库采购价',
  `warehouse_product_location` varchar(50) DEFAULT NULL COMMENT '库位编码',
  `warehouse_product_first_date` date DEFAULT NULL COMMENT '首次有库存日期',
  `warehouse_product_last_date` date DEFAULT NULL COMMENT '最近入库日期',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`warehouse_product_id`),
  UNIQUE KEY `uk_warehouse_product` (`warehouse_id`, `product_sku_id`),
  KEY `idx_product_sku_id` (`product_sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库商品属性表';

-- 4. 仓库库存表 (warehouse_stock_table)
DROP TABLE IF EXISTS `warehouse_stock_table`;
CREATE TABLE `warehouse_stock_table` (
  `warehouse_stock_id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库库存ID（主键）',
  `warehouse_product_id` bigint DEFAULT NULL COMMENT '关联warehouse_product',
  `warehouse_stock_qty` int DEFAULT NULL COMMENT '库存数量',
  `warehouse_stock_available_qty` int DEFAULT NULL COMMENT '可用量',
  `warehouse_stock_transit_qty` int DEFAULT NULL COMMENT '在途数量',
  `warehouse_stock_frozen_qty` int DEFAULT NULL COMMENT '冻结库存',
  `warehouse_stock_outstock_hours` int DEFAULT NULL COMMENT '缺货时长(小时)',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`warehouse_stock_id`),
  KEY `idx_warehouse_product_id` (`warehouse_product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库库存表';

-- 5. 仓库采购订单表 (warehouse_purchase_order_table)
DROP TABLE IF EXISTS `warehouse_purchase_order_table`;
CREATE TABLE `warehouse_purchase_order_table` (
  `purchase_order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购订单ID（主键）',
  `purchase_order_no` varchar(50) DEFAULT NULL COMMENT '采购单号',
  `supplier_id` varchar(50) DEFAULT NULL COMMENT '供应商ID',
  `supplier_name` varchar(100) DEFAULT NULL COMMENT '供应商名称',
  `warehouse_id` varchar(50) DEFAULT NULL COMMENT '收货仓库ID',
  `purchase_date` date DEFAULT NULL COMMENT '采购日期',
  `order_status` varchar(20) DEFAULT NULL COMMENT '订单状态',
  `receive_status` varchar(20) DEFAULT NULL COMMENT '收货状态',
  `total_qty` int DEFAULT NULL COMMENT '总商品量',
  `total_amount` decimal(12,2) DEFAULT NULL COMMENT '总金额',
  `total_inbound_qty` int DEFAULT NULL COMMENT '总入库数',
  `diff_qty` int DEFAULT NULL COMMENT '差异数',
  `return_qty` int DEFAULT NULL COMMENT '退货数',
  `purchaser` varchar(50) DEFAULT NULL COMMENT '采购员',
  `receive_address` varchar(255) DEFAULT NULL COMMENT '收货地址',
  `audit_date` date DEFAULT NULL COMMENT '审核日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`purchase_order_id`),
  UNIQUE KEY `uk_purchase_order_no` (`purchase_order_no`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_warehouse_id` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库采购订单表';

-- 6. 仓库采购订单明细表 (warehouse_purchase_order_detail_table)
DROP TABLE IF EXISTS `warehouse_purchase_order_detail_table`;
CREATE TABLE `warehouse_purchase_order_detail_table` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID（主键）',
  `purchase_order_id` bigint DEFAULT NULL COMMENT '采购订单ID',
  `purchase_order_no` varchar(50) DEFAULT NULL COMMENT '采购单号',
  `product_sku_id` bigint DEFAULT NULL COMMENT '商品SKU ID',
  `product_sku_code` varchar(50) DEFAULT NULL COMMENT '商品编码',
  `product_sku_name` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `purchase_qty` int DEFAULT NULL COMMENT '采购数量',
  `box_qty` int DEFAULT NULL COMMENT '箱数',
  `standard_box_qty` int DEFAULT NULL COMMENT '标准装箱数量',
  `purchase_price` decimal(12,2) DEFAULT NULL COMMENT '采购单价',
  `purchase_amount` decimal(12,2) DEFAULT NULL COMMENT '采购金额',
  `inbound_qty` int DEFAULT NULL COMMENT '已入库数量',
  `return_qty` int DEFAULT NULL COMMENT '退货数',
  `diff_qty` int DEFAULT NULL COMMENT '差异数',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`detail_id`),
  KEY `idx_purchase_order_id` (`purchase_order_id`),
  KEY `idx_product_sku_id` (`product_sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库采购订单明细表';

-- 7. 仓库销售表 (warehouse_sales_table)
DROP TABLE IF EXISTS `warehouse_sales_table`;
CREATE TABLE `warehouse_sales_table` (
  `sales_id` bigint NOT NULL AUTO_INCREMENT COMMENT '销售ID（主键）',
  `internal_order_no` varchar(64) DEFAULT NULL COMMENT '内部订单号',
  `platform_order_no` varchar(64) DEFAULT NULL COMMENT '线上订单号',
  `warehouse_id` varchar(50) DEFAULT NULL COMMENT '发货仓ID',
  `warehouse_name` varchar(100) DEFAULT NULL COMMENT '发货仓名称',
  `store_id` varchar(50) DEFAULT NULL COMMENT '店铺ID',
  `store_name` varchar(100) DEFAULT NULL COMMENT '店铺名称',
  `buyer_id` varchar(50) DEFAULT NULL COMMENT '买家ID',
  `buyer_account` varchar(100) DEFAULT NULL COMMENT '买家账号',
  `order_status` varchar(20) DEFAULT NULL COMMENT '订单状态',
  `order_source` varchar(50) DEFAULT NULL COMMENT '订单来源',
  `order_date` date DEFAULT NULL COMMENT '订单日期',
  `pay_date` date DEFAULT NULL COMMENT '付款日期',
  `deliver_date` date DEFAULT NULL COMMENT '发货日期',
  `confirm_date` date DEFAULT NULL COMMENT '确认收货日期',
  `salesman` varchar(50) DEFAULT NULL COMMENT '业务员',
  `receiver_name` varchar(50) DEFAULT NULL COMMENT '收货人',
  `receiver_phone` varchar(20) DEFAULT NULL COMMENT '收货人电话',
  `province` varchar(50) DEFAULT NULL COMMENT '省',
  `city` varchar(50) DEFAULT NULL COMMENT '市',
  `district` varchar(50) DEFAULT NULL COMMENT '区县',
  `address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `express_company` varchar(50) DEFAULT NULL COMMENT '快递公司',
  `express_no` varchar(50) DEFAULT NULL COMMENT '快递单号',
  `total_sale_amount` decimal(12,2) DEFAULT NULL COMMENT '销售总金额',
  `total_deliver_amount` decimal(12,2) DEFAULT NULL COMMENT '实发总金额',
  `total_return_amount` decimal(12,2) DEFAULT NULL COMMENT '退货总金额',
  `total_profit` decimal(12,2) DEFAULT NULL COMMENT '销售总毛利',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`sales_id`),
  KEY `idx_internal_order_no` (`internal_order_no`),
  KEY `idx_platform_order_no` (`platform_order_no`),
  KEY `idx_warehouse_id` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库销售表';

-- 8. 仓库销售明细表 (warehouse_sales_detail_table)
DROP TABLE IF EXISTS `warehouse_sales_detail_table`;
CREATE TABLE `warehouse_sales_detail_table` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID（主键）',
  `sales_id` bigint DEFAULT NULL COMMENT '销售ID',
  `product_sku_id` bigint DEFAULT NULL COMMENT '商品SKU ID',
  `product_sku_code` varchar(50) DEFAULT NULL COMMENT '商品编码',
  `product_sku_name` varchar(200) DEFAULT NULL COMMENT '商品名称(冗余)',
  `sale_qty` int DEFAULT NULL COMMENT '销售数量',
  `zero_price_qty` int DEFAULT NULL COMMENT '价格为零的商品数量',
  `unit_price` decimal(12,2) DEFAULT NULL COMMENT '单价',
  `sale_amount` decimal(12,2) DEFAULT NULL COMMENT '销售金额',
  `sale_cost` decimal(12,2) DEFAULT NULL COMMENT '销售成本',
  `actual_deliver_qty` int DEFAULT NULL COMMENT '实发数量',
  `actual_deliver_amount` decimal(12,2) DEFAULT NULL COMMENT '实发金额',
  `actual_deliver_cost` decimal(12,2) DEFAULT NULL COMMENT '实发成本',
  `sale_profit` decimal(12,2) DEFAULT NULL COMMENT '销售毛利',
  `return_qty` int DEFAULT NULL COMMENT '退货数量',
  `actual_return_qty` int DEFAULT NULL COMMENT '实退数量',
  `return_amount` decimal(12,2) DEFAULT NULL COMMENT '退货金额',
  `return_cost` decimal(12,2) DEFAULT NULL COMMENT '退货成本',
  `actual_return_amount` decimal(12,2) DEFAULT NULL COMMENT '实退金额',
  `actual_return_cost` decimal(12,2) DEFAULT NULL COMMENT '实退成本',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`detail_id`),
  KEY `idx_sales_id` (`sales_id`),
  KEY `idx_product_sku_id` (`product_sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库销售明细表';

-- 9. 仓库售后表 (warehouse_aftersale_table)
DROP TABLE IF EXISTS `warehouse_aftersale_table`;
CREATE TABLE `warehouse_aftersale_table` (
  `aftersale_id` bigint NOT NULL AUTO_INCREMENT COMMENT '售后ID（主键）',
  `aftersale_no` varchar(50) DEFAULT NULL COMMENT '售后单号',
  `internal_order_no` varchar(64) DEFAULT NULL COMMENT '关联内部订单号',
  `platform_order_no` varchar(64) DEFAULT NULL COMMENT '关联线上订单号',
  `warehouse_id` varchar(50) DEFAULT NULL COMMENT '发货仓编码',
  `aftersale_category` varchar(50) DEFAULT NULL COMMENT '售后分类',
  `issue_type` varchar(50) DEFAULT NULL COMMENT '问题类型',
  `aftersale_status` varchar(20) DEFAULT NULL COMMENT '售后状态',
  `register_date` date DEFAULT NULL COMMENT '售后登记日期',
  `confirm_date` date DEFAULT NULL COMMENT '售后确认日期',
  `inbound_date` date DEFAULT NULL COMMENT '售后进仓日期',
  `inbound_no` varchar(50) DEFAULT NULL COMMENT '售后进仓单号',
  `product_sku_id` bigint DEFAULT NULL COMMENT '商品SKU ID',
  `product_sku_code` varchar(50) DEFAULT NULL COMMENT '商品编码',
  `product_sku_name` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `return_qty` int DEFAULT NULL COMMENT '退货数量',
  `refund_amount` decimal(12,2) DEFAULT NULL COMMENT '退款金额',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`aftersale_id`),
  UNIQUE KEY `uk_aftersale_no` (`aftersale_no`),
  KEY `idx_internal_order_no` (`internal_order_no`),
  KEY `idx_warehouse_id` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库售后表';

-- 10. 仓库进销汇总表 (warehouse_inout_summary_table)
DROP TABLE IF EXISTS `warehouse_inout_summary_table`;
CREATE TABLE `warehouse_inout_summary_table` (
  `summary_id` bigint NOT NULL AUTO_INCREMENT COMMENT '汇总ID（主键）',
  `product_sku_id` bigint DEFAULT NULL COMMENT '商品SKU ID',
  `warehouse_id` varchar(50) DEFAULT NULL COMMENT '仓库ID',
  `period_type` varchar(20) DEFAULT NULL COMMENT '周期类型(MONTH月/WEEK周)',
  `period_code` varchar(20) DEFAULT NULL COMMENT '周期编码(如:202503)',
  `opening_qty` int DEFAULT NULL COMMENT '期初数量',
  `closing_qty` int DEFAULT NULL COMMENT '期末数量',
  `purchase_return_qty` int DEFAULT NULL COMMENT '采购退货数量',
  `purchase_inbound_qty` int DEFAULT NULL COMMENT '采购进仓数量',
  `sales_outbound_qty` int DEFAULT NULL COMMENT '销售出仓数量',
  `sales_return_qty` int DEFAULT NULL COMMENT '销售退货数量',
  `inventory_check_qty` int DEFAULT NULL COMMENT '盘点数量',
  `transfer_out_qty` int DEFAULT NULL COMMENT '调拨出数量',
  `transfer_in_qty` int DEFAULT NULL COMMENT '调拨入数量',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`summary_id`),
  KEY `idx_warehouse_product` (`warehouse_id`, `product_sku_id`),
  KEY `idx_period` (`period_type`, `period_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库进销汇总表';

-- 11. 门店SKU供货关系表 (warehouse_store_supply_table)
DROP TABLE IF EXISTS `warehouse_store_supply_table`;
CREATE TABLE `warehouse_store_supply_table` (
  `supply_id` bigint NOT NULL AUTO_INCREMENT COMMENT '供货关系ID（主键）',
  `store_product_id` bigint DEFAULT NULL COMMENT '门店商品ID',
  `warehouse_id` varchar(50) DEFAULT NULL COMMENT '供货仓库ID',
  `supply_is_primary` tinyint DEFAULT NULL COMMENT '是否主供货仓(0否1是)',
  `supply_is_active` tinyint DEFAULT NULL COMMENT '是否供货(0否1是)',
  `supply_status` varchar(20) DEFAULT NULL COMMENT '供货状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`supply_id`),
  KEY `idx_store_product_id` (`store_product_id`),
  KEY `idx_warehouse_id` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店SKU供货关系表';


-- ============================================
-- 菜单 SQL
-- 父级目录 ID: 5049 (业务管理)
-- 仓库管理目录 ID: 5100 (新建)
-- ============================================

-- 仓库管理目录
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5100, '仓库管理', '', 1, 5, 5049, 'warehouse', 'ep:box', '#', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 仓库信息管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5101, '仓库信息管理', '', 2, 1, 5100, 'warehouse', 'ep:office', 'business/warehouse/index', 'Warehouse', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 供应商管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5102, '供应商管理', '', 2, 2, 5100, 'warehouse-supplier', 'ep:company', 'business/warehouse/supplier/index', 'WarehouseSupplier', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 仓库商品管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5103, '仓库商品管理', '', 2, 3, 5100, 'warehouse-product', 'ep:goods', 'business/warehouse/product/index', 'WarehouseProduct', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 仓库库存管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5104, '仓库库存管理', '', 2, 4, 5100, 'warehouse-stock', 'ep:document', 'business/warehouse/stock/index', 'WarehouseStock', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 采购订单管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5105, '采购订单管理', '', 2, 5, 5100, 'warehouse-purchase', 'ep:shopping', 'business/warehouse/purchase/index', 'WarehousePurchase', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 销售订单管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5106, '销售订单管理', '', 2, 6, 5100, 'warehouse-sales', 'ep:sell', 'business/warehouse/sales/index', 'WarehouseSales', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 售后管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5107, '售后管理', '', 2, 7, 5100, 'warehouse-aftersale', 'ep:warning', 'business/warehouse/aftersale/index', 'WarehouseAftersale', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 进销汇总
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5108, '进销汇总', '', 2, 8, 5100, 'warehouse-inout-summary', 'ep:data-analysis', 'business/warehouse/inout-summary/index', 'WarehouseInoutSummary', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 仓库按钮权限
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5111, '仓库查询', 'business:warehouse:query', 3, 1, 5101, '', '#', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5112, '仓库创建', 'business:warehouse:create', 3, 2, 5101, '', '#', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5113, '仓库更新', 'business:warehouse:update', 3, 3, 5101, '', '#', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5114, '仓库删除', 'business:warehouse:delete', 3, 4, 5101, '', '#', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5115, '仓库导出', 'business:warehouse:export', 3, 5, 5101, '', '#', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (5116, '仓库导入', 'business:warehouse:import', 3, 6, 5101, '', '#', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
