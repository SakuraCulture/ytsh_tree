import test from 'node:test'
import assert from 'node:assert/strict'

import { loadOrderDetail, normalizeBillInfoResponse } from './orderPageLogic.ts'

test('账单汇总存在时即使明细为空也保留账单信息', () => {
  const result = normalizeBillInfoResponse({
    orderId: 'O-1',
    totalBillAmount: 18.88,
    totalStatus: 0,
    billDetails: []
  })

  assert.deepEqual(result, {
    orderId: 'O-1',
    totalBillAmount: 18.88,
    totalStatus: 0,
    billDetails: []
  })
})

test('加载订单详情时应调用详情接口并用详情结果覆盖当前订单', async () => {
  const order = {
    orderId: 'O-2',
    platformStoreId: 'PS-1',
    merchantCode: 'M-1',
    erpStoreCode: 'ES-1',
    goodsList: [],
    billInfo: {
      orderId: 'O-2',
      totalBillAmount: 12.34,
      totalStatus: 1,
      billDetails: []
    }
  }

  const requests: any[] = []
  const detailFetcher = async (params) => {
    requests.push(params)
    return {
      orderId: 'O-2',
      subOrders: [{ skuCode: 'SKU-1' }],
      merchantCode: 'M-1-DETAIL'
    }
  }
  const normalizeOrder = (payload) => ({
    ...payload,
    goodsList: payload.subOrders,
    merchantCode: payload.merchantCode
  })

  const result = await loadOrderDetail(detailFetcher, order, normalizeOrder)

  assert.deepEqual(requests, [
    {
      orderId: 'O-2',
      platformStoreId: 'PS-1',
      merchantCode: 'M-1',
      erpStoreCode: 'ES-1'
    }
  ])
  assert.deepEqual(result.goodsList, [{ skuCode: 'SKU-1' }])
  assert.equal(result.merchantCode, 'M-1-DETAIL')
  assert.deepEqual(result.billInfo, order.billInfo)
})
