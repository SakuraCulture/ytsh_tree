-- ============================================
-- 饿了么门店商品全量同步任务表结构 SQL (MySQL)
-- ============================================

DROP TABLE IF EXISTS `ele_store_goods_full_sync_task_store`;
DROP TABLE IF EXISTS `ele_store_goods_full_sync_task`;

CREATE TABLE `ele_store_goods_full_sync_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_no` varchar(64) NOT NULL COMMENT '任务编号',
  `scope` varchar(32) NOT NULL COMMENT '同步范围：CURRENT_STORE/ALL_OPEN_STORES',
  `merchant_code` varchar(64) DEFAULT NULL COMMENT '商家编码',
  `erp_store_code` varchar(64) DEFAULT NULL COMMENT 'ERP门店编码',
  `test_mode` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否测试模式',
  `status` varchar(32) NOT NULL COMMENT '任务状态：PENDING/RUNNING/SUCCESS/PARTIAL_FAIL/FAILED/CANCELLED',
  `total_store_count` int NOT NULL DEFAULT 0 COMMENT '总门店数',
  `finished_store_count` int NOT NULL DEFAULT 0 COMMENT '已完成门店数',
  `total_page_count` int NOT NULL DEFAULT 0 COMMENT '总页数',
  `finished_page_count` int NOT NULL DEFAULT 0 COMMENT '已完成页数',
  `total_sku_count` int NOT NULL DEFAULT 0 COMMENT '总SKU数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数',
  `governance_count` int NOT NULL DEFAULT 0 COMMENT '待治理数',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_task_no` (`task_no`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_scope_status` (`scope`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饿了么门店商品全量同步任务';

CREATE TABLE `ele_store_goods_full_sync_task_store` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务门店明细ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `task_no` varchar(64) NOT NULL COMMENT '任务编号',
  `store_id` varchar(64) DEFAULT NULL COMMENT '门店ID',
  `store_name` varchar(128) DEFAULT NULL COMMENT '门店名称',
  `merchant_code` varchar(64) DEFAULT NULL COMMENT '商家编码',
  `erp_store_code` varchar(64) NOT NULL COMMENT 'ERP门店编码',
  `platform_store_id` varchar(64) DEFAULT NULL COMMENT '平台门店ID',
  `status` varchar(32) NOT NULL COMMENT '明细状态：PENDING/RUNNING/SUCCESS/FAILED/CANCELLED',
  `current_page` int NOT NULL DEFAULT 0 COMMENT '当前页',
  `total_page` int NOT NULL DEFAULT 0 COMMENT '总页数',
  `page_size` int NOT NULL DEFAULT 20 COMMENT '每页条数',
  `total_sku_count` int NOT NULL DEFAULT 0 COMMENT '总SKU数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数',
  `governance_count` int NOT NULL DEFAULT 0 COMMENT '待治理数',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_task_no` (`task_no`),
  KEY `idx_store_status` (`erp_store_code`, `status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饿了么门店商品全量同步任务门店明细';
