import test from 'node:test'
import assert from 'node:assert/strict'

import { shouldEnableEslintPlugin } from './index.ts'

test('开发模式不启用 vite-plugin-eslint', () => {
  assert.equal(shouldEnableEslintPlugin('serve'), false)
})

test('构建模式启用 vite-plugin-eslint', () => {
  assert.equal(shouldEnableEslintPlugin('build'), true)
})
