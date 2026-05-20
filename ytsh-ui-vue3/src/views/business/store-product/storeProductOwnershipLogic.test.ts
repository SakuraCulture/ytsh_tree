import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const {
  buildStoreProductSavePayload,
  fillStoreProductFormData,
  formatOwnershipLabel,
  getOwnershipOptions
} = await import(new URL('./storeProductOwnershipLogic.ts', import.meta.url).href)

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

function readLocalFile(fileName: string) {
  return fs.readFileSync(path.join(__dirname, fileName), 'utf8')
}

test('fillStoreProductFormData 优先保留接口返回的原始 ownership 值', () => {
  const formData = fillStoreProductFormData(
    {
      storeProductId: 'SP001',
      storeId: 'S001',
      productSkuId: 'SKU001',
      productAttribution: '入店',
      posStatus: '1',
      storeRetailPrice: 99.5,
      enterShopStatus: 1,
      firstEnterShopDate: '2026-05-20'
    },
    {
      storeId: 'S001',
      productAttribution: '错误旧值'
    }
  )

  assert.equal(formData.productAttribution, '入店')
  assert.equal(formData.posStatus, 1)
})

test('buildStoreProductSavePayload 把原始 ownership 值原样写到保存字段', () => {
  const payload = buildStoreProductSavePayload(
    {
      storeProductId: 'SP001',
      storeId: 'S001',
      productSkuId: 'SKU001',
      productAttribution: '入店',
      storeRetailPrice: 88,
      firstEnterShopDate: '2026-05-20',
      posStatus: 1,
      enterShopStatus: 1
    },
    'update'
  )

  assert.equal(payload.storeProductOwnership, '入店')
  assert.equal(payload.storeProductPosStatus, '1')
  assert.equal(payload.storeProductId, 'SP001')
})

test('getOwnershipOptions 至少包含当前已知原始值，并兼容未知值回显', () => {
  const knownOptions = getOwnershipOptions()
  assert.ok(knownOptions.some((item) => item.value === '入店' && item.label === '入店'))

  const mergedOptions = getOwnershipOptions('联营')
  assert.ok(mergedOptions.some((item) => item.value === '联营' && item.label === '联营'))
})

test('formatOwnershipLabel 对未知原始值保持透传', () => {
  assert.equal(formatOwnershipLabel('入店'), '入店')
  assert.equal(formatOwnershipLabel('联营'), '联营')
  assert.equal(formatOwnershipLabel(undefined), '-')
})

test('store-product 页面不再把 ownership 协议值硬编码成 HQ 或 STORE', () => {
  const page = readLocalFile('index.vue')
  const form = readLocalFile('StoreProductForm.vue')

  assert.doesNotMatch(page, /value=\"HQ\"/)
  assert.doesNotMatch(page, /value=\"STORE\"/)
  assert.doesNotMatch(form, /value=\"HQ\"/)
  assert.doesNotMatch(form, /value=\"STORE\"/)
})
