<template>
  <div style="padding: 20px">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="门店" prop="platformStoreId">
        <el-select
          v-model="queryParams.platformStoreId"
          placeholder="请输入门店名称或平台门店ID"
          clearable
          filterable
          remote
          reserve-keyword
          :remote-method="searchStoreList"
          :loading="storeLoading"
          style="width: 240px"
        >
          <el-option
            v-for="store in storeList"
            :key="store.platformStoreId || store.storeName"
            :label="store.storeName"
            :value="store.platformStoreId || store.storeName"
            :style="{ color: store.storeStatus === 1 ? '#333' : '#999' }"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="同步状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border style="margin-top: 16px">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column
        label="门店名称"
        align="center"
        prop="storeName"
        min-width="150"
        show-overflow-tooltip
      />
      <el-table-column
        label="平台门店ID"
        align="center"
        prop="platformStoreId"
        width="120"
        show-overflow-tooltip
      />
      <el-table-column label="同步开始时间" align="center" prop="syncStartTime" width="180">
        <template #default="scope">
          {{ formatSyncTime(scope.row.syncStartTime) }}
        </template>
      </el-table-column>
      <el-table-column label="同步结束时间" align="center" prop="syncEndTime" width="180">
        <template #default="scope">
          {{ formatSyncTime(scope.row.syncEndTime) }}
        </template>
      </el-table-column>
      <el-table-column label="同步耗时(秒)" align="center" width="110">
        <template #default="scope">
          {{ calcDuration(scope.row.syncStartTime, scope.row.syncEndTime) }}
        </template>
      </el-table-column>
      <el-table-column label="同步数量" align="center" prop="syncCount" width="100" />
      <el-table-column label="成功数量" align="center" prop="successCount" width="100">
        <template #default="scope">
          <span :style="{ color: scope.row.successCount > 0 ? '#67c23a' : '#909399' }">
            {{ scope.row.successCount ?? 0 }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="失败数量" align="center" prop="failCount" width="100">
        <template #default="scope">
          <span :style="{ color: scope.row.failCount > 0 ? '#f56c6c' : '#909399' }">
            {{ scope.row.failCount ?? 0 }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="同步状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="失败原因"
        align="center"
        prop="errorMsg"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
    </el-table>

    <el-pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="getList"
      @current-change="getList"
      style="margin-top: 16px; justify-content: flex-end"
    />
  </div>
</template>

<script lang="ts" setup>
import * as SyncLogApi from '@/api/ele/syncLog'
import * as TableApi from '@/api/business/store'
import type { StoreSimpleRespVO } from '@/api/business/store'
import { ref, onMounted } from 'vue'

const ELE_PLATFORM_ID = 1

const loading = ref(false)
const total = ref(0)
const list = ref<SyncLogApi.EleSyncLogRespVO[]>([])
const storeList = ref<StoreSimpleRespVO[]>([])
const storeLoading = ref(false)
const queryRef = ref()

const queryParams = ref<SyncLogApi.EleSyncLogReqVO>({
  platformStoreId: '',
  status: undefined,
  pageNo: 1,
  pageSize: 10
})

const searchStoreList = async (keyword: string) => {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) {
    storeList.value = []
    return
  }
  storeLoading.value = true
  try {
    const res = await TableApi.searchPlatformStoreSimpleList(
      ELE_PLATFORM_ID,
      normalizedKeyword,
      1,
      20
    )
    const data = Array.isArray(res) ? res : []
    storeList.value = data.sort((a, b) => (b.storeStatus ?? 0) - (a.storeStatus ?? 0))
  } catch {
    storeList.value = []
  } finally {
    storeLoading.value = false
  }
}

const getList = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNo: queryParams.value.pageNo,
      pageSize: queryParams.value.pageSize
    }
    if (queryParams.value.platformStoreId) {
      params.platformStoreId = queryParams.value.platformStoreId
    }
    if (queryParams.value.status !== undefined && queryParams.value.status !== null) {
      params.status = queryParams.value.status
    }
    const data = await SyncLogApi.getSyncLogPage(params)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    console.error('查询同步日志失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.value.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.value = {
    platformStoreId: '',
    status: undefined,
    pageNo: 1,
    pageSize: 10
  }
  storeList.value = []
  getList()
}

const formatSyncTime = (time: number | string | undefined) => {
  if (!time) return '--'
  const ts = Number(time) > 100000000000 ? time : Number(time) * 1000
  const date = new Date(ts)
  if (isNaN(date.getTime())) return '--'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const calcDuration = (start: number | string | undefined, end: number | string | undefined) => {
  if (!start || !end) return '--'
  const startTs = Number(start) > 100000000000 ? Number(start) : Number(start) * 1000
  const endTs = Number(end) > 100000000000 ? Number(end) : Number(end) * 1000
  const diff = (endTs - startTs) / 1000
  return diff >= 0 ? `${diff.toFixed(0)}s` : '--'
}

onMounted(() => {
  getList()
})
</script>
