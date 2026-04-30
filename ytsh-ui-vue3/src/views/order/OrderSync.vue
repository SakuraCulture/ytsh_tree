<template>
  <div class="app-container">
    <div class="pull-card">
      <div class="pull-card-header">
        <span class="header-title">📦 订单同步</span>
      </div>
      <div class="pull-card-body">
        <div class="settings-bar">
          <div class="setting-group">
            <span class="setting-label">拉取模式</span>
            <el-radio-group v-model="pullMode" class="mode-radio" size="small">
              <el-radio-button label="single">按门店</el-radio-button>
              <el-radio-button label="all">全部门店</el-radio-button>
            </el-radio-group>
          </div>
          <div class="setting-group">
            <span class="setting-label">定时同步</span>
            <el-switch
              v-if="pullMode === 'all'"
              v-model="scheduleEnabled"
              inline-prompt
              active-text="开"
              inactive-text="关"
              size="small"
            />
            <span v-else class="setting-disabled-text">—</span>
          </div>
          <div class="setting-group">
            <span class="setting-label">实时推送</span>
            <el-switch
              v-model="pushSetting.orderPushEnabled"
              inline-prompt
              active-text="开"
              inactive-text="关"
              size="small"
              @change="handlePushSettingChange"
            />
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
        <div class="form-row" v-if="pullMode === 'single'">
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
        <div class="form-row" v-if="pullMode === 'all'">
          <div class="form-item">
            <span class="form-label">开始日期</span>
            <el-date-picker
              v-model="startDate"
              type="date"
              placeholder="选择开始日期"
              value-format="YYYY-MM-DD"
              class="date-picker"
            />
          </div>
          <div class="form-item">
            <span class="form-label">结束日期</span>
            <el-date-picker
              v-model="endDate"
              type="date"
              placeholder="选择结束日期（可选）"
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
        <div class="detail-sections" v-if="pullMode === 'all'">
          <div class="schedule-section" v-if="scheduleEnabled">
            <div class="form-row">
              <div class="form-item">
                <span class="form-label">定时模式</span>
                <el-radio-group v-model="scheduleType" class="schedule-mode-radio" size="small">
                  <el-radio-button label="time">指定时间</el-radio-button>
                  <el-radio-button label="dayOfMonth">指定天数</el-radio-button>
                  <el-radio-button label="weekDay">指定星期</el-radio-button>
                  <el-radio-button label="interval">间隔时间</el-radio-button>
                </el-radio-group>
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'time'">
              <div class="form-item">
                <span class="form-label">执行时间</span>
                <el-time-picker
                  v-model="newTimePoint"
                  format="HH:mm:ss"
                  value-format="HH:mm:ss"
                  placeholder="选择时间"
                  class="time-picker"
                  size="small"
                />
                <el-button @click="addTimePoint" :disabled="!newTimePoint" size="small"
                  >添加</el-button
                >
                <div class="time-points-list" v-if="scheduleTimePoints.length > 0">
                  <el-tag
                    v-for="(time, index) in scheduleTimePoints"
                    :key="index"
                    closable
                    @close="removeTimePoint(index)"
                    class="time-tag"
                    size="small"
                  >
                    {{ time }}
                  </el-tag>
                </div>
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'dayOfMonth'">
              <div class="form-item">
                <span class="form-label">选择天数</span>
                <el-date-picker
                  v-model="selectedMonthDays"
                  type="dates"
                  placeholder="点击日历选择多个日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  class="month-day-picker"
                  size="small"
                  :clearable="false"
                />
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'dayOfMonth'">
              <div class="form-item">
                <span class="form-label">执行时间</span>
                <el-time-picker
                  v-model="dayOfMonthTime"
                  format="HH:mm:ss"
                  value-format="HH:mm:ss"
                  class="time-picker"
                  size="small"
                />
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'weekDay'">
              <div class="form-item">
                <span class="form-label">选择星期</span>
                <el-select
                  v-model="selectedWeekDays"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="选择星期"
                  class="week-select"
                  size="small"
                >
                  <el-option
                    v-for="opt in weekDaysOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'weekDay'">
              <div class="form-item">
                <span class="form-label">执行时间</span>
                <el-time-picker
                  v-model="weekDayTime"
                  format="HH:mm:ss"
                  value-format="HH:mm:ss"
                  class="time-picker"
                  size="small"
                />
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'interval'">
              <div class="form-item">
                <span class="form-label">开始时间</span>
                <el-time-picker
                  v-model="intervalStartTime"
                  format="HH:mm:ss"
                  value-format="HH:mm:ss"
                  placeholder="选择开始时间"
                  class="time-picker"
                  size="small"
                />
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'interval'">
              <div class="form-item">
                <span class="form-label">间隔时间</span>
                <el-select
                  v-model="intervalHours"
                  placeholder="选择间隔"
                  class="interval-select"
                  size="small"
                >
                  <el-option
                    v-for="opt in intervalHoursOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </div>
            </div>
            <div class="form-row" v-if="generatedCron">
              <div class="form-item">
                <span class="form-label">CRON表达式</span>
                <span class="cron-text">{{ generatedCron }}</span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-item btn-item">
                <el-button
                  type="success"
                  :loading="scheduleSaving"
                  @click="saveScheduleConfig"
                  size="small"
                >
                  保存定时配置
                </el-button>
              </div>
            </div>
            <div class="schedule-tip">💡 按设定规则自动拉取全部门店订单</div>
          </div>

          <div class="push-section" v-if="pushSetting.orderPushEnabled">
            <div class="form-row">
              <div class="form-item">
                <span class="form-label">推送状态</span>
                <el-select
                  v-model="pushSetting.orderPushTypesArray"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="选择推送状态"
                  @change="handlePushSettingChange"
                  class="push-status-select"
                  size="small"
                >
                  <el-option label="已支付(1)" value="1" />
                  <el-option label="已接单(2)" value="2" />
                  <el-option label="已拣货(3)" value="3" />
                  <el-option label="已打包(4)" value="4" />
                  <el-option label="已发货(5)" value="5" />
                  <el-option label="交易成功(6)" value="6" />
                  <el-option label="交易关闭(-1)" value="-1" />
                </el-select>
              </div>
              <div class="form-item" style="margin-left: 16px">
                <span class="form-label">桌面通知</span>
                <el-switch
                  v-model="pushSetting.orderPushDesktop"
                  inline-prompt
                  active-text="开"
                  inactive-text="关"
                  size="small"
                  @change="handleDesktopNotificationChange"
                />
              </div>
            </div>
            <div class="push-status-tip">💡 订单状态变更时将实时推送通知</div>
          </div>
        </div>
      </div>
    </div>

    <div class="sync-progress-overlay" v-if="showBatchSyncProgress">
      <div class="sync-progress-container">
        <div class="sync-header">
          <span class="sync-icon">{{
            batchProgress.syncStatus === 'COMPLETED'
              ? '✅'
              : batchProgress.syncStatus === 'FAILED'
                ? '❌'
                : '🔄'
          }}</span>
          <span class="sync-title">{{ syncProgressTitle }}</span>
        </div>
        <div v-if="batchProgress.isSyncing" class="sync-detail">
          <el-progress
            :percentage="syncProgressPercent"
            :status="syncProgressPercent === 100 ? 'success' : ''"
          />
          <div class="sync-stats">
            <div class="stat-item">
              <span class="stat-label">已拉取订单</span>
              <span class="stat-value">{{ batchProgress.totalOrders }}</span>
            </div>
            <div class="stat-item success">
              <span class="stat-label">成功订单</span>
              <span class="stat-value">{{ batchProgress.successOrders }}</span>
            </div>
            <div class="stat-item failed">
              <span class="stat-label">失败订单</span>
              <span class="stat-value">{{ batchProgress.failOrders }}</span>
            </div>
          </div>
          <div v-if="batchProgress.currentSyncingStores.length > 0" class="syncing-info">
            正在同步: {{ batchProgress.currentSyncingStores.slice(0, 3).join('、') }}
            <span v-if="batchProgress.currentSyncingStores.length > 3"
              >等{{ batchProgress.currentSyncingCount }}家门店</span
            >
          </div>
        </div>
        <div v-else class="sync-complete">
          <div class="sync-stats">
            <div class="stat-item">
              <span class="stat-label">总计订单</span>
              <span class="stat-value">{{ batchProgress.totalOrders }}</span>
            </div>
            <div class="stat-item success">
              <span class="stat-label">成功订单</span>
              <span class="stat-value">{{ batchProgress.successOrders }}</span>
            </div>
            <div class="stat-item failed">
              <span class="stat-label">失败订单</span>
              <span class="stat-value">{{ batchProgress.failOrders }}</span>
            </div>
          </div>
          <div v-if="batchProgress.failedStores.length > 0" class="failed-stores-section">
            <div class="failed-section-title">失败门店详情</div>
            <div class="failed-store-list">
              <div
                v-for="(store, index) in batchProgress.failedStoreDetails"
                :key="index"
                class="failed-store-item"
              >
                <span class="failed-store-name">{{
                  store.storeName || store.platformStoreId
                }}</span>
                <span v-if="store.platformStoreId" class="failed-store-id"
                  >({{ store.platformStoreId }})</span
                >
                <span v-if="store.errorMsg" class="failed-store-error">{{ store.errorMsg }}</span>
              </div>
            </div>
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
        <el-table-column
          prop="syncBatchId"
          label="同步批次"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column prop="storeName" label="门店名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="syncMode" label="同步模式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.syncMode === 'MULTI' ? 'success' : 'info'" size="small">
              {{ row.syncMode === 'MULTI' ? '多线程' : '单线程' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="threadCount" label="线程数" width="80" align="center" />
        <el-table-column prop="totalPulled" label="API拉取" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.totalPulled || row.syncCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="syncCount" label="同步订单数" width="110" align="center">
          <template #default="{ row }">
            <span>{{ row.syncCount ?? '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="successCount" label="成功" width="90" align="center">
          <template #default="{ row }">
            <span class="text-success">{{ row.successCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="failCount" label="失败" width="90" align="center">
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

    <el-dialog
      :title="alertDialogType === 'critical' ? '🚨 严重告警' : '⚠️ 超时警告'"
      v-model="alertDialogVisible"
      :width="alertDialogWidth"
      destroy-on-close
      :class="alertDialogType === 'critical' ? 'critical-alert-dialog' : 'warning-alert-dialog'"
    >
      <div class="alert-detail">
        <p class="alert-summary-text">
          {{
            alertDialogType === 'critical'
              ? '以下订单已超过5天未完结，请尽快处理！'
              : '以下订单已超过3天未完结，请关注处理进度。'
          }}
        </p>
        <el-table :data="alertList" border stripe class="alert-table">
          <el-table-column prop="orderId" label="订单号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="erpStoreCode" label="门店编码" width="120" align="center" />
          <el-table-column
            prop="platformStoreId"
            label="平台门店ID"
            width="130"
            show-overflow-tooltip
          />
          <el-table-column prop="orderStatus" label="当前状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.orderStatus)" size="small">
                {{ getStatusName(row.orderStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTimeStr" label="创建时间" width="160" align="center" />
          <el-table-column prop="daysElapsed" label="已过天数" width="90" align="center">
            <template #default="{ row }">
              <span :class="row.daysElapsed >= 5 ? 'days-critical' : 'days-warning'">
                {{ row.daysElapsed }}天
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="handleDismissAlerts" type="primary">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  pullSingleStore,
  pullAllStores,
  getSyncLogPage,
  getSyncScheduleConfig,
  updateSyncScheduleConfig,
  getSyncProgress,
  getBatchSyncProgress,
  type SyncScheduleConfigReqVO
} from '@/api/ele/orderSync'
import { getOrderPushSetting, updateOrderPushSetting } from '@/api/ele/orderPush'
import {
  getUnshownAlerts,
  markAllAlertsAsShown,
  type OrderTrackingAlertVO
} from '@/api/ele/orderTracking'
import { TableApi } from '@/api/business/store'
import Pagination from '@/components/Pagination/index.vue'

const pullMode = ref<'single' | 'all'>('single')
const storeList = ref<any[]>([])
const storeLoading = ref(false)
const selectedStoreId = ref<string | null>(null)
const dateType = ref<'today' | 'custom'>('today')
const customDate = ref<string | null>(null)
const startDate = ref<string | null>(null)
const endDate = ref<string | null>(null)
const pullLoading = ref(false)
const loading = ref(true)
const total = ref(0)
const syncLogList = ref<any[]>([])
const errorDialogVisible = ref(false)
const currentError = ref('')

const showBatchSyncProgress = ref(false)
const batchProgress = ref({
  isSyncing: false,
  syncStatus: 'IDLE',
  totalStores: 0,
  completedStores: 0,
  successStores: 0,
  failedStores: 0,
  currentSyncingCount: 0,
  currentSyncingStores: [] as string[],
  failedStoreDetails: [] as { platformStoreId: string; storeName?: string; errorMsg?: string }[],
  startTime: 0,
  totalOrders: 0,
  successOrders: 0,
  failOrders: 0
})
const batchSyncPollingTimer = ref<ReturnType<typeof setInterval> | null>(null)

const scheduleEnabled = ref(false)
const intervalMinutes = ref(60)
const currentCron = ref('')
const scheduleSaving = ref(false)

const scheduleType = ref<'time' | 'dayOfMonth' | 'weekDay' | 'interval'>('time')
const scheduleTimePoints = ref<string[]>([])
const newTimePoint = ref<string | null>(null)
const selectedMonthDays = ref<string[]>([])
const selectedDaysOfMonth = ref<number[]>([])
const dayOfMonthTime = ref('00:00:00')
const selectedWeekDays = ref<number[]>([])
const weekDayTime = ref('09:00:00')
const intervalStartTime = ref('00:00:00')
const intervalHours = ref<number>(1)

const daysOfMonthOptions = Array.from({ length: 31 }, (_, i) => ({
  label: `${i + 1}号`,
  value: i + 1
}))

const intervalHoursOptions = [
  { label: '1小时', value: 1 },
  { label: '2小时', value: 2 },
  { label: '3小时', value: 3 },
  { label: '5小时', value: 5 },
  { label: '12小时', value: 12 },
  { label: '24小时', value: 24 }
]

const weekDaysOptions = [
  { label: '周一', value: 2 },
  { label: '周二', value: 3 },
  { label: '周三', value: 4 },
  { label: '周四', value: 5 },
  { label: '周五', value: 6 },
  { label: '周六', value: 7 },
  { label: '周日', value: 1 }
]

const currentTaskId = ref<string | null>(null)
const pollingTimer = ref<ReturnType<typeof setInterval> | null>(null)
const MAX_POLL_COUNT = 120
const POLL_INTERVAL = 1000

const pushSetting = ref({
  orderPushEnabled: true,
  orderPushTypes: '',
  orderPushTypesArray: [] as string[],
  orderPushDesktop: false
})

const websocket = ref<WebSocket | null>(null)

const alertDialogVisible = ref(false)
const alertDialogType = ref<'warning' | 'critical'>('warning')
const alertList = ref<OrderTrackingAlertVO[]>([])
const alertPollingTimer = ref<ReturnType<typeof setInterval> | null>(null)

const alertDialogWidth = computed(() => {
  return alertList.value.length > 5 ? '90%' : '800px'
})

const queryParams = ref({
  pageNum: 1,
  pageSize: 20,
  platformStoreId: null as string | null
})

const canPull = computed(() => {
  if (pullMode.value === 'single') {
    return !!selectedStoreId.value
  }
  return !!startDate.value
})

const syncProgressPercent = computed(() => {
  if (!batchProgress.value.totalStores) return 0
  return Math.round((batchProgress.value.completedStores / batchProgress.value.totalStores) * 100)
})

const syncProgressTitle = computed(() => {
  if (batchProgress.value.syncStatus === 'COMPLETED') {
    return '拉取完成'
  }
  if (batchProgress.value.syncStatus === 'FAILED') {
    return '拉取失败'
  }
  return `正在拉取门店订单 (${batchProgress.value.completedStores}/${batchProgress.value.totalStores})`
})

watch(
  () => selectedMonthDays.value,
  (dates: string[]) => {
    selectedDaysOfMonth.value = [...new Set(dates.map((d) => new Date(d).getDate()))].sort(
      (a, b) => a - b
    )
  },
  { deep: true }
)

const generatedCron = computed(() => {
  if (!scheduleEnabled.value) return ''

  if (scheduleType.value === 'time') {
    if (scheduleTimePoints.value.length === 0) return ''
    const hours = [...new Set(scheduleTimePoints.value.map((t) => t.split(':')[0]))].join(',')
    return `0 0 ${hours} * * ?`
  }

  if (scheduleType.value === 'dayOfMonth') {
    if (selectedDaysOfMonth.value.length === 0) return ''
    const [h, m, s] = dayOfMonthTime.value.split(':')
    const days = selectedDaysOfMonth.value.join(',')
    return `${s} ${m} ${h} ${days} * ?`
  }

  if (scheduleType.value === 'weekDay') {
    if (selectedWeekDays.value.length === 0) return ''
    const [h, m, s] = weekDayTime.value.split(':')
    const weeks = selectedWeekDays.value.join(',')
    return `${s} ${m} ${h} ? * ${weeks}`
  }

  if (scheduleType.value === 'interval') {
    if (!intervalStartTime.value) return ''
    const [h, m, s] = intervalStartTime.value.split(':')
    return `0 ${m} ${h}/${intervalHours.value} * * ?`
  }

  return ''
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

const getDayStartTimestamp = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  return Math.floor(date.getTime() / 1000)
}

const getDayEndTimestamp = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-').map(Number)
  const date = new Date(y, m - 1, d, 23, 59, 59)
  return Math.floor(date.getTime() / 1000)
}

const getPullTimeRange = () => {
  if (pullMode.value === 'all') {
    const startTs = getDayStartTimestamp(startDate.value!)
    const endTs = endDate.value ? getDayEndTimestamp(endDate.value) : Math.floor(Date.now() / 1000)
    return { start: startTs, end: endTs }
  }
  return dateType.value === 'today'
    ? getNowDayTimestamps()
    : getDayTimestamps(customDate.value || undefined)
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
  return store ? store.storeName : platformStoreId || '--'
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

const handlePaginationChange = ({ page, limit }) => {
  queryParams.value.pageNum = page
  queryParams.value.pageSize = limit
  getSyncLogs()
}

const getSyncLogs = async () => {
  loading.value = true

  const requestParams: any = {
    pageNo: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize
  }

  if (pullMode.value === 'single' && selectedStoreId.value) {
    requestParams.platformStoreId = selectedStoreId.value
  }

  try {
    const response = await getSyncLogPage(requestParams)
    const list = (response as any)?.list || (response as any)?.data?.list || []
    const totalVal = (response as any)?.total ?? (response as any)?.data?.total ?? 0
    syncLogList.value = list
    total.value = totalVal
  } catch (error: any) {
    console.error('同步日志查询失败:', error)
    syncLogList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const startPolling = (taskId: string) => {
  stopPolling()
  currentTaskId.value = taskId
  showBatchSyncProgress.value = true
  batchProgress.value = {
    isSyncing: true,
    syncStatus: 'RUNNING',
    totalStores: 1,
    completedStores: 0,
    successStores: 0,
    failedStores: 0,
    currentSyncingCount: 1,
    currentSyncingStores: selectedStoreId.value ? [getStoreNameById(selectedStoreId.value)] : [],
    failedStoreDetails: [],
    startTime: Date.now(),
    totalOrders: 0,
    successOrders: 0,
    failOrders: 0
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
        const isSuccess = progress.status === 'SUCCESS'
        batchProgress.value = {
          isSyncing: false,
          syncStatus: isSuccess ? 'COMPLETED' : 'FAILED',
          totalStores: 1,
          completedStores: 1,
          successStores: isSuccess ? 1 : 0,
          failedStores: isSuccess ? 0 : 1,
          currentSyncingCount: 0,
          currentSyncingStores: [],
          failedStoreDetails: isSuccess
            ? []
            : [
                {
                  platformStoreId: progress.platformStoreId || '',
                  storeName: progress.platformStoreId
                    ? getStoreNameById(progress.platformStoreId)
                    : '',
                  errorMsg: progress.errorMessage || '同步失败'
                }
              ],
          startTime: Date.now() - elapsed * 1000,
          totalOrders: progress.pulledCount || 0,
          successOrders: isSuccess ? progress.pulledCount || 0 : 0,
          failOrders: isSuccess ? 0 : 1
        }

        setTimeout(() => {
          showBatchSyncProgress.value = false
        }, 5000)

        if (isSuccess) {
          ElMessage.success('门店订单同步完成')
        } else {
          ElMessage.error(progress.errorMessage || '门店订单同步失败')
        }
        await getSyncLogs()
        currentTaskId.value = null
        return
      }

      batchProgress.value = {
        ...batchProgress.value,
        isSyncing: true,
        totalOrders: progress?.pulledCount || 0,
        successOrders: progress?.pulledCount || 0,
        failOrders: 0
      }

      if (pollCount >= MAX_POLL_COUNT) {
        stopPolling()
        batchProgress.value = {
          isSyncing: false,
          syncStatus: 'FAILED',
          totalStores: 1,
          completedStores: 1,
          successStores: 0,
          failedStores: 1,
          currentSyncingCount: 0,
          currentSyncingStores: [],
          failedStoreDetails: [],
          startTime: Date.now() - elapsed * 1000,
          totalOrders: 0,
          successOrders: 0,
          failOrders: 0
        }

        setTimeout(() => {
          showBatchSyncProgress.value = false
        }, 5000)

        ElMessage.warning('同步任务超时，请稍后在同步日志中查看结果')
        getSyncLogs()
      }
    } catch {
      if (pollCount >= MAX_POLL_COUNT) {
        stopPolling()
        batchProgress.value.isSyncing = false
        batchProgress.value.syncStatus = 'FAILED'

        setTimeout(() => {
          showBatchSyncProgress.value = false
        }, 5000)

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

const startBatchSyncPolling = () => {
  stopBatchSyncPolling()
  batchSyncPollingTimer.value = setInterval(async () => {
    try {
      const res = await getBatchSyncProgress()
      if (res) {
        const data = (res as any)?.data || res
        batchProgress.value = {
          isSyncing: data?.isSyncing ?? false,
          syncStatus: data?.syncStatus ?? 'IDLE',
          totalStores: data?.totalStores ?? 0,
          completedStores: data?.completedStores ?? 0,
          successStores: data?.successStores ?? 0,
          failedStores: data?.failedStores ?? 0,
          currentSyncingCount: data?.currentSyncingCount ?? 0,
          currentSyncingStores: data?.currentSyncingStores ?? [],
          failedStoreDetails: batchProgress.value.failedStoreDetails,
          startTime: data?.startTime ?? 0,
          totalOrders: data?.totalOrders ?? 0,
          successOrders: data?.successOrders ?? 0,
          failOrders: data?.failOrders ?? 0
        }

        if (!data?.isSyncing || data?.syncStatus === 'COMPLETED' || data?.syncStatus === 'FAILED') {
          stopBatchSyncPolling()

          if (batchProgress.value.failedStores > 0) {
            try {
              const logsRes = await getSyncLogPage({
                pageNo: 1,
                pageSize: 100,
                status: 0
              })
              const logs = (logsRes as any)?.list || (logsRes as any)?.data?.list || []
              batchProgress.value.failedStoreDetails = logs
                .filter((log: any) => log.failCount > 0 || log.status === 0)
                .map((log: any) => ({
                  platformStoreId: log.platformStoreId || '',
                  storeName: log.storeName || '',
                  errorMsg: log.errorMsg || '同步失败'
                }))
            } catch {
              // 静默处理
            }
          }

          if (data?.syncStatus === 'COMPLETED' || batchProgress.value.completedStores > 0) {
            showBatchSyncProgress.value = true
            setTimeout(() => {
              showBatchSyncProgress.value = false
            }, 5000)

            ElMessage.success(
              `拉取完成！成功${batchProgress.value.successStores}家，失败${batchProgress.value.failedStores}家`
            )
          } else if (data?.syncStatus === 'FAILED') {
            ElMessage.error(
              `拉取失败！成功${batchProgress.value.successStores}家，失败${batchProgress.value.failedStores}家`
            )
          }
          await getSyncLogs()
        }
      }
    } catch (e) {
      // Silent fail for polling errors
    }
  }, 1000)
}

const stopBatchSyncPolling = () => {
  if (batchSyncPollingTimer.value) {
    clearInterval(batchSyncPollingTimer.value)
    batchSyncPollingTimer.value = null
  }
}

const handlePull = async () => {
  if (pullMode.value === 'single' && !selectedStoreId.value) {
    ElMessage.warning('请选择要拉取的门店')
    return
  }
  if (pullMode.value === 'all' && !startDate.value) {
    ElMessage.warning('请选择开始日期')
    return
  }

  const timeRange = getPullTimeRange()

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

  try {
    if (pullMode.value === 'all') {
      showBatchSyncProgress.value = true
      batchProgress.value = {
        isSyncing: true,
        syncStatus: 'RUNNING',
        totalStores: 0,
        completedStores: 0,
        successStores: 0,
        failedStores: 0,
        currentSyncingCount: 0,
        currentSyncingStores: [],
        startTime: 0
      }

      try {
        await pullAllStores({ startTime: timeRange.start, endTime: timeRange.end })
      } catch {
        // 后端返回的CommonResult<Boolean>可能导致axios拦截器报错，静默处理
      }
      startBatchSyncPolling()
      ElMessage.info('订单同步任务已提交，正在等待结果...')
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
        batchProgress.value = {
          isSyncing: false,
          syncStatus: 'COMPLETED',
          totalStores: 1,
          completedStores: 1,
          successStores: 1,
          failedStores: 0,
          currentSyncingCount: 0,
          currentSyncingStores: [],
          startTime: Date.now(),
          totalOrders: 0,
          successOrders: 0,
          failOrders: 0
        }
        ElMessage.success('门店订单同步完成')
        await getSyncLogs()
      }
    }
  } catch (error: any) {
    batchProgress.value = {
      isSyncing: false,
      syncStatus: 'FAILED',
      totalStores: 1,
      completedStores: 1,
      successStores: 0,
      failedStores: 1,
      currentSyncingCount: 0,
      currentSyncingStores: [],
      startTime: Date.now(),
      totalOrders: 0,
      successOrders: 0,
      failOrders: 0
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

const addTimePoint = () => {
  if (newTimePoint.value && !scheduleTimePoints.value.includes(newTimePoint.value)) {
    scheduleTimePoints.value.push(newTimePoint.value)
    scheduleTimePoints.value.sort()
  }
  newTimePoint.value = null
}

const removeTimePoint = (index: number) => {
  scheduleTimePoints.value.splice(index, 1)
}

const loadScheduleConfig = async () => {
  try {
    const res = await getSyncScheduleConfig()
    const data = (res as any)?.data || res || {}
    if (data.exists) {
      scheduleEnabled.value = !!data.enabled
      scheduleType.value = data.scheduleType || 'time'
      if (data.scheduleType === 'time') {
        scheduleTimePoints.value = data.timePoints || []
      } else if (data.scheduleType === 'dayOfMonth') {
        selectedDaysOfMonth.value = data.daysOfMonth || []
        dayOfMonthTime.value = data.dayOfMonthTime || '00:00:00'
      } else if (data.scheduleType === 'weekDay') {
        selectedWeekDays.value = data.weekDays || []
        weekDayTime.value = data.weekDayTime || '09:00:00'
      } else if (data.scheduleType === 'interval') {
        intervalStartTime.value = data.intervalStartTime || '00:00:00'
        intervalHours.value = data.intervalHours || 1
      }
      currentCron.value = data.cronExpression || ''
    } else {
      scheduleEnabled.value = false
      scheduleType.value = 'time'
      scheduleTimePoints.value = []
      selectedDaysOfMonth.value = []
      selectedWeekDays.value = []
      dayOfMonthTime.value = '00:00:00'
      weekDayTime.value = '09:00:00'
      intervalStartTime.value = '00:00:00'
      intervalHours.value = 1
      currentCron.value = ''
      await updateSyncScheduleConfig({
        enabled: false,
        scheduleType: 'time',
        cronExpression: '',
        timePoints: [],
        dayOfMonthTime: '00:00:00',
        weekDays: [],
        weekDayTime: '09:00:00'
      })
    }
  } catch {
    scheduleEnabled.value = false
    scheduleType.value = 'time'
  }
}

const validateScheduleConfig = (): boolean => {
  if (scheduleType.value === 'time' && scheduleTimePoints.value.length === 0) {
    ElMessage.warning('请至少添加一个执行时间')
    return false
  }
  if (scheduleType.value === 'dayOfMonth' && selectedDaysOfMonth.value.length === 0) {
    ElMessage.warning('请至少选择一天')
    return false
  }
  if (scheduleType.value === 'weekDay' && selectedWeekDays.value.length === 0) {
    ElMessage.warning('请至少选择一个星期')
    return false
  }
  if (scheduleType.value === 'interval') {
    if (!intervalStartTime.value) {
      ElMessage.warning('请设置开始时间')
      return false
    }
    if (!intervalHours.value) {
      ElMessage.warning('请选择间隔时间')
      return false
    }
  }
  return true
}

const saveScheduleConfig = async () => {
  if (!scheduleEnabled.value) {
    await updateSyncScheduleConfig({
      enabled: false,
      scheduleType: scheduleType.value,
      cronExpression: '',
      timePoints: [],
      daysOfMonth: [],
      dayOfMonthTime: dayOfMonthTime.value,
      weekDays: [],
      weekDayTime: weekDayTime.value,
      intervalStartTime: intervalStartTime.value,
      intervalHours: intervalHours.value
    })
    ElMessage.success('定时同步已关闭')
    await loadScheduleConfig()
    return
  }

  if (!validateScheduleConfig()) return

  const cron = generatedCron.value
  if (!cron) {
    ElMessage.warning('请完善定时任务配置')
    return
  }

  scheduleSaving.value = true
  try {
    const config: SyncScheduleConfigReqVO = {
      enabled: scheduleEnabled.value,
      scheduleType: scheduleType.value,
      cronExpression: cron,
      timePoints: scheduleTimePoints.value.length > 0 ? scheduleTimePoints.value : undefined,
      daysOfMonth: selectedDaysOfMonth.value.length > 0 ? selectedDaysOfMonth.value : undefined,
      dayOfMonthTime: dayOfMonthTime.value,
      weekDays: selectedWeekDays.value.length > 0 ? selectedWeekDays.value : undefined,
      weekDayTime: weekDayTime.value,
      intervalStartTime: scheduleType.value === 'interval' ? intervalStartTime.value : undefined,
      intervalHours: scheduleType.value === 'interval' ? intervalHours.value : undefined
    }
    await updateSyncScheduleConfig(config)
    ElMessage.success('定时同步配置已保存')
    await loadScheduleConfig()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存定时配置失败')
  } finally {
    scheduleSaving.value = false
  }
}

const checkBatchSyncStatusOnMount = async () => {
  try {
    const res = await getBatchSyncProgress()
    const data = (res as any)?.data || res || {}

    if (data.isSyncing || data.syncStatus === 'RUNNING') {
      showBatchSyncProgress.value = true
      batchProgress.value = {
        isSyncing: true,
        syncStatus: data.syncStatus || 'RUNNING',
        totalStores: data.totalStores || 0,
        completedStores: data.completedStores || 0,
        successStores: data.successStores || 0,
        failedStores: data.failedStores || 0,
        currentSyncingCount: data.currentSyncingCount || 0,
        currentSyncingStores: data.currentSyncingStores || [],
        startTime: data.startTime || 0,
        totalOrders: data.totalOrders || 0,
        successOrders: data.successOrders || 0,
        failOrders: data.failOrders || 0
      }
      startBatchSyncPolling()
    }
  } catch (err) {
    console.warn('检查同步状态失败（请确认后端服务已重启）:', err)
  }
}

const loadPushSetting = async () => {
  try {
    const res = await getOrderPushSetting()
    if (res.code === 0 && res.data) {
      pushSetting.value = {
        orderPushEnabled: res.data.orderPushEnabled ?? true,
        orderPushTypes: res.data.orderPushTypes || '',
        orderPushTypesArray: res.data.orderPushTypes ? res.data.orderPushTypes.split(',') : [],
        orderPushDesktop: res.data.orderPushDesktop ?? false
      }
    }
  } catch (e) {
    console.error('加载推送设置失败:', e)
  }
}

const handlePushSettingChange = async () => {
  try {
    await updateOrderPushSetting({
      orderPushEnabled: pushSetting.value.orderPushEnabled,
      orderPushTypes: pushSetting.value.orderPushTypesArray.join(','),
      orderPushDesktop: pushSetting.value.orderPushDesktop
    })
  } catch (e: any) {
    ElMessage.error(e?.message || '保存推送设置失败')
  }
}

const handleDesktopNotificationChange = async () => {
  if (pushSetting.value.orderPushDesktop) {
    if (!('Notification' in window)) {
      ElMessage.warning('您的浏览器不支持桌面通知')
      pushSetting.value.orderPushDesktop = false
      return
    }

    const permission = await Notification.requestPermission()
    if (permission !== 'granted') {
      ElMessage.warning('桌面通知权限被拒绝，请在浏览器设置中开启')
      pushSetting.value.orderPushDesktop = false
    }
  }

  await handlePushSettingChange()
}

const connectWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/admin-api/ws`

  try {
    websocket.value = new WebSocket(wsUrl)

    websocket.value.onopen = () => {
      console.log('WebSocket连接成功')
    }

    websocket.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'order-status-change') {
          handleOrderStatusPush(data)
        }
      } catch (e) {
        console.error('解析WebSocket消息失败:', e)
      }
    }

    websocket.value.onerror = (error) => {
      console.error('WebSocket错误:', error)
    }

    websocket.value.onclose = () => {
      console.log('WebSocket连接关闭，3秒后重连...')
      setTimeout(connectWebSocket, 3000)
    }
  } catch (e) {
    console.warn('WebSocket连接失败（如未配置WebSocket服务可忽略）:', e)
  }
}

const disconnectWebSocket = () => {
  if (websocket.value) {
    websocket.value.close()
    websocket.value = null
  }
}

const handleOrderStatusPush = (data: any) => {
  const statusNames: Record<string, string> = {
    '1': '已支付',
    '2': '已接单',
    '3': '已拣货',
    '4': '已打包',
    '5': '已发货',
    '6': '交易成功',
    '-1': '交易关闭'
  }

  const newStatusName = statusNames[String(data.newStatus)] || '未知状态'

  ElNotification({
    title: '订单状态变更',
    message: `订单 ${data.orderId} 状态已变更为 ${newStatusName}`,
    type: 'info',
    duration: 5000
  })

  if (data.desktopEnabled && 'Notification' in window && Notification.permission === 'granted') {
    new Notification('订单状态变更', {
      body: `订单 ${data.orderId} 状态已变更为 ${newStatusName}`,
      icon: '/favicon.ico'
    })
  }

  getSyncLogs()
}

const getStatusName = (status: number): string => {
  const map: Record<number, string> = {
    1: '已支付',
    2: '已接单',
    3: '已拣货',
    4: '已打包',
    5: '已发货',
    6: '交易成功',
    '-1': '交易关闭'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status: number): string => {
  if (status === 6) return 'success'
  if (status === -1) return 'info'
  if (status >= 3) return 'warning'
  return ''
}

const checkOrderAlerts = async () => {
  try {
    const res = await getUnshownAlerts()
    const data = (res as any)?.data || res
    if (data && data.length > 0) {
      const hasCritical = data.some((item: OrderTrackingAlertVO) => item.alertLevel === 'CRITICAL')
      alertList.value = data
      alertDialogType.value = hasCritical ? 'critical' : 'warning'
      alertDialogVisible.value = true
      markAllAlertsAsShown()
    }
  } catch {
    // 静默处理
  }
}

const startAlertPolling = () => {
  stopAlertPolling()
  alertPollingTimer.value = setInterval(() => {
    if (!alertDialogVisible.value) {
      checkOrderAlerts()
    }
  }, 60000)
}

const stopAlertPolling = () => {
  if (alertPollingTimer.value) {
    clearInterval(alertPollingTimer.value)
    alertPollingTimer.value = null
  }
}

const handleDismissAlerts = async () => {
  alertDialogVisible.value = false
  alertList.value = []
}

onMounted(async () => {
  loadStoreList()
  getSyncLogs()
  loadScheduleConfig()
  checkBatchSyncStatusOnMount()
  await loadPushSetting()
  connectWebSocket()
  startAlertPolling()
  checkOrderAlerts()
})

onUnmounted(() => {
  stopPolling()
  stopBatchSyncPolling()
  stopAlertPolling()
  disconnectWebSocket()
})
</script>

<style lang="scss" scoped>
.app-container {
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
  padding: 24px;
  position: relative;
}

.pull-card {
  background: white;
  border-radius: 16px;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.05),
    0 1px 3px rgba(0, 0, 0, 0.03);
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
    padding: 20px 24px;

    .settings-bar {
      display: flex;
      align-items: center;
      gap: 32px;
      padding: 12px 16px;
      background: #f8fafc;
      border-radius: 8px;
      margin-bottom: 16px;
      flex-wrap: wrap;

      .setting-group {
        display: flex;
        align-items: center;
        gap: 8px;

        .setting-label {
          font-size: 12px;
          color: #64748b;
          font-weight: 500;
          white-space: nowrap;
        }

        .setting-disabled-text {
          color: #cbd5e1;
          font-size: 14px;
        }
      }
    }

    .detail-sections {
      margin-top: 8px;

      .schedule-section,
      .push-section {
        padding: 12px 16px;
        background: #fff;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        margin-bottom: 12px;
      }

      .push-section .form-row {
        margin-bottom: 8px;
      }

      .push-section .form-row:last-child {
        margin-bottom: 0;
      }
    }

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

    .schedule-mode-radio {
      :deep(.el-radio-button__inner) {
        padding: 6px 14px;
        font-size: 13px;
        background: #f8fafc;
        border-color: #e2e8f0;
        color: #64748b;
        transition: all 0.2s ease;
      }

      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
        border-color: #8b5cf6;
        color: #fff;
        box-shadow: 0 4px 12px rgba(139, 92, 246, 0.35);
      }
    }

    .time-picker {
      width: 140px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #8b5cf6;
        }
      }
    }

    .time-points-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 8px;

      .time-tag {
        margin-right: 0;
      }
    }

    .day-mode-form-item {
      flex-direction: column;
      align-items: flex-start;

      .day-selector {
        display: flex;
        flex-direction: column;
        gap: 10px;
        width: 100%;
        max-width: 560px;

        .day-actions {
          display: flex;
          align-items: center;
          gap: 8px;

          .selected-count {
            font-size: 12px;
            color: #64748b;
            margin-left: 4px;
          }
        }

        .day-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;

          .day-chip {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 56px;
            padding: 6px 12px;
            border-radius: 20px;
            background: #f1f5f9;
            color: #475569;
            font-size: 13px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.15s ease;
            user-select: none;
            border: 1px solid transparent;

            &:hover {
              background: #e2e8f0;
              transform: translateY(-1px);
            }

            &.selected {
              background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
              color: #fff;
              border-color: #8b5cf6;
              box-shadow: 0 2px 8px rgba(139, 92, 246, 0.3);
            }
          }
        }
      }
    }

    .month-day-picker {
      width: 260px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #8b5cf6;
        }
      }
    }

    :deep(.el-picker__popper) {
      .el-date-picker {
        .el-picker-panel__content {
          tr td .el-date-table-cell__text {
            border-radius: 4px;
          }

          tr td.in-range .el-date-table-cell__text {
            background: rgba(59, 130, 246, 0.1);
          }

          tr td.selected .el-date-table-cell__text {
            background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
            color: #fff !important;
            font-weight: 700;
            box-shadow: 0 2px 6px rgba(59, 130, 246, 0.4);

            &::after {
              content: '✔';
              position: absolute;
              top: -2px;
              right: 1px;
              font-size: 10px;
              color: #fff;
              opacity: 0.9;
            }
          }

          tr td.today .el-date-table-cell__text {
            border: 1px solid #3b82f6;
            color: #3b82f6;
          }
        }
      }
    }

    .selected-days-preview {
      margin-top: 8px;
      font-size: 12px;
      color: #64748b;
      line-height: 1.6;
      max-width: 400px;
      word-break: break-all;
    }

    .week-select {
      width: 300px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #8b5cf6;
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
          border-color: #8b5cf6;
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

    .push-status-select {
      width: 260px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #8b5cf6;
        }
      }
    }

    .push-status-tip {
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

.table-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 1px 2px rgba(0, 0, 0, 0.02);
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

.sync-progress-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);

  .sync-progress-container {
    background: white;
    border-radius: 16px;
    padding: 32px;
    width: 480px;
    max-width: 90vw;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
    animation: slideUp 0.3s ease;

    .sync-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;

      .sync-icon {
        font-size: 28px;
      }

      .sync-title {
        font-size: 18px;
        font-weight: 700;
        color: #1e293b;
      }
    }

    .sync-detail {
      .el-progress {
        margin-bottom: 20px;
      }

      .sync-stats {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 16px;
        padding: 16px;
        background: #f8fafc;
        border-radius: 12px;
        margin-bottom: 16px;

        .stat-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 6px;

          .stat-label {
            font-size: 12px;
            color: #64748b;
          }

          .stat-value {
            font-size: 22px;
            font-weight: 700;
            color: #1e293b;
          }

          &.success {
            .stat-value {
              color: #10b981;
            }
          }

          &.failed {
            .stat-value {
              color: #ef4444;
            }
          }
        }
      }

      .syncing-info {
        padding: 10px 14px;
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        border-radius: 8px;
        font-size: 13px;
        color: #1e40af;
        line-height: 1.6;
      }
    }

    .sync-complete {
      .sync-stats {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 16px;
        padding: 16px;
        background: #f0fdf4;
        border-radius: 12px;

        .stat-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 6px;

          .stat-label {
            font-size: 12px;
            color: #64748b;
          }

          .stat-value {
            font-size: 22px;
            font-weight: 700;
            color: #1e293b;
          }

          &.success {
            .stat-value {
              color: #10b981;
            }
          }

          &.failed {
            .stat-value {
              color: #ef4444;
            }
          }
        }
      }

      .failed-stores-section {
        margin-top: 16px;

        .failed-section-title {
          font-size: 14px;
          font-weight: 600;
          color: #ef4444;
          margin-bottom: 12px;
        }

        .failed-store-list {
          max-height: 300px;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: 8px;

          .failed-store-item {
            padding: 12px;
            background: #fef2f2;
            border: 1px solid #fecaca;
            border-radius: 8px;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            align-items: center;
            font-size: 13px;

            .failed-store-name {
              color: #dc2626;
              font-weight: 600;
            }

            .failed-store-id {
              color: #94a3b8;
            }

            .failed-store-error {
              color: #ef4444;
              font-size: 12px;
              flex-basis: 100%;
              padding-left: 4px;
            }
          }
        }
      }
    }
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
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

.alert-detail {
  .alert-summary-text {
    font-size: 14px;
    color: #64748b;
    margin-bottom: 16px;
    padding: 12px;
    background: #f8fafc;
    border-radius: 8px;
    border-left: 4px solid #f59e0b;
  }

  .alert-table {
    :deep(.el-table__header-wrapper) {
      .el-table__header th {
        background-color: #f8fafc !important;
        color: #334155;
        font-weight: 700;
      }
    }
  }

  .days-critical {
    color: #dc2626;
    font-weight: 700;
    font-size: 14px;
  }

  .days-warning {
    color: #f59e0b;
    font-weight: 600;
  }
}

:deep(.critical-alert-dialog) {
  .el-dialog__header {
    background: linear-gradient(135deg, #dc2626, #ef4444);
    padding: 18px 24px;
    border-bottom: none;

    .el-dialog__title {
      color: #fff !important;
      font-size: 18px;
      font-weight: 700;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: #fff;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }
}

:deep(.warning-alert-dialog) {
  .el-dialog__header {
    background: linear-gradient(135deg, #f59e0b, #fbbf24);
    padding: 18px 24px;
    border-bottom: none;

    .el-dialog__title {
      color: #fff !important;
      font-size: 18px;
      font-weight: 700;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: #fff;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }
}
</style>
