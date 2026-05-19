import test from 'node:test'
import assert from 'node:assert/strict'

import {
  adaptEleInventoryMetrics,
  adaptShadowSnapshotMetrics,
  adaptStoreProductMetrics,
  buildInventoryQueryRequest,
  findMatchingInventoryRow,
  parseInventoryCodes
} from './storeInventoryPageLogic.ts'

test('parseInventoryCodes trims blanks and keeps first-seen order', () => {
  assert.deepEqual(parseInventoryCodes(' SKU-1\nSKU-1, SKU-2  '), ['SKU-1', 'SKU-2'])
})

test('buildInventoryQueryRequest requires at least one query key', () => {
  assert.throws(
    () =>
      buildInventoryQueryRequest({
        platformStoreId: '',
        merchantCode: 'merchant-1',
        erpStoreCode: 'store-1',
        skuCodesText: ' ',
        subSkuCodesText: ' '
      }),
    /至少提供一个 SKU 查询条件/
  )
})

test('buildInventoryQueryRequest keeps trimmed platform store id', () => {
  assert.deepEqual(
    buildInventoryQueryRequest({
      platformStoreId: ' ELE_STORE_001 ',
      merchantCode: 'merchant-1',
      erpStoreCode: 'store-1',
      skuCodesText: 'SKU-1',
      subSkuCodesText: ' '
    }),
    {
      platformStoreId: 'ELE_STORE_001',
      merchantCode: 'merchant-1',
      erpStoreCode: 'store-1',
      skuCodes: ['SKU-1']
    }
  )
})

test('buildInventoryQueryRequest ignores sub sku input completely', () => {
  assert.deepEqual(
    buildInventoryQueryRequest({
      platformStoreId: '185154',
      merchantCode: 'LY_TT_QQD',
      erpStoreCode: '185154',
      skuCodesText: '5005484192178',
      subSkuCodesText: 'SHOULD-NOT-BE-SENT'
    }),
    {
      platformStoreId: '185154',
      merchantCode: 'LY_TT_QQD',
      erpStoreCode: '185154',
      skuCodes: ['5005484192178']
    }
  )
})

test('buildInventoryQueryRequest rejects sub sku only input', () => {
  assert.throws(
    () =>
      buildInventoryQueryRequest({
        platformStoreId: '185154',
        merchantCode: 'LY_TT_QQD',
        erpStoreCode: '185154',
        skuCodesText: ' ',
        subSkuCodesText: '5005484192178'
      }),
    /至少提供一个 SKU 查询条件/
  )
})

test('adaptStoreProductMetrics keeps formal stock shape', () => {
  assert.equal(
    adaptStoreProductMetrics({
      storeProductId: 'formal-1',
      inventoryQuantity: 10,
      availableQuantity: 8,
      inTransitQuantity: 1,
      frozenQuantity: 2
    }).totalStock,
    10
  )
})

test('adaptEleInventoryMetrics maps realtime stock fields', () => {
  assert.equal(
    adaptEleInventoryMetrics({
      physicalStockTotalAmount: 9,
      availableForSale: 7,
      physicalStockIntransitAmount: 1,
      reservedAmount: 2,
      lastQueryTime: '2026-05-14 09:00:00'
    }).totalStock,
    9
  )
})

test('adaptShadowSnapshotMetrics keeps shadow snapshot stock fields', () => {
  const metrics = adaptShadowSnapshotMetrics({
    physicalStockTotalAmount: 11,
    physicalStockAvailableAmount: 8,
    physicalStockIntransitAmount: 2,
    reservedAmount: 1,
    lastQueryTime: '2026-05-14 10:00:00'
  })

  assert.equal(metrics.source, 'snapshot')
  assert.equal(metrics.totalStock, 11)
  assert.equal(metrics.availableStock, 8)
})

test('findMatchingInventoryRow prefers exact sku and subSku key match', () => {
  const row = findMatchingInventoryRow(
    [
      { skuCode: 'SKU-1', subSkuCode: 'SUB-OTHER' },
      { skuCode: 'SKU-1', subSkuCode: 'SUB-1', physicalStockTotalAmount: 9 }
    ],
    {
      skuCode: 'SKU-1',
      subSkuCode: 'SUB-1'
    }
  )

  assert.equal(row?.physicalStockTotalAmount, 9)
})
