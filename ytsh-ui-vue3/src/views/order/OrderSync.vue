<template>
  <div class="app-container">
    <div class="pull-card">
      <div class="pull-card-header">
        <span class="header-title">📦 订单同步</span>
      </div>
      <div class="pull-card-body">
        <div class="form-row">
          <div class="form-item">
            <span class="form-label">拉取模式</span>
            <el-radio-group v-model="pullMode" class="mode-radio">
              <el-radio-button label="single">按门店拉取</el-radio-button>
              <el-radio-button label="all">拉取全部门店</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="form-row" v-if="pullMode === 'single'">
          <div class="form-item">
            <span class="form-label">选择门店</span>
            <el-select
              v-model="selectedStoreId"
              placeholder="请选择门店"
              clearable
              filterable
              :loading="storeLoading"
              class="store-select"
            >
              <el-option
                v-for="store in storeList"
                :key="store.platformStoreId || store.storeName"
                :label="store.storeName"
                :value="store.platformStoreId || store.storeName"
                :disabled="store.storeStatus === 0"
              >
                <span
                  :class="{
                    'store-open': store.storeStatus === 1,
                    'store-closed': store.storeStatus === 0
                  }"
                >
                  {{ store.storeName }}
                </span>
              </el-option>
            </el-select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item">
            <span class="form-label">拉取日期</span>
            <el-radio-group v-model="dateType" class="date-radio">
              <el-radio-button label="today">当日</el-radio-button>
              <el-radio-button label="custom">自定义</el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-if="dateType === 'custom'"
              v-model="customDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              class="date-picker"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-item btn-item">
            <el-button
              type="primary"
              size="large"
              :loading="pullLoading"
              :disabled="!canPull"
              @click="handlePull"
            >
              {{ pullMode === 'all' ? '立即拉取全部门店' : '立即拉取门店订单' }}
            </el-button>
          </div>
        </div>

        <el-divider>
          <span class="divider-text">定时同步</span>
        </el-divider>

        <div class="form-row">
          <div class="form-item">
            <span class="form-label">定时同步</span>
            <el-switch
              v-model="scheduleEnabled"
              active-text="开启"
              inactive-text="关闭"
              inline-prompt
            />
          </div>
        </div>
        <div class="form-row" v-if="scheduleEnabled">
          <div class="form-item">
            <span class="form-label">拉取间隔</span>
            <el-select v-model="intervalMinutes" class="interval-select" placeholder="选择间隔">
              <el-option
                v-for="opt in intervalOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
        </div>
        <div class="form-row" v-if="scheduleEnabled && currentCron">
          <div class="form-item">
            <span class="form-label">当前CRON</span>
            <span class="cron-text">{{ currentCron }}</span>
          </div>
        </div>
        <div class="form-row" v-if="scheduleEnabled">
          <div class="form-item btn-item">
            <el-button
              type="success"
              :loading="scheduleSaving"
              @click="saveScheduleConfig"
            >
              保存定时配置
            </el-button>
          </div>
        </div>
        <div class="schedule-tip" v-if="scheduleEnabled">
          💡 开启后按设定间隔自动拉取全部门店增量订单（从上次同步时间到当前）
        </div>
      </div>
    </div>

    <div class="pull-progress-card" v-if="pullProgress">
      <div class="section-header">
        <span class="section-title">拉取进度</span>
      </div>
      <div class="progress-summary">
        <div class="summary-item">
          <span class="summary-label">门店总数:</span>
          <span class="summary-value">{{ pullProgress.totalCount }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">成功:</span>
          <span class="summary-value success">{{ pullProgress.successCount }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">失败:</span>
          <span class="summary-value fail">{{ pullProgress.failCount }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">耗时:</span>
          <span class="summary-value">{{ pullProgress.elapsedSeconds }}秒</span>
        </div>
      </div>
      <el-alert
        v-if="pullProgress.completed"
        :title="pullProgress.failCount > 0 ? '部分门店拉取失败' : '全部拉取完成'"
        :type="pullProgress.failCount > 0 ? 'warning' : 'success'"
        :closable="false"
        show-icon
      />
      <el-alert v-else title="正在拉取订单..." type="info" :closable="false" show-icon />
      <div
        v-if="pullProgress.failedStores && pullProgress.failedStores.length > 0"
        class="failed-stores"
      >
        <div class="failed-title">失败门店列表:</div>
        <div class="failed-list">
          <div v-for="store in pullProgress.failedStores" :key="store" class="failed-item">
            <span class="failed-store-name">{{ getStoreNameById(store) }}</span>
            <span class="failed-store-id">({{ store }})</span>
          </div>
        </div>
      </div>
    </div>

    <div class="table-container">
      <div class="section-header">
        <span class="section-title">同步日志</span>
      </div>
      <el-table
        :data="syncLogList"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
      >
        <el-table-column prop="storeName" label="门店名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="platformStoreId" label="平台门店ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="syncCount" label="同步订单数" width="120" align="center" />
        <el-table-column prop="successCount" label="成功" width="100" align="center">
          <template #default="{ row }">
            <span class="text-success">{{ row.successCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="failCount" label="失败" width="100" align="center">
          <template #default="{ row }">
            <span class="text-danger">{{ row.failCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="同步耗时" width="100" align="center">
          <template #default="{ row }">
            <span>{{ calculateDuration(row) }}秒</span>
          </template>
        </el-table-column>
        <el-table-column label="同步开始时间" min-width="170">
          <template #default="{ row }">
            <span>{{ formatTimestamp(row.syncStartTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="同步结束时间" min-width="170">
          <template #default="{ row }">
            <span>{{ formatTimestamp(row.syncEndTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span
              v-if="row.failCount > 0 && row.errorMsg"
              class="error-msg"
              @click="showErrorDetail(row.errorMsg)"
            >
              {{ row.errorMsg }}
            </span>
            <span v-else>--</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @pagination="getSyncLogs"
      />
    </div>

    <el-dialog title="失败详情" v-model="errorDialogVisible" width="600px" destroy-on-close>
      <div class="error-detail">
        <pre>{{ currentError }}</pre>
      </div>
      <template #footer>
        <el-button @click="errorDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pullSingleStore,
  pullAllStores,
  getSyncLogPage,
  getSyncScheduleConfig,
  updateSyncScheduleConfig,
  getSyncProgress
} from '@/api/ele/orderSync'
import { TableApi } from '@/api/business/store'
import Pagination from '@/components/Pagination/index.vue'

const pullMode = ref<'single' | 'all'>('single')
const storeList = ref<any[]>([])
const storeLoading = ref(false)
const selectedStoreId = ref<string | null>(null)
const dateType = ref<'today' | 'custom'>('today')
const customDate = ref<string | null>(null)
const pullLoading = ref(false)
const loading = ref(true)
const total = ref(0)
const syncLogList = ref<any[]>([])
const errorDialogVisible = ref(false)
const currentError = ref('')
const pullProgress = ref<any>(null)

const scheduleEnabled = ref(false)
const intervalMinutes = ref(60)
const currentCron = ref('')
const scheduleSaving = ref(false)

const currentTaskId = ref<string | null>(null)
const pollingTimer = ref<ReturnType<typeof setInterval> | null>(null)
const MAX_POLL_COUNT = 120
const POLL_INTERVAL = 3000

const intervalOptions = [
  { label: '5分钟', value: 5 },
  { label: '10分钟', value: 10 },
  { label: '15分钟', value: 15 },
  { label: '30分钟', value: 30 },
  { label: '60分钟', value: 60 },
  { label: '120分钟', value: 120 }
]

const queryParams = ref({
  pageNum: 1,
  pageSize: 20,
  platformStoreId: null as string | null,
  startTime: null as number | null,
  endTime: null as number | null
})

const canPull = computed(() => {
  if (pullMode.value === 'single') {
    return !!selectedStoreId.value
  }
  return true
})

const getDayTimestamps = (dateStr?: string) => {
  let date: Date
  if (dateStr) {
    const [y, m, d] = dateStr.split('-').map(Number)
    date = new Date(y, m - 1, d)
  } else {
    date = new Date()
    date.setHours(0, 0, 0, 0)
  }
  const start = Math.floor(date.getTime() / 1000)
  const end = start + 86399
  return { start, end }
}

const getNowDayTimestamps = () => {
  const now = new Date()
  const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const start = Math.floor(startOfDay.getTime() / 1000)
  const nowSec = Math.floor(now.getTime() / 1000)
  return { start, end: nowSec }
}

const normalizeTimestamp = (ts: number | null | undefined): number | null => {
  if (!ts) return null
  return ts > 100000000000 ? ts : ts * 1000
}

const getStoreNameById = (platformStoreId: string) => {
  const store = storeList.value.find((s) => s.platformStoreId === platformStoreId)
  return store ? store.storeName : platformStore || '--'
}

const loadStoreList = async () => {
  storeLoading.value = true
  try {
    const res = await TableApi.getTableAllSimpleList(1)
    const list = Array.isArray(res) ? res : []
    storeList.value = list.sort((a: any, b: any) => {
      const aStatus = a.storeStatus ?? 1
      const bStatus = b.storeStatus ?? 1
      return bStatus - aStatus
    })
  } catch {
    storeList.value = []
  } finally {
    storeLoading.value = false
  }
}

const getSyncLogs = async () => {
  loading.value = true
  const timeRange = dateType.value === 'today' ? getNowDayTimestamps() : getDayTimestamps(customDate.value || undefined)

  try {
    const response = await getSyncLogPage({
      pageNo: queryParams.value.pageNum,
      pageSize: queryParams.value.pageSize,
      platformStoreId: pullMode.value === 'single' ? selectedStoreId.value : undefined,
      startTime: timeRange.start,
      endTime: timeRange.end
    })
    syncLogList.value = response?.list || []
    total.value = response?.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '查询失败')
    syncLogList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const startPolling = (taskId: string) => {
  stopPolling()
  currentTaskId.value = taskId
  pullProgress.value = {
    totalCount: 1,
    successCount: 0,
    failCount: 0,
    elapsedSeconds: 0,
    completed: false,
    failedStores: []
  }

  let pollCount = 0
  const startMs = Date.now()

  pollingTimer.value = setInterval(async () => {
    pollCount++
    try {
      const progress = await getSyncProgress(taskId)
      const elapsed = Math.floor((Date.now() - startMs) / 1000)

      if (progress && (progress.status === 'SUCCESS' || progress.status === 'FAILED')) {
        stopPolling()
        pullProgress.value = {
          totalCount: 1,
          successCount: progress.status === 'SUCCESS' ? 1 : 0,
          failCount: progress.status === 'FAILED' ? 1 : 0,
          elapsedSeconds: elapsed,
          completed: true,
          failedStores: progress.status === 'FAILED' ? [progress.platformStoreId || ''] : []
        }
        if (progress.status === 'SUCCESS') {
          ElMessage.success('订单同步完成')
        } else {
          ElMessage.error(progress.errorMessage || '订单同步失败')
        }
        await getSyncLogs()
        currentTaskId.value = null
        return
      }

      pullProgress.value = {
        totalCount: 1,
        successCount: 0,
        failCount: 0,
        elapsedSeconds: elapsed,
        completed: false,
        failedStores: []
      }

      if (pollCount >= MAX_POLL_COUNT) {
        stopPolling()
        pullProgress.value = {
          totalCount: 1,
          successCount: 0,
          failCount: 0,
          elapsedSeconds: elapsed,
          completed: true,
          failedStores: []
        }
        ElMessage.warning('同步任务超时，请稍后在同步日志中查看结果')
        getSyncLogs()
      }
    } catch {
      if (pollCount >= MAX_POLL_COUNT) {
        stopPolling()
        getSyncLogs()
      }
    }
  }, POLL_INTERVAL)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

const handlePull = async () => {
  if (pullMode.value === 'single' && !selectedStoreId.value) {
    ElMessage.warning('请选择要拉取的门店')
    return
  }

  const timeRange = dateType.value === 'today' ? getNowDayTimestamps() : getDayTimestamps(customDate.value || undefined)

  const confirmMsg =
    pullMode.value === 'all'
      ? '确定要拉取全部门店的订单吗？'
      : `确定要拉取门店「${getStoreNameById(selectedStoreId.value!)}」的订单吗？`

  try {
    await ElMessageBox.confirm(confirmMsg, '确认拉取订单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  pullLoading.value = true
  pullProgress.value = null

  try {
    if (pullMode.value === 'all') {
      const result = await pullAllStores({ startTime: timeRange.start, endTime: timeRange.end })
      if (result && typeof result === 'object' && (result as any).taskId) {
        startPolling((result as any).taskId)
        ElMessage.info('订单同步任务已提交，正在等待结果...')
      } else {
        pullProgress.value = {
          totalCount: 1,
          successCount: 1,
          failCount: 0,
          elapsedSeconds: 0,
          completed: true,
          failedStores: []
        }
        ElMessage.success('全部门店订单同步完成')
        await getSyncLogs()
      }
    } else {
      const result = await pullSingleStore({
        platformStoreId: selectedStoreId.value!,
        startTime: timeRange.start,
        endTime: timeRange.end
      })
      if (result && typeof result === 'object' && (result as any).taskId) {
        startPolling((result as any).taskId)
        ElMessage.info('订单同步任务已提交，正在等待结果...')
      } else {
        pullProgress.value = {
          totalCount: 1,
          successCount: 1,
          failCount: 0,
          elapsedSeconds: 0,
          completed: true,
          failedStores: []
        }
        ElMessage.success('门店订单同步完成')
        await getSyncLogs()
      }
    }
  } catch (error: any) {
    pullProgress.value = {
      totalCount: 1,
      successCount: 0,
      failCount: 1,
      elapsedSeconds: 0,
      completed: true,
      failedStores: []
    }
    ElMessage.error(error?.message || '订单拉取失败')
  } finally {
    pullLoading.value = false
  }
}

const calculateDuration = (row: any) => {
  if (row.syncStartTime && row.syncEndTime) {
    let startTs = Number(row.syncStartTime)
    let endTs = Number(row.syncEndTime)
    if (startTs < 100000000000) startTs *= 1000
    if (endTs < 100000000000) endTs *= 1000
    const durationMs = endTs - startTs
    if (durationMs > 0 && durationMs < 86400000) {
      return (durationMs / 1000).toFixed(1)
    }
  }
  return '--'
}

const formatTimestamp = (timestamp: number | null | undefined) => {
  if (!timestamp) return '--'
  const ts = normalizeTimestamp(timestamp)
  if (!ts) return '--'
  const date = new Date(ts)
  if (isNaN(date.getTime())) return '--'
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hours = `${date.getHours()}`.padStart(2, '0')
  const minutes = `${date.getMinutes()}`.padStart(2, '0')
  const seconds = `${date.getSeconds()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const showErrorDetail = (errorMsg: string) => {
  currentError.value = errorMsg
  errorDialogVisible.value = true
}

const loadScheduleConfig = async () => {
  try {
    const res = await getSyncScheduleConfig()
    const data = (res as any)?.data || res || {}
    if (data.exists) {
      scheduleEnabled.value = !!data.enabled
      intervalMinutes.value = data.intervalMinutes || 60
      currentCron.value = data.cronExpression || ''
    }
  } catch {
    scheduleEnabled.value = false
  }
}

const saveScheduleConfig = async () => {
  scheduleSaving.value = true
  try {
    await updateSyncScheduleConfig({
      intervalMinutes: intervalMinutes.value,
      enabled: scheduleEnabled.value
    })
    ElMessage.success('定时同步配置已保存')
    await loadScheduleConfig()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存定时配置失败')
  } finally {
    scheduleSaving.value = false
  }
}

onMounted(() => {
  loadStoreList()
  getSyncLogs()
  loadScheduleConfig()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style lang="scss" scoped>
.app-container {
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
  padding: 24px;
}

.pull-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05), 0 1px 3px rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(226, 232, 240, 0.6);
  margin-bottom: 20px;
  overflow: hidden;

  .pull-card-header {
    padding: 16px 24px;
    border-bottom: 1px solid rgba(226, 232, 240, 0.6);
    background: #f8fafc;

    .header-title {
      font-size: 16px;
      font-weight: 700;
      color: #1e293b;
    }
  }

  .pull-card-body {
    padding: 24px;

    .form-row {
      display: flex;
      align-items: center;
      margin-bottom: 20px;
      gap: 16px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .form-item {
      display: flex;
      align-items: center;
      gap: 12px;

      .form-label {
        font-size: 13px;
        color: #64748b;
        white-space: nowrap;
        font-weight: 500;
        min-width: 70px;
      }
    }

    .mode-radio {
      :deep(.el-radio-button__inner) {
        padding: 8px 16px;
        font-size: 14px;
        background: #f8fafc;
        border-color: #e2e8f0;
        color: #64748b;
        transition: all 0.2s ease;
      }

      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
        border-color: #6366f1;
        color: #fff;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
      }
    }

    .date-radio {
      :deep(.el-radio-button__inner) {
        padding: 6px 14px;
        font-size: 13px;
        background: #f8fafc;
        border-color: #e2e8f0;
        color: #64748b;
        transition: all 0.2s ease;
      }

      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
        border-color: #0ea5e9;
        color: #fff;
        box-shadow: 0 4px 12px rgba(14, 165, 233, 0.35);
      }
    }

    .store-select {
      width: 240px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #6366f1;
        }
      }

      :deep(.el-input__inner) {
        color: #334155;
      }
    }

    .date-picker {
      width: 160px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #0ea5e9;
        }
      }
    }

    .btn-item {
      margin-top: 8px;

      .el-button {
        min-width: 200px;
        background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
        border: none;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
        transition: all 0.2s ease;
        border-radius: 8px;
        font-size: 15px;
        font-weight: 600;

        &:hover:not(:disabled) {
          transform: translateY(-1px);
          box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }

      .el-button--success {
        min-width: 140px;
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);

        &:hover:not(:disabled) {
          box-shadow: 0 6px 20px rgba(16, 185, 129, 0.45);
        }
      }
    }

    .interval-select {
      width: 160px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #10b981;
        }
      }
    }

    .cron-text {
      font-size: 13px;
      color: #64748b;
      font-family: 'Courier New', monospace;
      background: #f1f5f9;
      padding: 4px 10px;
      border-radius: 6px;
      border: 1px solid #e2e8f0;
    }

    .schedule-tip {
      margin-top: 8px;
      font-size: 12px;
      color: #94a3b8;
      line-height: 1.6;
    }
  }

  :deep(.el-divider) {
    margin: 20px 0 16px;

    .divider-text {
      font-size: 13px;
      color: #94a3b8;
      font-weight: 500;
    }
  }

  :deep(.el-switch) {
    --el-switch-on-color: #10b981;
  }
}

.pull-progress-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(226, 232, 240, 0.6);

  .progress-summary {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    padding: 16px;
    background: #f8fafc;
    border-radius: 12px;
    margin-bottom: 16px;

    .summary-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;

      .summary-label {
        font-size: 12px;
        color: #64748b;
      }

      .summary-value {
        font-size: 24px;
        font-weight: 700;
        color: #1e293b;

        &.success {
          color: #10b981;
        }

        &.fail {
          color: #ef4444;
        }
      }
    }
  }

  .failed-stores {
    margin-top: 16px;

    .failed-title {
      font-size: 14px;
      font-weight: 600;
      color: #334155;
      margin-bottom: 10px;
    }

    .failed-list {
      max-height: 200px;
      overflow-y: auto;

      .failed-item {
        padding: 8px 12px;
        background: #fef2f2;
        border: 1px solid #fecaca;
        border-radius: 8px;
        margin-bottom: 8px;
        font-size: 13px;

        .failed-store-name {
          color: #ef4444;
          font-weight: 600;
        }

        .failed-store-id {
          color: #94a3b8;
          margin-left: 8px;
          font-size: 12px;
        }
      }
    }
  }
}

.table-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.section-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);

  .section-title {
    font-size: 16px;
    font-weight: 700;
    color: #1e293b;
    display: flex;
    align-items: center;
    gap: 10px;

    &::before {
      content: '';
      width: 4px;
      height: 20px;
      background: linear-gradient(180deg, #6366f1, #4f46e5);
      border-radius: 2px;
    }
  }
}

.text-success {
  color: #10b981;
  font-weight: 600;
}

.text-danger {
  color: #ef4444;
  font-weight: 600;
}

.error-msg {
  color: #ef4444;
  cursor: pointer;
  transition: color 0.2s ease;

  &:hover {
    color: #dc2626;
    text-decoration: underline;
  }
}

.el-table {
  font-size: 14px;

  :deep(.el-table__header-wrapper) {
    .el-table__header th {
      background-color: #f8fafc !important;
      color: #334155;
      font-weight: 700;
      text-align: center;
      border-bottom: 1px solid #e2e8f0 !important;
    }
  }

  :deep(.el-table__body-wrapper) {
    .el-table__row td {
      background: white !important;
      border-bottom: 1px solid #f1f5f9 !important;
      color: #475569;
    }

    .el-table__row:hover {
      background: #f8fafc !important;
    }
  }
}

.el-dialog {
  :deep(.el-dialog__header) {
    padding: 18px 24px;
    border-bottom: 1px solid #e2e8f0;
  }

  :deep(.el-dialog__title) {
    font-size: 17px;
    font-weight: 700;
    color: #1e293b;
  }

  :deep(.el-dialog__body) {
    padding: 24px;
    max-height: 60vh;
    overflow-y: auto;
    background: #f8fafc;
  }

  :deep(.el-dialog__footer) {
    padding: 14px 24px;
    border-top: 1px solid #e2e8f0;
  }
}

.error-detail {
  pre {
    white-space: pre-wrap;
    word-wrap: break-word;
    font-size: 14px;
    line-height: 1.6;
    color: #ef4444;
    padding: 16px;
    background: #fef2f2;
    border-radius: 8px;
    border: 1px solid #fecaca;
  }
}

:deep(.el-pagination) {
  margin-top: 24px;
  justify-content: center;

  .el-pager li {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
    border-radius: 8px;

    &:hover {
      color: #6366f1;
      border-color: #6366f1;
    }

    &.is-active {
      background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
      border-color: #6366f1;
      color: #fff;
    }
  }

  button {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
  }
}

@media (max-width: 768px) {
  .pull-card .pull-card-body .form-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .pull-progress-card .progress-summary {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
