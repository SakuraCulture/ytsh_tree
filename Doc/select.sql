-- 按渠道门店名称统计（渠道）
SELECT 
    channel_source_name AS 渠道,
    COUNT(*) AS 订单数量
FROM order_table
WHERE deleted = 0
    AND create_time >= UNIX_TIMESTAMP('2026-03-01 00:00:00') * 1000
    AND create_time < UNIX_TIMESTAMP('2026-03-01 23:59:59') * 1000
GROUP BY channel_source_name
ORDER BY 订单数量 DESC;
-- 删除订单


-- 1、先确定当日订单数量
SELECT COUNT(*) AS 待删除数量
FROM order_table
WHERE deleted = 0
    AND create_time >= UNIX_TIMESTAMP('2026-03-01 00:00:00') * 1000
    AND create_time < UNIX_TIMESTAMP('2026-03-01 23:59:59') * 1000;
-- 2、逻辑删除
UPDATE order_table
SET deleted = 1,
    updater = 'admin',
    update_time = UNIX_TIMESTAMP() * 1000
WHERE deleted = 0
    AND create_time >= UNIX_TIMESTAMP('2006-03-01 00:00:00') * 1000
    AND create_time < UNIX_TIMESTAMP('2006-03-01 23:59:59') * 1000;
-- 3、恢复逻辑删除
UPDATE order_table 
SET deleted = 0, 
    updater = 'admin', 
    update_time = UNIX_TIMESTAMP() * 1000 
WHERE deleted = 1 
    AND create_time >= UNIX_TIMESTAMP('2026-03-01 00:00:00') * 1000 
    AND create_time < UNIX_TIMESTAMP('2026-03-01 23:59:59') * 1000;
-- 4、物理删除
DELETE FROM order_table 
WHERE deleted = 0 
    AND create_time >= UNIX_TIMESTAMP('2026-03-01 00:00:00') * 1000 
    AND create_time < UNIX_TIMESTAMP('2026-03-02 00:00:00') * 1000;
		
-- 查询3月1日各门店订单数量的 SQL
SELECT 
    order_from AS 订单来源,
    channel_source_name AS 渠道门店, 
    COUNT(*) AS 订单数量, 
    SUM(pay_fee) AS 实付总额
FROM order_table 
WHERE deleted = 0 
    AND create_time >= UNIX_TIMESTAMP('2026-03-01 00:00:00') * 1000 
    AND create_time < UNIX_TIMESTAMP('2026-03-01 23:59:59') * 1000 
GROUP BY order_from, channel_source_name 
ORDER BY order_from, 订单数量 DESC;

-- 查询订单
SELECT * FROM order_table WHERE order_id ='5000034417220780215'
-- 查询订单渠道类型
SELECT * FROM order_platform_table WHERE order_id ='5000034417220780215'