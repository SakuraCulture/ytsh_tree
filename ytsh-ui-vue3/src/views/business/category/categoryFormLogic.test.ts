import test from 'node:test'
import assert from 'node:assert/strict'

const { buildCategoryMap, buildCategoryPathNames, deriveCategoryFieldsByParent } = await import(
  new URL('./categoryFormLogic.ts', import.meta.url).href
)

const categories = [
  { categoryId: 1, parentId: 0, categoryLevel: '1', categoryName: '一级水果', categoryPath: '' },
  { categoryId: 2, parentId: 1, categoryLevel: '2', categoryName: '二级苹果', categoryPath: '1/' },
  { categoryId: 3, parentId: 2, categoryLevel: '3', categoryName: '三级红富士', categoryPath: '1/2/' }
]

const categoryMap = buildCategoryMap(categories)

test('deriveCategoryFieldsByParent 在顶级父类目时返回默认值', () => {
  assert.deepEqual(deriveCategoryFieldsByParent(undefined, categoryMap), {
    categoryLevel: '1',
    categoryPath: '',
    categoryPathNames: '-'
  })

  assert.deepEqual(deriveCategoryFieldsByParent(0, categoryMap), {
    categoryLevel: '1',
    categoryPath: '',
    categoryPathNames: '-'
  })
})

test('deriveCategoryFieldsByParent 选择一级父类目时派生二级路径', () => {
  assert.deepEqual(deriveCategoryFieldsByParent(1, categoryMap), {
    categoryLevel: '2',
    categoryPath: '1/',
    categoryPathNames: '一级水果'
  })
})

test('deriveCategoryFieldsByParent 选择二级父类目时派生三级路径', () => {
  assert.deepEqual(deriveCategoryFieldsByParent(2, categoryMap), {
    categoryLevel: '3',
    categoryPath: '1/2/',
    categoryPathNames: '一级水果 / 二级苹果'
  })
})

test('deriveCategoryFieldsByParent 能兼容 tree-select 回传的字符串父类目值', () => {
  assert.deepEqual(deriveCategoryFieldsByParent('2', categoryMap), {
    categoryLevel: '3',
    categoryPath: '1/2/',
    categoryPathNames: '一级水果 / 二级苹果'
  })

  assert.deepEqual(deriveCategoryFieldsByParent('0', categoryMap), {
    categoryLevel: '1',
    categoryPath: '',
    categoryPathNames: '-'
  })
})

test('buildCategoryPathNames 能从现有路径回算路径名', () => {
  assert.equal(buildCategoryPathNames('1/2/', categoryMap), '一级水果 / 二级苹果')
  assert.equal(buildCategoryPathNames('', categoryMap), '-')
})
