-- 修复 order_table 缺失字段问题
-- 执行时间: 2026-04-27

-- 添加 sub_orders_json 字段
ALTER TABLE `order_table` ADD COLUMN `sub_orders_json` TEXT COMMENT '子订单JSON' AFTER `deleted`;

-- 添加 discounts_json 字段
ALTER TABLE `order_table` ADD COLUMN `discounts_json` TEXT COMMENT '优惠信息JSON' AFTER `sub_orders_json`;
