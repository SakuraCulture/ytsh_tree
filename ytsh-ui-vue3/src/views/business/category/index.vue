<template>
  <ContentWrap>
    <div class="category-search-container" role="search" aria-label="商品类目搜索">
      <el-form
        class="category-search-form"
        :model="searchParams"
        ref="searchFormRef"
        @submit.prevent="handleSearch"
        aria-label="类目搜索表单"
      >
        <fieldset class="search-fieldset">
          <legend class="sr-only">核心搜索条件</legend>

          <el-row :gutter="16" class="category-search-row">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="类目名称" prop="categoryName">
                <el-input
                  v-model="searchParams.categoryName"
                  placeholder="请输入类目名称"
                  clearable
                  @keyup.enter="handleSearch"
                  class="category-search-input"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="父类目" prop="parentId">
                <el-tree-select
                  v-model="searchParams.parentId"
                  :data="categoryTableTree"
                  :props="{ label: 'categoryName', value: 'categoryId', children: 'children' }"
                  check-strictly
                  default-expand-all
                  placeholder="选择父类目"
                  filterable
                  filter-placeholder="搜索父类目..."
                  clearable
                  class="category-search-input"
                  aria-label="选择父类目"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="层级" prop="categoryLevel">
                <el-select
                  v-model="searchParams.categoryLevel"
                  placeholder="请选择层级"
                  filterable
                  filter-placeholder="搜索层级..."
                  clearable
                  class="category-search-input"
                  aria-label="选择类目层级"
                >
                  <el-option label="一级" value="1" />
                  <el-option label="二级" value="2" />
                  <el-option label="三级" value="3" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="状态" prop="status">
                <el-select
                  v-model="searchParams.status"
                  placeholder="请选择状态"
                  clearable
                  class="category-search-input"
                  aria-label="选择类目状态"
                >
                  <el-option label="禁用" value="0" />
                  <el-option label="启用" value="1" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </fieldset>

        <div class="category-search-actions" role="group" aria-label="搜索操作">
          <el-button type="primary" @click="handleSearch" aria-label="执行类目搜索">
            <Icon icon="ep:search" class="mr-5px" /> 搜索
          </el-button>
          <el-button @click="handleReset" aria-label="重置搜索条件">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
          <el-button
            type="success"
            plain
            @click="handleOpenCreateForm"
            v-hasPermi="['business:category-table:create']"
            aria-label="新增类目"
          >
            <Icon icon="ep:plus" class="mr-5px" /> 新增
          </el-button>
          <el-button
            type="primary"
            plain
            @click="handleOpenImportForm"
            aria-label="导入类目数据"
          >
            <Icon icon="ep:upload" class="mr-5px" /> 导入
          </el-button>
          <el-button
            type="warning"
            plain
            @click="handleExport"
            :loading="exportLoading"
            v-hasPermi="['business:category-table:export']"
            aria-label="导出类目数据"
          >
            <Icon icon="ep:download" class="mr-5px" /> 导出
          </el-button>
          <el-button
            type="danger"
            plain
            :disabled="checkedCategories.length === 0"
            @click="handleBatchDelete"
            v-hasPermi="['business:category-table:delete']"
            aria-label="批量删除类目"
          >
            <Icon icon="ep:delete" class="mr-5px" /> 批量删除
          </el-button>
        </div>
      </el-form>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-table
      v-loading="tableLoading"
      :data="categoryList"
      :stripe="true"
      row-key="categoryId"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      :row-selection-type="'multiple'"
      :row-height="48"
      @selection-change="handleSelectionChange"
      v-if="refreshTable"
    >
      <el-table-column type="selection" width="60" fixed />
      <el-table-column label="类目名称" header-align="center" align="center" prop="categoryName" width="400" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="cell-text-ellipsis">{{ row.categoryName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="父类目名称" header-align="center" align="center" prop="parentCategoryName" width="350" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="cell-text-ellipsis">{{ row.parentCategoryName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="层级" align="center" prop="categoryLevel" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.categoryLevel === 1" type="success">一级</el-tag>
          <el-tag v-else-if="row.categoryLevel === 2" type="warning">二级</el-tag>
          <el-tag v-else-if="row.categoryLevel === 3" type="info">三级</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类目路径" header-align="center" align="center" prop="categoryPathNames" width="350" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="cell-text-ellipsis">{{ row.categoryPathNames }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showSortOrderColumn" label="排序" align="center" prop="sortOrder" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success">正常</el-tag>
          <el-tag v-else type="danger">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="160"
        v-if="false"
      />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="scope">
          <div class="operation-buttons">
            <el-button
              link
              type="primary"
              @click="handleOpenEditForm(scope.row.categoryId)"
              v-hasPermi="['business:category-table:update']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(scope.row.categoryId)"
              v-hasPermi="['business:category-table:delete']"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <CategoryTableForm ref="formRef" @success="fetchCategoryList" />
  <CategoryTableImportForm ref="importFormRef" @success="fetchCategoryList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { handleTree } from '@/utils/tree'
import download from '@/utils/download'
import { CategoryTableApi, CategoryTable } from '@/api/business/category'
import CategoryTableForm from './CategoryTableForm.vue'
import CategoryTableImportForm from './CategoryTableImportForm.vue'

defineOptions({ name: 'CategoryTable' })

const message = useMessage()
const { t } = useI18n()

const tableLoading = ref(true)
const categoryList = ref<CategoryTable[]>([])
const searchParams = reactive({
  pageNo: 1,
  pageSize: 10,
  categoryName: undefined as string | undefined,
  parentId: undefined as number | undefined,
  categoryLevel: undefined as string | undefined,
  categoryPath: undefined as string | undefined,
  status: undefined as string | undefined,
  createTime: [] as string[]
})
const searchFormRef = ref()
const exportLoading = ref(false)
const categoryTableTree = ref()
const checkedCategories = ref<CategoryTable[]>([])
const showSortOrderColumn = ref(false)

const handleSelectionChange = (selection: CategoryTable[]) => {
  checkedCategories.value = selection
}

const fetchCategoryList = async () => {
  tableLoading.value = true
  try {
    const allData = await CategoryTableApi.getCategoryTableList({ pageNo: 1, pageSize: 999999 })
    const allList = allData.list || allData || []
    const categoryMap = new Map<number, CategoryTable>()
    allList.forEach(item => {
      categoryMap.set(item.categoryId, item)
    })

    const data = await CategoryTableApi.getCategoryTableList(searchParams)
    const list = data.list || data || []
    categoryList.value = handleTree(list, 'categoryId', 'parentId')
    processCategoryDisplayData(categoryList.value, list, categoryMap)
  } finally {
    tableLoading.value = false
  }
}

const processCategoryDisplayData = (treeData: CategoryTable[], flatList: CategoryTable[], categoryMap: Map<number, CategoryTable>) => {
  const buildPathNames = (node: CategoryTable): string => {
    if (!node.parentId || node.parentId === 0) {
      return '-'
    }

    const pathNames: string[] = []
    let currentParentId = node.parentId

    while (currentParentId && currentParentId !== 0) {
      const parent = categoryMap.get(currentParentId)
      if (parent) {
        pathNames.unshift(parent.categoryName)
        currentParentId = parent.parentId
      } else {
        break
      }
    }

    return pathNames.length > 0 ? pathNames.join(' / ') : '-'
  }

  const processNode = (node: CategoryTable) => {
    if (node.parentId && node.parentId !== 0) {
      if (!node.parentCategoryName) {
        const parent = categoryMap.get(node.parentId)
        node.parentCategoryName = parent?.categoryName || `类目${node.parentId}`
      }
    } else {
      node.parentCategoryName = '-'
    }

    node.categoryPathNames = buildPathNames(node)

    if (node.children && node.children.length > 0) {
      node.children.forEach(child => processNode(child))
    }
  }

  treeData.forEach(node => processNode(node))
}

const handleSearch = () => {
  searchParams.pageNo = 1
  fetchCategoryList()
}

const handleReset = () => {
  searchFormRef.value?.resetFields()
  searchParams.pageNo = 1
  handleSearch()
}

const formRef = ref()
const importFormRef = ref()
const handleOpenCreateForm = () => {
  formRef.value.open('create')
}

const handleOpenImportForm = () => {
  importFormRef.value.open()
}

const handleOpenEditForm = (id: number) => {
  formRef.value.open('update', id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await CategoryTableApi.deleteCategoryTable(id)
    message.success(t('common.delSuccess'))
    await fetchCategoryList()
  } catch {}
}

const handleBatchDelete = async () => {
  if (checkedCategories.value.length === 0) {
    message.warning('请选择要删除的类目')
    return
  }
  const flatList = getFlatCategoryList(categoryList.value)
  const checkedIds = checkedCategories.value.map(item => item.categoryId)
  const hasChildrenIds = checkedIds.filter(id => {
    return flatList.some(item => item.parentId === id)
  })
  if (hasChildrenIds.length > 0) {
    const totalCount = countDescendants(checkedIds, flatList)
    try {
      await message.confirm(
        `选中的类目中包含有子节点的父类目，删除后将同时删除其所有子级类目（共影响 ${totalCount} 个类目），此操作不可恢复！是否确认删除？`
      )
      await doBatchDelete(checkedIds)
    } catch {}
  } else {
    try {
      await message.delConfirm()
      await doBatchDelete(checkedIds)
    } catch {}
  }
}

const doBatchDelete = async (ids: number[]) => {
  try {
    await CategoryTableApi.deleteCategoryTableByIds(ids)
    message.success(t('common.delSuccess'))
    checkedCategories.value = []
    await fetchCategoryList()
    await fetchCategoryTree()
  } catch {}
}

const getFlatCategoryList = (list: CategoryTable[]): CategoryTable[] => {
  const result: CategoryTable[] = []
  const flat = (items: CategoryTable[]) => {
    for (const item of items) {
      result.push(item)
      if (item.children && item.children.length > 0) {
        flat(item.children)
      }
    }
  }
  flat(list)
  return result
}

const countDescendants = (parentIds: number[], flatList: CategoryTable[]): number => {
  let count = 0
  const queue = [...parentIds]
  while (queue.length > 0) {
    const id = queue.shift()!
    const children = flatList.filter(item => item.parentId === id)
    for (const child of children) {
      count++
      queue.push(child.categoryId)
    }
  }
  return count + parentIds.length
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await CategoryTableApi.exportCategoryTable(searchParams)
    download.excel(data, '商品类目表（三级树形结构）.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const isExpandAll = ref(true)
const refreshTable = ref(true)

onMounted(() => {
  fetchCategoryList()
  fetchCategoryTree()
})

const fetchCategoryTree = async () => {
  categoryTableTree.value = []
  const data = await CategoryTableApi.getCategoryTableList()
  const list = data.list || data || []
  const root = { categoryId: 0, categoryName: '顶级（无父类目）', children: [] }
  root.children = handleTree(list, 'categoryId', 'parentId')
  categoryTableTree.value.push(root)
}
</script>
<style scoped lang="scss">
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.category-search-container {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
}

.category-search-form {
  .search-fieldset {
    border: none;
    padding: 0;
    margin: 0;
  }

  .category-search-row {
    margin-bottom: 8px;
  }

  .category-search-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    padding-top: 16px;
    border-top: 1px solid #e5e7eb;
    margin-top: 16px;

    .el-button {
      flex-shrink: 0;
    }
  }
}

.category-search-input {
  width: 100%;

  :deep(.el-input__wrapper) {
    min-height: 34px;
    font-size: 14px;
  }

  :deep(.el-select__wrapper) {
    min-height: 34px;
    line-height: 34px;
  }

  :deep(.el-tree-select__wrapper) {
    min-height: 34px;
  }
}

.category-search-date-picker {
  width: 100%;

  :deep(.el-input__wrapper) {
    min-height: 34px;
    font-size: 14px;
  }
}

.date-range-item {
  :deep(.el-form-item__label) {
    min-width: 80px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .el-collapse-transition,
  * {
    transition: none !important;
    animation: none !important;
  }
}

@media (forced-colors: active) {
  .category-search-input:focus-within {
    outline: 2px solid CanvasText;
  }

  :deep(.el-input__wrapper.is-focus),
  :deep(.el-select__wrapper.is-focused) {
    outline: 2px solid CanvasText;
    outline-offset: 2px;
  }

  .el-button:focus {
    outline: 2px solid CanvasText;
    outline-offset: 2px;
  }
}

.cell-text-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.operation-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
