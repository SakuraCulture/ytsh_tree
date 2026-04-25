import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildDimensionTreeOptions,
  filterDimensionsByDomain,
  getDimensionNameMap
} from './valuePageLogic.ts'

const dimensions = [
  { id: 1, domainType: 'PRODUCT', parentId: 0, level: 1, name: '商品一级', code: 'p1' },
  { id: 2, domainType: 'PRODUCT', parentId: 1, level: 2, name: '商品二级', code: 'p2' },
  { id: 3, domainType: 'PRODUCT', parentId: 2, level: 3, name: '商品三级A', code: 'p3a' },
  { id: 4, domainType: 'PRODUCT', parentId: 2, level: 3, name: '商品三级B', code: 'p3b' },
  { id: 5, domainType: 'STORE', parentId: 0, level: 1, name: '门店一级', code: 's1' },
  { id: 6, domainType: 'STORE', parentId: 5, level: 2, name: '门店二级', code: 's2' },
  { id: 7, domainType: 'STORE', parentId: 6, level: 3, name: '门店三级', code: 's3' }
]

test('filterDimensionsByDomain 未指定对象域时返回全部维度', () => {
  assert.equal(filterDimensionsByDomain(dimensions).length, 7)
})

test('buildDimensionTreeOptions 只返回三级维度并带完整路径', () => {
  const options = buildDimensionTreeOptions(dimensions, 'PRODUCT')

  assert.equal(options.length, 2)
  assert.equal(options[0].name, '商品一级 / 商品二级 / 商品三级A')
  assert.equal(options[1].name, '商品一级 / 商品二级 / 商品三级B')
})

test('getDimensionNameMap 返回维度完整路径映射', () => {
  const nameMap = getDimensionNameMap(dimensions)

  assert.equal(nameMap.get(3), '商品一级 / 商品二级 / 商品三级A')
  assert.equal(nameMap.get(7), '门店一级 / 门店二级 / 门店三级')
})
