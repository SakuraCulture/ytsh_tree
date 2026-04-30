<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="680">
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
      <el-form-item label="门店选择" prop="storeId">
        <el-select v-model="formData.storeId" placeholder="请选择门店" filterable class="!w-100%">
          <el-option
            v-for="item in storeList"
            :key="item.storeId"
            :label="`${item.storeId} - ${item.storeName}`"
            :value="item.storeId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="主仓" prop="isPrimary">
        <el-radio-group v-model="formData.isPrimary">
          <el-radio :value="1">是</el-radio>
          <el-radio :value="0">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态" prop="supplyStatus">
        <el-select v-model="formData.supplyStatus" placeholder="请选择状态" class="!w-100%">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import {
  WarehouseStoreSupplyApi,
  type WarehouseStoreSupplyRespVO,
  type WarehouseStoreSupplySaveReqVO
} from '@/api/business/warehouse/storeSupply'
import { WarehouseApi, type WarehouseSimpleRespVO } from '@/api/business/warehouse/warehouse'
import { TableApi, type StoreSimpleRespVO } from '@/api/business/store'

defineOptions({ name: 'BusinessWarehouseStoreSupplyForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const warehouseList = ref<WarehouseSimpleRespVO[]>([])
const storeList = ref<StoreSimpleRespVO[]>([])

const createDefaultFormData = (): WarehouseStoreSupplySaveReqVO => ({
  id: undefined,
  warehouseId: undefined as unknown as string,
  storeId: undefined as unknown as string,
  isPrimary: 0,
  supplyStatus: 1,
  remark: undefined
})

const formData = ref<WarehouseStoreSupplySaveReqVO>(createDefaultFormData())
const formRules = reactive({
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  isPrimary: [{ required: true, message: '请选择主仓标记', trigger: 'change' }],
  supplyStatus: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

const loadOptions = async () => {
  const [warehouses, stores] = await Promise.all([
    WarehouseApi.getWarehouseSimpleList(),
    TableApi.getTableAllSimpleList()
  ])
  warehouseList.value = warehouses || []
  storeList.value = stores || []
}

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    await loadOptions()
    if (!id) {
      return
    }
    const data = (await WarehouseStoreSupplyApi.get(id)) as WarehouseStoreSupplyRespVO
    formData.value = {
      ...createDefaultFormData(),
      ...data
    }
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
    if (formType.value === 'create') {
      await WarehouseStoreSupplyApi.create(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await WarehouseStoreSupplyApi.update(formData.value)
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
  formRef.value?.resetFields()
}
</script>
