import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeUserSession } from './userSessionLogic.ts'

test('空权限信息返回 null', () => {
  assert.equal(normalizeUserSession(null), null)
})

test('缺省字段会被归一化为安全默认值', () => {
  assert.deepEqual(
    normalizeUserSession({
      permissions: null,
      roles: undefined,
      user: null,
      menus: undefined
    }),
    {
      permissions: [],
      roles: [],
      user: {
        id: 0,
        avatar: '',
        nickname: '',
        deptId: 0
      },
      menus: []
    }
  )
})
