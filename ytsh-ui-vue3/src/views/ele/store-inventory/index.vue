<template>
  <div class="app-container inventory-page">
    <div class="action-card">
      <div class="section-header">
        <span class="section-title">库存查询</span>
      </div>

      <div class="action-body">
        <div class="form-row">
          <div class="form-item wide-item">
            <span class="form-label">门店</span>
            <el-select
              v-model="selectedStoreCode"
              placeholder="请选择门店"
              clearable
              filterable
              :loading="storeLoading"
              class="store-select"
              @change="handleStoreChange"
            >
              <el-option
                v-for="store in storeList"
                :key="`${store.platformStoreId || ''}-${store.storeName}`"
                :label="`${store.storeName} (${store.platformStoreId || '--'})`"
                :value="store.platformStoreId || ''"
              >
                <span :class="store.storeStatus === 1 ? 'store-open' : 'store-closed'">
                  {{ store.storeName }} ({{ store.platformStoreId || '--' }})
                </span>
              </el-option>
            </el-select>
          </div>
          <div class="form-item wide-item">
            <span class="form-label">商家编码</span>
            <el-input
              v-model="queryForm.merchantCode"
              placeholder="请输入 merchantCode"
              clearable
              class="text-input"
            />
          </div>
        </div>

        <div class="form-row">
          <div class="form-item wide-item">
            <span class="form-label">ERP门店编码</span>
            <el-input
              v-model="queryForm.erpStoreCode"
              placeholder="请选择门店或手动输入"
              clearable
              class="text-input"
            />
          </div>
        </div>

        <div class="form-row">
          <div class="form-item full-item">
            <span class="form-label top-label">SKU编码</span>
            <el-input
              v-model="queryForm.skuCodesText"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="支持英文逗号、空格或换行分隔"
              class="textarea-input"
            />
          </div>
        </div>


        <div class="form-row btn-row">
          <el-button type="primary" :loading="queryLoading" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <el-alert
      v-if="errorMessages.length"
      type="warning"
      :closable="false"
      class="result-alert"
    >
      <template #title>
        <div class="error-title">查询提示</div>
      </template>
      <div v-for="message in errorMessages" :key="message" class="error-item">{{ message }}</div>
    </el-alert>

    <div class="result-card">
      <div class="section-header">
        <span class="section-title">状态摘要</span>
      </div>

      <div class="summary-grid">
        <div v-for="item in summaryItems" :key="item.label" class="summary-item">
          <span class="summary-label">{{ item.label }}</span>
          <span class="summary-value">{{ item.value }}</span>
        </div>
      </div>
    </div>

    <div class="result-card">
      <div class="section-header">
        <span class="section-title">结果表格</span>
      </div>

      <el-table
        :data="resultRows"
        v-loading="queryLoading"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
      >
        <el-table-column label="SKU编码" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.skuCode || '--' }}</template>
        </el-table-column>
        <el-table-column label="子SKU编码" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.subSkuCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="persistStatus" label="落库状态" min-width="120" show-overflow-tooltip />
        <el-table-column prop="reasonCode" label="结果原因" min-width="140" show-overflow-tooltip />
        <el-table-column label="库存详情" min-width="360">
          <template #default="{ row }">
            <InventoryMetricsCard :model="adaptEleInventoryMetrics(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row, $index }">
            <el-button
              link
              type="primary"
              :loading="refreshingRows[buildRowKey(row, $index)]"
              @click="handleRefreshRow(row, $index)"
            >
              刷新库存
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!queryLoading && resultRows.length === 0" description="未查询到库存结果" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { TableApi, type StoreSimpleRespVO } from '@/api/business/store'
import {
  queryStoreInventory,
  type EleStoreInventoryQueryRespVO,
  type EleStoreInventoryRowVO
} from '@/api/ele/storeInventory'
import InventoryMetricsCard from '@/views/ele/components/InventoryMetricsCard.vue'
import {
  adaptEleInventoryMetrics,
  buildInventoryQueryRequest,
  findMatchingInventoryRow,
  type InventoryQueryFormModel
} from '@/views/ele/storeInventoryPageLogic'

defineOptions({ name: 'EleStoreInventory' })

const createQueryForm = (): InventoryQueryFormModel => ({
  platformStoreId: '',
  merchantCode: '',
  erpStoreCode: '',
  skuCodesText: ''
})

const queryForm = reactive<InventoryQueryFormModel>(createQueryForm())
const selectedStoreCode = ref('')
const storeLoading = ref(false)
const queryLoading = ref(false)
const storeList = ref<StoreSimpleRespVO[]>([])
const queryResult = ref<EleStoreInventoryQueryRespVO | null>(null)
const resultRows = ref<EleStoreInventoryRowVO[]>([])
const pageError = ref('')
const refreshingRows = ref<Record<string, boolean>>({})

const summaryItems = computed(() => {
  const summary = queryResult.value

  return [
    { label: '请求 SKU 数', value: summary?.requestSkuCount ?? 0 },
    { label: '返回行数', value: summary?.responseRowCount ?? 0 },
    { label: '正式落库数', value: summary?.formalSuccessCount ?? 0 },
    { label: '影子库存数', value: summary?.shadowSuccessCount ?? 0 },
    { label: '治理数', value: summary?.governanceCount ?? 0 },
    { label: '缺失数', value: summary?.missingRowCount ?? 0 },
    { label: '失败数', value: summary?.failureCount ?? 0 },
    { label: '总体状态', value: summary?.status || '--' }
  ]
})

const errorMessages = computed(() => {
  const messages = [...(queryResult.value?.errorDetails || [])]
  if (pageError.value) {
    messages.unshift(pageError.value)
  }
  return messages
})

const buildRowKey = (row: EleStoreInventoryRowVO, index: number) => {
  return `${row.skuCode || '--'}::${row.subSkuCode || '--'}::${index}`
}

const loadStoreList = async () => {
  storeLoading.value = true
  try {
    const data = await TableApi.getTableAllSimpleList(1)
    storeList.value = Array.isArray(data) ? data : []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载门店列表失败')
  } finally {
    storeLoading.value = false
  }
}

const handleStoreChange = (value?: string) => {
  queryForm.platformStoreId = value || ''
  queryForm.erpStoreCode = value || ''
}

const handleQuery = async () => {
  try {
    const payload = buildInventoryQueryRequest(queryForm)
    pageError.value = ''
    queryLoading.value = true
    const data = await queryStoreInventory(payload)
    queryResult.value = data
    resultRows.value = data.inventoryRows || []
  } catch (error: any) {
    pageError.value = error?.message || '库存查询失败'
    ElMessage.error(pageError.value)
  } finally {
    queryLoading.value = false
  }
}

const handleReset = () => {
  Object.assign(queryForm, createQueryForm())
  selectedStoreCode.value = ''
  queryResult.value = null
  resultRows.value = []
  pageError.value = ''
  refreshingRows.value = {}
}

const handleRefreshRow = async (row: EleStoreInventoryRowVO, index: number) => {
  const rowKey = buildRowKey(row, index)

  try {
    const payload = buildInventoryQueryRequest({
      platformStoreId: queryResult.value?.platformStoreId || queryForm.platformStoreId,
      merchantCode: queryResult.value?.merchantCode || queryForm.merchantCode,
      erpStoreCode: queryResult.value?.erpStoreCode || queryForm.erpStoreCode,
      skuCodesText: row.skuCode || ''
    })

    refreshingRows.value = { ...refreshingRows.value, [rowKey]: true }
    const data = await queryStoreInventory(payload)
    const refreshedRow = findMatchingInventoryRow(data.inventoryRows, row)

    if (!refreshedRow) {
      ElMessage.warning('本次未查询到库存结果')
      return
    }

    const nextRows = [...resultRows.value]
    nextRows[index] = refreshedRow
    resultRows.value = nextRows
    ElMessage.success('当前行库存已刷新')
  } catch (error: any) {
    ElMessage.error(error?.message || '刷新库存失败')
  } finally {
    refreshingRows.value = { ...refreshingRows.value, [rowKey]: false }
  }
}

onMounted(() => {
  loadStoreList()
})
</script>

<style scoped>
.inventory-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-card,
.result-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 0.05);
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.action-body,
.form-row,
.form-item {
  display: flex;
}

.action-body {
  flex-direction: column;
  gap: 16px;
}

.form-row {
  gap: 16px;
}

.form-item {
  flex-direction: column;
  gap: 8px;
}

.wide-item {
  flex: 1;
}

.full-item {
  width: 100%;
}

.form-label {
  font-size: 13px;
  color: #475569;
}

.top-label {
  align-self: flex-start;
}

.text-input,
.textarea-input,
.store-select {
  width: 100%;
}

.btn-row {
  justify-content: flex-start;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.summary-label,
.error-item {
  font-size: 12px;
  color: #64748b;
}

.summary-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.result-alert {
  margin-bottom: 0;
}

.error-title {
  font-weight: 600;
}

.store-open {
  color: #16a34a;
}

.store-closed {
  color: #94a3b8;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
