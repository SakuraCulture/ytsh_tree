<template>
  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="82px"
    >
      <el-form-item label="供应商ID" prop="supplierId">
        <el-input
          v-model="queryParams.supplierId"
          placeholder="请输入供应商ID"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="供应商名称" prop="supplierName">
        <el-input
          v-model="queryParams.supplierName"
          placeholder="请输入供应商名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="负责人" prop="managerName">
        <el-input
          v-model="queryParams.managerName"
          placeholder="请输入负责人"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="联系电话" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入联系电话"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="supplierStatus">
        <el-select
          v-model="queryParams.supplierStatus"
          placeholder="请选择供应商状态"
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
          v-hasPermi="['business:warehouse-supplier:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['business:warehouse-supplier:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="供应商ID" align="center" prop="supplierId" width="130" />
      <el-table-column label="供应商名称" align="center" prop="supplierName" min-width="160" />
      <el-table-column label="供应商分类" align="center" prop="categoryName" width="120" />
      <el-table-column label="负责人" align="center" prop="managerName" width="120" />
      <el-table-column label="联系电话" align="center" prop="phone" width="140" />
      <el-table-column label="联系地址" align="center" prop="address" min-width="220" />
      <el-table-column label="付款方式" align="center" prop="paymentMethod" width="110" />
      <el-table-column label="账期天数" align="center" prop="paymentDays" width="100" />
      <el-table-column label="状态" align="center" prop="supplierStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.supplierStatus === 1 ? 'success' : 'info'">
            {{ scope.row.supplierStatus === 1 ? '正常' : '停用' }}
          </el-tag>
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
            @click="openForm('update', scope.row.supplierId)"
            v-hasPermi="['business:warehouse-supplier:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.supplierId)"
            v-hasPermi="['business:warehouse-supplier:delete']"
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

  <SupplierForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import {
  WarehouseSupplierApi,
  type WarehouseSupplierPageReqVO,
  type WarehouseSupplierRespVO
} from '@/api/business/warehouse/supplier'
import SupplierForm from './SupplierForm.vue'

defineOptions({ name: 'BusinessWarehouseSupplier' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<WarehouseSupplierRespVO[]>([])
const total = ref(0)
const queryParams = reactive<WarehouseSupplierPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  supplierId: undefined,
  supplierName: undefined,
  categoryName: undefined,
  managerName: undefined,
  phone: undefined,
  supplierStatus: undefined,
  createTime: undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)

const getList = async () => {
  loading.value = true
  try {
    const data = await WarehouseSupplierApi.getWarehouseSupplierPage(queryParams)
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
const openForm = (type: 'create' | 'update', supplierId?: string) => {
  formRef.value.open(type, supplierId)
}

const handleDelete = async (supplierId: string) => {
  try {
    await message.delConfirm()
    await WarehouseSupplierApi.deleteWarehouseSupplier(supplierId)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await WarehouseSupplierApi.exportWarehouseSupplier(queryParams)
    download.excel(data, '供应商.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
