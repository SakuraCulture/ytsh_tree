<template>
  <ContentWrap>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">库存记录数</div>
          <div class="stat-value">{{ statistics.stockCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">总库存量</div>
          <div class="stat-value">{{ statistics.totalQuantity ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">总可用量</div>
          <div class="stat-value">{{ statistics.totalAvailableQuantity ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">总在途/冻结</div>
          <div class="stat-value">
            {{ statistics.totalTransitQuantity ?? 0 }} / {{ statistics.totalFrozenQuantity ?? 0 }}
          </div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="82px"
    >
      <el-form-item label="仓库" prop="warehouseId">
        <el-select
          v-model="queryParams.warehouseId"
          placeholder="请选择仓库"
          filterable
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="item in warehouseList"
            :key="item.warehouseId"
            :label="`${item.warehouseId} - ${item.warehouseName}`"
            :value="item.warehouseId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="SKU编码" prop="skuCode">
        <el-input
          v-model="queryParams.skuCode"
          placeholder="请输入SKU编码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="SKU名称" prop="skuName">
        <el-input
          v-model="queryParams.skuName"
          placeholder="请输入SKU名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['business:warehouse-stock:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="仓库库存ID" align="center" prop="warehouseStockId" width="120" />
      <el-table-column label="仓库" align="center" prop="warehouseName" min-width="140" />
      <el-table-column label="SKU编码" align="center" prop="skuCode" width="160" />
      <el-table-column label="SKU名称" align="center" prop="skuName" min-width="180" />
      <el-table-column label="库存数量" align="center" prop="warehouseStockQty" width="100" />
      <el-table-column label="可用量" align="center" prop="warehouseStockAvailableQty" width="100" />
      <el-table-column label="在途量" align="center" prop="warehouseStockTransitQty" width="100" />
      <el-table-column label="冻结量" align="center" prop="warehouseStockFrozenQty" width="100" />
      <el-table-column label="缺货时长(小时)" align="center" prop="warehouseStockOutstockHours" width="130" />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import {
  WarehouseStockApi,
  type WarehouseStockPageReqVO,
  type WarehouseStockRespVO,
  type WarehouseStockStatisticsRespVO
} from '@/api/business/warehouse/stock'
import { WarehouseApi, type WarehouseSimpleRespVO } from '@/api/business/warehouse/warehouse'

defineOptions({ name: 'BusinessWarehouseStock' })

const message = useMessage()

const loading = ref(true)
const list = ref<WarehouseStockRespVO[]>([])
const total = ref(0)
const statistics = ref<WarehouseStockStatisticsRespVO>({})
const warehouseList = ref<WarehouseSimpleRespVO[]>([])
const queryParams = reactive<WarehouseStockPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  warehouseId: undefined,
  skuCode: undefined,
  skuName: undefined,
  createTime: undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)

const getList = async () => {
  loading.value = true
  try {
    const [pageData, statisticsData] = await Promise.all([
      WarehouseStockApi.getWarehouseStockPage(queryParams),
      WarehouseStockApi.getWarehouseStockStatistics(queryParams)
    ])
    list.value = pageData.list || []
    total.value = pageData.total || 0
    statistics.value = statisticsData || {}
  } finally {
    loading.value = false
  }
}

const loadWarehouseList = async () => {
  warehouseList.value = await WarehouseApi.getWarehouseSimpleList()
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await WarehouseStockApi.exportWarehouseStock(queryParams)
    download.excel(data, '仓库库存.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

onMounted(async () => {
  await getList()
  await loadWarehouseList()
})
</script>

<style scoped>
.stat-card {
  border-radius: 8px;
}

.stat-label {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.stat-value {
  margin-top: 10px;
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
