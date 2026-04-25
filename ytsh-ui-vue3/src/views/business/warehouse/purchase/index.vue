<template>
  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="94px"
    >
      <el-form-item label="采购单号" prop="purchaseOrderNo">
        <el-input
          v-model="queryParams.purchaseOrderNo"
          placeholder="请输入采购单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierId">
        <el-select
          v-model="queryParams.supplierId"
          placeholder="请选择供应商"
          filterable
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="item in supplierList"
            :key="item.supplierId"
            :label="formatSupplierLabel(item)"
            :value="item.supplierId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="收货仓库" prop="warehouseId">
        <el-select
          v-model="queryParams.warehouseId"
          placeholder="请选择收货仓库"
          filterable
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="item in warehouseList"
            :key="item.warehouseId"
            :label="formatWarehouseLabel(item)"
            :value="item.warehouseId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态" prop="orderStatus">
        <el-select v-model="queryParams.orderStatus" placeholder="请选择订单状态" clearable class="!w-240px">
          <el-option v-for="item in orderStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="收货状态" prop="receiveStatus">
        <el-select v-model="queryParams.receiveStatus" placeholder="请选择收货状态" clearable class="!w-240px">
          <el-option v-for="item in receiveStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-form-item label="采购员" prop="purchaser">
        <el-input
          v-model="queryParams.purchaser"
          placeholder="请输入采购员"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="采购日期" prop="purchaseDate">
        <el-date-picker
          v-model="queryParams.purchaseDate"
          value-format="YYYY-MM-DD"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
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
          v-hasPermi="['business:warehouse-purchase:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['business:warehouse-purchase:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="采购单号" align="center" prop="purchaseOrderNo" min-width="190" />
      <el-table-column label="供应商" align="center" prop="supplierName" min-width="140" />
      <el-table-column label="收货仓库" align="center" prop="warehouseName" min-width="140" />
      <el-table-column label="商品汇总" align="center" prop="productNames" min-width="220" />
      <el-table-column label="采购日期" align="center" prop="purchaseDate" width="120" />
      <el-table-column label="采购员" align="center" prop="purchaser" width="120" />
      <el-table-column label="总数量" align="center" prop="totalQty" width="90" />
      <el-table-column label="总金额" align="center" width="120">
        <template #default="scope">
          {{ formatPrice(scope.row.totalAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="总入库数" align="center" prop="totalInboundQty" width="100" />
      <el-table-column label="订单状态" align="center" prop="orderStatus" width="100">
        <template #default="scope">
          <el-tag :type="getOrderStatusTag(scope.row.orderStatus)">
            {{ getOrderStatusLabel(scope.row.orderStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="收货状态" align="center" prop="receiveStatus" width="100">
        <template #default="scope">
          <el-tag :type="getReceiveStatusTag(scope.row.receiveStatus)">
            {{ getReceiveStatusLabel(scope.row.receiveStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核日期" align="center" prop="auditDate" width="120" />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" fixed="right" width="280">
        <template #default="scope">
          <el-button
            link
            @click="openForm('detail', scope.row.purchaseOrderId)"
            v-hasPermi="['business:warehouse-purchase:query']"
          >
            详情
          </el-button>
          <el-button
            v-if="scope.row.orderStatus === '0'"
            link
            type="primary"
            @click="openForm('update', scope.row.purchaseOrderId)"
            v-hasPermi="['business:warehouse-purchase:update']"
          >
            编辑
          </el-button>
          <el-button
            v-if="scope.row.orderStatus === '0'"
            link
            type="warning"
            @click="handleSubmit(scope.row.purchaseOrderId)"
            v-hasPermi="['business:warehouse-purchase:update']"
          >
            提交
          </el-button>
          <el-button
            v-if="scope.row.orderStatus === '1'"
            link
            type="primary"
            @click="handleAudit(scope.row.purchaseOrderId)"
            v-hasPermi="['business:warehouse-purchase:update']"
          >
            审核
          </el-button>
          <el-button
            v-if="scope.row.orderStatus === '2'"
            link
            type="success"
            @click="handleConfirmInbound(scope.row.purchaseOrderId)"
            v-hasPermi="['business:warehouse-purchase:update']"
          >
            确认入库
          </el-button>
          <el-button
            v-if="['0', '1', '2'].includes(scope.row.orderStatus || '')"
            link
            type="danger"
            @click="handleCancel(scope.row.purchaseOrderId)"
            v-hasPermi="['business:warehouse-purchase:update']"
          >
            取消
          </el-button>
          <el-button
            v-if="['0', '5'].includes(scope.row.orderStatus || '')"
            link
            type="danger"
            @click="handleDelete(scope.row.purchaseOrderId)"
            v-hasPermi="['business:warehouse-purchase:delete']"
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

  <PurchaseForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import {
  WarehousePurchaseApi,
  type WarehousePurchasePageReqVO,
  type WarehousePurchaseRespVO
} from '@/api/business/warehouse/purchase'
import { WarehouseApi, type WarehouseSimpleRespVO } from '@/api/business/warehouse/warehouse'
import {
  WarehouseSupplierApi,
  type WarehouseSupplierSimpleRespVO
} from '@/api/business/warehouse/supplier'
import PurchaseForm from './PurchaseForm.vue'

const ORDER_STATUS_MAP: Record<string, { label: string; type: string }> = {
  '0': { label: '草稿', type: 'info' },
  '1': { label: '待审核', type: 'warning' },
  '2': { label: '已审核', type: 'primary' },
  '3': { label: '已入库', type: 'success' },
  '4': { label: '已完成', type: 'success' },
  '5': { label: '已取消', type: 'danger' }
}

const RECEIVE_STATUS_MAP: Record<string, { label: string; type: string }> = {
  '1': { label: '待收货', type: 'warning' },
  '2': { label: '部分收货', type: 'primary' },
  '3': { label: '已收货', type: 'success' },
  '4': { label: '有差异', type: 'danger' }
}

defineOptions({ name: 'BusinessWarehousePurchase' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<WarehousePurchaseRespVO[]>([])
const total = ref(0)
const exportLoading = ref(false)
const supplierList = ref<WarehouseSupplierSimpleRespVO[]>([])
const warehouseList = ref<WarehouseSimpleRespVO[]>([])
const queryParams = reactive<WarehousePurchasePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  purchaseOrderNo: undefined,
  supplierId: undefined,
  warehouseId: undefined,
  orderStatus: undefined,
  receiveStatus: undefined,
  skuCode: undefined,
  skuName: undefined,
  purchaser: undefined,
  purchaseDate: undefined
})
const queryFormRef = ref()

const orderStatusOptions = Object.entries(ORDER_STATUS_MAP).map(([value, item]) => ({
  value,
  label: item.label
}))
const receiveStatusOptions = Object.entries(RECEIVE_STATUS_MAP).map(([value, item]) => ({
  value,
  label: item.label
}))

const getOrderStatusLabel = (status?: string) => {
  return status ? ORDER_STATUS_MAP[status]?.label || status : '-'
}

const getOrderStatusTag = (status?: string) => {
  return status ? ORDER_STATUS_MAP[status]?.type || 'info' : 'info'
}

const getReceiveStatusLabel = (status?: string) => {
  return status ? RECEIVE_STATUS_MAP[status]?.label || status : '-'
}

const getReceiveStatusTag = (status?: string) => {
  return status ? RECEIVE_STATUS_MAP[status]?.type || 'info' : 'info'
}

const formatSupplierLabel = (item: WarehouseSupplierSimpleRespVO) => {
  return item.supplierName || item.supplierId
}

const formatWarehouseLabel = (item: WarehouseSimpleRespVO) => {
  return item.warehouseName || item.warehouseId
}

const formatPrice = (value?: number) => {
  return value != null ? `¥${Number(value).toFixed(2)}` : '-'
}

const getList = async () => {
  loading.value = true
  try {
    const data = await WarehousePurchaseApi.getWarehousePurchasePage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  const [suppliers, warehouses] = await Promise.all([
    WarehouseSupplierApi.getWarehouseSupplierSimpleList(),
    WarehouseApi.getWarehouseSimpleList()
  ])
  supplierList.value = suppliers || []
  warehouseList.value = warehouses || []
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
const openForm = (type: 'create' | 'update' | 'detail', purchaseOrderId?: number) => {
  formRef.value.open(type, purchaseOrderId)
}

const handleDelete = async (purchaseOrderId: number) => {
  try {
    await message.delConfirm()
    await WarehousePurchaseApi.deleteWarehousePurchase(purchaseOrderId)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleSubmit = async (purchaseOrderId: number) => {
  try {
    await message.confirm('确认提交该采购单吗？')
    await WarehousePurchaseApi.submitWarehousePurchase(purchaseOrderId)
    message.success('提交成功')
    await getList()
  } catch {}
}

const handleAudit = async (purchaseOrderId: number) => {
  try {
    await message.confirm('确认审核通过该采购单吗？')
    await WarehousePurchaseApi.auditWarehousePurchase(purchaseOrderId)
    message.success('审核成功')
    await getList()
  } catch {}
}

const handleConfirmInbound = async (purchaseOrderId: number) => {
  try {
    await message.confirm('确认执行入库吗？入库后库存将增加。')
    await WarehousePurchaseApi.confirmInbound(purchaseOrderId)
    message.success('入库成功')
    await getList()
  } catch {}
}

const handleCancel = async (purchaseOrderId: number) => {
  try {
    await message.confirm('确认取消该采购单吗？')
    await WarehousePurchaseApi.cancelWarehousePurchase(purchaseOrderId)
    message.success('取消成功')
    await getList()
  } catch {}
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await WarehousePurchaseApi.exportWarehousePurchase(queryParams)
    download.excel(data, '仓库采购订单.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

onMounted(async () => {
  await getList()
  await loadOptions()
})
</script>
