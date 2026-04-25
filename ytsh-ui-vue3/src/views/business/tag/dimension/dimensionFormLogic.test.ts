import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildParentOptions,
  collectDescendantIds,
  filterTreeOptionsByDomain,
  getNextLevelByParent,
  hasChildren
} from './dimensionFormLogic.ts'

const dimensions = [
  { id: 1, domainType: 'PRODUCT', parentId: 0, level: 1, name: '一级A', code: 'l1-a' },
  { id: 2, domainType: 'PRODUCT', parentId: 1, level: 2, name: '二级A', code: 'l2-a' },
  { id: 3, domainType: 'PRODUCT', parentId: 2, level: 3, name: '三级A', code: 'l3-a' },
  { id: 4, domainType: 'PRODUCT', parentId: 1, level: 2, name: '二级B', code: 'l2-b' },
  { id: 5, domainType: 'STORE', parentId: 0, level: 1, name: '门店一级', code: 'store-l1' }
]

test('collectDescendantIds 返回当前节点全部后代', () => {
  const result = collectDescendantIds(dimensions, 1)

  assert.deepEqual([...result].sort((a, b) => a - b), [2, 3, 4])
})

test('buildParentOptions 会排除三级节点和当前节点后代', () => {
  const options = buildParentOptions(dimensions, 'PRODUCT', 1)
  const children = options[0].children || []

  assert.equal(children.length, 0)
})

test('buildParentOptions 创建态只保留同域且可挂子级的节点', () => {
  const options = buildParentOptions(dimensions, 'PRODUCT')
  const firstLevel = options[0].children || []

  assert.equal(firstLevel.length, 1)
  assert.equal(firstLevel[0].id, 1)
  assert.equal((firstLevel[0].children || []).length, 2)
  assert.equal((firstLevel[0].children || [])[0].id, 2)
  assert.equal((firstLevel[0].children || [])[1].id, 4)
})

test('getNextLevelByParent 根据父节点层级计算下一层级', () => {
  assert.equal(getNextLevelByParent(dimensions, 0), 1)
  assert.equal(getNextLevelByParent(dimensions, 1), 2)
  assert.equal(getNextLevelByParent(dimensions, 2), 3)
})

test('hasChildren 能识别节点是否存在子节点', () => {
  assert.equal(hasChildren(dimensions, 1), true)
  assert.equal(hasChildren(dimensions, 3), false)
})

test('filterTreeOptionsByDomain 会按对象域过滤查询树', () => {
  const options = filterTreeOptionsByDomain(dimensions, 'STORE')
  const children = options[0].children || []

  assert.equal(children.length, 1)
  assert.equal(children[0].id, 5)
})
