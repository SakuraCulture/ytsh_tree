<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="620">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="仓库ID" prop="warehouseId">
        <el-input
          v-model="formData.warehouseId"
          :disabled="formType === 'update'"
          placeholder="请输入仓库ID"
        />
      </el-form-item>
      <el-form-item label="仓库编码" prop="warehouseCode">
        <el-input v-model="formData.warehouseCode" placeholder="请输入仓库编码" />
      </el-form-item>
      <el-form-item label="仓库名称" prop="warehouseName">
        <el-input v-model="formData.warehouseName" placeholder="请输入仓库名称" />
      </el-form-item>
      <el-form-item label="仓库类型" prop="warehouseType">
        <el-input v-model="formData.warehouseType" placeholder="请输入仓库类型" />
      </el-form-item>
      <el-form-item label="行政区划" prop="regionCode">
        <AreaSelect
          v-model="formData.regionCode"
          :level="AreaLevelEnum.DISTRICT"
          placeholder="请选择行政区划"
          clearable
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item label="详细地址" prop="address">
        <el-input v-model="formData.address" type="textarea" placeholder="请输入详细地址" />
      </el-form-item>
      <el-form-item label="状态" prop="warehouseStatus">
        <el-radio-group v-model="formData.warehouseStatus">
          <el-radio :value="1">正常</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="默认仓" prop="isDefault">
        <el-radio-group v-model="formData.isDefault">
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
</template>

<script setup lang="ts">
import { WarehouseApi, type WarehouseRespVO, type WarehouseSaveReqVO } from '@/api/business/warehouse/warehouse'
import AreaSelect from '@/components/FormCreate/src/components/AreaSelect.vue'
import { AreaLevelEnum } from '@/utils/constants'

interface FormDataType extends Omit<WarehouseSaveReqVO, 'regionCode'> {
  regionCode?: string | string[]
}

defineOptions({ name: 'BusinessWarehouseForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()

const createDefaultFormData = (): FormDataType => ({
  warehouseId: undefined,
  warehouseCode: undefined,
  warehouseName: undefined,
  warehouseType: undefined,
  regionCode: undefined,
  address: undefined,
  warehouseStatus: 1,
  isDefault: 0
})

const formData = ref<FormDataType>(createDefaultFormData())
const formRules = reactive({
  warehouseId: [{ required: true, message: '仓库ID不能为空', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '仓库名称不能为空', trigger: 'blur' }],
  warehouseStatus: [{ required: true, message: '请选择仓库状态', trigger: 'change' }]
})

const open = async (type: 'create' | 'update', warehouseId?: string) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (!warehouseId) {
    return
  }
  formLoading.value = true
  try {
    const data = (await WarehouseApi.getWarehouse(warehouseId)) as WarehouseRespVO
    formData.value = {
      ...createDefaultFormData(),
      ...data,
      regionCode: data.regionCode ? [data.regionCode] : undefined
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
    const data = {
      ...formData.value,
      regionCode: Array.isArray(formData.value.regionCode)
        ? formData.value.regionCode[formData.value.regionCode.length - 1]
        : formData.value.regionCode
    } as WarehouseSaveReqVO
    if (formType.value === 'create') {
      await WarehouseApi.createWarehouse(data)
      message.success(t('common.createSuccess'))
    } else {
      await WarehouseApi.updateWarehouse(data)
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
