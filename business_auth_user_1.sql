-- business 模块权限补齐（用户 ID = 1）
-- 目的：确保本次收口到正式认证后的接口对用户 1 仍可正常使用
-- 本 SQL 复用现有权限标识：
--   business:table:query
--   business:table:update
--   business:store-product:query
--
-- 说明：当前项目 RBAC 采用 用户-角色-菜单/权限 的关系模型。
-- 因此这里采用“为用户 1 已拥有的角色补齐菜单权限”的方式。
-- 如果用户 1 已经具备这些权限，INSERT IGNORE / NOT EXISTS 会避免重复数据。

-- 1) 可先查看用户 1 当前角色
SELECT *
FROM system_user_role
WHERE user_id = 1 AND deleted = FALSE;

-- 2) 查看这三个权限对应的菜单/权限记录
SELECT id, name, permission, type, parent_id, status
FROM system_menu
WHERE deleted = FALSE
  AND permission IN (
    'business:table:query',
    'business:table:update',
    'business:store-product:query'
  );

-- 3) 将这三个权限补到用户 1 已绑定的全部角色上
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT ur.role_id,
       m.id,
       'script',
       NOW(),
       'script',
       NOW(),
       FALSE,
       0
FROM system_user_role ur
JOIN system_menu m
  ON m.deleted = FALSE
 AND m.permission IN (
      'business:table:query',
      'business:table:update',
      'business:store-product:query'
 )
LEFT JOIN system_role_menu rm
  ON rm.role_id = ur.role_id
 AND rm.menu_id = m.id
 AND rm.deleted = FALSE
WHERE ur.user_id = 1
  AND ur.deleted = FALSE
  AND rm.id IS NULL;

-- 4) 执行后复核：确认用户 1 的角色已经拥有所需权限
SELECT ur.user_id,
       ur.role_id,
       m.id   AS menu_id,
       m.name AS menu_name,
       m.permission
FROM system_user_role ur
JOIN system_role_menu rm
  ON rm.role_id = ur.role_id
 AND rm.deleted = FALSE
JOIN system_menu m
  ON m.id = rm.menu_id
 AND m.deleted = FALSE
WHERE ur.user_id = 1
  AND ur.deleted = FALSE
  AND m.permission IN (
    'business:table:query',
    'business:table:update',
    'business:store-product:query'
  )
ORDER BY ur.role_id, m.permission;
