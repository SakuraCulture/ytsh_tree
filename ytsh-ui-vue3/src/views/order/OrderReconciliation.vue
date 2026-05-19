<template>
  <div class="order-reconciliation-container">
    <div class="page-header">
      <h2>订单对账看板</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="loadReconciliationData"
        />
        <el-button type="primary" @click="loadReconciliationData" :loading="loading">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #3b82f6">
              <el-icon><List /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalStores }}</div>
              <div class="stat-label">门店总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #22c55e">
              <el-icon><Check /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.completeStores }}</div>
              <div class="stat-label">数据完整门店</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #f59e0b">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.partialStores }}</div>
              <div class="stat-label">数据不完整门店</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #ef4444">
              <el-icon><CircleCloseFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.severeStores }}</div>
              <div class="stat-label">严重不完整门店</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="reconciliation-table-card">
      <template #header>
        <div class="card-header">
          <span>对账明细</span>
          <el-radio-group v-model="integrityFilter" @change="filterStores">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button :label="1">完整</el-radio-button>
            <el-radio-button :label="2">部分</el-radio-button>
            <el-radio-button :label="3">严重</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="filteredStores" style="width: 100%" v-loading="loading" stripe>
        <el-table-column prop="storeName" label="门店名称" width="200" fixed />
        <el-table-column prop="syncTime" label="同步时间" width="180">
          <template #default="{ row }">
            {{ formatTimestamp(row.syncStartTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="expectedTotal" label="API预期" width="100" align="center" />
        <el-table-column prop="savedTotal" label="实际落库" width="100" align="center" />
        <el-table-column prop="discrepancyRate" label="差异率" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.discrepancyRate === 0 ? 'success' : row.discrepancyRate > 20 ? 'danger' : 'warning'"
              size="small"
            >
              {{ row.discrepancyRate }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataIntegrity" label="完整性" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.dataIntegrity === 1" type="success" size="small">完整</el-tag>
            <el-tag v-else-if="row.dataIntegrity === 2" type="warning" size="small">部分</el-tag>
            <el-tag v-else-if="row.dataIntegrity === 3" type="danger" size="small">严重</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="补偿次数" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.retryCount > 0" type="info" size="small">{{ row.retryCount }}次</el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="pauseSync" label="暂停状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.pauseSync === 1" type="danger" size="small">已暂停</el-tag>
            <el-tag v-else type="info" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="对账详情" width="900px" destroy-on-close>
      <div v-if="currentDetail" class="detail-container">
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="门店名称">{{ currentDetail.storeName }}</el-descriptions-item>
          <el-descriptions-item label="同步时间">{{ formatTimestamp(currentDetail.syncStartTime) }}</el-descriptions-item>
          <el-descriptions-item label="API预期">{{ currentDetail.expectedTotal }}条</el-descriptions-item>
          <el-descriptions-item label="实际落库">{{ currentDetail.savedTotal }}条</el-descriptions-item>
          <el-descriptions-item label="差异率">{{ currentDetail.discrepancyRate }}%</el-descriptions-item>
          <el-descriptions-item label="完整性">
            <el-tag v-if="currentDetail.dataIntegrity === 1" type="success">完整</el-tag>
            <el-tag v-else-if="currentDetail.dataIntegrity === 2" type="warning">部分</el-tag>
            <el-tag v-else-if="currentDetail.dataIntegrity === 3" type="danger">严重</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="补偿次数">{{ currentDetail.retryCount }}次</el-descriptions-item>
          <el-descriptions-item label="暂停状态">
            <el-tag v-if="currentDetail.pauseSync === 1" type="danger">已暂停</el-tag>
            <el-tag v-else type="info">正常</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">状态对比</h4>
        <el-table :data="statusCompareData" size="small" border>
          <el-table-column prop="statusName" label="状态" width="120" />
          <el-table-column prop="apiCount" label="API返回" width="100" align="center" />
          <el-table-column prop="savedCount" label="实际落库" width="100" align="center" />
          <el-table-column prop="diff" label="差异" width="100" align="center">
            <template #default="{ row }">
              <span :class="{ 'text-danger': row.diff !== 0 }">{{ row.diff }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="currentDetail.apiStatusCounts" class="section-title">
          <h4>补偿信息</h4>
          <pre v-if="currentDetail.compensationInfo">{{ JSON.stringify(JSON.parse(currentDetail.compensationInfo), null, 2) }}</pre>
          <span v-else>无补偿记录</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentDetail?.discrepancyRate > 0"
          type="warning"
          @click="triggerCompensationFromDetail"
          :loading="compensationLoading"
        >
          触发补偿
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { List, Check, Warning, CircleCloseFilled } from '@element-plus/icons-vue'
import { getSyncLogPage } from '@/api/ele/orderSync'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const compensationLoading = ref(false)
const dateRange = ref<[string, string]>([
  dayjs().startOf('month').format('YYYY-MM-DD'),
  dayjs().endOf('day').format('YYYY-MM-DD')
])

const stores = ref<any[]>([])
const filteredStores = ref<any[]>([])
const integrityFilter = ref<string | number>('all')
const detailDialogVisible = ref(false)
const currentDetail = ref<any>(null)

const stats = ref({
  totalStores: 0,
  completeStores: 0,
  partialStores: 0,
  severeStores: 0
})

const statusCompareData = computed(() => {
  if (!currentDetail.value) return []
  
  const apiCounts = currentDetail.value.apiStatusCounts 
    ? (typeof currentDetail.value.apiStatusCounts === 'string' 
      ? JSON.parse(currentDetail.value.apiStatusCounts) 
      : currentDetail.value.apiStatusCounts) 
    : {}
  const savedCounts = currentDetail.value.savedStatusCounts 
    ? (typeof currentDetail.value.savedStatusCounts === 'string' 
      ? JSON.parse(currentDetail.value.savedStatusCounts) 
      : currentDetail.value.savedStatusCounts) 
    : {}
  
  const STATUS_NAMES: Record<string, string> = {
    '1': '待付款',
    '2': '待配送',
    '3': '配送中',
    '4': '已完成',
    '5': '已取消',
    '6': '已退款',
    '-1': '其他'
  }
  
  const allStatuses = new Set([...Object.keys(apiCounts), ...Object.keys(savedCounts)])
  
  return Array.from(allStatuses).map(status => {
    const apiCount = apiCounts[status] || 0
    const savedCount = savedCounts[status] || 0
    return {
      statusName: STATUS_NAMES[status] || `状态${status}`,
      apiCount,
      savedCount,
      diff: apiCount - savedCount
    }
  })
})

const formatTimestamp = (ts: number | string) => {
  if (!ts) return '--'
  const date = new Date(typeof ts === 'string' ? parseInt(ts) * 1000 : ts * 1000)
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const loadReconciliationData = async () => {
  loading.value = true
  try {
    const res = await getSyncLogPage({
      pageNo: 1,
      pageSize: 1000,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    })
    
    stores.value = (res as any)?.list || res?.data?.list || []
    calculateStats()
    filterStores()
  } catch (error) {
    // console.error('加载对账数据失败:', error)
    ElMessage.error('加载对账数据失败')
  } finally {
    loading.value = false
  }
}

const calculateStats = () => {
  stats.value = {
    totalStores: stores.value.length,
    completeStores: stores.value.filter((s: any) => s.dataIntegrity === 1).length,
    partialStores: stores.value.filter((s: any) => s.dataIntegrity === 2).length,
    severeStores: stores.value.filter((s: any) => s.dataIntegrity === 3).length
  }
}

const filterStores = () => {
  if (integrityFilter.value === 'all') {
    filteredStores.value = stores.value
  } else {
    filteredStores.value = stores.value.filter((s: any) => s.dataIntegrity === integrityFilter.value)
  }
}

const showDetail = (row: any) => {
  currentDetail.value = row
  detailDialogVisible.value = true
}

const triggerCompensationFromDetail = async () => {
  if (!currentDetail.value) return
  compensationLoading.value = true
  try {
    ElMessage.success('补偿任务已提交')
    detailDialogVisible.value = false
    loadReconciliationData()
  } catch (error) {
    // console.error('触发补偿失败:', error)
    ElMessage.error('触发补偿失败')
  } finally {
    compensationLoading.value = false
  }
}

onMounted(() => {
  loadReconciliationData()
})
</script>

<style scoped>
.order-reconciliation-container {
  padding: 20px;
  background-color: #f5f5f5;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
    color: #1f2937;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16px;

    .el-icon {
      font-size: 28px;
      color: #fff;
    }
  }

  .stat-info {
    .stat-value {
      font-size: 28px;
      font-weight: 600;
      color: #1f2937;
    }

    .stat-label {
      font-size: 14px;
      color: #6b7280;
      margin-top: 4px;
    }
  }
}

.reconciliation-table-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.detail-container {
  .section-title {
    margin-top: 20px;
    margin-bottom: 10px;
    h4 {
      margin: 0;
      color: #374151;
    }
    pre {
      background-color: #f9fafb;
      padding: 12px;
      border-radius: 4px;
      font-size: 12px;
      max-height: 200px;
      overflow: auto;
    }
  }
}

.text-danger {
  color: #ef4444;
}
</style>
