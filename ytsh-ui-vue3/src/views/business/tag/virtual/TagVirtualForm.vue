<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="对象域" prop="domainType">
        <el-select v-model="formData.domainType" placeholder="请选择对象域">
          <el-option label="商品" value="PRODUCT" />
          <el-option label="门店" value="STORE" />
          <el-option label="会员" value="MEMBER" />
        </el-select>
      </el-form-item>
      <el-form-item label="虚拟标签名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入虚拟标签名称" />
      </el-form-item>
      <el-form-item label="虚拟标签编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入虚拟标签编码" />
      </el-form-item>
      <el-form-item label="表达式 JSON" prop="expressionJson">
        <el-input
          v-model="formData.expressionJson"
          type="textarea"
          :rows="10"
          placeholder='请输入合法 JSON 对象，例如 {"op":"and","conditions":[]}'
        />
      </el-form-item>
      <el-form-item label="表达式摘要" prop="expressionSummary">
        <el-input v-model="formData.expressionSummary" placeholder="请输入表达式摘要" />
      </el-form-item>
      <el-form-item label="使用场景" prop="usageScenario">
        <el-input v-model="formData.usageScenario" placeholder="请输入使用场景" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitForm" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { TagVirtualApi, type TagVirtual } from '@/api/business/tag/virtual'
import { isValidExpressionJsonObject } from './virtualFormLogic'

defineOptions({ name: 'BusinessTagVirtualForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()

const createDefaultFormData = () => ({
  id: undefined as number | undefined,
  domainType: undefined as string | undefined,
  name: undefined as string | undefined,
  code: undefined as string | undefined,
  expressionJson: '{\n  "op": "and",\n  "conditions": []\n}',
  expressionSummary: undefined as string | undefined,
  usageScenario: undefined as string | undefined,
  status: 1
})

const formData = ref(createDefaultFormData())
const formRules = reactive({
  domainType: [{ required: true, message: '对象域不能为空', trigger: 'change' }],
  name: [{ required: true, message: '虚拟标签名称不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '虚拟标签编码不能为空', trigger: 'blur' }],
  expressionJson: [
    { required: true, message: '表达式 JSON 不能为空', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (!value) {
          callback(new Error('表达式 JSON 不能为空'))
          return
        }
        if (!isValidExpressionJsonObject(value)) {
          callback(new Error('表达式 JSON 必须是合法 JSON 对象'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
})

const emit = defineEmits(['success'])

const open = async (type: 'create' | 'update', id?: number, domainType?: string) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()

  if (type === 'create') {
    formData.value.domainType = domainType
    return
  }

  if (!id) {
    return
  }

  formLoading.value = true
  try {
    const data = await TagVirtualApi.getTagVirtual(id)
    formData.value = {
      ...createDefaultFormData(),
      ...data,
      status: data.status ?? 1
    }
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const payload = { ...formData.value } as TagVirtual
    if (formType.value === 'create') {
      await TagVirtualApi.createTagVirtual(payload)
      message.success(t('common.createSuccess'))
    } else {
      await TagVirtualApi.updateTagVirtual(payload)
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
