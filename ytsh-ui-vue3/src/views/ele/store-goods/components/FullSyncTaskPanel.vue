<template>
  <div class="full-sync-panel">
    <div class="full-sync-card">
      <div class="section-header">
        <span class="section-title">全量同步任务</span>
      </div>

      <div class="action-bar">
        <div class="action-tip">
          <span>当前门店与所有开业门店的全量同步都会在后台异步执行</span>
        </div>
        <div class="action-buttons">
          <el-button type="primary" :loading="creatingCurrent" @click="createCurrentTask">
            当前门店全量同步
          </el-button>
          <el-button type="success" :loading="creatingAllOpen" @click="createAllOpenTask">
            所有开业门店同步
          </el-button>
          <el-button @click="loadTasks">刷新</el-button>
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
        <el-table-column prop="taskNo" label="任务编号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="scope" label="范围" width="140" align="center">
          <template #default="{ row }">{{ formatScope(row.scope) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="门店进度" min-width="180" align="center">
          <template #default="{ row }">
            {{ row.finishedStoreCount || 0 }}/{{ row.totalStoreCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="页进度" min-width="160" align="center">
          <template #default="{ row }">
            {{ row.finishedPageCount || 0 }}/{{ row.totalPageCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="SKU结果" min-width="210" align="center">
          <template #default="{ row }">
            成功 {{ row.successCount || 0 }} / 失败 {{ row.failCount || 0 }} / 治理 {{ row.governanceCount || 0 }}
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

      <pagination
        v-show="total > 0"
        v-model:total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @pagination="loadTasks"
      />
    </div>

    <el-dialog v-model="detailVisible" title="全量同步任务详情" width="920px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务编号">{{ currentTask?.taskNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="范围">{{ formatScope(currentTask?.scope) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(currentTask?.status) }}</el-descriptions-item>
        <el-descriptions-item label="门店进度">{{ currentTask?.finishedStoreCount || 0 }}/{{ currentTask?.totalStoreCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="页进度">{{ currentTask?.finishedPageCount || 0 }}/{{ currentTask?.totalPageCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="SKU结果">成功 {{ currentTask?.successCount || 0 }} / 失败 {{ currentTask?.failCount || 0 }} / 治理 {{ currentTask?.governanceCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="错误原因" :span="2">{{ currentTask?.errorMsg || '--' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-title">门店明细</div>
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
        <el-table-column label="页进度" width="120" align="center">
          <template #default="{ row }">{{ row.currentPage || 0 }}/{{ row.totalPage || 0 }}</template>
        </el-table-column>
        <el-table-column label="SKU结果" min-width="200" align="center">
          <template #default="{ row }">
            成功 {{ row.successCount || 0 }} / 失败 {{ row.failCount || 0 }} / 治理 {{ row.governanceCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误原因" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import {
  cancelFullSyncTask,
  createAllOpenStoresFullSync,
  createCurrentStoreFullSync,
  getFullSyncTask,
  getFullSyncTaskPage,
  getFullSyncTaskStores,
  type StoreGoodsFullSyncAllOpenReqVO,
  type StoreGoodsFullSyncCurrentReqVO,
  type StoreGoodsFullSyncTaskRespVO,
  type StoreGoodsFullSyncTaskStoreRespVO,
  type StoreGoodsFullSyncTaskStoreReqVO,
  type StoreGoodsFullSyncTaskReqVO
} from '@/api/ele/storeGoods'

const loading = ref(false)
const creatingCurrent = ref(false)
const creatingAllOpen = ref(false)
const taskList = ref<StoreGoodsFullSyncTaskRespVO[]>([])
const total = ref(0)
const currentTask = ref<StoreGoodsFullSyncTaskRespVO>()
const storeList = ref<StoreGoodsFullSyncTaskStoreRespVO[]>([])
const detailVisible = ref(false)
const storeLoading = ref(false)
let pollTimer: ReturnType<typeof window.setTimeout> | undefined

const queryParams = reactive<StoreGoodsFullSyncTaskReqVO>({
  pageNo: 1,
  pageSize: 10,
  status: '',
  scope: ''
})

const clearPollTimer = () => {
  if (pollTimer) {
    window.clearTimeout(pollTimer)
    pollTimer = undefined
  }
}

const hasRunningTask = () => taskList.value.some((task) => canCancel(task.status))

const schedulePolling = () => {
  clearPollTimer()
  if (!hasRunningTask()) return
  pollTimer = window.setTimeout(() => {
    loadTasks(false)
  }, 5000)
}

const loadTasks = async (showLoading = true) => {
  if (showLoading) {
    loading.value = true
  }
  try {
    const data = await getFullSyncTaskPage({
      taskNo: queryParams.taskNo || undefined,
      scope: queryParams.scope || undefined,
      status: queryParams.status || undefined,
      merchantCode: queryParams.merchantCode || undefined,
      erpStoreCode: queryParams.erpStoreCode || undefined,
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

const createCurrentTask = async () => {
  const payload: StoreGoodsFullSyncCurrentReqVO = {
    merchantCode: '',
    erpStoreCode: ''
  }
  try {
    await ElMessageBox.prompt('请输入当前门店的商家编码和ERP门店编码，格式为 merchantCode,erpStoreCode', '创建当前门店全量同步', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }).then(async ({ value }) => {
      const [merchantCode, erpStoreCode] = String(value || '')
        .split(',')
        .map((item) => item.trim())
      if (!merchantCode || !erpStoreCode) {
        throw new Error('请输入 merchantCode,erpStoreCode')
      }
      payload.merchantCode = merchantCode
      payload.erpStoreCode = erpStoreCode
      payload.testMode = false
      creatingCurrent.value = true
      try {
        const taskId = await createCurrentStoreFullSync(payload)
        ElMessage.success(`已创建任务 ${taskId || ''}`)
        await loadTasks()
      } finally {
        creatingCurrent.value = false
      }
    })
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '创建当前门店任务失败')
    }
  }
}

const createAllOpenTask = async () => {
  try {
    await ElMessageBox.confirm('确定创建所有开业门店的全量同步任务吗？', '确认创建', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  creatingAllOpen.value = true
  try {
    const taskId = await createAllOpenStoresFullSync({ testMode: false })
    ElMessage.success(`已创建任务 ${taskId || ''}`)
    await loadTasks()
  } catch (error: any) {
    ElMessage.error(error?.message || '创建所有开业门店任务失败')
  } finally {
    creatingAllOpen.value = false
  }
}

const showDetail = async (id?: number) => {
  if (!id) return
  try {
    currentTask.value = await getFullSyncTask(id)
    storeLoading.value = true
    const data = await getFullSyncTaskStores(id, { pageNo: 1, pageSize: 100 })
    storeList.value = data?.list || []
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '获取任务详情失败')
  } finally {
    storeLoading.value = false
  }
}

const cancelTask = async (id?: number) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定取消该任务吗？', '确认取消', { type: 'warning' })
    await cancelFullSyncTask(id)
    ElMessage.success('已提交取消请求')
    await loadTasks()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '取消任务失败')
    }
  }
}

const canCancel = (status?: string) => ['PENDING', 'RUNNING'].includes(status || '')

const formatScope = (scope?: string) => {
  if (scope === 'CURRENT_STORE') return '当前门店'
  if (scope === 'ALL_OPEN_STORES') return '所有开业门店'
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
</script>

<style scoped lang="scss">
.full-sync-panel {
  padding: 0;
}

.full-sync-card {
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
}
</style>
