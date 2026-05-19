const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const projectRoot = path.resolve(__dirname, '../../../..')

function readFile(relativePath) {
  return fs.readFileSync(path.join(projectRoot, relativePath), 'utf8')
}

test('正式商品库存展开卡复用共享库存卡片', () => {
  const file = readFile('src/views/business/store-product/components/StockExpandCard.vue')

  assert.match(file, /InventoryMetricsCard/)
  assert.match(file, /adaptStoreProductMetrics/)
})

test('影子商品展开区不再固定显示暂无库存明细', () => {
  const page = readFile('src/views/business/store-product/index.vue')

  assert.doesNotMatch(page, /影子商品暂无库存明细/)
  assert.match(page, /ShadowInventoryExpandCard/)
})

test('影子商品库存卡支持快照与刷新入口', () => {
  const card = readFile('src/views/business/store-product/components/ShadowInventoryExpandCard.vue')

  assert.match(card, /InventoryMetricsCard/)
  assert.match(card, /queryStoreInventory/)
  assert.match(card, /getShadow/)
  assert.match(card, /platformStoreId/)
  assert.match(card, /findMatchingInventoryRow/)
  assert.doesNotMatch(card, /inventoryRows\?\.\[0\]/)
})
