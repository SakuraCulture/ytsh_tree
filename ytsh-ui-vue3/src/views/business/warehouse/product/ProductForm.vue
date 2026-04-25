<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="formLoading"
    >
      <el-form-item label="仓库选择" prop="warehouseId">
        <el-select v-model="formData.warehouseId" placeholder="请选择仓库" filterable class="!w-100%">
          <el-option
            v-for="item in warehouseList"
            :key="item.warehouseId"
            :label="`${item.warehouseId} - ${item.warehouseName}`"
            :value="item.warehouseId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="SKU选择" prop="productSkuId">
        <div class="sku-selector-row">
          <el-input :model-value="selectedSkuDisplay" readonly placeholder="请选择SKU" />
          <el-button type="primary" plain @click="openSkuDialog">选择SKU</el-button>
          <el-button v-if="formData.productSkuId" @click="clearSkuSelection">清空</el-button>
        </div>
      </el-form-item>
      <el-form-item label="仓库采购价" prop="warehouseProductCostPrice">
        <el-input-number
          v-model="formData.warehouseProductCostPrice"
          :min="0"
          :precision="2"
          placeholder="请输入仓库采购价"
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item label="库位编码" prop="warehouseProductLocation">
        <el-input v-model="formData.warehouseProductLocation" placeholder="请输入库位编码" />
      </el-form-item>
      <el-form-item label="首次有库存日期" prop="warehouseProductFirstDate">
        <el-date-picker
          v-model="formData.warehouseProductFirstDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择首次有库存日期"
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item label="最近入库日期" prop="warehouseProductLastDate">
        <el-date-picker
          v-model="formData.warehouseProductLastDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择最近入库日期"
          class="!w-100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <Dialog v-model="skuDialogVisible" :appendToBody="true" title="选择SKU" width="980px">
    <ContentWrap>
      <el-form ref="skuQueryFormRef" :inline="true" :model="skuQueryParams" class="-mb-15px">
        <el-form-item label="SKU编码" prop="productSkuCode">
          <el-input
            v-model="skuQueryParams.productSkuCode"
            class="!w-240px"
            clearable
            placeholder="请输入SKU编码"
            @keyup.enter="handleSkuQuery"
          />
        </el-form-item>
        <el-form-item label="SKU名称" prop="productSkuName">
          <el-input
            v-model="skuQueryParams.productSkuName"
            class="!w-240px"
            clearable
            placeholder="请输入SKU名称"
            @keyup.enter="handleSkuQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleSkuQuery">
            <Icon class="mr-5px" icon="ep:search" />搜索
          </el-button>
          <el-button @click="resetSkuQuery">
            <Icon class="mr-5px" icon="ep:refresh" />重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="skuLoading" :data="filteredSkuList" show-overflow-tooltip>
        <el-table-column label="SKU编码" align="center" prop="productSkuCode" width="180" />
        <el-table-column label="SKU名称" align="center" prop="productSkuName" min-width="220" />
        <el-table-column label="主EAN码" align="center" prop="productSkuEan" width="160" />
        <el-table-column label="基准零售价" align="center" width="140">
          <template #default="{ row }">
            {{ row.productRetailPrice != null ? `¥${Number(row.productRetailPrice).toFixed(2)}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.productSkuStatus === 1 ? 'success' : 'info'">
              {{ row.productSkuStatus === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleSelectSku(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </Dialog>
</template>

<script setup lang="ts">
import {
  WarehouseProductApi,
  type SkuSimpleRespVO,
  type WarehouseProductRespVO,
  type WarehouseProductSaveReqVO
} from '@/api/business/warehouse/product'
import { WarehouseApi, type WarehouseSimpleRespVO } from '@/api/business/warehouse/warehouse'

interface FormDataType extends WarehouseProductSaveReqVO {
  selectedSkuCode?: string
  selectedSkuName?: string
}

defineOptions({ name: 'BusinessWarehouseProductForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()

const warehouseList = ref<WarehouseSimpleRespVO[]>([])
const skuDialogVisible = ref(false)
const skuLoading = ref(false)
const skuList = ref<SkuSimpleRespVO[]>([])
const skuQueryFormRef = ref()
const skuQueryParams = reactive({
  productSkuCode: undefined as string | undefined,
  productSkuName: undefined as string | undefined
})

const createDefaultFormData = (): FormDataType => ({
  warehouseProductId: undefined,
  warehouseId: undefined,
  productSkuId: undefined,
  warehouseProductCostPrice: undefined,
  warehouseProductLocation: undefined,
  warehouseProductFirstDate: undefined,
  warehouseProductLastDate: undefined,
  selectedSkuCode: undefined,
  selectedSkuName: undefined
})

const formData = ref<FormDataType>(createDefaultFormData())
const formRules = reactive({
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  productSkuId: [{ required: true, message: '请选择SKU', trigger: 'change' }]
})

const selectedSkuDisplay = computed(() => {
  const values = [formData.value.selectedSkuCode, formData.value.selectedSkuName].filter(Boolean)
  return values.join(' - ')
})

const filteredSkuList = computed(() => {
  return skuList.value.filter((item) => {
    const matchCode =
      !skuQueryParams.productSkuCode || item.productSkuCode?.includes(skuQueryParams.productSkuCode)
    const matchName =
      !skuQueryParams.productSkuName || item.productSkuName?.includes(skuQueryParams.productSkuName)
    const matchStatus = item.productSkuStatus === 1
    return matchCode && matchName && matchStatus
  })
})

const loadWarehouseList = async () => {
  warehouseList.value = await WarehouseApi.getWarehouseSimpleList()
}

const loadSkuList = async () => {
  skuLoading.value = true
  try {
    skuList.value = await WarehouseProductApi.getSkuSimpleList()
  } finally {
    skuLoading.value = false
  }
}

const handleSkuQuery = () => {
  skuQueryFormRef.value?.validate?.()
}

const resetSkuQuery = () => {
  skuQueryFormRef.value?.resetFields()
}

const openSkuDialog = async () => {
  await loadSkuList()
  skuDialogVisible.value = true
}

const handleSelectSku = (row: SkuSimpleRespVO) => {
  formData.value.productSkuId = row.productSkuId
  formData.value.selectedSkuCode = row.productSkuCode
  formData.value.selectedSkuName = row.productSkuName
  if (formData.value.warehouseProductCostPrice === undefined && row.productRetailPrice !== undefined) {
    formData.value.warehouseProductCostPrice = row.productRetailPrice
  }
  skuDialogVisible.value = false
}

const clearSkuSelection = () => {
  formData.value.productSkuId = undefined
  formData.value.selectedSkuCode = undefined
  formData.value.selectedSkuName = undefined
}

const fillSelectedSku = async (respVO: WarehouseProductRespVO) => {
  formData.value.selectedSkuCode = respVO.skuCode
  formData.value.selectedSkuName = respVO.skuName
  if (!respVO.skuCode && !respVO.skuName && respVO.productSkuId) {
    await loadSkuList()
    const currentSku = skuList.value.find((item) => item.productSkuId === respVO.productSkuId)
    if (currentSku) {
      formData.value.selectedSkuCode = currentSku.productSkuCode
      formData.value.selectedSkuName = currentSku.productSkuName
    }
  }
}

const open = async (type: 'create' | 'update', warehouseProductId?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadWarehouseList()
  if (!warehouseProductId) {
    return
  }
  formLoading.value = true
  try {
    const data = (await WarehouseProductApi.getWarehouseProduct(
      warehouseProductId
    )) as WarehouseProductRespVO
    formData.value = {
      ...createDefaultFormData(),
      ...data
    }
    await fillSelectedSku(data)
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const payload: WarehouseProductSaveReqVO = {
      warehouseProductId: formType.value === 'update' ? formData.value.warehouseProductId : undefined,
      warehouseId: formData.value.warehouseId,
      productSkuId: formData.value.productSkuId,
      warehouseProductCostPrice: formData.value.warehouseProductCostPrice,
      warehouseProductLocation: formData.value.warehouseProductLocation,
      warehouseProductFirstDate: formData.value.warehouseProductFirstDate,
      warehouseProductLastDate: formData.value.warehouseProductLastDate
    }
    if (formType.value === 'create') {
      await WarehouseProductApi.createWarehouseProduct(payload)
      message.success(t('common.createSuccess'))
    } else {
      await WarehouseProductApi.updateWarehouseProduct(payload)
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
  skuList.value = []
  skuDialogVisible.value = false
  formRef.value?.resetFields()
}
</script>

<style scoped>
.sku-selector-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  width: 100%;
}
</style>
