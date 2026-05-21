<template>
  <div class="app-container">
    <div class="action-card">
      <div class="section-header">
        <span class="section-title">商品查询与同步</span>
      </div>
      <div class="action-body">
        <div class="form-row">
          <div class="form-item wide-item">
            <span class="form-label">商家编码</span>
            <el-input
              v-model="queryForm.merchantCode"
              placeholder="请输入 merchantCode"
              clearable
              class="text-input"
            />
          </div>
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
          <div class="form-item small-item">
            <span class="form-label">页码</span>
            <el-input-number v-model="queryForm.pageNo" :min="1" :max="999" class="number-input" />
          </div>
          <div class="form-item small-item">
            <span class="form-label">每页条数</span>
            <el-input-number v-model="queryForm.pageSize" :min="1" :max="20" class="number-input" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-item full-item">
            <span class="form-label top-label">SKU编码</span>
            <el-input
              v-model="skuText"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="支持英文逗号、中文逗号、空格或换行分隔；留空表示按分页查询门店商品"
              class="textarea-input"
            />
          </div>
        </div>

        <div class="form-row switch-row">
          <div class="switch-card">
            <div>
              <div class="switch-title">测试模式</div>
              <div class="switch-desc">
                配置键：{{ testModeInfo.configKey || '--' }}
                <span class="status-text" :class="testModeInfo.enabled ? 'is-enabled' : 'is-disabled'">
                  {{ testModeInfo.enabled ? '已开启' : '未开启' }}
                </span>
              </div>
            </div>
            <el-switch
              v-model="syncInTestMode"
              :disabled="!testModeInfo.enabled"
              active-text="测试模式"
              inactive-text="正式模式"
              inline-prompt
            />
          </div>
        </div>

        <div class="form-row btn-row">
          <el-button type="primary" :loading="queryLoading" @click="handlePreview">
            查询预览
          </el-button>
          <el-button type="success" :loading="syncLoading" @click="handleQueryAndSync">
            查询并同步
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="result-card">
      <div class="section-header">
        <span class="section-title">查询结果</span>
      </div>
      <div class="summary-grid">
        <div class="summary-item">
          <span class="summary-label">商家编码</span>
          <span class="summary-value">{{ queryResult?.merchantCode || '--' }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">门店编码</span>
          <span class="summary-value">{{ queryResult?.storeCode || '--' }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">页码</span>
          <span class="summary-value">{{ queryResult?.page || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">总条数</span>
          <span class="summary-value">{{ queryResult?.total || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">每页条数</span>
          <span class="summary-value">{{ queryResult?.pageSize || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">当前SKU数</span>
          <span class="summary-value">{{ previewRows.length }}</span>
        </div>
      </div>

      <el-table
        :data="previewRows"
        v-loading="queryLoading"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
      >
        <el-table-column prop="title" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="spuCode" label="SPU编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="skuCode" label="SKU编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="subSkuCode" label="子SKU编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="specification" label="规格" min-width="140" show-overflow-tooltip />
        <el-table-column label="售价" width="110" align="right">
          <template #default="{ row }">
            <span>{{ formatPrice(row.salePrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 3 ? 'danger' : 'success'">
              {{ row.status === 3 ? '下架' : '上架/更新' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="log-card">
      <div class="section-header">
        <span class="section-title">同步日志</span>
      </div>

      <div class="log-filter">
        <div class="filter-item wide-item">
          <span class="form-label">门店</span>
          <el-select
            v-model="logFilters.erpStoreCode"
            placeholder="按 ERP 门店编码筛选"
            clearable
            filterable
            :loading="storeLoading"
            class="store-select"
          >
            <el-option
              v-for="store in storeList"
              :key="`log-${store.platformStoreId || ''}-${store.storeName}`"
              :label="`${store.storeName} (${store.platformStoreId || '--'})`"
              :value="store.platformStoreId || ''"
            />
          </el-select>
        </div>
        <div class="filter-item wide-item">
          <span class="form-label">SKU编码</span>
          <el-input v-model="logFilters.skuCode" placeholder="请输入 SKU 编码" clearable class="text-input" />
        </div>
        <div class="filter-item small-item">
          <span class="form-label">结果</span>
          <el-select v-model="logSuccessValue" placeholder="全部" clearable class="number-input">
            <el-option label="成功" value="true" />
            <el-option label="失败" value="false" />
          </el-select>
        </div>
        <div class="filter-item btn-group">
          <el-button type="primary" @click="handleLogQuery">搜索</el-button>
          <el-button @click="resetLogQuery">重置</el-button>
        </div>
      </div>

      <el-table
        :data="logList"
        v-loading="logLoading"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
      >
        <el-table-column prop="createTime" label="时间" min-width="180">
          <template #default="{ row }">{{ formatTimestamp(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="erpStoreCode" label="ERP门店编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="platformStoreId" label="平台门店编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="skuCode" label="SKU编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="subSkuCode" label="子SKU编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="operationType" label="操作类型" width="100" align="center" />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultCode" label="结果码" min-width="110" show-overflow-tooltip />
        <el-table-column prop="resultMsg" label="结果消息" min-width="220" show-overflow-tooltip />
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
        </el-table-column>
        <el-table-column label="详情" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showLogDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="logTotal > 0"
        v-model:total="logTotal"
        v-model:page="logFilters.pageNo"
        v-model:limit="logFilters.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @pagination="loadLogs"
      />
    </div>

    <el-dialog v-model="detailDialogVisible" title="同步日志详情" width="860px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商家编码">{{ currentLog?.merchantCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="ERP门店编码">{{ currentLog?.erpStoreCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="平台门店编码">{{ currentLog?.platformStoreId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="SKU编码">{{ currentLog?.skuCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="子SKU编码">{{ currentLog?.subSkuCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ currentLog?.operationType || '--' }}</el-descriptions-item>
        <el-descriptions-item label="结果码">{{ currentLog?.resultCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="结果消息">{{ currentLog?.resultMsg || '--' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-title">请求报文</div>
      <pre class="detail-pre">{{ currentLog?.requestBody || '--' }}</pre>

      <div class="detail-section-title">响应报文</div>
      <pre class="detail-pre">{{ currentLog?.responseBody || '--' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import { TableApi } from '@/api/business/store'
import {
  getSyncLog,
  getSyncLogPage,
  getTestMode,
  queryAndSyncStoreGoods,
  queryStoreGoods,
  type StoreGoodsQueryReqVO,
  type StoreGoodsQueryRespVO,
  type StoreGoodsSyncLogRespVO,
  type StoreGoodsTestModeRespVO
} from '@/api/ele/storeGoods'
import type { StoreSimpleRespVO } from '@/api/business/store'

const storeList = ref<StoreSimpleRespVO[]>([])
const storeLoading = ref(false)
const selectedStoreCode = ref('')
const skuText = ref('')
const queryLoading = ref(false)
const syncLoading = ref(false)
const logLoading = ref(false)
const queryResult = ref<StoreGoodsQueryRespVO>()
const testModeInfo = ref<StoreGoodsTestModeRespVO>({ enabled: false })
const syncInTestMode = ref(false)
const logList = ref<StoreGoodsSyncLogRespVO[]>([])
const logTotal = ref(0)
const currentLog = ref<StoreGoodsSyncLogRespVO>()
const detailDialogVisible = ref(false)
const logSuccessValue = ref<string | undefined>()

const queryForm = reactive<StoreGoodsQueryReqVO>({
  merchantCode: '',
  erpStoreCode: '',
  pageNo: 1,
  pageSize: 20
})

const logFilters = reactive({
  platformStoreId: '',
  erpStoreCode: '',
  skuCode: '',
  pageNo: 1,
  pageSize: 10
})

const parseSkuCodeList = () => {
  return skuText.value
    .split(/[\n,，\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const buildQueryPayload = (): StoreGoodsQueryReqVO | null => {
  if (!queryForm.merchantCode?.trim()) {
    ElMessage.warning('请先输入商家编码')
    return null
  }
  if (!queryForm.erpStoreCode?.trim()) {
    ElMessage.warning('请先选择或输入 ERP 门店编码')
    return null
  }
  return {
    merchantCode: queryForm.merchantCode.trim(),
    erpStoreCode: queryForm.erpStoreCode.trim(),
    skuCodeList: parseSkuCodeList(),
    pageNo: queryForm.pageNo || 1,
    pageSize: queryForm.pageSize || 20
  }
}

const previewRows = computed(() => {
  return (queryResult.value?.goodsList || []).flatMap((goods) =>
    (goods.skuList || []).map((sku) => ({
      title: goods.title,
      spuCode: goods.spuCode,
      skuCode: sku.skuCode,
      subSkuCode: sku.subSkuCode,
      specification: sku.specification,
      salePrice: sku.salePrice,
      status: sku.status
    }))
  )
})

const loadStores = async () => {
  storeLoading.value = true
  try {
    const res = await TableApi.getTableAllSimpleList(1)
    const data = Array.isArray(res) ? res : []
    storeList.value = data.sort((a, b) => (a.storeStatus ?? 0) - (b.storeStatus ?? 0))
    const firstOpenStore = storeList.value.find((item) => item.storeStatus === 1)
    if (firstOpenStore?.platformStoreId && !queryForm.erpStoreCode) {
      selectedStoreCode.value = firstOpenStore.platformStoreId
      queryForm.erpStoreCode = firstOpenStore.platformStoreId
      logFilters.erpStoreCode = firstOpenStore.platformStoreId
    }
  } catch {
    storeList.value = []
  } finally {
    storeLoading.value = false
  }
}

const loadTestMode = async () => {
  try {
    const data = await getTestMode()
    testModeInfo.value = data || { enabled: false }
    syncInTestMode.value = !!data?.enabled
  } catch {
    testModeInfo.value = { enabled: false }
    syncInTestMode.value = false
  }
}

const loadLogs = async () => {
  logLoading.value = true
  try {
    const data = await getSyncLogPage({
      platformStoreId: logFilters.platformStoreId || undefined,
      erpStoreCode: logFilters.erpStoreCode || undefined,
      skuCode: logFilters.skuCode || undefined,
      success:
        logSuccessValue.value === undefined ? undefined : logSuccessValue.value === 'true',
      pageNo: logFilters.pageNo,
      pageSize: logFilters.pageSize
    })
    logList.value = data?.list || []
    logTotal.value = data?.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '查询同步日志失败')
    logList.value = []
    logTotal.value = 0
  } finally {
    logLoading.value = false
  }
}

const handleStoreChange = (value?: string) => {
  queryForm.erpStoreCode = value || ''
}

const handlePreview = async () => {
  const payload = buildQueryPayload()
  if (!payload) return
  queryLoading.value = true
  try {
    queryResult.value = await queryStoreGoods(payload)
    ElMessage.success(`查询完成，当前返回 ${previewRows.value.length} 条 SKU`)
  } catch (error: any) {
    ElMessage.error(error?.message || '查询商品失败')
  } finally {
    queryLoading.value = false
  }
}

const handleQueryAndSync = async () => {
  const payload = buildQueryPayload()
  if (!payload) return

  try {
    await ElMessageBox.confirm(
      syncInTestMode.value ? '确定以测试模式执行查询并同步吗？' : '确定执行查询并同步吗？',
      '确认同步',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  syncLoading.value = true
  try {
    const count = await queryAndSyncStoreGoods(payload, syncInTestMode.value)
    ElMessage.success(`同步提交完成，共处理 ${count || 0} 条 SKU`)
    await handlePreview()
    await loadLogs()
  } catch (error: any) {
    ElMessage.error(error?.message || '查询并同步失败')
  } finally {
    syncLoading.value = false
  }
}

const handleReset = () => {
  queryForm.merchantCode = ''
  queryForm.erpStoreCode = selectedStoreCode.value || ''
  queryForm.pageNo = 1
  queryForm.pageSize = 20
  skuText.value = ''
  queryResult.value = undefined
}

const handleLogQuery = () => {
  logFilters.pageNo = 1
  loadLogs()
}

const resetLogQuery = () => {
  logFilters.platformStoreId = ''
  logFilters.erpStoreCode = selectedStoreCode.value || ''
  logFilters.skuCode = ''
  logFilters.pageNo = 1
  logFilters.pageSize = 10
  logSuccessValue.value = undefined
  loadLogs()
}

const showLogDetail = async (id?: number) => {
  if (!id) return
  try {
    currentLog.value = await getSyncLog(id)
    detailDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '获取日志详情失败')
  }
}

const formatPrice = (price?: number) => {
  if (price === null || price === undefined) return '--'
  return `¥${(Number(price) / 100).toFixed(2)}`
}

const formatDuration = (durationMs?: number) => {
  if (durationMs === null || durationMs === undefined) return '--'
  return `${durationMs}ms`
}

const formatTimestamp = (value?: string) => {
  if (!value) return '--'
  if (/^\d+$/.test(value)) {
    const ts = Number(value) > 100000000000 ? Number(value) : Number(value) * 1000
    return new Date(ts).toLocaleString('zh-CN', { hour12: false })
  }
  return value.replace('T', ' ')
}

onMounted(async () => {
  await Promise.all([loadStores(), loadTestMode()])
  await loadLogs()
})
</script>

<style lang="scss" scoped>
.app-container {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
}

.action-card,
.result-card,
.log-card {
  margin-bottom: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 16px;
  background: #fff;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 1px 2px rgba(0, 0, 0, 0.02);
}

.action-body,
.result-card,
.log-card {
  padding: 20px;
}

.section-header {
  margin-bottom: 16px;
  padding: 18px 20px 0;

  .section-title {
    display: flex;
    gap: 10px;
    align-items: center;
    font-size: 16px;
    font-weight: 700;
    color: #1e293b;

    &::before {
      width: 4px;
      height: 18px;
      border-radius: 2px;
      background: linear-gradient(180deg, #6366f1, #4f46e5);
      content: '';
    }
  }
}

.form-row,
.log-filter {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.form-item,
.filter-item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.form-label {
  min-width: 88px;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.top-label {
  align-self: flex-start;
  padding-top: 10px;
}

.wide-item {
  flex: 1;
  min-width: 320px;
}

.small-item {
  min-width: 220px;
}

.full-item {
  width: 100%;
  align-items: flex-start;
}

.text-input,
.store-select,
.number-input {
  width: 100%;
}

.number-input {
  max-width: 140px;
}

.textarea-input {
  width: 100%;
}

.switch-row {
  margin-top: 8px;
}

.switch-card {
  width: 100%;
  padding: 16px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.switch-title {
  margin-bottom: 6px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.switch-desc {
  color: #64748b;
  font-size: 13px;
}

.status-text {
  margin-left: 10px;
  font-weight: 600;

  &.is-enabled {
    color: #10b981;
  }

  &.is-disabled {
    color: #ef4444;
  }
}

.btn-row,
.btn-group {
  .el-button--primary {
    background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
    border: none;
  }

  .el-button--success {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    border: none;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.summary-item {
  padding: 14px 16px;
  border: 1px solid rgba(226, 232, 240, 0.7);
  border-radius: 12px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  color: #94a3b8;
  font-size: 12px;
}

.summary-value {
  color: #1e293b;
  font-size: 18px;
  font-weight: 700;
}

.detail-section-title {
  margin: 18px 0 10px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.detail-pre {
  max-height: 220px;
  overflow: auto;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 13px;
  line-height: 1.6;
}

.store-open {
  color: #334155;
}

.store-closed {
  color: #94a3b8;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-select__wrapper) {
  border-color: #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  box-shadow: none;
}

:deep(.el-table__header-wrapper th) {
  text-align: center;
}

:deep(.el-pagination) {
  margin-top: 20px;
  justify-content: center;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .switch-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
