import test from 'node:test'
import assert from 'node:assert/strict'

import { isValidExpressionJsonObject } from './virtualFormLogic.ts'

test('合法 JSON 对象返回 true', () => {
  assert.equal(isValidExpressionJsonObject('{"op":"and","conditions":[]}'), true)
})

test('JSON 数组返回 false', () => {
  assert.equal(isValidExpressionJsonObject('[]'), false)
})

test('非法 JSON 返回 false', () => {
  assert.equal(isValidExpressionJsonObject('{'), false)
})

test('空值返回 false', () => {
  assert.equal(isValidExpressionJsonObject(''), false)
})
