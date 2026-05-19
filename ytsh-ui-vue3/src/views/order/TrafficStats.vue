<template>
  <div class="traffic-dashboard">
    <!-- 主要头部区域 -->
    <div class="dashboard-header">
      <div class="header-content">
        <div class="header-left">
          <h1 class="dashboard-title">翱象流量监控中心</h1>
          <p class="dashboard-subtitle">实时监控API请求流量与响应统计</p>
        </div>
        <div class="header-actions">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYYMMDD"
            :disabled-date="disabledDate"
            @change="onDateChange"
            class="date-picker"
          />
          <div class="refresh-control">
            <el-icon><Refresh /></el-icon>
            <span class="refresh-label">刷新：</span>
            <el-select v-model="refreshInterval" class="refresh-select" @change="onRefreshIntervalChange" size="small">
              <el-option label="1秒" :value="1000" />
              <el-option label="2秒" :value="2000" />
              <el-option label="5秒" :value="5000" />
              <el-option label="10秒" :value="10000" />
              <el-option label="30秒" :value="30000" />
              <el-option label="手动" :value="0" />
            </el-select>
            <el-tag v-if="refreshInterval > 0" type="success" size="small" class="live-tag">
              <span class="pulse-dot"></span>
              自动
            </el-tag>
          </div>
          <el-button type="primary" :icon="Refresh" @click="refreshAll" :loading="loading">
            刷新数据
          </el-button>
          <el-button type="danger" :icon="Delete" @click="handleReset"> 重置统计 </el-button>
        </div>
      </div>
      <!-- 限流告警 -->
      <el-alert
        v-if="rateLimitStatus.queueAlert"
        class="rate-limit-alert"
        type="warning"
        show-icon
        :closable="false"
      >
        <template #title>
          <div class="rate-limit-alert-title">
            <span>翱象接口已触发接口级限流，后续请求正在暂停排队</span>
            <el-tag type="danger" effect="dark" size="small">
              总排队：{{ rateLimitStatus.waitingCount || 0 }}
            </el-tag>
          </div>
        </template>
        <template #default>
          <div class="rate-limit-alert-desc">
            <div>为避免接口请求堆积，系统已暂停后续批量请求提交；待排队请求清空后将自动恢复。</div>
            <div class="rate-limit-api-list">
              <el-tag
                v-for="api in rateLimitStatus.apis || []"
                :key="api.apiName || api.apiCode"
                :type="api.hasBacklog ? 'danger' : 'success'"
                size="small"
              >
                {{ api.displayName || api.apiCode || api.apiName }}：排队 {{ api.waitingCount || 0 }} / {{ api.qps || 0 }} QPS
              </el-tag>
            </div>
          </div>
        </template>
      </el-alert>

      <!-- 搜索区域 -->
      <div class="search-section">
        <el-input
          v-model="searchTraceId"
          placeholder="输入Trace ID查询单条请求流量信息"
          clearable
          class="trace-search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="primary" @click="handleSearch" :loading="searchLoading">
              搜索
            </el-button>
          </template>
        </el-input>
        <el-button @click="clearSearch">清除</el-button>
      </div>
    </div>

    <!-- 搜索结果展示 -->
    <div v-if="singleRecord" class="search-result-section">
      <el-card class="result-card" shadow="never">
        <template #header>
          <div class="result-header">
            <span class="result-title">单条记录详情</span>
            <el-tag type="info">Trace ID: {{ singleRecord.traceId }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="API接口">
            <el-tag type="primary" size="small">{{ singleRecord.apiCode || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="请求时间">{{
            singleRecord.timestamp || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="请求大小">{{
            formatBytes(singleRecord.requestBytes || 0)
          }}</el-descriptions-item>
          <el-descriptions-item label="响应大小">{{
            formatBytes(singleRecord.responseBytes || 0)
          }}</el-descriptions-item>
          <el-descriptions-item label="响应时间">
            <el-tag
              :type="
                singleRecord.durationMs > 1000
                  ? 'danger'
                  : singleRecord.durationMs > 500
                    ? 'warning'
                    : 'success'
              "
            >
              {{ singleRecord.durationMs || 0 }} ms
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="singleRecord.success ? 'success' : 'danger'">
              {{ singleRecord.success ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <!-- 概览指标卡 -->
    <div class="metrics-overview">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card primary-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><Connection /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ todayStats.totalRequests || 0 }}</div>
                <div class="metric-label">总请求数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card success-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><CircleCheck /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ todayStats.successRequests || 0 }}</div>
                <div class="metric-label">成功请求</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card danger-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><CircleClose /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ todayStats.failedRequests || 0 }}</div>
                <div class="metric-label">失败请求</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card info-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><TrendCharts /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ (todayStats.successRate || 0).toFixed(1) }}%</div>
                <div class="metric-label">成功率</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 数据流量指标 -->
    <div class="data-metrics">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card upload-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><Upload /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ formatBytes(todayStats.totalRequestBytes || 0) }}</div>
                <div class="metric-label">请求流量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card download-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><Download /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{
                  formatBytes(todayStats.totalResponseBytes || 0)
                }}</div>
                <div class="metric-label">响应流量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card avg-upload-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><DataLine /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ formatBytes(todayStats.avgRequestBytes || 0) }}</div>
                <div class="metric-label">平均请求</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="metric-card avg-download-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">
                <el-icon :size="32"><DataBoard /></el-icon>
              </div>
              <div class="metric-text">
                <div class="metric-value">{{ formatBytes(todayStats.avgResponseBytes || 0) }}</div>
                <div class="metric-label">平均响应</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <!-- 流量趋势图 -->
        <el-col :xs="24" :lg="14">
          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">小时级流量趋势</span>
              </div>
            </template>
            <div class="chart-container">
              <div v-if="hourlyStats.length === 0" class="empty-chart">
                <el-empty description="暂无数据" :image-size="100" />
              </div>
              <div v-else class="hourly-trend">
                <div v-for="item in hourlyStats" :key="item.hour" class="trend-item">
                  <div class="trend-hour">{{ item.hour }}:00</div>
                  <div class="trend-bar">
                    <div
                      class="trend-fill"
                      :style="{ height: getTrendHeight(item.requests) }"
                    ></div>
                  </div>
                  <div class="trend-count">{{ item.requests }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 状态分布饼图 -->
        <el-col :xs="24" :lg="10">
          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">请求状态分布</span>
              </div>
            </template>
            <div class="pie-chart-container">
              <div class="pie-section">
                <div class="pie-value success-pie">{{ successPercent.toFixed(1) }}%</div>
                <div class="pie-label">成功</div>
              </div>
              <div class="pie-section">
                <div class="pie-value failed-pie">{{ failPercent.toFixed(1) }}%</div>
                <div class="pie-label">失败</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 分接口RPS实时监控区域 -->
    <div class="api-rps-section">
      <el-card class="api-rps-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <span class="card-title">分接口实时RPS监控</span>
              <span class="update-time" v-if="lastUpdateTime">最后更新：{{ lastUpdateTime }}</span>
            </div>
            <div class="header-right">
              <el-input
                v-model="apiSearchKeyword"
                placeholder="搜索接口..."
                clearable
                class="api-search"
                size="small"
                prefix-icon="Search"
              />
              <el-select v-model="rpsSortBy" size="small" class="sort-select" @change="handleRpsSortChange">
                <el-option label="按当前RPS排序" value="currentRps" />
                <el-option label="按5秒平均排序" value="avgRps5s" />
                <el-option label="按60秒峰值排序" value="maxRps60s" />
              </el-select>
            </div>
          </div>
        </template>

        <el-table
          :data="filteredApiRpsData"
          stripe
          border
          v-loading="rpsLoading"
          class="api-rps-table"
          :row-class-name="getTableRowClassName"
        >
          <el-table-column label="接口名称" min-width="180">
            <template #default="{ row }">
              <div class="api-name-cell">
                <div class="api-name">{{ row.apiName }}</div>
                <el-tag size="small" type="info">{{ row.apiCode }}</el-tag>
                <span class="api-path">{{ row.apiPath }}</span>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column label="当前RPS" width="160" align="center">
            <template #default="{ row }">
              <div class="rps-cell" :class="getRpsClass(row.currentRps)">
                <span class="rps-value">{{ row.currentRps }}</span>
                <span class="rps-unit">req/s</span>
                <el-progress
                  :percentage="getRpsPercentage(row.currentRps)"
                  :stroke-width="4"
                  :show-text="false"
                  :color="getRpsColor(row.currentRps)"
                  class="rps-progress"
                />
              </div>
            </template>
          </el-table-column>

          <el-table-column label="5秒平均" width="120" align="center">
            <template #default="{ row }">
              <div class="rps-value small">{{ row.avgRps5s }}</div>
            </template>
          </el-table-column>

          <el-table-column label="60秒峰值" width="120" align="center">
            <template #default="{ row }">
              <div class="rps-value peak">{{ row.maxRps60s }}</div>
            </template>
          </el-table-column>

          <el-table-column label="总请求数" width="120" align="center">
            <template #default="{ row }">
              <div class="rps-value">{{ row.totalRequests }}</div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 小时级详细数据表格 -->
    <div class="details-section">
      <el-card class="table-card" shadow="never">
        <template #header>
          <div class="table-header">
            <div class="header-left">
              <span class="table-title">小时级详细统计</span>
              <el-tag size="small" type="info" class="stats-count">共 {{ hourlyStats.length }} 条记录</el-tag>
            </div>
            <div class="header-right">
              <el-select v-model="hourSortBy" size="small" class="sort-select" @change="handleHourSortChange">
                <el-option label="按时段排序" value="hour" />
                <el-option label="按请求数排序" value="requests" />
                <el-option label="按成功率排序" value="successRate" />
                <el-option label="按平均耗时排序" value="avgDuration" />
              </el-select>
              <el-button type="primary" size="small" @click="refreshAll" :loading="loading">
                刷新
              </el-button>
            </div>
          </div>
        </template>
        <el-table 
          :data="sortedHourlyStats" 
          stripe 
          border 
          style="width: 100%" 
          v-loading="loading"
          class="hourly-table"
          :row-class-name="getHourlyRowClassName"
        >
          <el-table-column prop="hour" label="时段" width="120" align="center" sortable>
            <template #default="{ row }">
              <div class="hour-cell">
                <el-icon class="hour-icon"><Clock /></el-icon>
                <el-tag size="small" type="info">{{ row.hour }}:00</el-tag>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column label="请求数" width="120" align="center" sortable prop="requests">
            <template #default="{ row }">
              <div class="requests-cell">
                <div class="requests-value">{{ row.requests || 0 }}</div>
                <el-progress 
                  :percentage="getRequestsPercentage(row.requests)" 
                  :stroke-width="4" 
                  :show-text="false"
                  :color="getRequestsColor(row.requests)"
                  class="requests-progress"
                />
              </div>
            </template>
          </el-table-column>

          <el-table-column label="请求大小" width="130" align="center" sortable>
            <template #default="{ row }">
              <div class="size-cell">
                <el-icon class="size-icon"><Upload /></el-icon>
                <span>{{ formatBytes(row.requestBytes || 0) }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="响应大小" width="130" align="center" sortable>
            <template #default="{ row }">
              <div class="size-cell">
                <el-icon class="size-icon"><Download /></el-icon>
                <span>{{ formatBytes(row.responseBytes || 0) }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="平均耗时" width="120" align="center" sortable>
            <template #default="{ row }">
              <div class="duration-cell" :class="getDurationClass(row.avgDurationMs)">
                <span class="duration-value">{{ (row.avgDurationMs || 0).toFixed(0) }}</span>
                <span class="duration-unit">ms</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="成功率" width="140" align="center" sortable>
            <template #default="{ row }">
              <div class="success-rate-cell">
                <div class="success-rate-text" :class="getSuccessRateClass(row)">
                  <el-icon v-if="getHourlySuccessRate(row) >= 95"><CircleCheck /></el-icon>
                  <el-icon v-else><CircleClose /></el-icon>
                  <span>{{ getHourlySuccessRate(row).toFixed(1) }}%</span>
                </div>
                <el-progress 
                  :percentage="getHourlySuccessRate(row)" 
                  :stroke-width="6" 
                  :show-text="false"
                  :color="getSuccessRateColor(row)"
                  class="success-rate-progress"
                />
              </div>
            </template>
          </el-table-column>

          <el-table-column label="成功/失败" min-width="140" align="center">
            <template #default="{ row }">
              <div class="count-cell">
                <span class="count-success">{{ row.successCount || 0 }}</span>
                <span class="count-divider">/</span>
                <span class="count-failed">{{ row.failedCount || 0 }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <el-empty v-if="hourlyStats.length === 0" description="暂无小时级统计数据" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Delete,
  Search,
  Connection,
  Upload,
  Download,
  CircleCheck,
  CircleClose,
  TrendCharts,
  DataLine,
  DataBoard,
  Clock
} from '@element-plus/icons-vue'
import {
  getTodayStats,
  getStatsByDate,
  getHourlyStats,
  getHourlyStatsByDate,
  getSingleRecord,
  resetStats,
  getSyncConfig,
  getApiRps,
  getApiRateLimitStatus,
  type EleApiRateLimitStatusRespVO,
  type EleTrafficTodayStatsRespVO,
  type EleTrafficHourlyStatsRespVO,
  type EleTrafficRecordVO
} from '@/api/ele/traffic'

// 页面状态
const loading = ref(false)
const searchLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const selectedDate = ref(getTodayDateStr())

const refreshInterval = ref(10000)
const rpsLoading = ref(false)
const apiRpsData = ref<any[]>([])
const rateLimitStatus = ref<EleApiRateLimitStatusRespVO>({})
const apiSearchKeyword = ref('')
const rpsSortBy = ref('currentRps')
const lastUpdateTime = ref('')
let rpsTimer: ReturnType<typeof setInterval> | null = null

const filteredApiRpsData = computed(() => {
  let data = [...apiRpsData.value]
  if (apiSearchKeyword.value) {
    const keyword = apiSearchKeyword.value.toLowerCase()
    data = data.filter(
      api =>
        api.apiName.toLowerCase().includes(keyword) ||
        api.apiCode.toLowerCase().includes(keyword)
    )
  }
  data.sort((a, b) => {
    const sortKey = rpsSortBy.value
    return (b[sortKey] || 0) - (a[sortKey] || 0)
  })
  return data
})

// 自动刷新定时器
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null
const AUTO_REFRESH_INTERVAL = ref<number>(15 * 60 * 1000) // 默认15分钟，后续从后端获取

// 搜索参数
const searchTraceId = ref('')

// 统计数据
const todayStats = ref<EleTrafficTodayStatsRespVO>({})
const hourlyStats = ref<EleTrafficHourlyStatsRespVO[]>([])
const singleRecord = ref<EleTrafficRecordVO | null>(null)

// 获取今天日期字符串
function getTodayDateStr(): string {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}${month}${day}`
}

// 禁用日期（只允许3天内）
const disabledDate = (time: Date) => {
  const today = new Date()
  const threeDaysAgo = new Date(today.getTime() - 2 * 24 * 60 * 60 * 1000)
  return time.getTime() > today.getTime() || time.getTime() < threeDaysAgo.getTime()
}

// 日期变更处理
const onDateChange = () => {
  refreshAll()
}

// 计算属性
const successPercent = computed(() => {
  const total = (todayStats.value.successRequests || 0) + (todayStats.value.failedRequests || 0)
  if (total === 0) return 0
  return ((todayStats.value.successRequests || 0) / total) * 100
})

const failPercent = computed(() => {
  const total = (todayStats.value.successRequests || 0) + (todayStats.value.failedRequests || 0)
  if (total === 0) return 0
  return ((todayStats.value.failedRequests || 0) / total) * 100
})

// 获取趋势高度
const getTrendHeight = (requests: number) => {
  if (hourlyStats.value.length === 0) return '0%'
  const maxRequests = Math.max(...hourlyStats.value.map((item) => item.requests || 0))
  if (maxRequests === 0) return '0%'
  return `${Math.min(100, (requests / maxRequests) * 100)}%`
}

// 格式化字节单位
const formatBytes = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 获取小时级成功率
const getHourlySuccessRate = (row: EleTrafficHourlyStatsRespVO): number => {
  const total = (row.successCount || 0) + (row.failedCount || 0)
  if (total === 0) return 0
  return ((row.successCount || 0) / total) * 100
}

// 分页处理
const handlePageChange = (page: number) => {
  currentPage.value = page
}

// 加载限流状态
const loadRateLimitStatus = async () => {
  try {
    rateLimitStatus.value = await getApiRateLimitStatus()
  } catch (error: any) {
    // console.error('加载限流状态失败:', error)
  }
}

// 加载今日统计数据
const loadTodayStats = async () => {
  try {
    const data = await getStatsByDate(selectedDate.value)
    todayStats.value = data
  } catch (error: any) {
    // console.error('加载当日统计失败:', error)
  }
}

// 加载小时级统计数据
const loadHourlyStats = async () => {
  try {
    const data = await getHourlyStatsByDate(selectedDate.value)
    hourlyStats.value = data || []
  } catch (error: any) {
    // console.error('加载小时统计失败:', error)
  }
}

// 刷新所有数据
const refreshAll = async () => {
  loading.value = true
  try {
    await Promise.all([loadTodayStats(), loadHourlyStats(), loadRateLimitStatus()])
    ElMessage.success('数据刷新成功')
  } catch (error: any) {
    ElMessage.error('数据刷新失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 重置统计数据
const handleReset = async () => {
  try {
    await ElMessageBox.confirm('确定要重置当日流量统计吗？此操作不可恢复。', '确认重置', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await resetStats()
    ElMessage.success('统计已重置')
    await refreshAll()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('重置失败: ' + (error.message || '未知错误'))
    }
  }
}

// 搜索单条记录
const handleSearch = async () => {
  if (!searchTraceId.value.trim()) {
    ElMessage.warning('请输入Trace ID')
    return
  }
  searchLoading.value = true
  try {
    const data = await getSingleRecord(searchTraceId.value.trim())
    singleRecord.value = data
    if (!data) {
      ElMessage.info('未找到该Trace ID的记录')
    }
  } catch (error: any) {
    ElMessage.error('查询失败: ' + (error.message || '未知错误'))
  } finally {
    searchLoading.value = false
  }
}

// 清除搜索
const clearSearch = () => {
  searchTraceId.value = ''
  singleRecord.value = null
}

// 加载同步配置
const loadSyncConfig = async () => {
  try {
    const config = await getSyncConfig()
    if (config && config.syncIntervalMs) {
      AUTO_REFRESH_INTERVAL.value = config.syncIntervalMs
    }
  } catch (error: any) {
    // console.error('加载同步配置失败:', error)
  }
}

// 初始化数据
onMounted(async () => {
  await loadSyncConfig()
  await refreshAll()
  autoRefreshTimer = setInterval(() => {
    refreshAll()
  }, AUTO_REFRESH_INTERVAL.value)
  onRefreshIntervalChange(refreshInterval.value)
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
  if (rpsTimer) {
    clearInterval(rpsTimer)
    rpsTimer = null
  }
})

const getRpsClass = (rps: number) => {
  if (rps >= 100) return 'rps-critical'
  if (rps >= 50) return 'rps-danger'
  if (rps >= 20) return 'rps-warning'
  return 'rps-normal'
}

const getRpsPercentage = (rps: number) => {
  const max = 150
  return Math.min(100, (rps / max) * 100)
}

const getRpsColor = (rps: number) => {
  if (rps >= 100) return '#ef4444'
  if (rps >= 50) return '#f97316'
  if (rps >= 20) return '#f59e0b'
  return '#10b981'
}

const loadApiRps = async () => {
  try {
    rpsLoading.value = true
    const data = await getApiRps()
    apiRpsData.value = data || []
    const now = new Date()
    lastUpdateTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
  } catch (error: any) {
    // console.error('获取RPS数据失败:', error)
  } finally {
    rpsLoading.value = false
  }
}

const onRefreshIntervalChange = (value: number) => {
  if (rpsTimer) {
    clearInterval(rpsTimer)
    rpsTimer = null
  }
  if (value > 0) {
    loadApiRps()
    loadRateLimitStatus()
    rpsTimer = setInterval(() => {
      loadApiRps()
      loadRateLimitStatus()
    }, value)
  }
}

const handleRpsSortChange = () => {}

const hourSortBy = ref('hour')
const sortedHourlyStats = computed(() => {
  const sorted = [...hourlyStats.value]
  sorted.sort((a, b) => {
    if (hourSortBy.value === 'requests') {
      return (b.requests || 0) - (a.requests || 0)
    } else if (hourSortBy.value === 'successRate') {
      return getHourlySuccessRate(b) - getHourlySuccessRate(a)
    } else if (hourSortBy.value === 'avgDuration') {
      return (b.avgDurationMs || 0) - (a.avgDurationMs || 0)
    }
    return (Number(a.hour) || 0) - (Number(b.hour) || 0)
  })
  return sorted
})
const handleHourSortChange = () => {}

const getRequestsPercentage = (requests: number) => {
  if (hourlyStats.value.length === 0) return 0
  const maxRequests = Math.max(...hourlyStats.value.map((item) => item.requests || 0))
  if (maxRequests === 0) return 0
  return Math.min(100, (requests / maxRequests) * 100)
}

const getRequestsColor = (requests: number) => {
  if (hourlyStats.value.length === 0) return '#10b981'
  const maxRequests = Math.max(...hourlyStats.value.map((item) => item.requests || 0))
  if (maxRequests === 0) return '#10b981'
  const ratio = requests / maxRequests
  if (ratio >= 0.8) return '#ef4444'
  if (ratio >= 0.5) return '#f59e0b'
  return '#10b981'
}

const getDurationClass = (avgDurationMs: number) => {
  if (avgDurationMs > 1000) return 'duration-danger'
  if (avgDurationMs > 500) return 'duration-warning'
  return 'duration-normal'
}

const getSuccessRateClass = (row: EleTrafficHourlyStatsRespVO) => {
  const rate = getHourlySuccessRate(row)
  if (rate < 80) return 'rate-danger'
  if (rate < 95) return 'rate-warning'
  return 'rate-success'
}

const getSuccessRateColor = (row: EleTrafficHourlyStatsRespVO) => {
  const rate = getHourlySuccessRate(row)
  if (rate < 80) return '#ef4444'
  if (rate < 95) return '#f59e0b'
  return '#10b981'
}

const getHourlyRowClassName = ({ row }: { row: any }) => {
  const rate = getHourlySuccessRate(row)
  if (rate < 80) return 'low-success-rate-row'
  if (rate < 95) return 'medium-success-rate-row'
  return ''
}

const getTableRowClassName = ({ row, rowIndex }: { row: any; rowIndex: number }) => {
  if (row.currentRps >= 100) return 'rps-critical-row'
  if (row.currentRps >= 50) return 'rps-danger-row'
  return ''
}
</script>

<style lang="scss" scoped>
.traffic-dashboard {
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
    margin-bottom: 20px;

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

      .date-picker {
        width: 180px;
      }

      .refresh-control {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 0 12px;
        border-left: 1px solid #e2e8f0;
        border-right: 1px solid #e2e8f0;

        .refresh-label {
          font-size: 13px;
          color: #64748b;
        }

        .refresh-select {
          width: 90px;
        }

        .live-tag {
          margin-left: 4px;
        }
      }
    }
  }

  .rate-limit-alert {
  margin-top: 20px;
  border-radius: 12px;
}

.rate-limit-alert-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-weight: 700;
}

.rate-limit-alert-desc {
  margin-top: 6px;
  color: #92400e;
  line-height: 1.6;
}

.rate-limit-api-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.search-section {
    display: flex;
    gap: 12px;
    align-items: center;

    .trace-search-input {
      flex: 1;
    }
  }
}

.search-result-section {
  margin-bottom: 24px;

  .result-card {
    border-radius: 16px;
    border: 1px solid rgba(226, 232, 240, 0.6);

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #f1f5f9;
    }

    .result-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .result-title {
        font-size: 16px;
        font-weight: 700;
        color: #1e293b;
      }
    }
  }
}

.metrics-overview {
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

    &.primary-card {
      .metric-icon {
        background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
        box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35);
      }
    }

    &.success-card {
      .metric-icon {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        box-shadow: 0 6px 16px rgba(16, 185, 129, 0.35);
      }
    }

    &.danger-card {
      .metric-icon {
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
        box-shadow: 0 6px 16px rgba(239, 68, 68, 0.35);
      }
    }

    &.info-card {
      .metric-icon {
        background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
        box-shadow: 0 6px 16px rgba(14, 165, 233, 0.35);
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
          font-size: 24px;
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

.data-metrics {
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

    &.upload-card {
      .metric-icon {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        box-shadow: 0 6px 16px rgba(245, 158, 11, 0.35);
      }
    }

    &.download-card {
      .metric-icon {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        box-shadow: 0 6px 16px rgba(16, 185, 129, 0.35);
      }
    }

    &.avg-upload-card {
      .metric-icon {
        background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
        box-shadow: 0 6px 16px rgba(139, 92, 246, 0.35);
      }
    }

    &.avg-download-card {
      .metric-icon {
        background: linear-gradient(135deg, #ec4899 0%, #db2777 100%);
        box-shadow: 0 6px 16px rgba(236, 72, 153, 0.35);
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
          font-size: 20px;
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

.charts-section {
  margin-bottom: 24px;

  .chart-card {
    border-radius: 16px;
    border: 1px solid rgba(226, 232, 240, 0.6);
    height: 100%;

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #f1f5f9;
    }

    .chart-header {
      .chart-title {
        font-size: 16px;
        font-weight: 700;
        color: #1e293b;
      }
    }

    .chart-container {
      padding: 20px;

      .empty-chart {
        padding: 40px 0;
      }

      .hourly-trend {
        display: flex;
        gap: 8px;
        align-items: flex-end;
        height: 200px;
        padding: 0 10px;

        .trend-item {
          flex: 1;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;

          .trend-hour {
            font-size: 12px;
            color: #64748b;
          }

          .trend-bar {
            width: 100%;
            height: 150px;
            background: #f1f5f9;
            border-radius: 8px;
            display: flex;
            align-items: flex-end;
            overflow: hidden;

            .trend-fill {
              width: 100%;
              background: linear-gradient(180deg, #6366f1 0%, #4f46e5 100%);
              border-radius: 8px;
              transition: height 0.3s ease;
            }
          }

          .trend-count {
            font-size: 12px;
            font-weight: 600;
            color: #1e293b;
          }
        }
      }
    }

    .pie-chart-container {
      padding: 40px 20px;
      display: flex;
      justify-content: space-around;
      align-items: center;

      .pie-section {
        text-align: center;

        .pie-value {
          font-size: 36px;
          font-weight: 700;
          margin-bottom: 8px;

          &.success-pie {
            color: #10b981;
          }

          &.failed-pie {
            color: #ef4444;
          }
        }

        .pie-label {
          font-size: 14px;
          color: #64748b;
        }
      }
    }
  }
}

.api-rps-section {
  margin-bottom: 24px;

  .api-rps-card {
    border-radius: 16px;
    border: 1px solid rgba(226, 232, 240, 0.6);

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #f1f5f9;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        .card-title {
          font-size: 16px;
          font-weight: 700;
          color: #1e293b;
          margin-right: 16px;
        }

        .update-time {
          font-size: 12px;
          color: #94a3b8;
        }
      }

      .header-right {
        display: flex;
        gap: 12px;
        align-items: center;

        .api-search {
          width: 180px;
        }

        .sort-select {
          width: 160px;
        }
      }
    }

    .api-rps-table {
      .api-name-cell {
        display: flex;
        flex-direction: column;
        gap: 4px;
        align-items: flex-start;

        .api-name {
          font-weight: 600;
          color: #334155;
          font-size: 14px;
        }

        .api-path {
          font-size: 11px;
          color: #94a3b8;
        }
      }

      .rps-cell {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;

        .rps-value {
          font-size: 22px;
          font-weight: 700;
        }

        .rps-unit {
          font-size: 10px;
          color: #94a3b8;
        }

        .rps-progress {
          width: 80%;
        }

        &.rps-normal .rps-value { color: #10b981; }
        &.rps-warning .rps-value { color: #f59e0b; }
        &.rps-danger .rps-value { color: #f97316; }
        &.rps-critical .rps-value {
          color: #ef4444;
          animation: pulse 1s ease-in-out infinite;
        }
      }

      .rps-value {
        &.small {
          font-size: 16px;
          color: #64748b;
        }

        &.peak {
          font-size: 16px;
          font-weight: 600;
          color: #f43f5e;
        }
      }

      &.api-rps-table :deep(.el-table__row.rps-critical-row) {
        background-color: #fef2f2 !important;
      }

      &.api-rps-table :deep(.el-table__row.rps-danger-row) {
        background-color: #fff7ed !important;
      }
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.05);
  }
}

.pulse-dot {
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
  display: inline-block;
}

.details-section {
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
      flex-wrap: wrap;
      gap: 12px;

      .header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        .table-title {
          font-size: 16px;
          font-weight: 700;
          color: #1e293b;
        }

        .stats-count {
          flex-shrink: 0;
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 12px;

        .sort-select {
          width: 160px;
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

    .hour-cell {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;

      .hour-icon {
        color: #94a3b8;
      }
    }

    .requests-cell {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;

      .requests-value {
        font-weight: 600;
        color: #1e293b;
      }

      .requests-progress {
        width: 80%;
      }
    }

    .size-cell {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;

      .size-icon {
        color: #94a3b8;
        font-size: 14px;
      }
    }

    .duration-cell {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;

      .duration-value {
        font-weight: 600;
      }

      .duration-unit {
        font-size: 11px;
        color: #94a3b8;
      }

      &.duration-danger .duration-value {
        color: #ef4444;
      }

      &.duration-warning .duration-value {
        color: #f59e0b;
      }

      &.duration-normal .duration-value {
        color: #10b981;
      }
    }

    .success-rate-cell {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;

      .success-rate-text {
        display: flex;
        align-items: center;
        gap: 4px;
        font-weight: 600;

        &.rate-danger {
          color: #ef4444;
        }

        &.rate-warning {
          color: #f59e0b;
        }

        &.rate-success {
          color: #10b981;
        }
      }

      .success-rate-progress {
        width: 80%;
      }
    }

    .count-cell {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;

      .count-success {
        color: #10b981;
        font-weight: 600;
      }

      .count-divider {
        color: #94a3b8;
      }

      .count-failed {
        color: #ef4444;
        font-weight: 600;
      }
    }

    :deep(.el-table__row.low-success-rate-row) {
      background-color: #fef2f2 !important;

      td {
        background-color: #fef2f2 !important;
      }
    }

    :deep(.el-table__row.medium-success-rate-row) {
      background-color: #fffbeb !important;

      td {
        background-color: #fffbeb !important;
      }
    }

    .table-footer {
      padding: 16px 0;
      display: flex;
      justify-content: center;
    }
  }
}

.text-danger {
  color: #ef4444 !important;
  font-weight: 600;
}

.text-success {
  color: #10b981 !important;
  font-weight: 600;
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

:deep(.el-date-editor.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  &.is-focus {
    box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
  }
}

@media (max-width: 1280px) {
  .metric-content {
    .metric-icon {
      width: 48px !important;
      height: 48px !important;
    }
    .metric-value {
      font-size: 20px !important;
    }
  }
}
</style>
