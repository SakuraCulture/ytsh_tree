import { hasPermission } from '@/directives/permission/hasPermi'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n() // 国际化

/**
 * 字符权限校验
 * @param {Array} value 校验值
 * @returns {Boolean}
 */
export function checkPermi(permission: string[]) {
  return hasPermission(permission)
}

/**
 * 角色权限校验
 * @param {string[]} value 校验值
 * @returns {Boolean}
 */
export function checkRole(value: string[]) {
  if (value && value instanceof Array && value.length > 0) {
    const permissionRoles = value
    const super_admin = 'super_admin'
    const userStore = useUserStore()
    const roles = userStore.roles
    const hasRole = roles.some((role: string) => {
      return super_admin === role || permissionRoles.includes(role)
    })
    return !!hasRole
  } else {
    console.error(t('permission.hasRole'))
    return false
  }
}
