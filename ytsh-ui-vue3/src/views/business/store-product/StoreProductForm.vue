<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="formLoading"
    >
      <el-form-item label="门店选择" prop="storeId">
        <el-select
          v-model="formData.storeId"
          placeholder="请输入门店编码或名称搜索"
          filterable
          remote
          reserve-keyword
          :remote-method="searchStoreSuggestions"
          :loading="storeLoading"
          class="!w-100%"
        >
          <el-option
            v-for="item in storeList"
            :key="item.storeId"
            :label="`${item.storeId} - ${item.storeName}`"
            :value="item.storeId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="商品选择" prop="productSkuId">
        <div class="product-selector-row">
          <el-input
            :model-value="formData.productDisplay"
            readonly
            placeholder="请选择商品"
          />
          <el-button type="primary" plain @click="openProductDialog">选择商品</el-button>
          <el-button v-if="formData.productSkuId" @click="clearProductSelection">清空</el-button>
        </div>
        <div v-if="formData.skuDisplay" class="selected-sku-text">
          已选规格：{{ formData.skuDisplay }}
        </div>
      </el-form-item>
      <el-form-item label="商品归属" prop="productAttribution">
        <el-radio-group v-model="formData.productAttribution">
          <el-radio value="HQ">总部</el-radio>
          <el-radio value="STORE">门店</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="门店零售价" prop="storeRetailPrice">
        <el-input-number
          v-model="formData.storeRetailPrice"
          :min="0"
          :precision="2"
          placeholder="请输入门店零售价"
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item label="首次入店日期" prop="firstEnterShopDate">
        <el-date-picker
          v-model="formData.firstEnterShopDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择首次入店日期"
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item label="POS状态" prop="posStatus">
        <el-radio-group v-model="formData.posStatus">
          <el-radio :value="1">已上架</el-radio>
          <el-radio :value="0">未上架</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="是否入店" prop="enterShopStatus">
        <el-radio-group v-model="formData.enterShopStatus">
          <el-radio :value="1">是</el-radio>
          <el-radio :value="0">否</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <Dialog v-model="productDialogVisible" :appendToBody="true" title="选择商品" width="70%">
    <ContentWrap>
      <el-form ref="productQueryFormRef" :inline="true" :model="productQueryParams" class="-mb-15px">
        <el-form-item label="商品编码" prop="productSpuCode">
          <el-input
            v-model="productQueryParams.productSpuCode"
            class="!w-240px"
            clearable
            placeholder="请输入商品编码"
            @keyup.enter="handleProductQuery"
          />
        </el-form-item>
        <el-form-item label="商品名称" prop="productSpuName">
          <el-input
            v-model="productQueryParams.productSpuName"
            class="!w-240px"
            clearable
            placeholder="请输入商品名称"
            @keyup.enter="handleProductQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleProductQuery">
            <Icon class="mr-5px" icon="ep:search" />
            搜索
          </el-button>
          <el-button @click="resetProductQuery">
            <Icon class="mr-5px" icon="ep:refresh" />
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="productLoading" :data="productList" show-overflow-tooltip>
        <el-table-column label="商品编码" align="center" prop="productSpuCode" width="160" />
        <el-table-column label="商品名称" align="center" prop="productSpuName" min-width="220" />
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.productSpuStatus === 1 ? 'success' : 'info'">
              {{ row.productSpuStatus === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleSelectProduct(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:page="productQueryParams.pageNo"
        v-model:limit="productQueryParams.pageSize"
        :total="productTotal"
        @pagination="getProductList"
      />
    </ContentWrap>
  </Dialog>

  <Dialog v-model="skuDialogVisible" :appendToBody="true" title="选择规格" width="900px">
    <el-table v-loading="skuLoading" :data="skuList" show-overflow-tooltip>
      <el-table-column label="SKU编码" align="center" prop="productSkuCode" width="180" />
      <el-table-column label="SKU名称" align="center" prop="productSkuName" min-width="220" />
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
  </Dialog>
</template>

<script setup lang="ts">
import { StoreProductApi, StoreProductDetailRespVO, StoreProductSaveReqVO, StoreProductTable } from '@/api/business/store-product'
import { TableApi } from '@/api/business/store'
import { SpuTableApi, SpuTable, SkuTable } from '@/api/business/product'

interface FormDataType {
  storeProductId?: string | number
  storeId?: string
  productSkuId?: string
  productAttribution?: string
  storeRetailPrice?: number
  firstEnterShopDate?: string
  posStatus?: number
  enterShopStatus?: number
  productSpuId?: number
  productDisplay?: string
  skuDisplay?: string
}

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref<FormDataType>({})
const formRules = reactive({
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  productSkuId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  productAttribution: [{ required: true, message: '请选择商品归属', trigger: 'change' }],
  storeRetailPrice: [{ required: true, message: '请输入门店零售价', trigger: 'blur' }],
  posStatus: [{ required: true, message: '请选择POS状态', trigger: 'change' }],
  enterShopStatus: [{ required: true, message: '请选择是否入店', trigger: 'change' }]
})

const storeLoading = ref(false)
const storeList = ref<any[]>([])

const productDialogVisible = ref(false)
const productQueryFormRef = ref()
const productLoading = ref(false)
const productTotal = ref(0)
const productList = ref<SpuTable[]>([])
const productQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  productSpuCode: undefined as string | undefined,
  productSpuName: undefined as string | undefined
})

const skuDialogVisible = ref(false)
const skuLoading = ref(false)
const skuList = ref<SkuTable[]>([])
const selectedProduct = ref<SpuTable | null>(null)

const searchStoreSuggestions = async (query: string) => {
  if (!query) {
    storeList.value = []
    return
  }
  storeLoading.value = true
  try {
    const data = await TableApi.getTableSimpleList(query)
    storeList.value = data || []
  } catch {
    storeList.value = []
  } finally {
    storeLoading.value = false
  }
}

const getProductList = async () => {
  productLoading.value = true
  try {
    const data = await SpuTableApi.getSpuTablePage(productQueryParams)
    productList.value = data.list || []
    productTotal.value = data.total || 0
  } finally {
    productLoading.value = false
  }
}

const handleProductQuery = () => {
  productQueryParams.pageNo = 1
  getProductList()
}

const resetProductQuery = () => {
  productQueryFormRef.value?.resetFields()
  productQueryParams.pageNo = 1
  productQueryParams.pageSize = 10
  getProductList()
}

const openProductDialog = () => {
  productDialogVisible.value = true
  resetProductQuery()
}

const clearProductSelection = () => {
  formData.value.productSpuId = undefined
  formData.value.productSkuId = undefined
  formData.value.productDisplay = undefined
  formData.value.skuDisplay = undefined
  selectedProduct.value = null
  skuList.value = []
}

const applyProductSelection = (product: SpuTable, sku: SkuTable) => {
  formData.value.productSpuId = product.productSpuId
  formData.value.productSkuId = sku.productSkuId != null ? String(sku.productSkuId) : undefined
  formData.value.productDisplay = [product.productSpuCode, product.productSpuName].filter(Boolean).join(' - ')
  formData.value.skuDisplay = [sku.productSkuCode, sku.productSkuName].filter(Boolean).join(' - ')
  selectedProduct.value = product
  productDialogVisible.value = false
  skuDialogVisible.value = false
}

const handleSelectProduct = async (row: SpuTable) => {
  if (!row.productSpuId) {
    return
  }
  skuLoading.value = true
  try {
    const detail = await SpuTableApi.getSpuTable(row.productSpuId)
    const currentProduct = detail || row
    const currentSkuList = currentProduct.skuTables || []
    if (currentSkuList.length === 0) {
      message.warning('该商品暂无可选规格')
      return
    }
    if (currentSkuList.length === 1) {
      applyProductSelection(currentProduct, currentSkuList[0])
      return
    }
    selectedProduct.value = currentProduct
    skuList.value = currentSkuList
    productDialogVisible.value = false
    skuDialogVisible.value = true
  } finally {
    skuLoading.value = false
  }
}

const handleSelectSku = (row: SkuTable) => {
  if (!selectedProduct.value) {
    return
  }
  applyProductSelection(selectedProduct.value, row)
}

const fillStoreOption = (storeId?: string, storeName?: string) => {
  if (!storeId) {
    return
  }
  storeList.value = [
    {
      storeId,
      storeName: storeName || storeId
    }
  ]
}

const fillSkuDisplay = (skuCode?: string, skuName?: string, productSkuId?: string) => {
  const currentSkuDisplay = [skuCode, skuName].filter(Boolean).join(' - ')
  if (currentSkuDisplay) {
    formData.value.productDisplay = currentSkuDisplay
    formData.value.skuDisplay = currentSkuDisplay
    return
  }
  if (productSkuId) {
    formData.value.productDisplay = productSkuId
    formData.value.skuDisplay = productSkuId
  }
}

const normalizePosStatus = (value?: string | number) => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const normalizedValue = Number(value)
  return Number.isNaN(normalizedValue) ? undefined : normalizedValue
}

const fillFormByDetail = (data: StoreProductDetailRespVO, row?: StoreProductTable) => {
  const productSkuId = data.productSkuId != null ? String(data.productSkuId) : row?.productSkuId
  formData.value = {
    storeProductId: data.storeProductId,
    storeId: data.storeId ?? row?.storeId,
    productSkuId,
    productAttribution: data.productAttribution ?? row?.productAttribution,
    storeRetailPrice: data.storeRetailPrice ?? row?.storeRetailPrice,
    firstEnterShopDate: data.firstEnterShopDate ?? row?.firstEnterShopDate,
    posStatus: normalizePosStatus(data.posStatus ?? row?.posStatus),
    enterShopStatus: data.enterShopStatus ?? row?.enterShopStatus
  }
  fillStoreOption(formData.value.storeId, data.storeName ?? row?.storeName)
  fillSkuDisplay(data.skuCode ?? row?.skuCode, data.skuName ?? row?.skuName, productSkuId)
}

const open = async (type: string, id?: number | string, row?: StoreProductTable) => {
  resetForm()
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  if (id) {
    formLoading.value = true
    try {
      const data = await StoreProductApi.getTable(id)
      fillFormByDetail(data, row)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const payload: StoreProductSaveReqVO = {
      storeProductId: formType.value === 'update' ? formData.value.storeProductId : undefined,
      storeId: formData.value.storeId,
      productSkuId: formData.value.productSkuId,
      storeProductOwnership: formData.value.productAttribution,
      storeProductPrice: formData.value.storeRetailPrice,
      storeProductFirstDate: formData.value.firstEnterShopDate,
      storeProductPosStatus:
        formData.value.posStatus === undefined ? undefined : String(formData.value.posStatus),
      storeProductIsActive: formData.value.enterShopStatus
    }
    if (formType.value === 'create') {
      await StoreProductApi.createTable(payload)
      message.success(t('common.createSuccess'))
    } else {
      await StoreProductApi.updateTable(payload)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    storeProductId: undefined,
    storeId: undefined,
    productSkuId: undefined,
    productAttribution: undefined,
    storeRetailPrice: undefined,
    firstEnterShopDate: undefined,
    posStatus: undefined,
    enterShopStatus: undefined,
    productSpuId: undefined,
    productDisplay: undefined,
    skuDisplay: undefined
  }
  storeList.value = []
  productList.value = []
  productTotal.value = 0
  skuList.value = []
  selectedProduct.value = null
  productDialogVisible.value = false
  skuDialogVisible.value = false
  formRef.value?.resetFields()
}
</script>

<style scoped>
.product-selector-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  width: 100%;
}

.selected-sku-text {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
</style>
