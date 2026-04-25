import test from 'node:test'
import assert from 'node:assert/strict'

import {
  assignContactRowKeys,
  createContactRow,
  deleteContactRowByKey,
  getOptionalSubTableData
} from './storeFormLogic.ts'

test('删除联系人时按稳定行标识删除目标行', () => {
  const rows = assignContactRowKeys([
    { contactId: 101, contactName: 'A' },
    { contactId: 102, contactName: 'B' }
  ])

  const result = deleteContactRowByKey(rows, rows[0].__rowKey)

  assert.equal(result.length, 1)
  assert.equal(result[0].contactId, 102)
  assert.equal(result[0].contactName, 'B')
})

test('新增联系人会生成临时稳定行标识', () => {
  const row = createContactRow('S001')

  assert.equal(row.storeId, 'S001')
  assert.equal(typeof row.__rowKey, 'string')
  assert.ok(row.__rowKey.startsWith('tmp-'))
})

test('一对一子表存在数值 0 时不应被判空删除', () => {
  const data = getOptionalSubTableData(
    { buildingArea: 0, coldStorageArea: undefined },
    ['buildingArea', 'coldStorageArea']
  )

  assert.deepEqual(data, { buildingArea: 0, coldStorageArea: undefined })
})

test('一对一子表字段全空时返回 null 表示删除', () => {
  const data = getOptionalSubTableData(
    { franchiseeName: '', franchiseePhone: undefined, franchiseeFee: null },
    ['franchiseeName', 'franchiseePhone', 'franchiseeFee']
  )

  assert.equal(data, null)
})
