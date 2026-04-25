<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="1200">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
      :disabled="disabled"
    >
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="采购单号">
            <el-input v-model="formData.purchaseOrderNo" disabled placeholder="保存后自动生成" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="采购日期" prop="purchaseDate">
            <el-date-picker
              v-model="formData.purchaseDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择采购日期"
              class="!w-100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="供应商" prop="supplierId">
            <el-select v-model="formData.supplierId" filterable placeholder="请选择供应商" class="!w-100%">
              <el-option
                v-for="item in supplierList"
                :key="item.supplierId"
                :label="formatSupplierLabel(item)"
                :value="item.supplierId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="收货仓库" prop="warehouseId">
            <el-select
              v-model="formData.warehouseId"
              filterable
              placeholder="请选择收货仓库"
              class="!w-100%"
              @change="handleWarehouseChange"
            >
              <el-option
                v-for="item in warehouseList"
                :key="item.warehouseId"
                :label="formatWarehouseLabel(item)"
                :value="item.warehouseId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="采购员" prop="purchaser">
            <el-input v-model="formData.purchaser" placeholder="请输入采购员" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="收货地址" prop="receiveAddress">
            <el-input v-model="formData.receiveAddress" placeholder="请输入收货地址" />
          </el-form-item>
        </el-col>
        <el-col :span="24" v-if="formType !== 'create'">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="订单状态">
                <el-tag :type="getOrderStatusTag(formData.orderStatus)">
                  {{ getOrderStatusLabel(formData.orderStatus) }}
                </el-tag>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="收货状态">
                <el-tag :type="getReceiveStatusTag(formData.receiveStatus)">
                  {{ getReceiveStatusLabel(formData.receiveStatus) }}
                </el-tag>
              </el-form-item>
            </el-col>
          </el-row>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" />
          </el-form-item>
        </el-col>
      </el-row>

      <ContentWrap :body-style="{ padding: '16px 20px' }">
        <div class="detail-wrap">
          <div class="mb-12px flex items-center justify-between">
            <div class="text-14px font-600">采购明细</div>
            <el-button v-if="!disabled" type="primary" plain @click="handleAddItem">
              <Icon icon="ep:plus" class="mr-5px" /> 添加明细
            </el-button>
          </div>
          <el-table :data="formData.items" border>
            <el-table-column label="序号" type="index" align="center" width="60" />
            <el-table-column label="SKU" min-width="220">
              <template #default="{ row, $index }">
                <el-form-item
                  :prop="`items.${$index}.productSkuId`"
                  :rules="itemRules.productSkuId"
                  label-width="0"
                  class="mb-0px!"
                >
                  <el-select
                    v-model="row.productSkuId"
                    filterable
                    clearable
                    placeholder="请选择SKU"
                    class="!w-100%"
                    @change="(value) => handleSelectSku(value, row)"
                  >
                    <el-option
                      v-for="item in availableSkuOptions"
                      :key="item.productSkuId"
                      :label="item.label"
                      :value="item.productSkuId"
                    />
                  </el-select>
                </el-form-item>
              </template>
            </el-table-column>
            <el-table-column label="SKU编码" align="center" min-width="140">
              <template #default="{ row }">
                <span>{{ row.productSkuCode || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="SKU名称" align="center" min-width="160">
              <template #default="{ row }">
                <span>{{ row.productSkuName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="采购数量" min-width="120">
              <template #default="{ row, $index }">
                <el-form-item
                  :prop="`items.${$index}.purchaseQty`"
                  :rules="itemRules.purchaseQty"
                  label-width="0"
                  class="mb-0px!"
                >
                  <el-input-number
                    v-model="row.purchaseQty"
                    controls-position="right"
                    :min="1"
                    :precision="0"
                    class="!w-100%"
                  />
                </el-form-item>
              </template>
            </el-table-column>
            <el-table-column label="箱数" min-width="110">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.boxQty"
                  controls-position="right"
                  :min="0"
                  :precision="0"
                  class="!w-100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="标准装箱数量" min-width="140">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.standardBoxQty"
                  controls-position="right"
                  :min="0"
                  :precision="0"
                  class="!w-100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="采购单价" min-width="120">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.purchasePrice"
                  controls-position="right"
                  :min="0"
                  :precision="2"
                  class="!w-100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="采购金额" align="center" min-width="120">
              <template #default="{ row }">
                <span>{{ formatPrice(row.purchaseAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已入库数量" align="center" min-width="110">
              <template #default="{ row }">
                <span>{{ row.inboundQty ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="差异数" align="center" min-width="90">
              <template #default="{ row }">
                <span>{{ row.diffQty ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="退货数" align="center" min-width="90">
              <template #default="{ row }">
                <span>{{ row.returnQty ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="!disabled" label="操作" align="center" width="80" fixed="right">
              <template #default="{ $index }">
                <el-button link type="danger" @click="handleDeleteItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="summary-bar">
            <span>总数量：{{ totalQty }}</span>
            <span>总金额：{{ formatPrice(totalAmount) }}</span>
          </div>
        </div>
      </ContentWrap>
    </el-form>
    <template #footer>
      <el-button v-if="!disabled" @click="submitForm" type="primary" :disabled="formLoading">
        确 定
      </el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { erpPriceMultiply } from '@/utils'
import {
  WarehousePurchaseApi,
  type WarehousePurchaseRespItemVO,
  type WarehousePurchaseRespVO,
  type WarehousePurchaseSaveItemReqVO,
  type WarehousePurchaseSaveReqVO
} from '@/api/business/warehouse/purchase'
import {
  WarehouseProductApi,
  type SkuSimpleRespVO,
  type WarehouseProductSimpleRespVO
} from '@/api/business/warehouse/product'
import { WarehouseApi, type WarehouseSimpleRespVO } from '@/api/business/warehouse/warehouse'
import {
  WarehouseSupplierApi,
  type WarehouseSupplierSimpleRespVO
} from '@/api/business/warehouse/supplier'

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

interface PurchaseItemForm extends WarehousePurchaseSaveItemReqVO {
  productSkuCode?: string
  productSkuName?: string
  purchaseAmount?: number
  inboundQty?: number
  returnQty?: number
  diffQty?: number
}

interface FormDataType extends Omit<WarehousePurchaseSaveReqVO, 'items'> {
  purchaseOrderNo?: string
  orderStatus?: string
  receiveStatus?: string
  items: PurchaseItemForm[]
}

defineOptions({ name: 'BusinessWarehousePurchaseForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update' | 'detail'>('create')
const formRef = ref()

const supplierList = ref<WarehouseSupplierSimpleRespVO[]>([])
const warehouseList = ref<WarehouseSimpleRespVO[]>([])
const warehouseProductList = ref<WarehouseProductSimpleRespVO[]>([])
const skuList = ref<SkuSimpleRespVO[]>([])

const disabled = computed(() => formType.value === 'detail')

const createEmptyItem = (): PurchaseItemForm => ({
  detailId: undefined,
  productSkuId: undefined,
  productSkuCode: undefined,
  productSkuName: undefined,
  purchaseQty: 1,
  boxQty: undefined,
  standardBoxQty: undefined,
  purchasePrice: undefined,
  purchaseAmount: undefined,
  inboundQty: 0,
  returnQty: 0,
  diffQty: 0
})

const createDefaultFormData = (): FormDataType => ({
  purchaseOrderId: undefined,
  purchaseOrderNo: undefined,
  supplierId: undefined,
  warehouseId: undefined,
  purchaseDate: undefined,
  purchaser: undefined,
  receiveAddress: undefined,
  remark: undefined,
  orderStatus: undefined,
  receiveStatus: undefined,
  items: [createEmptyItem()]
})

const formData = ref<FormDataType>(createDefaultFormData())
const formRules = reactive({
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择收货仓库', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择采购日期', trigger: 'change' }]
})
const itemRules = reactive({
  productSkuId: [{ required: true, message: '请选择SKU', trigger: 'change' }],
  purchaseQty: [{ required: true, message: '请输入采购数量', trigger: 'blur' }]
})

const skuInfoMap = computed(() => {
  const map = new Map<number, SkuSimpleRespVO>()
  skuList.value.forEach((item) => {
    map.set(item.productSkuId, item)
  })
  return map
})

const availableSkuOptions = computed(() => {
  return warehouseProductList.value.map((item) => {
    const skuInfo = skuInfoMap.value.get(item.productSkuId || 0)
    const productSkuCode = skuInfo?.productSkuCode
    const productSkuName = item.skuName || skuInfo?.productSkuName
    return {
      productSkuId: item.productSkuId,
      productSkuCode,
      productSkuName,
      label: [productSkuCode, productSkuName].filter(Boolean).join(' - ') || String(item.productSkuId)
    }
  })
})

const totalQty = computed(() => {
  return formData.value.items.reduce((sum, item) => sum + Number(item.purchaseQty || 0), 0)
})

const totalAmount = computed(() => {
  return formData.value.items.reduce((sum, item) => sum + Number(item.purchaseAmount || 0), 0)
})

watch(
  () => formData.value.items,
  (items) => {
    items.forEach((item) => {
      item.purchaseAmount = erpPriceMultiply(Number(item.purchasePrice), Number(item.purchaseQty)) || 0
    })
  },
  { deep: true }
)

const formatPrice = (value?: number) => {
  return value != null ? `¥${Number(value).toFixed(2)}` : '-'
}

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

const fillSupplierOption = (supplierId?: string, supplierName?: string) => {
  if (!supplierId || supplierList.value.some((item) => item.supplierId === supplierId)) {
    return
  }
  supplierList.value = [
    {
      supplierId,
      supplierName: supplierName || supplierId
    },
    ...supplierList.value
  ]
}

const fillWarehouseOption = (warehouseId?: string, warehouseName?: string) => {
  if (!warehouseId || warehouseList.value.some((item) => item.warehouseId === warehouseId)) {
    return
  }
  warehouseList.value = [
    {
      warehouseId,
      warehouseName: warehouseName || warehouseId,
      isDefault: 0
    },
    ...warehouseList.value
  ]
}

const loadBaseOptions = async () => {
  const [suppliers, warehouses, skus] = await Promise.all([
    WarehouseSupplierApi.getWarehouseSupplierSimpleList(),
    WarehouseApi.getWarehouseSimpleList(),
    WarehouseProductApi.getSkuSimpleList()
  ])
  supplierList.value = suppliers || []
  warehouseList.value = warehouses || []
  skuList.value = skus || []
}

const loadWarehouseProductList = async (warehouseId?: string) => {
  if (!warehouseId) {
    warehouseProductList.value = []
    return
  }
  warehouseProductList.value = await WarehouseProductApi.getWarehouseProductSimpleList(warehouseId)
}

const handleWarehouseChange = async (warehouseId?: string) => {
  await loadWarehouseProductList(warehouseId)
  if (formType.value === 'create' || formType.value === 'update') {
    formData.value.items = formData.value.items.map((item) => ({
      ...item,
      productSkuId: undefined,
      productSkuCode: undefined,
      productSkuName: undefined
    }))
  }
}

const handleSelectSku = (productSkuId: number | undefined, row: PurchaseItemForm) => {
  if (productSkuId == null) {
    row.productSkuCode = undefined
    row.productSkuName = undefined
    return
  }
  const target = availableSkuOptions.value.find((item) => item.productSkuId === productSkuId)
  row.productSkuCode = target?.productSkuCode
  row.productSkuName = target?.productSkuName
}

const handleAddItem = () => {
  if (!formData.value.warehouseId) {
    message.warning('请先选择收货仓库')
    return
  }
  formData.value.items.push(createEmptyItem())
}

const handleDeleteItem = (index: number) => {
  formData.value.items.splice(index, 1)
}

const mapRespItemToFormItem = (item: WarehousePurchaseRespItemVO): PurchaseItemForm => ({
  detailId: item.detailId,
  productSkuId: item.productSkuId,
  productSkuCode: item.productSkuCode,
  productSkuName: item.productSkuName,
  purchaseQty: item.purchaseQty,
  boxQty: item.boxQty,
  standardBoxQty: item.standardBoxQty,
  purchasePrice: item.purchasePrice,
  purchaseAmount: item.purchaseAmount,
  inboundQty: item.inboundQty,
  returnQty: item.returnQty,
  diffQty: item.diffQty
})

const open = async (type: 'create' | 'update' | 'detail', purchaseOrderId?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadBaseOptions()
  if (!purchaseOrderId) {
    return
  }
  formLoading.value = true
  try {
    const data = (await WarehousePurchaseApi.getWarehousePurchase(purchaseOrderId)) as WarehousePurchaseRespVO
    formData.value = {
      purchaseOrderId: data.purchaseOrderId,
      purchaseOrderNo: data.purchaseOrderNo,
      supplierId: data.supplierId,
      warehouseId: data.warehouseId,
      purchaseDate: data.purchaseDate,
      purchaser: data.purchaser,
      receiveAddress: data.receiveAddress,
      remark: data.remark,
      orderStatus: data.orderStatus,
      receiveStatus: data.receiveStatus,
      items: data.items?.length ? data.items.map(mapRespItemToFormItem) : [createEmptyItem()]
    }
    fillSupplierOption(data.supplierId, data.supplierName)
    fillWarehouseOption(data.warehouseId, data.warehouseName)
    await loadWarehouseProductList(data.warehouseId)
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  await formRef.value.validate()
  if (!formData.value.items.length) {
    message.warning('请至少添加一条采购明细')
    return
  }
  formLoading.value = true
  try {
    const payload: WarehousePurchaseSaveReqVO = {
      purchaseOrderId: formType.value === 'update' ? formData.value.purchaseOrderId : undefined,
      supplierId: formData.value.supplierId,
      warehouseId: formData.value.warehouseId,
      purchaseDate: formData.value.purchaseDate,
      purchaser: formData.value.purchaser,
      receiveAddress: formData.value.receiveAddress,
      remark: formData.value.remark,
      items: formData.value.items.map((item) => ({
        detailId: item.detailId,
        productSkuId: item.productSkuId,
        purchaseQty: item.purchaseQty,
        boxQty: item.boxQty,
        standardBoxQty: item.standardBoxQty,
        purchasePrice: item.purchasePrice
      }))
    }
    if (formType.value === 'create') {
      await WarehousePurchaseApi.createWarehousePurchase(payload)
      message.success(t('common.createSuccess'))
    } else {
      await WarehousePurchaseApi.updateWarehousePurchase(payload)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  warehouseProductList.value = []
  supplierList.value = []
  warehouseList.value = []
  formRef.value?.resetFields()
}
</script>

<style scoped>
.detail-wrap {
  margin-top: 4px;
}

.summary-bar {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  margin-top: 16px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}
</style>
