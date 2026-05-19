const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const projectRoot = path.resolve(__dirname, '../../..')

function readFile(relativePath) {
  return fs.readFileSync(path.join(projectRoot, relativePath), 'utf8')
}

test('库存查询菜单指向的页面文件必须存在，避免动态路由解析为空白页', () => {
  const pagePath = path.join(projectRoot, 'src/views/ele/store-inventory/index.vue')

  assert.equal(fs.existsSync(pagePath), true)
})

test('库存查询页面提供基础页面标识，便于和菜单 componentName 对齐', () => {
  const pagePath = path.join(projectRoot, 'src/views/ele/store-inventory/index.vue')

  assert.equal(fs.existsSync(pagePath), true)

  const page = readFile('src/views/ele/store-inventory/index.vue')
  assert.match(page, /defineOptions\(\{\s*name:\s*'EleStoreInventory'\s*\}\)/)
  assert.match(page, /库存查询/)
})

test('库存查询页不再是空壳，而是接入查询工作台与共享库存卡片', () => {
  const page = readFile('src/views/ele/store-inventory/index.vue')

  assert.match(page, /InventoryMetricsCard/)
  assert.match(page, /handleQuery/)
  assert.match(page, /summaryItems/)
  assert.match(page, /platformStoreId/)
  assert.match(page, /findMatchingInventoryRow/)
  assert.doesNotMatch(page, /inventoryRows\?\.\[0\]/)
  assert.doesNotMatch(page, /el-empty\s+description="库存查询"/)
})
