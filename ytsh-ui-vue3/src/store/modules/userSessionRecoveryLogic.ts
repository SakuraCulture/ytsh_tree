import type { NormalizedUserSession, UserSessionPayload } from './userSessionLogic'

interface RecoverUserSessionOptions {
  hasAccessToken: boolean
  cachedUserInfo: UserSessionPayload | null | undefined
  getInfo: () => Promise<UserSessionPayload | null | undefined>
  normalizeUserSession: (payload?: UserSessionPayload | null) => NormalizedUserSession | null
  onRemoveToken: () => void
  onResetState: () => void
  onDeleteUserCache: () => void
  onCacheUserSession: (value: NormalizedUserSession) => void
}

const clearSession = (options: RecoverUserSessionOptions) => {
  options.onRemoveToken()
  options.onResetState()
  options.onDeleteUserCache()
  return null
}

export const recoverUserSession = async (
  options: RecoverUserSessionOptions
): Promise<NormalizedUserSession | null> => {
  if (!options.hasAccessToken) {
    options.onResetState()
    return null
  }

  let userInfo = options.cachedUserInfo
  if (!userInfo) {
    try {
      userInfo = await options.getInfo()
    } catch {
      return clearSession(options)
    }
  } else {
    try {
      userInfo = await options.getInfo()
    } catch {
      userInfo = options.cachedUserInfo
    }
  }

  const normalizedUserInfo = options.normalizeUserSession(userInfo)
  if (!normalizedUserInfo) {
    return clearSession(options)
  }

  options.onCacheUserSession(normalizedUserInfo)
  return normalizedUserInfo
}
