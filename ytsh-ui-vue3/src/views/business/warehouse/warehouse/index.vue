<template>
  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="82px"
    >
      <el-form-item label="仓库ID" prop="warehouseId">
        <el-input
          v-model="queryParams.warehouseId"
          placeholder="请输入仓库ID"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="仓库编码" prop="warehouseCode">
        <el-input
          v-model="queryParams.warehouseCode"
          placeholder="请输入仓库编码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="仓库名称" prop="warehouseName">
        <el-input
          v-model="queryParams.warehouseName"
          placeholder="请输入仓库名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="仓库状态" prop="warehouseStatus">
        <el-select
          v-model="queryParams.warehouseStatus"
          placeholder="请选择仓库状态"
          clearable
          class="!w-240px"
        >
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="默认仓" prop="isDefault">
        <el-select
          v-model="queryParams.isDefault"
          placeholder="请选择默认仓"
          clearable
          class="!w-240px"
        >
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
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
          v-hasPermi="['business:warehouse:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['business:warehouse:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="仓库ID" align="center" prop="warehouseId" width="120" />
      <el-table-column label="仓库编码" align="center" prop="warehouseCode" width="140" />
      <el-table-column label="仓库名称" align="center" prop="warehouseName" min-width="160" />
      <el-table-column label="仓库类型" align="center" prop="warehouseType" width="120" />
      <el-table-column label="行政区划" align="center" prop="regionCode" width="120" />
      <el-table-column label="详细地址" align="center" prop="address" min-width="220" />
      <el-table-column label="状态" align="center" prop="warehouseStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.warehouseStatus === 1 ? 'success' : 'info'">
            {{ scope.row.warehouseStatus === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认仓" align="center" prop="isDefault" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.isDefault"
            :active-value="1"
            :inactive-value="0"
            @change="handleDefaultStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
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
            @click="openForm('update', scope.row.warehouseId)"
            v-hasPermi="['business:warehouse:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.warehouseId)"
            v-hasPermi="['business:warehouse:delete']"
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

  <WarehouseForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import {
  WarehouseApi,
  type WarehousePageReqVO,
  type WarehouseRespVO
} from '@/api/business/warehouse/warehouse'
import WarehouseForm from './WarehouseForm.vue'

defineOptions({ name: 'BusinessWarehouse' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<WarehouseRespVO[]>([])
const total = ref(0)
const queryParams = reactive<WarehousePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  warehouseId: undefined,
  warehouseCode: undefined,
  warehouseName: undefined,
  warehouseStatus: undefined,
  isDefault: undefined,
  createTime: undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)

const getList = async () => {
  loading.value = true
  try {
    const data = await WarehouseApi.getWarehousePage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
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
const openForm = (type: 'create' | 'update', warehouseId?: string) => {
  formRef.value.open(type, warehouseId)
}

const handleDelete = async (warehouseId: string) => {
  try {
    await message.delConfirm()
    await WarehouseApi.deleteWarehouse(warehouseId)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleDefaultStatusChange = async (row: WarehouseRespVO) => {
  try {
    const text = row.isDefault === 1 ? '设置' : '取消'
    await message.confirm(`确认要${text}“${row.warehouseName || row.warehouseId}”为默认仓吗？`)
    await WarehouseApi.updateWarehouseDefaultStatus(row.warehouseId, row.isDefault || 0)
    await getList()
  } catch {
    row.isDefault = row.isDefault === 1 ? 0 : 1
  }
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await WarehouseApi.exportWarehouse(queryParams)
    download.excel(data, '仓库.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
