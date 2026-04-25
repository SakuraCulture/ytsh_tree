<template>
  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="94px"
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
      <el-form-item label="库位编码" prop="warehouseProductLocation">
        <el-input
          v-model="queryParams.warehouseProductLocation"
          placeholder="请输入库位编码"
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
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['business:warehouse-product:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button type="primary" plain @click="handleDownloadTemplate" class="mr-10px">
          <Icon icon="ep:download" class="mr-5px" /> 下载模板
        </el-button>
        <el-upload
          accept=".xlsx, .xls, .csv"
          :show-file-list="false"
          :before-upload="handleImport"
          class="mr-10px"
        >
          <el-button type="primary" plain>
            <Icon icon="ep:upload" class="mr-5px" /> 导入
          </el-button>
        </el-upload>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['business:warehouse-product:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="仓库商品ID" align="center" prop="warehouseProductId" width="120" />
      <el-table-column label="仓库" align="center" prop="warehouseName" min-width="140" />
      <el-table-column label="SKU编码" align="center" prop="skuCode" width="160" />
      <el-table-column label="SKU名称" align="center" prop="skuName" min-width="180" />
      <el-table-column label="基准零售价" align="center" width="120">
        <template #default="scope">
          {{ formatPrice(scope.row.retailPrice) }}
        </template>
      </el-table-column>
      <el-table-column label="仓库采购价" align="center" width="120">
        <template #default="scope">
          {{ formatPrice(scope.row.warehouseProductCostPrice) }}
        </template>
      </el-table-column>
      <el-table-column label="库位编码" align="center" prop="warehouseProductLocation" width="120" />
      <el-table-column label="首次有库存日期" align="center" prop="warehouseProductFirstDate" width="130" />
      <el-table-column label="最近入库日期" align="center" prop="warehouseProductLastDate" width="130" />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.warehouseProductId)"
            v-hasPermi="['business:warehouse-product:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.warehouseProductId)"
            v-hasPermi="['business:warehouse-product:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <ProductForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import {
  WarehouseProductApi,
  type WarehouseProductPageReqVO,
  type WarehouseProductRespVO
} from '@/api/business/warehouse/product'
import { WarehouseApi, type WarehouseSimpleRespVO } from '@/api/business/warehouse/warehouse'
import ProductForm from './ProductForm.vue'

defineOptions({ name: 'BusinessWarehouseProduct' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<WarehouseProductRespVO[]>([])
const total = ref(0)
const warehouseList = ref<WarehouseSimpleRespVO[]>([])
const queryParams = reactive<WarehouseProductPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  warehouseId: undefined,
  productSkuId: undefined,
  skuCode: undefined,
  skuName: undefined,
  warehouseProductLocation: undefined,
  createTime: undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)

const formatPrice = (value?: number) => {
  return value != null ? `¥${Number(value).toFixed(2)}` : '-'
}

const getList = async () => {
  loading.value = true
  try {
    const data = await WarehouseProductApi.getWarehouseProductPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
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

const formRef = ref()
const openForm = (type: 'create' | 'update', warehouseProductId?: number) => {
  formRef.value.open(type, warehouseProductId)
}

const handleDelete = async (warehouseProductId: number) => {
  try {
    await message.delConfirm()
    await WarehouseProductApi.deleteWarehouseProduct(warehouseProductId)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error: any) {
    if (error?.message) {
      message.warning(error.message)
      return
    }
  }
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await WarehouseProductApi.exportWarehouseProduct(queryParams)
    download.excel(data, '仓库商品.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const handleDownloadTemplate = async () => {
  try {
    const data = await WarehouseProductApi.getImportTemplate()
    download.excel(data, '仓库商品导入模板.xls')
  } catch {}
}

const handleImport = async (rawFile: any) => {
  try {
    const data = new FormData()
    data.append('file', rawFile)
    const res = await WarehouseProductApi.importWarehouseProduct(data)
    if (res.failureWarehouseProductIds && Object.keys(res.failureWarehouseProductIds).length > 0) {
      const failureText = Object.entries(res.failureWarehouseProductIds)
        .map(([name, reason]: [string, string]) => `${name}: ${reason}`)
        .join('\n')
      message.warning(`导入失败：\n${failureText}`)
    } else {
      message.success(`导入成功：创建 ${res.createCount || 0}，更新 ${res.updateCount || 0}`)
    }
    await getList()
  } catch {}
  return false
}

onMounted(async () => {
  await getList()
  await loadWarehouseList()
})
</script>
