<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="720px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="对象域">
        <el-select v-model="selectedDomainType" placeholder="请选择对象域" @change="handleDomainTypeChange">
          <el-option label="商品" value="PRODUCT" />
          <el-option label="门店" value="STORE" />
          <el-option label="会员" value="MEMBER" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签维度" prop="dimensionId">
        <el-tree-select
          v-model="formData.dimensionId"
          :data="dimensionTreeOptions"
          :props="{ label: 'name', value: 'id', children: 'children' }"
          check-strictly
          default-expand-all
          filterable
          clearable
          :disabled="!selectedDomainType"
          placeholder="请先选择对象域，再选择三级标签维度"
        />
      </el-form-item>
      <el-form-item label="标签值名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入标签值名称" />
      </el-form-item>
      <el-form-item label="标签值编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入标签值编码" />
      </el-form-item>
      <el-form-item label="打标方式" prop="tagMethod">
        <el-select v-model="formData.tagMethod" placeholder="请选择打标方式">
          <el-option label="人工" value="MANUAL" />
          <el-option label="规则" value="RULE" />
          <el-option label="算法" value="ALGORITHM" />
          <el-option label="继承" value="INHERIT" />
        </el-select>
      </el-form-item>
      <el-form-item label="数据来源" prop="dataSource">
        <el-input v-model="formData.dataSource" placeholder="请输入数据来源" />
      </el-form-item>
      <el-form-item label="更新频率" prop="updateFrequency">
        <el-input v-model="formData.updateFrequency" placeholder="请输入更新频率" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" class="!w-100%" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="逻辑说明" prop="logicDescription">
        <el-input v-model="formData.logicDescription" type="textarea" :rows="4" placeholder="请输入逻辑说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitForm" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { TagValueApi, type TagValue } from '@/api/business/tag/value'
import type { TagDimension } from '@/api/business/tag/dimension'
import { buildDimensionTreeOptions, type DimensionOption } from './valuePageLogic'

defineOptions({ name: 'BusinessTagValueForm' })

const { t } = useI18n()
const message = useMessage()

const props = defineProps<{
  dimensions: TagDimension[]
}>()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const dimensionTreeOptions = ref<DimensionOption[]>([])
const selectedDomainType = ref<string | undefined>()

const createDefaultFormData = () => ({
  id: undefined as number | undefined,
  dimensionId: undefined as number | undefined,
  name: undefined as string | undefined,
  code: undefined as string | undefined,
  tagMethod: 'MANUAL',
  dataSource: undefined as string | undefined,
  updateFrequency: undefined as string | undefined,
  logicDescription: undefined as string | undefined,
  sort: 0,
  status: 1
})

const formData = ref(createDefaultFormData())
const formRules = reactive({
  dimensionId: [{ required: true, message: '标签维度不能为空', trigger: 'change' }],
  name: [{ required: true, message: '标签值名称不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '标签值编码不能为空', trigger: 'blur' }],
  tagMethod: [{ required: true, message: '打标方式不能为空', trigger: 'change' }]
})

const emit = defineEmits(['success'])

const refreshDimensionTreeOptions = (domainType?: string) => {
  dimensionTreeOptions.value = buildDimensionTreeOptions(props.dimensions, domainType)
}

const handleDomainTypeChange = () => {
  formData.value.dimensionId = undefined
  refreshDimensionTreeOptions(selectedDomainType.value)
}

const open = async (type: 'create' | 'update', id?: number, domainType?: string) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()

  if (type === 'create') {
    selectedDomainType.value = domainType
    refreshDimensionTreeOptions(domainType)
    return
  }

  if (!id) {
    return
  }

  formLoading.value = true
  try {
    const data = await TagValueApi.getTagValue(id)
    const matchedDimension = props.dimensions.find((item) => item.id === data.dimensionId)
    formData.value = {
      ...createDefaultFormData(),
      ...data
    }
    selectedDomainType.value = matchedDimension?.domainType
    refreshDimensionTreeOptions(selectedDomainType.value)
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })

const submitForm = async () => {
  if (!selectedDomainType.value) {
    message.warning('请先选择对象域')
    return
  }
  await formRef.value.validate()
  formLoading.value = true
  try {
    const payload = { ...formData.value } as TagValue
    if (formType.value === 'create') {
      await TagValueApi.createTagValue(payload)
      message.success(t('common.createSuccess'))
    } else {
      await TagValueApi.updateTagValue(payload)
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
  selectedDomainType.value = undefined
  dimensionTreeOptions.value = []
  formRef.value?.resetFields()
}
</script>
