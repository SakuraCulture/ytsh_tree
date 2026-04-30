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
      <el-form-item label="门店" prop="storeId">
        <el-select
          v-model="queryParams.storeId"
          placeholder="请选择门店"
          filterable
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="item in storeList"
            :key="item.storeId"
            :label="`${item.storeId} - ${item.storeName}`"
            :value="item.storeId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="主仓" prop="isPrimary">
        <el-select
          v-model="queryParams.isPrimary"
          placeholder="请选择主仓标记"
          clearable
          class="!w-240px"
        >
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="supplyStatus">
        <el-select
          v-model="queryParams.supplyStatus"
          placeholder="请选择状态"
          clearable
          class="!w-240px"
        >
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
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
          v-hasPermi="['business:warehouse-store-supply:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button type="primary" plain @click="handleDownloadTemplate" class="mr-10px">
          <Icon icon="ep:download" class="mr-5px" /> 下载模板
        </el-button>
        <el-upload
          accept=".xlsx, .xls"
          :show-file-list="false"
          :before-upload="handleImport"
          class="mr-10px"
          v-hasPermi="['business:warehouse-store-supply:import']"
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
          v-hasPermi="['business:warehouse-store-supply:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="仓库ID" align="center" prop="warehouseId" width="120" />
      <el-table-column label="仓库名称" align="center" prop="warehouseName" min-width="150" />
      <el-table-column label="门店ID" align="center" prop="storeId" width="120" />
      <el-table-column label="门店名称" align="center" prop="storeName" min-width="150" />
      <el-table-column label="主仓" align="center" prop="isPrimary" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.isPrimary === 1 ? 'success' : 'info'">
            {{ scope.row.isPrimary === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="supplyStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.supplyStatus === 1 ? 'success' : 'info'">
            {{ scope.row.supplyStatus === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="180" />
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
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['business:warehouse-store-supply:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['business:warehouse-store-supply:delete']"
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

  <StoreSupplyForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import {
  WarehouseStoreSupplyApi,
  type WarehouseStoreSupplyPageReqVO,
  type WarehouseStoreSupplyRespVO
} from '@/api/business/warehouse/storeSupply'
import { WarehouseApi, type WarehouseSimpleRespVO } from '@/api/business/warehouse/warehouse'
import { TableApi, type StoreSimpleRespVO } from '@/api/business/store'
import StoreSupplyForm from './StoreSupplyForm.vue'

defineOptions({ name: 'BusinessWarehouseStoreSupply' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<WarehouseStoreSupplyRespVO[]>([])
const total = ref(0)
const warehouseList = ref<WarehouseSimpleRespVO[]>([])
const storeList = ref<StoreSimpleRespVO[]>([])
const queryParams = reactive<WarehouseStoreSupplyPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  warehouseId: undefined,
  storeId: undefined,
  isPrimary: undefined,
  supplyStatus: undefined,
  createTime: undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)

const getList = async () => {
  loading.value = true
  try {
    const data = await WarehouseStoreSupplyApi.getPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  const [warehouses, stores] = await Promise.all([
    WarehouseApi.getWarehouseSimpleList(),
    TableApi.getTableAllSimpleList()
  ])
  warehouseList.value = warehouses || []
  storeList.value = stores || []
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
const openForm = (type: 'create' | 'update', id?: number) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await WarehouseStoreSupplyApi.delete(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await WarehouseStoreSupplyApi.export(queryParams)
    download.excel(data, '仓库门店供货关系.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const handleDownloadTemplate = async () => {
  try {
    const data = await WarehouseStoreSupplyApi.getImportTemplate()
    download.excel(data, '仓库门店供货关系导入模板.xls')
  } catch {}
}

const handleImport = async (rawFile: File) => {
  try {
    const data = new FormData()
    data.append('file', rawFile)
    const res = await WarehouseStoreSupplyApi.importExcel(data)
    if (res.failureRows && Object.keys(res.failureRows).length > 0) {
      const failureText = Object.entries(res.failureRows)
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
  await Promise.all([getList(), loadOptions()])
})
</script>
