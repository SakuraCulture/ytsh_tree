import test from 'node:test'
import assert from 'node:assert/strict'

import { recoverUserSession } from './userSessionRecoveryLogic.ts'

test('首次拉取用户信息失败时清空会话并返回 null', async () => {
  let getInfoCalls = 0
  let removeTokenCalls = 0
  let resetStateCalls = 0
  let deleteUserCacheCalls = 0

  const result = await recoverUserSession({
    hasAccessToken: true,
    cachedUserInfo: null,
    getInfo: async () => {
      getInfoCalls += 1
      throw new Error('network error')
    },
    normalizeUserSession: () => {
      throw new Error('should not normalize after failure')
    },
    onRemoveToken: () => {
      removeTokenCalls += 1
    },
    onResetState: () => {
      resetStateCalls += 1
    },
    onDeleteUserCache: () => {
      deleteUserCacheCalls += 1
    },
    onCacheUserSession: () => {
      throw new Error('should not cache after failure')
    }
  })

  assert.equal(result, null)
  assert.equal(getInfoCalls, 1)
  assert.equal(removeTokenCalls, 1)
  assert.equal(resetStateCalls, 1)
  assert.equal(deleteUserCacheCalls, 1)
})

test('有缓存时首次拉取失败会回退缓存并继续恢复会话', async () => {
  const cachedPayload = {
    permissions: ['system:user:list'],
    roles: ['admin'],
    user: {
      id: 1,
      avatar: '',
      nickname: '管理员',
      deptId: 2
    },
    menus: [{ name: '首页' }]
  }
  let cachedValue: unknown = null

  const result = await recoverUserSession({
    hasAccessToken: true,
    cachedUserInfo: cachedPayload,
    getInfo: async () => {
      throw new Error('temporary backend error')
    },
    normalizeUserSession: (payload) => {
      assert.deepEqual(payload, cachedPayload)
      return cachedPayload
    },
    onRemoveToken: () => {
      throw new Error('should not remove token when cached session exists')
    },
    onResetState: () => {
      throw new Error('should not reset state when cached session exists')
    },
    onDeleteUserCache: () => {
      throw new Error('should not clear cache when cached session exists')
    },
    onCacheUserSession: (value) => {
      cachedValue = value
    }
  })

  assert.deepEqual(result, cachedPayload)
  assert.deepEqual(cachedValue, cachedPayload)
})

test('无缓存但首次拉取成功时返回归一化会话', async () => {
  const normalized = {
    permissions: ['system:user:list'],
    roles: ['admin'],
    user: {
      id: 1,
      avatar: '',
      nickname: '管理员',
      deptId: 2
    },
    menus: [{ name: '首页' }]
  }
  let cachedValue: unknown = null

  const result = await recoverUserSession({
    hasAccessToken: true,
    cachedUserInfo: null,
    getInfo: async () => ({ raw: true }),
    normalizeUserSession: (payload) => {
      assert.deepEqual(payload, { raw: true })
      return normalized
    },
    onRemoveToken: () => {
      throw new Error('should not remove token on success')
    },
    onResetState: () => {
      throw new Error('should not reset state on success')
    },
    onDeleteUserCache: () => {
      throw new Error('should not clear cache on success')
    },
    onCacheUserSession: (value) => {
      cachedValue = value
    }
  })

  assert.deepEqual(result, normalized)
  assert.deepEqual(cachedValue, normalized)
})
