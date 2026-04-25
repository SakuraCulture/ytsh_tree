export interface UserSessionPayload {
  permissions?: string[] | null
  roles?: string[] | null
  user?: {
    id?: number
    avatar?: string
    nickname?: string
    deptId?: number
  } | null
  menus?: unknown[] | null
}

export interface NormalizedUserSession {
  permissions: string[]
  roles: string[]
  user: {
    id: number
    avatar: string
    nickname: string
    deptId: number
  }
  menus: unknown[]
}

export const normalizeUserSession = (payload?: UserSessionPayload | null): NormalizedUserSession | null => {
  if (!payload) {
    return null
  }

  return {
    permissions: payload.permissions || [],
    roles: payload.roles || [],
    user: {
      id: payload.user?.id || 0,
      avatar: payload.user?.avatar || '',
      nickname: payload.user?.nickname || '',
      deptId: payload.user?.deptId || 0
    },
    menus: payload.menus || []
  }
}
