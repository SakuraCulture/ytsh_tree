<template>
  <ContentWrap>
    <div class="flex items-center justify-between mb-16px">
      <el-button type="primary" @click="handleRetryAll" :loading="retryAllLoading" :disabled="retryAllLoading">
        批量重试全部
      </el-button>
      <el-button @click="fetchLogs" :loading="loading">刷新</el-button>
    </div>

    <el-table :data="logList" v-loading="loading" border stripe>
      <el-table-column prop="billDate" label="账单日期" width="120" />
      <el-table-column prop="merchantCode" label="商家编码" width="140" />
      <el-table-column prop="storeCode" label="门店编码" width="120" />
      <el-table-column prop="storeName" label="门店名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="failPage" label="失败页码" width="100" />
      <el-table-column prop="retryCount" label="重试次数" width="100" />
      <el-table-column prop="retryStatusText" label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="row.retryStatus === 2 ? 'success' : row.retryStatus === 1 ? 'warning' : 'danger'"
            size="small"
          >
            {{ row.retryStatusText }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="failReason" label="失败原因" min-width="240" show-overflow-tooltip />
      <el-table-column prop="syncTime" label="同步时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.retryStatus !== 1"
            type="primary"
            size="small"
            @click="handleRetryByStore(row)"
            :loading="row.retrying"
          >
            重试
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="mt-16px text-right text-gray-400" v-if="logList.length > 0">
      共 {{ logList.length }} 条记录
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFailLogList, retryByStore, retryAllPending } from '@/api/ele/billSync'
import { useRouter } from 'vue-router'

defineOptions({ name: 'EleBillSyncLog' })

const router = useRouter()
const logList = ref<any[]>([])
const loading = ref(false)
const retryAllLoading = ref(false)

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await getFailLogList()
    if (res.code === 0) {
      logList.value = (res.data || []).map((item: any) => ({ ...item, retrying: false }))
    }
  } catch (e: any) {
    ElMessage.error('获取失败日志失败')
  } finally {
    loading.value = false
  }
}

const handleRetryByStore = async (row: any) => {
  row.retrying = true
  try {
    const res = await retryByStore(row.merchantCode, row.storeCode, row.billDate)
    if (res.code === 0) {
      ElMessage.success(res.data || '重试成功')
      fetchLogs()
    } else {
      ElMessage.error(res.msg || '重试失败')
    }
  } catch (e: any) {
    ElMessage.error('请求失败')
  } finally {
    row.retrying = false
  }
}

const handleRetryAll = async () => {
  try {
    await ElMessageBox.confirm('确定要批量重试所有待重试记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    retryAllLoading.value = true
    const res = await retryAllPending()
    if (res.code === 0) {
      ElMessage.success(res.data || '批量重试完成')
      fetchLogs()
    } else {
      ElMessage.error(res.msg || '批量重试失败')
    }
  } catch (e: any) {
    // 用户取消或请求失败
  } finally {
    retryAllLoading.value = false
  }
}

onMounted(() => {
  fetchLogs()
})
</script>
