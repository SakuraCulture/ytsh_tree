<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="620">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="供应商ID" prop="supplierId">
        <el-input
          v-model="formData.supplierId"
          :disabled="formType === 'update'"
          placeholder="请输入供应商ID"
        />
      </el-form-item>
      <el-form-item label="供应商名称" prop="supplierName">
        <el-input v-model="formData.supplierName" placeholder="请输入供应商名称" />
      </el-form-item>
      <el-form-item label="供应商分类" prop="categoryName">
        <el-input v-model="formData.categoryName" placeholder="请输入供应商分类" />
      </el-form-item>
      <el-form-item label="负责人" prop="managerName">
        <el-input v-model="formData.managerName" placeholder="请输入负责人" />
      </el-form-item>
      <el-form-item label="联系电话" prop="phone">
        <el-input v-model="formData.phone" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="联系地址" prop="address">
        <el-input v-model="formData.address" type="textarea" placeholder="请输入联系地址" />
      </el-form-item>
      <el-form-item label="付款方式" prop="paymentMethod">
        <el-input v-model="formData.paymentMethod" placeholder="请输入付款方式" />
      </el-form-item>
      <el-form-item label="账期天数" prop="paymentDays">
        <el-input-number
          v-model="formData.paymentDays"
          :min="0"
          :precision="0"
          placeholder="请输入账期天数"
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item label="状态" prop="supplierStatus">
        <el-radio-group v-model="formData.supplierStatus">
          <el-radio :value="1">正常</el-radio>
          <el-radio :value="0">停用</el-radio>
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
import {
  WarehouseSupplierApi,
  type WarehouseSupplierRespVO,
  type WarehouseSupplierSaveReqVO
} from '@/api/business/warehouse/supplier'

defineOptions({ name: 'BusinessWarehouseSupplierForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()

const createDefaultFormData = (): WarehouseSupplierSaveReqVO => ({
  supplierId: undefined,
  supplierName: undefined,
  categoryName: undefined,
  managerName: undefined,
  phone: undefined,
  address: undefined,
  paymentMethod: undefined,
  paymentDays: undefined,
  supplierStatus: 1
})

const formData = ref<WarehouseSupplierSaveReqVO>(createDefaultFormData())
const formRules = reactive({
  supplierId: [{ required: true, message: '供应商ID不能为空', trigger: 'blur' }],
  supplierName: [{ required: true, message: '供应商名称不能为空', trigger: 'blur' }],
  supplierStatus: [{ required: true, message: '请选择供应商状态', trigger: 'change' }]
})

const open = async (type: 'create' | 'update', supplierId?: string) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (!supplierId) {
    return
  }
  formLoading.value = true
  try {
    const data = (await WarehouseSupplierApi.getWarehouseSupplier(supplierId)) as WarehouseSupplierRespVO
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
      await WarehouseSupplierApi.createWarehouseSupplier(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await WarehouseSupplierApi.updateWarehouseSupplier(formData.value)
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
