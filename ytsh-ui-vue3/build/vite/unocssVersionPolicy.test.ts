import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const packageJson = JSON.parse(
  fs.readFileSync('C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3/package.json', 'utf8')
)

const devDependencies = packageJson.devDependencies ?? {}

function getVersion(name) {
  return devDependencies[name]
}

test('UnoCSS 运行与 lint 相关依赖统一在 0.58 版本线', () => {
  assert.equal(getVersion('unocss'), '^0.58.5')
  assert.equal(getVersion('@unocss/transformer-variant-group'), '^0.58.5')
  assert.equal(getVersion('@unocss/eslint-plugin'), '^0.58.5')
})

test('UnoCSS eslint 配置与插件版本保持同一主次版本线', () => {
  assert.equal(getVersion('@unocss/eslint-config'), '^0.58.5')
})
