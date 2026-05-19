import test from 'node:test'
import assert from 'node:assert/strict'

import { shouldLoadUnreadMessages } from './messageLogic.ts'

test('仅当存在 accessToken 且用户态已初始化时才允许拉取未读通知', () => {
  assert.equal(shouldLoadUnreadMessages({ hasAccessToken: false, isUserReady: false }), false)
  assert.equal(shouldLoadUnreadMessages({ hasAccessToken: true, isUserReady: false }), false)
  assert.equal(shouldLoadUnreadMessages({ hasAccessToken: false, isUserReady: true }), false)
  assert.equal(shouldLoadUnreadMessages({ hasAccessToken: true, isUserReady: true }), true)
})
