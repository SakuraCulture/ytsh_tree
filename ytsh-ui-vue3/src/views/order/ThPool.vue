<template>
  <div class="thread-pool-dashboard">
    <!-- 主要头部区域 -->
    <div class="dashboard-header">
      <div class="header-content">
        <div class="header-left">
          <h1 class="dashboard-title">线程池监控中心</h1>
          <p class="dashboard-subtitle">实时监控所有线程池状态与告警信息</p>
        </div>
        <div class="header-actions">
          <el-switch
            v-model="autoRefresh"
            active-text="自动刷新"
            inactive-text="手动刷新"
            class="auto-refresh-switch"
          />
          <el-select v-model="refreshInterval" placeholder="刷新间隔" class="interval-select" @change="updateRefreshTimer">
            <el-option label="5秒" :value="5000" />
            <el-option label="10秒" :value="10000" />
            <el-option label="30秒" :value="30000" />
            <el-option label="60秒" :value="60000" />
          </el-select>
          <el-button type="primary" :icon="Refresh" @click="refreshAll" :loading="loading">
            刷新数据
          </el-button>
          <el-button type="warning" :icon="Bell" @click="openAlarmConfig">
            告警配置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 健康状态概览 -->
    <div class="health-overview">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card total-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><Monitor /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ poolList.length }}</div>
                <div class="metric-label">线程池总数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card healthy-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><CircleCheck /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ healthyCount }}</div>
                <div class="metric-label">健康</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card warning-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><Warning /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ warningCount }}</div>
                <div class="metric-label">告警</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card critical-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><CircleCloseFilled /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ criticalCount }}</div>
                <div class="metric-label">严重</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 整体健康状态 -->
    <div class="overall-health">
      <el-card :class="['health-status-card', overallHealthClass]" shadow="hover">
        <div class="health-status-content">
          <el-icon :size="24" class="health-icon"><component :is="overallHealthIcon" /></el-icon>
          <div class="health-info">
            <h3 class="health-title">{{ overallHealthText }}</h3>
            <p class="health-desc">
              共 {{ poolList.length }} 个线程池，
              <span class="healthy-text">{{ healthyCount }} 健康</span>，
              <span class="warning-text">{{ warningCount }} 告警</span>，
              <span class="critical-text">{{ criticalCount }} 严重</span>
            </p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 线程池状态表格 -->
    <div class="pool-details">
      <el-card class="table-card" shadow="never">
        <template #header>
          <div class="table-header">
            <span class="table-title">线程池状态详情</span>
            <div class="table-controls">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索线程池名称..."
                clearable
                class="search-input"
                @input="filterPools"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button type="primary" size="small" @click="refreshAll" :loading="loading">
                刷新
              </el-button>
            </div>
          </div>
        </template>

        <el-table :data="filteredPools" stripe border style="width: 100%" v-loading="loading">
          <el-table-column prop="poolName" label="线程池名称" min-width="220" sortable>
            <template #default="{ row }">
              <div class="pool-name-cell">
                <span class="pool-name">{{ row.poolName }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="健康状态" width="120" align="center" sortable>
            <template #default="{ row }">
              <el-tag :type="getHealthTagType(row.healthStatus)" size="small">
                {{ row.healthStatus }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="核心/最大线程" width="140" align="center" sortable>
            <template #default="{ row }">
              <span class="pool-size-text">{{ row.corePoolSize }} / {{ row.maxPoolSize }}</span>
            </template>
          </el-table-column>

          <el-table-column label="当前线程" width="120" align="center" sortable>
            <template #default="{ row }">
              <span class="current-pool-size">{{ row.poolSize }}</span>
            </template>
          </el-table-column>

          <el-table-column label="活跃线程" width="120" align="center" sortable>
            <template #default="{ row }">
              <span class="active-count">{{ row.activeCount }}</span>
            </template>
          </el-table-column>

          <el-table-column label="活跃率" width="120" align="center" sortable>
            <template #default="{ row }">
              <el-progress
                :percentage="Math.round(row.activePercent || 0)"
                :color="getActiveColor(row.activePercent)"
                :stroke-width="8"
                :show-text="true"
              />
            </template>
          </el-table-column>

          <el-table-column label="队列使用" width="140" align="center" sortable>
            <template #default="{ row }">
              <div class="queue-usage">
                <el-progress
                  :percentage="Math.round(row.queueUsagePercent || 0)"
                  :color="getQueueColor(row.queueUsagePercent)"
                  :stroke-width="8"
                  :show-text="true"
                />
                <span class="queue-text">{{ row.queueSize }} / {{ row.queueCapacity }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="完成任务" width="120" align="center" sortable>
            <template #default="{ row }">
              <span class="completed-count">{{ formatNumber(row.completedTaskCount) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="拒绝策略" width="140" align="center">
            <template #default="{ row }">
              <el-tag type="info" size="small">{{ row.rejectedPolicy }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openAlarmDialog(row)">
                告警配置
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <el-pagination
            v-if="filteredPools.length > 0"
            :current-page="currentPage"
            :page-size="pageSize"
            :total="filteredPools.length"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
          <el-empty v-else description="暂无线程池数据" />
        </div>
      </el-card>
    </div>

    <!-- 告警配置对话框 -->
    <el-dialog
      v-model="alarmDialogVisible"
      title="告警阈值配置"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="alarmForm" label-width="140px" label-position="right">
        <el-form-item label="线程池名称">
          <el-input v-model="alarmForm.poolName" disabled />
        </el-form-item>
        <el-form-item label="启用告警">
          <el-switch v-model="alarmForm.enabled" />
        </el-form-item>
        <el-form-item label="队列使用率阈值">
          <el-slider
            v-model="alarmForm.queueThresholdPercent"
            :min="1"
            :max="100"
            :step="5"
            show-input
            input-size="small"
          />
          <div class="slider-tip">当队列使用率超过此值时触发告警</div>
        </el-form-item>
        <el-form-item label="线程活跃率阈值">
          <el-slider
            v-model="alarmForm.activeThresholdPercent"
            :min="1"
            :max="100"
            :step="5"
            show-input
            input-size="small"
          />
          <div class="slider-tip">当线程活跃率超过此值时触发告警</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alarmDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAlarmConfig" :loading="savingAlarm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Bell,
  Search,
  Monitor,
  CircleCheck,
  Warning,
  CircleCloseFilled
} from '@element-plus/icons-vue'
import {
  getAllPoolStatus,
  getHealthCheck,
  getAllAlarmThresholds,
  setAlarmThreshold,
  type ThreadPoolStatusRespVO
} from '@/api/ele/threadPool'

// 页面状态
const loading = ref(false)
const autoRefresh = ref(true)
const refreshInterval = ref(10000)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

// 数据
const poolList = ref<ThreadPoolStatusRespVO[]>([])
const alarmConfigs = ref<Record<string, any>>({})
const alarmDialogVisible = ref(false)
const savingAlarm = ref(false)
const alarmForm = ref({
  poolName: '',
  queueThresholdPercent: 80,
  activeThresholdPercent: 90,
  enabled: true
})
let refreshTimer: number | null = null

// 过滤后的线程池列表
const filteredPools = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  let list = poolList.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter((p) => p.poolName?.toLowerCase().includes(keyword))
  }
  return list.slice(start, end)
})

// 统计信息
const healthyCount = computed(() => poolList.value.filter((p) => p.healthStatus === 'HEALTHY').length)
const warningCount = computed(() => poolList.value.filter((p) => p.healthStatus === 'WARNING').length)
const criticalCount = computed(() => poolList.value.filter((p) => p.healthStatus === 'CRITICAL').length)

// 整体健康状态
const overallHealth = computed(() => {
  if (criticalCount.value > 0) return 'CRITICAL'
  if (warningCount.value > 0) return 'WARNING'
  return 'HEALTHY'
})

const overallHealthClass = computed(() => {
  const health = overallHealth.value
  if (health === 'CRITICAL') return 'health-critical'
  if (health === 'WARNING') return 'health-warning'
  return 'health-healthy'
})

const overallHealthIcon = computed(() => {
  const health = overallHealth.value
  if (health === 'CRITICAL') return 'CircleCloseFilled'
  if (health === 'WARNING') return 'Warning'
  return 'CircleCheck'
})

const overallHealthText = computed(() => {
  const health = overallHealth.value
  if (health === 'CRITICAL') return '系统告警 - 有线程池处于严重状态'
  if (health === 'WARNING') return '系统警告 - 有线程池负载较高'
  return '系统健康 - 所有线程池运行正常'
})

// 工具函数
const getHealthTagType = (status?: string) => {
  switch (status) {
    case 'HEALTHY':
      return 'success'
    case 'WARNING':
      return 'warning'
    case 'CRITICAL':
      return 'danger'
    default:
      return 'info'
  }
}

const getActiveColor = (percent?: number) => {
  if (!percent) return '#67c23a'
  if (percent > 90) return '#f56c6c'
  if (percent > 70) return '#e6a23c'
  return '#67c23a'
}

const getQueueColor = (percent?: number) => {
  if (!percent) return '#67c23a'
  if (percent > 80) return '#f56c6c'
  if (percent > 60) return '#e6a23c'
  return '#67c23a'
}

const formatNumber = (num?: number) => {
  if (!num) return '0'
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

// 数据加载
const loadPoolStatus = async () => {
  try {
    const data = await getAllPoolStatus()
    poolList.value = data || []
  } catch (error: any) {
    // console.error('加载线程池状态失败:', error)
  }
}

const loadAlarmConfigs = async () => {
  try {
    const data = await getAllAlarmThresholds()
    alarmConfigs.value = data || {}
  } catch (error: any) {
    // console.error('加载告警配置失败:', error)
  }
}

const refreshAll = async () => {
  loading.value = true
  try {
    await Promise.all([loadPoolStatus(), loadAlarmConfigs()])
    if (!loading.value) {
      ElMessage.success('数据刷新成功')
    }
  } catch (error: any) {
    ElMessage.error('数据刷新失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 过滤搜索
const filterPools = () => {
  currentPage.value = 1
}

// 分页处理
const handlePageChange = (page: number) => {
  currentPage.value = page
}

// 告警配置
const openAlarmConfig = () => {
  ElMessage.info('请在线程池表格中点击具体线程池的"告警配置"按钮进行配置')
}

const openAlarmDialog = (row: ThreadPoolStatusRespVO) => {
  const config = alarmConfigs.value[row.poolName || '']
  alarmForm.value = {
    poolName: row.poolName || '',
    queueThresholdPercent: config?.queueThresholdPercent || 80,
    activeThresholdPercent: config?.activeThresholdPercent || 90,
    enabled: config?.enabled ?? true
  }
  alarmDialogVisible.value = true
}

const saveAlarmConfig = async () => {
  savingAlarm.value = true
  try {
    await setAlarmThreshold(alarmForm.value)
    ElMessage.success('告警配置保存成功')
    alarmDialogVisible.value = false
    await loadAlarmConfigs()
  } catch (error: any) {
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    savingAlarm.value = false
  }
}

// 自动刷新控制
const updateRefreshTimer = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (autoRefresh.value) {
    refreshTimer = setInterval(() => {
      loadPoolStatus()
    }, refreshInterval.value) as unknown as number
  }
}

// 生命周期
onMounted(() => {
  refreshAll()
  updateRefreshTimer()
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style lang="scss" scoped>
.thread-pool-dashboard {
  padding: 24px;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
}

.dashboard-header {
  margin-bottom: 24px;
  padding: 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.6);

  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      .dashboard-title {
        font-size: 24px;
        font-weight: 700;
        color: #1e293b;
        margin: 0 0 4px 0;
      }
      .dashboard-subtitle {
        font-size: 14px;
        color: #64748b;
        margin: 0;
      }
    }

    .header-actions {
      display: flex;
      gap: 12px;
      align-items: center;

      .auto-refresh-switch {
        margin-right: 8px;
      }

      .interval-select {
        width: 120px;
      }
    }
  }
}

.health-overview {
  margin-bottom: 24px;

  .metric-card {
    border-radius: 16px;
    border: 1px solid rgba(226, 232, 240, 0.6);
    transition: all 0.25s ease;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 28px rgba(0, 0, 0, 0.08);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    &.total-card {
      .metric-icon {
        background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
        box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35);
      }
    }

    &.healthy-card {
      .metric-icon {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        box-shadow: 0 6px 16px rgba(16, 185, 129, 0.35);
      }
    }

    &.warning-card {
      .metric-icon {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        box-shadow: 0 6px 16px rgba(245, 158, 11, 0.35);
      }
    }

    &.critical-card {
      .metric-icon {
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
        box-shadow: 0 6px 16px rgba(239, 68, 68, 0.35);
      }
    }

    .metric-content {
      display: flex;
      align-items: center;
      gap: 16px;

      .metric-icon {
        width: 56px;
        height: 56px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
      }

      .metric-text {
        .metric-value {
          font-size: 28px;
          font-weight: 700;
          color: #1e293b;
        }
        .metric-label {
          font-size: 14px;
          color: #64748b;
          margin-top: 4px;
        }
      }
    }
  }
}

.overall-health {
  margin-bottom: 24px;

  .health-status-card {
    border-radius: 16px;
    border: 1px solid rgba(226, 232, 240, 0.6);
    transition: all 0.3s ease;

    :deep(.el-card__body) {
      padding: 20px;
    }

    &.health-healthy {
      border-left: 4px solid #10b981;
      .health-icon {
        color: #10b981;
      }
    }

    &.health-warning {
      border-left: 4px solid #f59e0b;
      animation: warning-pulse 2s infinite;
      .health-icon {
        color: #f59e0b;
      }
    }

    &.health-critical {
      border-left: 4px solid #ef4444;
      animation: critical-pulse 1s infinite;
      .health-icon {
        color: #ef4444;
      }
    }

    .health-status-content {
      display: flex;
      align-items: center;
      gap: 16px;

      .health-icon {
        font-size: 32px;
      }

      .health-info {
        .health-title {
          font-size: 18px;
          font-weight: 700;
          color: #1e293b;
          margin: 0 0 4px 0;
        }
        .health-desc {
          font-size: 14px;
          color: #64748b;
          margin: 0;

          .healthy-text {
            color: #10b981;
            font-weight: 600;
          }
          .warning-text {
            color: #f59e0b;
            font-weight: 600;
          }
          .critical-text {
            color: #ef4444;
            font-weight: 600;
          }
        }
      }
    }
  }
}

.pool-details {
  .table-card {
    border-radius: 16px;
    border: 1px solid rgba(226, 232, 240, 0.6);

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #f1f5f9;
    }

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .table-title {
        font-size: 16px;
        font-weight: 700;
        color: #1e293b;
      }

      .table-controls {
        display: flex;
        gap: 12px;
        align-items: center;

        .search-input {
          width: 240px;
        }
      }
    }

    :deep(.el-table) {
      font-size: 14px;

      .el-table__header-wrapper {
        .el-table__header {
          th {
            background-color: #f8fafc !important;
            color: #334155;
            font-weight: 700;
            border-bottom: 1px solid #e2e8f0 !important;
          }
        }
      }

      .el-table__body-wrapper {
        .el-table__row {
          td {
            background: white !important;
            border-bottom: 1px solid #f1f5f9 !important;
            color: #475569;
          }
          &:hover {
            background: #f8fafc !important;
          }
        }
      }
    }

    .table-footer {
      padding: 16px 0;
      display: flex;
      justify-content: center;
    }
  }
}

.pool-name-cell {
  .pool-name {
    font-weight: 600;
    color: #334155;
    font-family: monospace;
  }
}

.pool-size-text {
  font-weight: 600;
  color: #475569;
}

.current-pool-size {
  font-weight: 600;
  color: #6366f1;
}

.active-count {
  font-weight: 600;
  color: #0ea5e9;
}

.queue-usage {
  .queue-text {
    font-size: 12px;
    color: #94a3b8;
    margin-top: 4px;
    display: block;
  }
}

.completed-count {
  font-weight: 600;
  color: #10b981;
}

.slider-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 8px;
}

@keyframes warning-pulse {
  0%, 100% {
    box-shadow: 0 4px 16px rgba(245, 158, 11, 0.1);
  }
  50% {
    box-shadow: 0 4px 24px rgba(245, 158, 11, 0.2);
  }
}

@keyframes critical-pulse {
  0%, 100% {
    box-shadow: 0 4px 16px rgba(239, 68, 68, 0.1);
  }
  50% {
    box-shadow: 0 4px 32px rgba(239, 68, 68, 0.3);
  }
}

:deep(.el-tag) {
  border-radius: 6px;
  font-weight: 500;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
  }
}

@media (max-width: 1280px) {
  .metric-content {
    .metric-icon {
      width: 48px !important;
      height: 48px !important;
    }
    .metric-value {
      font-size: 24px !important;
    }
  }
}
</style>
