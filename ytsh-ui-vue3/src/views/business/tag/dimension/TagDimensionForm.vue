<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="680px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="对象域" prop="domainType">
        <el-select
          v-model="formData.domainType"
          placeholder="请选择对象域"
          :disabled="structureLocked"
          @change="handleDomainTypeChange"
        >
          <el-option label="商品" value="PRODUCT" />
          <el-option label="门店" value="STORE" />
          <el-option label="会员" value="MEMBER" />
        </el-select>
      </el-form-item>
      <el-form-item label="父级维度" prop="parentId">
        <el-tree-select
          v-model="formData.parentId"
          :data="parentOptions"
          :props="{ label: 'name', value: 'id', children: 'children' }"
          check-strictly
          default-expand-all
          clearable
          filterable
          :disabled="structureLocked"
          placeholder="请选择父级维度"
          @change="handleParentChange"
        />
      </el-form-item>
      <el-form-item label="层级" prop="level">
        <el-input :model-value="formatLevel(formData.level)" disabled />
        <div v-if="structureLocked" class="el-form-item__error !static !mt-4px">
          当前维度已有子节点，不能修改对象域、父级和层级
        </div>
      </el-form-item>
      <el-form-item label="维度名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入维度名称" />
      </el-form-item>
      <el-form-item label="维度编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入维度编码" />
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
      <el-form-item label="描述" prop="description">
        <el-input v-model="formData.description" type="textarea" placeholder="请输入描述" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitForm" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { TagDimensionApi, type TagDimension } from '@/api/business/tag/dimension'
import {
  buildParentOptions,
  getNextLevelByParent,
  hasChildren
} from './dimensionFormLogic'

defineOptions({ name: 'BusinessTagDimensionForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const allDimensions = ref<TagDimension[]>([])
const parentOptions = ref<Array<{ id: number; name: string; children?: TagDimension[] }>>([])
const structureLocked = ref(false)

const createDefaultFormData = () => ({
  id: undefined as number | undefined,
  domainType: undefined as string | undefined,
  parentId: 0,
  level: 1,
  name: undefined as string | undefined,
  code: undefined as string | undefined,
  sort: 0,
  status: 1,
  description: undefined as string | undefined
})

const formData = ref(createDefaultFormData())
const formRules = reactive({
  domainType: [{ required: true, message: '对象域不能为空', trigger: 'change' }],
  name: [{ required: true, message: '维度名称不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '维度编码不能为空', trigger: 'blur' }]
})

const emit = defineEmits(['success'])

const refreshParentOptions = () => {
  parentOptions.value = buildParentOptions(
    allDimensions.value,
    formData.value.domainType,
    formData.value.id
  )
}

const handleDomainTypeChange = () => {
  formData.value.parentId = 0
  formData.value.level = 1
  refreshParentOptions()
}

const handleParentChange = (parentId?: number) => {
  formData.value.parentId = parentId || 0
  formData.value.level = getNextLevelByParent(allDimensions.value, parentId)
}

const open = async (type: 'create' | 'update', id?: number, dimensions: TagDimension[] = []) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  allDimensions.value = dimensions
  resetForm()

  if (type === 'update' && id) {
    formLoading.value = true
    try {
      const data = await TagDimensionApi.getTagDimension(id)
      formData.value = {
        ...createDefaultFormData(),
        ...data,
        parentId: data.parentId ?? 0,
        level: data.level ?? 1,
        sort: data.sort ?? 0,
        status: data.status ?? 1
      }
      structureLocked.value = hasChildren(allDimensions.value, id)
      refreshParentOptions()
    } finally {
      formLoading.value = false
    }
    return
  }

  structureLocked.value = false
  refreshParentOptions()
}

defineExpose({ open })

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = { ...formData.value } as TagDimension
    if (formType.value === 'create') {
      await TagDimensionApi.createTagDimension(data)
      message.success(t('common.createSuccess'))
    } else {
      await TagDimensionApi.updateTagDimension(data)
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
  structureLocked.value = false
  formRef.value?.resetFields()
}

const formatLevel = (level?: number) => {
  if (level === 1) {
    return '一级'
  }
  if (level === 2) {
    return '二级'
  }
  if (level === 3) {
    return '三级'
  }
  return '-'
}
</script>
