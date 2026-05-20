<template>
  <ContentWrap>
    <div class="search-section">
      <el-form
        class="query-form-grid"
        :model="queryParams"
        ref="queryFormRef"
        label-width="90px"
      >
        <el-form-item label="门店" prop="storeId">
          <el-select
            v-model="queryParams.storeId"
            placeholder="请输入门店编码搜索"
            filterable
            remote
            reserve-keyword
            :remote-method="searchStoreSuggestions"
            :loading="storeLoading"
            clearable
            class="!w-100%"
          >
            <el-option
              v-for="item in storeList"
              :key="item.storeId"
              :label="`${item.storeId} - ${item.storeName}`"
              :value="item.storeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="SKU编码" prop="skuCode">
          <el-input
            v-model="queryParams.skuCode"
            placeholder="请输入SKU编码"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="SKU名称" prop="skuName">
          <el-input
            v-model="queryParams.skuName"
            placeholder="请输入SKU名称"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="商品归属" prop="productAttribution">
          <el-select
            v-model="queryParams.productAttribution"
            placeholder="请选择商品归属"
            clearable
            class="!w-100%"
          >
            <el-option
              v-for="item in ownershipOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="POS状态" prop="posStatus">
          <el-select
            v-model="queryParams.posStatus"
            placeholder="请选择POS状态"
            clearable
            class="!w-100%"
          >
            <el-option label="已上架" :value="1" />
            <el-option label="未上架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="入店状态" prop="enterShopStatus">
          <el-select
            v-model="queryParams.enterShopStatus"
            placeholder="请选择入店状态"
            clearable
            class="!w-100%"
          >
            <el-option label="是" :value="1" />
            <el-option label="否" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品标签" prop="tagValueId">
          <el-select
            v-model="queryParams.tagValueId"
            placeholder="请选择标签"
            clearable
            filterable
            class="!w-100%"
          >
            <el-option
              v-for="item in selectableTagList"
              :key="item.tagValueId"
              :label="`${item.tagValueName}（${item.tagValueCode}）`"
              :value="item.tagValueId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime">
          <el-date-picker
            v-model="queryParams.createTime"
            value-format="YYYY-MM-DD HH:mm:ss"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
            class="!w-100%"
          />
        </el-form-item>
      </el-form>
      <div class="actions-bar">
        <div class="actions-left">
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-5px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
        </div>
        <div class="actions-right">
          <el-button
            type="primary"
            @click="openForm('create')"
            v-hasPermi="['business:store-product:create']"
          >
            <Icon icon="ep:plus" class="mr-5px" /> 新增
          </el-button>
          <div class="action-group">
            <el-dropdown trigger="click">
              <el-button type="primary" plain>
                <Icon icon="ep:download" class="mr-5px" /> 下载模板
                <Icon icon="ep:arrow-down" class="ml-5px" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleDownloadTemplate('excel')">
                    <Icon icon="ep:document" class="mr-5px" /> Excel 模板
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleDownloadTemplate('csv')">
                    <Icon icon="ep:document" class="mr-5px" /> CSV 模板
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-upload
              accept=".xlsx, .xls, .csv"
              :show-file-list="false"
              :before-upload="handleImport"
            >
              <el-button type="primary" plain>
                <Icon icon="ep:upload" class="mr-5px" /> 导入
              </el-button>
            </el-upload>
            <el-button
              type="success"
              plain
              @click="handleExport"
              :loading="exportLoading"
              v-hasPermi="['business:store-product:export']"
            >
              <Icon icon="ep:download" class="mr-5px" /> 导出
            </el-button>
          </div>
          <div class="action-group">
            <el-button
              type="warning"
              plain
              :disabled="isEmpty(checkedIds)"
              @click="openBatchTagForm"
              v-hasPermi="['business:store-product:update']"
            >
              <Icon icon="ep:collection-tag" class="mr-5px" /> 批量挂标
            </el-button>
            <el-button
              type="danger"
              plain
              :disabled="isEmpty(checkedIds)"
              @click="handleDeleteBatch"
              v-hasPermi="['business:store-product:delete']"
            >
              <Icon icon="ep:delete" class="mr-5px" /> 批量删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-table
      :row-key="getRowKey"
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      @selection-change="handleRowCheckboxChange"
      @expand-change="handleExpandChange"
    >
      <el-table-column type="selection" width="55" :selectable="isSelectableRow" />
      <el-table-column type="expand">
        <template #default="{ row }">
          <div v-if="canExpandRow(row)" class="expand-content">
            <StockExpandCard v-if="isFormalRow(row)" :store-product-id="row.storeProductId" />
            <ShadowInventoryExpandCard v-else :row="row" />
          </div>
          <div v-else class="expand-placeholder">暂无库存明细</div>
        </template>
      </el-table-column>
      <el-table-column label="门店名称" align="center" prop="storeName" min-width="150" />
      <el-table-column label="SKU编码" align="center" prop="skuCode" width="150" />
      <el-table-column label="SKU名称" align="center" prop="skuName" min-width="150" />
      <el-table-column label="商品归属" align="center" prop="productAttribution" width="100">
        <template #default="scope">
          <span>{{ formatOwnershipLabel(scope.row.productAttribution) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="POS状态" align="center" prop="posStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.posStatus === 1 ? 'success' : 'info'">
            {{ scope.row.posStatus === 1 ? '已上架' : '未上架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="门店零售价" align="center" prop="storeRetailPrice" width="120">
        <template #default="scope">
          <span>{{ scope.row.storeRetailPrice != null ? '¥' + scope.row.storeRetailPrice.toFixed(2) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="入店状态" align="center" prop="enterShopStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.enterShopStatus === 1 ? 'success' : 'info'">
            {{ scope.row.enterShopStatus === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="首次入店日期" align="center" prop="firstEnterShopDate" width="130" />
      <el-table-column label="标签" min-width="220">
        <template #default="{ row }">
          <el-space v-if="isFormalRow(row)" wrap>
            <el-tag v-for="tag in row.tags || []" :key="`${row.storeProductId}-${tag.tagValueId}`" type="success">
              {{ tag.tagValueName }}
            </el-tag>
            <span v-if="!(row.tags || []).length" class="text-gray-400">-</span>
          </el-space>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" fixed="right" width="240">
        <template #default="scope">
          <template v-if="isFormalRow(scope.row)">
            <el-button
              link
              type="primary"
              @click="openForm('update', scope.row.storeProductId, scope.row)"
              v-hasPermi="['business:store-product:update']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="primary"
              @click="openTagForm(String(scope.row.storeProductId))"
              v-hasPermi="['business:store-product:update']"
            >
              管理标签
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(scope.row.storeProductId!)"
              v-hasPermi="['business:store-product:delete']"
            >
              删除
            </el-button>
          </template>
          <span v-else class="shadow-operation-tip">影子商品请前往治理池处理</span>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <StoreProductForm ref="formRef" @success="getList" />
  <StoreProductTagForm ref="tagFormRef" @success="handleTagSaved" />
  <StoreProductBatchTagForm ref="batchTagFormRef" @success="handleTagSaved" />
</template>

<script setup lang="ts">
import { isEmpty } from '@/utils/is'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { downloadByData } from '@/utils/filt'
import { StoreProductApi, StoreProductTable } from '@/api/business/store-product'
import { TableApi } from '@/api/business/store'
import { TagValueApi, type TagSelectableValue } from '@/api/business/tag/value'
import { formatOwnershipLabel, getOwnershipOptions } from './storeProductOwnershipLogic'
import StoreProductBatchTagForm from './StoreProductBatchTagForm.vue'
import StoreProductForm from './StoreProductForm.vue'
import StoreProductTagForm from './StoreProductTagForm.vue'
import ShadowInventoryExpandCard from './components/ShadowInventoryExpandCard.vue'
import StockExpandCard from './components/StockExpandCard.vue'

defineOptions({ name: 'StoreProduct' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<StoreProductTable[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined as string | undefined,
  skuCode: undefined as string | undefined,
  skuName: undefined as string | undefined,
  productAttribution: undefined as string | undefined,
  posStatus: undefined as number | undefined,
  enterShopStatus: undefined as number | undefined,
  tagValueId: undefined as number | undefined,
  createTime: [] as string[] | undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)

const storeLoading = ref(false)
const storeList = ref<any[]>([])
const selectableTagList = ref<TagSelectableValue[]>([])
const ownershipOptions = computed(() => getOwnershipOptions(queryParams.productAttribution))

const searchStoreSuggestions = async (queryString: string) => {
  if (!queryString) {
    storeList.value = []
    return
  }
  storeLoading.value = true
  try {
    const data = await TableApi.getTableSimpleList(queryString)
    storeList.value = data || []
  } catch {
    storeList.value = []
  } finally {
    storeLoading.value = false
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await StoreProductApi.getTablePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadSelectableTags = async () => {
  selectableTagList.value = await TagValueApi.getTagValueListForObject('STORE_PRODUCT')
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const formRef = ref()
const tagFormRef = ref()
const batchTagFormRef = ref()
const openForm = (type: string, id?: number | string, row?: StoreProductTable) => {
  formRef.value.open(type, id, row)
}

const openTagForm = (storeProductId: string) => {
  tagFormRef.value.open(storeProductId)
}

const openBatchTagForm = () => {
  batchTagFormRef.value.open(checkedIds.value.map(String))
}

const handleTagSaved = async () => {
  await getList()
}

const handleDelete = async (id: number | string) => {
  try {
    await message.delConfirm()
    await StoreProductApi.deleteTable(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleDeleteBatch = async () => {
  try {
    await message.delConfirm()
    await StoreProductApi.deleteTableList(checkedIds.value)
    checkedIds.value = []
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const isShadowRow = (row: StoreProductTable) => row.rowSource === 'SHADOW' || !!row.shadowId
const isFormalRow = (row: StoreProductTable) => !isShadowRow(row)
const canExpandRow = (row: StoreProductTable) =>
  (isFormalRow(row) && !!row.storeProductId) || (isShadowRow(row) && !!row.shadowId)
const isSelectableRow = (row: StoreProductTable) => isFormalRow(row) && !!row.storeProductId
const getRowKey = (row: StoreProductTable) => {
  if (row.storeProductId) {
    return `formal-${row.storeProductId}`
  }
  if (row.shadowId) {
    return `shadow-${row.shadowId}`
  }
  return `fallback-${row.storeId || ''}-${row.skuCode || ''}-${row.createTime || ''}`
}

const checkedIds = ref<Array<number | string>>([])
const handleRowCheckboxChange = (records: StoreProductTable[]) => {
  checkedIds.value = records
    .filter((item) => isFormalRow(item) && item.storeProductId !== undefined)
    .map((item) => item.storeProductId as number | string)
}

const handleExpandChange = (row: StoreProductTable) => {
  if (!canExpandRow(row)) {
    return
  }
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await StoreProductApi.exportTable(queryParams)
    download.excel(data, '门店商品.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const handleDownloadTemplate = async (format: string = 'excel') => {
  try {
    const data = await StoreProductApi.getImportTemplate(format)
    const filename = format === 'csv' ? '门店商品导入模板.csv' : '门店商品导入模板.xls'
    if (format === 'csv') {
      downloadByData(data, filename, 'text/csv;charset=utf-8')
    } else {
      download.excel(data, filename)
    }
  } catch {}
}

const handleImport = async (file: File) => {
  try {
    await message.importConfirm()
    const res = await StoreProductApi.importTable(file, true)
    const createdIds = res.createStoreProductIds || []
    const updatedIds = res.updateStoreProductIds || []
    if (createdIds.length > 0) {
      message.success(`成功创建 ${createdIds.length} 条数据`)
    }
    if (updatedIds.length > 0) {
      message.success(`成功更新 ${updatedIds.length} 条数据`)
    }
    if (res.failureStoreProductIds && Object.keys(res.failureStoreProductIds).length > 0) {
      const failureText = Object.entries(res.failureStoreProductIds)
        .map(([name, reason]) => `${name}: ${reason}`)
        .join('\n')
      message.warning(`导入失败：\n${failureText}`)
    }
    await getList()
  } catch {}
  return false
}

onMounted(() => {
  loadSelectableTags()
  getList()
})
</script>

<style scoped>
.search-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 24px;
}

.query-form-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px 24px;
}

.query-form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.actions-left,
.actions-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 16px;
  border-left: 1px solid #e5e7eb;
  margin-left: 4px;
}

@media (max-width: 1200px) {
  .query-form-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .query-form-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .actions-bar {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .actions-left,
  .actions-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .action-group {
    border-left: none;
    padding-left: 0;
    margin-left: 0;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .query-form-grid {
    grid-template-columns: 1fr;
  }
}

.expand-content {
  padding: 16px;
}

.expand-placeholder {
  padding: 16px;
  color: #909399;
}

.shadow-operation-tip {
  color: #909399;
  font-size: 12px;
}
</style>
