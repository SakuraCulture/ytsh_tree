<template>
  <div class="governance-panel">
    <div class="governance-card">
      <div class="section-header">
        <span class="section-title">待治理池</span>
      </div>

      <div class="filter-grid">
        <div class="filter-item">
          <span class="form-label">ERP门店编码</span>
          <el-input
            v-model="queryParams.erpStoreCode"
            placeholder="请输入 ERP 门店编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="filter-item">
          <span class="form-label">平台门店编码</span>
          <el-input
            v-model="queryParams.platformStoreId"
            placeholder="请输入平台门店编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="filter-item">
          <span class="form-label">SKU编码</span>
          <el-input
            v-model="queryParams.skuCode"
            placeholder="请输入 SKU 编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="filter-item">
          <span class="form-label">SPU编码</span>
          <el-input
            v-model="queryParams.spuCode"
            placeholder="请输入 SPU 编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </div>
        <div class="filter-item">
          <span class="form-label">原因编码</span>
          <el-select v-model="queryParams.reasonCode" placeholder="全部" clearable>
            <el-option label="SKU未匹配" value="SKU_NOT_FOUND" />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="form-label">处理状态</span>
          <el-select v-model="queryParams.processStatus" placeholder="全部" clearable>
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="PROCESSED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>

      <el-table
        :data="list"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
      >
        <el-table-column prop="createTime" label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatTimestamp(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="erpStoreCode" label="ERP门店编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="platformStoreId" label="平台门店编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="skuCode" label="SKU编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="subSkuCode" label="子SKU编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="spuCode" label="SPU编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="operationType" label="操作类型" width="100" align="center" />
        <el-table-column label="原因" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatReason(row.reasonCode, row.reasonMsg) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.processStatus)">
              {{ getStatusText(row.processStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">报文</el-button>
            <el-button
              link
              type="success"
              :disabled="row.processStatus !== 'PENDING'"
              @click="markProcessed(row.id)"
            >
              已处理
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.processStatus !== 'PENDING'"
              @click="markIgnored(row.id)"
            >
              忽略
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
        @pagination="loadList"
      />
    </div>

    <el-dialog v-model="payloadDialogVisible" title="待治理原始报文" width="860px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ERP门店编码">{{ currentDetail?.erpStoreCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="平台门店编码">{{ currentDetail?.platformStoreId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="SKU编码">{{ currentDetail?.skuCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="原因">{{ formatReason(currentDetail?.reasonCode, currentDetail?.reasonMsg) }}</el-descriptions-item>
      </el-descriptions>
      <pre class="payload-pre">{{ formattedPayload }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import {
  getGovernancePool,
  getGovernancePoolPage,
  markGovernancePoolIgnored,
  markGovernancePoolProcessed,
  type StoreGoodsGovernancePoolReqVO,
  type StoreGoodsGovernancePoolRespVO
} from '@/api/ele/storeGoods'

const loading = ref(false)
const list = ref<StoreGoodsGovernancePoolRespVO[]>([])
const total = ref(0)
const currentDetail = ref<StoreGoodsGovernancePoolRespVO>()
const payloadDialogVisible = ref(false)

const queryParams = reactive<StoreGoodsGovernancePoolReqVO>({
  erpStoreCode: '',
  platformStoreId: '',
  skuCode: '',
  spuCode: '',
  reasonCode: 'SKU_NOT_FOUND',
  processStatus: 'PENDING',
  pageNo: 1,
  pageSize: 10
})

const formattedPayload = computed(() => {
  const payload = currentDetail.value?.rawPayload
  if (!payload) return '--'
  try {
    return JSON.stringify(JSON.parse(payload), null, 2)
  } catch {
    return payload
  }
})

const loadList = async () => {
  loading.value = true
  try {
    const data = await getGovernancePoolPage({
      erpStoreCode: queryParams.erpStoreCode || undefined,
      platformStoreId: queryParams.platformStoreId || undefined,
      skuCode: queryParams.skuCode || undefined,
      spuCode: queryParams.spuCode || undefined,
      reasonCode: queryParams.reasonCode || undefined,
      processStatus: queryParams.processStatus || undefined,
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize
    })
    list.value = data?.list || []
    total.value = data?.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '查询待治理池失败')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  loadList()
}

const resetQuery = () => {
  queryParams.erpStoreCode = ''
  queryParams.platformStoreId = ''
  queryParams.skuCode = ''
  queryParams.spuCode = ''
  queryParams.reasonCode = 'SKU_NOT_FOUND'
  queryParams.processStatus = 'PENDING'
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  loadList()
}

const showDetail = async (id?: number) => {
  if (!id) return
  try {
    currentDetail.value = await getGovernancePool(id)
    payloadDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '获取待治理详情失败')
  }
}

const markProcessed = async (id?: number) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定将该记录标记为已处理吗？', '确认处理', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await markGovernancePoolProcessed(id)
    ElMessage.success('已标记为已处理')
    await loadList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '标记已处理失败')
    }
  }
}

const markIgnored = async (id?: number) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定忽略该待治理记录吗？', '确认忽略', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await markGovernancePoolIgnored(id)
    ElMessage.success('已标记为忽略')
    await loadList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '标记忽略失败')
    }
  }
}

const formatReason = (reasonCode?: string, reasonMsg?: string) => {
  if (!reasonCode && !reasonMsg) return '--'
  if (reasonCode === 'SKU_NOT_FOUND') return reasonMsg || 'SKU未匹配本地商品主档'
  return reasonMsg ? `${reasonCode || '--'}：${reasonMsg}` : reasonCode || '--'
}

const getStatusText = (status?: string) => {
  if (status === 'PROCESSED') return '已处理'
  if (status === 'IGNORED') return '已忽略'
  return '待处理'
}

const getStatusTagType = (status?: string) => {
  if (status === 'PROCESSED') return 'success'
  if (status === 'IGNORED') return 'info'
  return 'warning'
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
  loadList()
})
</script>

<style lang="scss" scoped>
.governance-card {
  padding: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 16px;
  background: #fff;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 1px 2px rgba(0, 0, 0, 0.02);
}

.section-header {
  margin-bottom: 16px;

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

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(260px, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.filter-item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-actions {
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

.payload-pre {
  max-height: 420px;
  overflow: auto;
  margin-top: 18px;
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

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-color: #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  box-shadow: none;
}

:deep(.el-pagination) {
  margin-top: 20px;
  justify-content: center;
}

@media (max-width: 1200px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
