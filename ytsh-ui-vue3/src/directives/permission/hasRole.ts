import type { App } from 'vue'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n() // 国际化

export function hasRole(app: App<Element>) {
  app.directive('hasRole', (el, binding) => {
    const { value } = binding
    const super_admin = 'super_admin'
    const userStore = useUserStore()
    const roles = userStore.roles

    if (value && value instanceof Array && value.length > 0) {
      const roleFlag = value

      const hasRole = roles.some((role: string) => {
        return super_admin === role || roleFlag.includes(role)
      })

      if (!hasRole) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error(t('permission.hasRole'))
    }
  })
}
