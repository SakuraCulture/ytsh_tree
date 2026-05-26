<template>
  <div class="inventory-task-panel">
    <div class="task-card">
      <div class="section-header">
        <span class="section-title">库存批量任务</span>
      </div>

      <div class="action-bar">
        <div class="action-tip">
          <span>所有开业门店库存拉取会在后台异步执行，执行中会自动刷新。</span>
        </div>
        <div class="action-buttons">
          <el-button type="success" :loading="creatingAllOpen" @click="createAllOpenTask">
            所有开业门店库存拉取
          </el-button>
          <el-button type="primary" :loading="creatingSelected" @click="openStoresDialog()">
            指定门店拉取
          </el-button>
          <el-button @click="loadTasks()">刷新</el-button>
        </div>
      </div>

      <el-table
        :data="taskList"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
      >
        <el-table-column prop="createTime" label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatTimestamp(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="taskNo" label="任务编号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="scope" label="范围" width="140" align="center">
          <template #default="{ row }">{{ formatScope(row.scope) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="门店进度" min-width="140" align="center">
          <template #default="{ row }">{{ row.finishedStoreCount || 0 }}/{{ row.totalStoreCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="批次进度" min-width="140" align="center">
          <template #default="{ row }">{{ row.finishedBatchCount || 0 }}/{{ row.totalBatchCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="库存结果" min-width="240" align="center">
          <template #default="{ row }">
            正式 {{ row.formalSuccessCount || 0 }} / 影子 {{ row.shadowSuccessCount || 0 }} / 治理
            {{ row.governanceCount || 0 }} / 失败 {{ row.failureCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
            <el-button link type="danger" :disabled="!canCancel(row.status)" @click="cancelTask(row.id)">
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-show="total > 0"
        v-model:total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @pagination="loadTasks"
      />
    </div>

    <el-dialog v-model="detailVisible" title="库存批量任务详情" width="920px" destroy-on-close @closed="handleDetailClosed">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务编号">{{ currentTask?.taskNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="范围">{{ formatScope(currentTask?.scope) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(currentTask?.status) }}</el-descriptions-item>
        <el-descriptions-item label="门店进度">{{ currentTask?.finishedStoreCount || 0 }}/{{ currentTask?.totalStoreCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="批次进度">{{ currentTask?.finishedBatchCount || 0 }}/{{ currentTask?.totalBatchCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="库存结果">
          正式 {{ currentTask?.formalSuccessCount || 0 }} / 影子 {{ currentTask?.shadowSuccessCount || 0 }} / 治理
          {{ currentTask?.governanceCount || 0 }} / 失败 {{ currentTask?.failureCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="错误原因" :span="2">{{ currentTask?.errorMsg || '--' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-title">
        门店明细
        <span v-if="currentTask && canCancel(currentTask.status)" class="detail-tip">执行中，明细会自动刷新</span>
      </div>
      <el-table
        :data="storeList"
        v-loading="storeLoading"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
      >
        <el-table-column prop="storeName" label="门店名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="storeId" label="门店ID" min-width="130" show-overflow-tooltip />
        <el-table-column prop="erpStoreCode" label="ERP门店编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="platformStoreId" label="平台门店编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="批次进度" width="120" align="center">
          <template #default="{ row }">{{ row.currentBatchNo || 0 }}/{{ row.totalBatchNo || 0 }}</template>
        </el-table-column>
        <el-table-column label="库存结果" min-width="220" align="center">
          <template #default="{ row }">
            正式 {{ row.formalSuccessCount || 0 }} / 影子 {{ row.shadowSuccessCount || 0 }} / 治理
            {{ row.governanceCount || 0 }} / 失败 {{ row.failureCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误原因" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="storeSelectVisible" title="选择门店进行库存拉取" width="720px" destroy-on-close>
      <div class="store-select-header">
        <el-input
          v-model="storeSearchKeyword"
          placeholder="搜索门店名称或编码"
          clearable
          style="width: 260px"
        />
        <span class="selected-count">已选 {{ selectedStoreIds.length }} 个门店</span>
      </div>
      <el-table
        ref="storeTableRef"
        :data="filteredStoreList"
        v-loading="storeDialogLoading"
        border
        stripe
        max-height="420"
        style="width: 100%"
        @selection-change="handleStoreSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="platformStoreId" label="平台门店ID" min-width="120" show-overflow-tooltip />
        <el-table-column prop="storeName" label="门店名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="storeStatus" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.storeStatus === 1 ? 'success' : 'info'">
              {{ row.storeStatus === 1 ? '开业' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="storeSelectVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="creatingSelected"
          :disabled="selectedStoreIds.length === 0"
          @click="createStoresTask"
        >
          确认拉取 ({{ selectedStoreIds.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import Pagination from '@/components/Pagination/index.vue'
import {
  cancelInventoryTask,
  createAllOpenInventoryTask,
  createStoresInventoryTask,
  getInventoryTask,
  getInventoryTaskPage,
  getInventoryTaskStores,
  type EleStoreInventoryBatchTaskPageReqVO,
  type EleStoreInventoryBatchTaskRespVO,
  type EleStoreInventoryBatchTaskStoreRespVO
} from '@/api/ele/storeInventory'
import { TableApi } from '@/api/business/store'

const loading = ref(false)
const creatingAllOpen = ref(false)
const creatingSelected = ref(false)
const storeSelectVisible = ref(false)
const storeDialogLoading = ref(false)
const storeAllList = ref<any[]>([])
const storeSearchKeyword = ref('')
const selectedStoreIds = ref<string[]>([])
const storeTableRef = ref()
const taskList = ref<EleStoreInventoryBatchTaskRespVO[]>([])
const total = ref(0)
const currentTask = ref<EleStoreInventoryBatchTaskRespVO>()
const storeList = ref<EleStoreInventoryBatchTaskStoreRespVO[]>([])
const detailVisible = ref(false)
const storeLoading = ref(false)
const currentDetailTaskId = ref<number>()
let pollTimer: number | undefined
let detailPollTimer: number | undefined

const queryParams = reactive<EleStoreInventoryBatchTaskPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  status: '',
  scope: ''
})

const filteredStoreList = computed(() => {
  if (!storeSearchKeyword.value) return storeAllList.value
  const kw = storeSearchKeyword.value.toLowerCase()
  return storeAllList.value.filter(
    (s: any) =>
      (s.storeName || '').toLowerCase().includes(kw) ||
      (s.platformStoreId || '').toLowerCase().includes(kw)
  )
})

const clearPollTimer = () => {
  if (pollTimer) {
    window.clearTimeout(pollTimer)
    pollTimer = undefined
  }
}

const clearDetailPollTimer = () => {
  if (detailPollTimer) {
    window.clearTimeout(detailPollTimer)
    detailPollTimer = undefined
  }
}

const canCancel = (status?: string) => ['PENDING', 'RUNNING'].includes(status || '')

const hasRunningTask = () => taskList.value.some((task) => canCancel(task.status))

const schedulePolling = () => {
  clearPollTimer()
  if (!hasRunningTask()) return
  pollTimer = window.setTimeout(() => {
    loadTasks(false)
  }, 5000)
}

const scheduleDetailPolling = () => {
  clearDetailPollTimer()
  if (!detailVisible.value || !currentTask.value || !canCancel(currentTask.value.status) || !currentDetailTaskId.value) {
    return
  }
  detailPollTimer = window.setTimeout(() => {
    refreshDetail(false)
  }, 3000)
}

const loadTasks = async (showLoading = true) => {
  if (showLoading) {
    loading.value = true
  }
  try {
    const data = await getInventoryTaskPage({
      taskNo: queryParams.taskNo || undefined,
      sourceType: queryParams.sourceType || undefined,
      scope: queryParams.scope || undefined,
      status: queryParams.status || undefined,
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize
    })
    taskList.value = data?.list || []
    total.value = data?.total || 0
    schedulePolling()
  } catch (error: any) {
    clearPollTimer()
    ElMessage.error(error?.message || '查询任务失败')
    taskList.value = []
    total.value = 0
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

const refreshDetail = async (showLoading = true) => {
  if (!currentDetailTaskId.value) return
  if (showLoading) {
    storeLoading.value = true
  }
  try {
    currentTask.value = await getInventoryTask(currentDetailTaskId.value)
    const data = await getInventoryTaskStores(currentDetailTaskId.value, { pageNo: 1, pageSize: 100 })
    storeList.value = data?.list || []
    scheduleDetailPolling()
  } catch (error: any) {
    clearDetailPollTimer()
    if (showLoading) {
      ElMessage.error(error?.message || '获取任务详情失败')
    }
  } finally {
    if (showLoading) {
      storeLoading.value = false
    }
  }
}

const createAllOpenTask = async () => {
  try {
    await ElMessageBox.confirm('确定创建所有开业门店的库存拉取任务吗？', '确认创建', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  creatingAllOpen.value = true
  try {
    const taskId = await createAllOpenInventoryTask()
    ElMessage.success(`已创建任务 ${taskId || ''}`)
    await loadTasks()
  } catch (error: any) {
    ElMessage.error(error?.message || '创建所有开业门店任务失败')
  } finally {
    creatingAllOpen.value = false
  }
}

const openStoresDialog = async () => {
  storeSelectVisible.value = true
  storeSearchKeyword.value = ''
  selectedStoreIds.value = []
  storeDialogLoading.value = true
  try {
    const data = await TableApi.getTableAllSimpleList(1)
    storeAllList.value = Array.isArray(data) ? data : []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载门店列表失败')
  } finally {
    storeDialogLoading.value = false
  }
}

const handleStoreSelectionChange = (rows: any[]) => {
  selectedStoreIds.value = rows.map((r) => r.platformStoreId || '').filter(Boolean)
}

const createStoresTask = async () => {
  if (selectedStoreIds.value.length === 0) {
    ElMessage.warning('请至少选择一个门店')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定对已选的 ${selectedStoreIds.value.length} 个门店创建库存拉取任务吗？`,
      '确认创建',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  creatingSelected.value = true
  try {
    const taskId = await createStoresInventoryTask(selectedStoreIds.value)
    ElMessage.success(`已创建任务 ${taskId || ''}`)
    storeSelectVisible.value = false
    await loadTasks()
  } catch (error: any) {
    ElMessage.error(error?.message || '创建任务失败')
  } finally {
    creatingSelected.value = false
  }
}

const normalizeTaskId = (id?: number | string) => {
  const normalized = typeof id === 'number' ? id : Number(id)
  if (!Number.isInteger(normalized) || normalized <= 0) {
    ElMessage.error('任务ID无效，请刷新任务列表后重试')
    return undefined
  }
  return normalized
}

const showDetail = async (id?: number | string) => {
  const taskId = normalizeTaskId(id)
  if (!taskId) return
  currentDetailTaskId.value = taskId
  detailVisible.value = true
  await refreshDetail(true)
}

const handleDetailClosed = () => {
  clearDetailPollTimer()
  currentDetailTaskId.value = undefined
  currentTask.value = undefined
  storeList.value = []
}

const cancelTask = async (id?: number | string) => {
  const taskId = normalizeTaskId(id)
  if (!taskId) return
  try {
    await ElMessageBox.confirm('确定取消该任务吗？', '确认取消', { type: 'warning' })
    await cancelInventoryTask(taskId)
    ElMessage.success('已提交取消请求')
    await loadTasks()
    if (currentDetailTaskId.value === taskId) {
      await refreshDetail(true)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '取消任务失败')
    }
  }
}

const formatScope = (scope?: string) => {
  if (scope === 'CURRENT_STORE') return '当前门店'
  if (scope === 'ALL_OPEN_STORES') return '所有开业门店'
  if (scope === 'SELECTED_STORES') return '指定门店'
  return scope || '--'
}

const getStatusText = (status?: string) => {
  if (status === 'PENDING') return '待执行'
  if (status === 'RUNNING') return '执行中'
  if (status === 'SUCCESS') return '成功'
  if (status === 'PARTIAL_FAIL') return '部分失败'
  if (status === 'FAILED') return '失败'
  if (status === 'CANCELLED') return '已取消'
  return status || '--'
}

const getStatusTagType = (status?: string) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'RUNNING') return 'warning'
  if (status === 'PARTIAL_FAIL') return 'warning'
  if (status === 'FAILED' || status === 'CANCELLED') return 'danger'
  return 'info'
}

const formatTimestamp = (value?: string) => {
  if (!value) return '--'
  if (/^\d+$/.test(value)) {
    const ts = Number(value) > 100000000000 ? Number(value) : Number(value) * 1000
    return new Date(ts).toLocaleString('zh-CN', { hour12: false })
  }
  return value.replace('T', ' ')
}

onMounted(() => {
  loadTasks()
})

onUnmounted(() => {
  clearPollTimer()
  clearDetailPollTimer()
})
</script>

<style scoped lang="scss">
.inventory-task-panel {
  padding: 0;
}

.task-card {
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 16px;
  background: #fff;
  padding: 20px;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.section-title::before {
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: linear-gradient(180deg, #10b981, #059669);
  content: '';
}

.action-bar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.action-tip {
  color: #64748b;
  font-size: 13px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-title {
  margin: 20px 0 12px;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-tip {
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
}

.store-select-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.selected-count {
  font-size: 13px;
  color: #64748b;
}
</style>
