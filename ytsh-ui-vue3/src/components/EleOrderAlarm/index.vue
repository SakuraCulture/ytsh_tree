<template>
  <el-dialog
    v-model="alarmVisible"
    title="订单同步异常报警"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="alarm-content">
      <div class="alarm-icon">
        <Icon icon="ep:warning-filled" class="icon-warning" />
      </div>
      <div class="alarm-message">
        <p class="alarm-title">检测到订单同步失败！</p>
        <p class="alarm-detail">
          待重试：<span class="danger">{{ alarmData.pendingRetryCount || 0 }}</span> 条
        </p>
        <p class="alarm-detail">
          重试失败：<span class="danger">{{ alarmData.failedCount || 0 }}</span> 条
        </p>
        <p class="alarm-total">
          合计：<span class="danger">{{ alarmData.totalUnhandleCount || 0 }}</span> 条
        </p>
      </div>
    </div>

    <template #footer>
      <el-button type="primary" @click="handleGoToFailRecord">
        查看失败记录
      </el-button>
      <el-button @click="handleClose">稍后处理</el-button>
    </template>
  </el-dialog>

  <div class="alarm-switch" @click.stop>
    <span class="alarm-switch-label">失败告警</span>
    <el-switch v-model="alarmEnabled" @change="handleSwitchChange" />
  </div>
</template>

<script lang="ts" setup>
import * as FailRecordApi from '@/api/ele/failRecord'
import { useRouter } from 'vue-router'
import { useCache, CACHE_KEY } from '@/hooks/web/useCache'

const router = useRouter()
const { wsCache } = useCache()
const ALARM_ENABLED_KEY = 'eleAlarmEnabled'

const alarmVisible = ref(false)
const alarmData = ref<FailRecordApi.FailCountRespVO>({})
const alarmEnabled = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null
const POLL_INTERVAL = 30000

const loadAlarmState = () => {
  const saved = wsCache.get(ALARM_ENABLED_KEY)
  alarmEnabled.value = saved !== null ? saved : false
}

const checkAlarm = async () => {
  if (!alarmEnabled.value) return
  try {
    const data = await FailRecordApi.getUnhandledFailCount()
    if (data.totalUnhandleCount && data.totalUnhandleCount > 0) {
      alarmData.value = data
      alarmVisible.value = true
      stopPolling()
    }
  } catch (error) {
    console.error('检查失败记录异常', error)
  }
}

const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(() => {
    checkAlarm()
  }, POLL_INTERVAL)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const handleClose = () => {
  alarmVisible.value = false
  startPolling()
}

const handleGoToFailRecord = () => {
  alarmVisible.value = false
  stopPolling()
  router.push('/order/fail-record')
}

const handleSwitchChange = (val: boolean) => {
  wsCache.set(ALARM_ENABLED_KEY, val)
}

onMounted(() => {
  loadAlarmState()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.alarm-switch {
  position: fixed;
  bottom: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  padding: 8px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
  font-size: 13px;
  color: #606266;
}

.alarm-switch:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.alarm-switch-label {
  white-space: nowrap;
}

.alarm-content {
  display: flex;
  align-items: center;
  padding: 20px 0;
}

.alarm-icon {
  font-size: 48px;
  margin-right: 20px;
}

.icon-warning {
  color: #f56c6c;
  font-size: 48px;
}

.alarm-title {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 12px;
  margin-top: 0;
}

.alarm-detail {
  font-size: 14px;
  margin: 8px 0;
  color: #606266;
}

.alarm-total {
  font-size: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
  font-weight: bold;
}

.danger {
  color: #f56c6c;
  font-weight: bold;
  font-size: 18px;
}
</style>
