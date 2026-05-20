<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="680px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
      aria-label="类目表单"
    >
      <el-form-item label="状态" prop="status" class="status-field">
        <el-radio-group v-model="formData.status" aria-label="选择类目状态">
          <el-radio value="1">启用</el-radio>
          <el-radio value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>

      <fieldset class="form-fieldset basic-info-fieldset">
        <legend class="form-legend">基础信息</legend>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类目名称" prop="categoryName" required>
              <el-input
                v-model="formData.categoryName"
                placeholder="请输入类目名称"
                @blur="validateField('categoryName')"
                aria-describedby="categoryName-error"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="层级" prop="categoryLevel" required>
              <el-select
                v-model="formData.categoryLevel"
                placeholder="请选择层级"
                @change="handleLevelChange"
                aria-label="选择类目层级"
              >
                <el-option label="一级" value="1" />
                <el-option label="二级" value="2" />
                <el-option label="三级" value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父类目" prop="parentId">
              <el-tree-select
                v-model="formData.parentId"
                :data="categoryTableTree"
                :props="{ label: 'categoryName', value: 'categoryId', children: 'children' }"
                check-strictly
                default-expand-all
                placeholder="0表示一级类目"
                @change="handleParentChange"
                aria-label="选择父类目"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </fieldset>

      <fieldset class="form-fieldset relation-info-fieldset">
        <legend class="form-legend">关联信息</legend>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类目路径" prop="categoryPath">
              <el-input
                v-model="formData.categoryPathNames"
                placeholder="自动生成"
                readonly
                class="path-input"
                aria-label="类目路径自动生成"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="同级排序" prop="sortOrder">
              <el-input
                v-model="formData.sortOrder"
                placeholder="请输入同级排序"
                type="number"
                min="0"
                aria-label="输入同级排序"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="是否叶子类目" prop="isLeaf">
              <el-select
                v-model="formData.isLeaf"
                placeholder="请选择"
                :disabled="formData.categoryLevel === '3'"
                aria-label="选择是否为叶子类目"
              >
                <el-option label="否" value="0" />
                <el-option label="是" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </fieldset>

      <fieldset class="form-fieldset display-info-fieldset">
        <legend class="form-legend">展示信息</legend>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类目图标URL" prop="categoryIcon">
              <el-input
                v-model="formData.categoryIcon"
                placeholder="请输入图标URL"
                @blur="handleIconPreview"
                aria-label="输入类目图标URL"
              />
              <div v-if="iconPreviewUrl" class="icon-preview">
                <img :src="iconPreviewUrl" alt="图标预览" @error="handleIconError" />
              </div>
              <div v-else-if="iconError" class="icon-preview error">
                <span class="placeholder-icon">?</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类目配图URL" prop="categoryImage">
              <UploadImg v-model="formData.categoryImage" height="120px" width="120px" />
            </el-form-item>
          </el-col>
        </el-row>
      </fieldset>
    </el-form>

    <template #footer>
      <div class="form-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm" :disabled="formLoading">确 定</el-button>
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { CategoryTableApi, CategoryTable } from '@/api/business/category'
import {
  buildCategoryMap,
  buildCategoryPathNames,
  deriveCategoryFieldsByParent
} from './categoryFormLogic'
import { handleTree } from '@/utils/tree'

defineOptions({ name: 'CategoryTableForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref({
  categoryId: undefined,
  categoryName: '',
  parentId: undefined,
  parentCategoryName: '',
  categoryLevel: '',
  categoryPath: '',
  categoryPathNames: '-',
  categoryIcon: '',
  categoryImage: '',
  sortOrder: undefined,
  isLeaf: '',
  status: '1'
})
const formRules = reactive({
  categoryName: [{ required: true, message: '类目名称不能为空', trigger: 'blur' }],
  categoryLevel: [{ required: true, message: '层级不能为空', trigger: 'change' }]
})
const formRef = ref()
const categoryTableTree = ref()
const allCategoryList = ref<CategoryTable[]>([])
const categoryMap = ref<Map<number, CategoryTable>>(new Map())

const iconPreviewUrl = ref('')
const iconError = ref(false)

const validateField = (field: string) => {
  formRef.value?.validateField(field)
}

const handleLevelChange = (value: string) => {
  if (value === '3') {
    formData.value.isLeaf = '1'
  }
}

const syncDerivedCategoryFields = (parentId: string | number | undefined | null) => {
  Object.assign(formData.value, deriveCategoryFieldsByParent(parentId, categoryMap.value))
}

const handleParentChange = (parentId: string | number | undefined) => {
  syncDerivedCategoryFields(parentId)
}

watch(
  () => formData.value.parentId,
  (parentId, previousParentId) => {
    if (!dialogVisible.value || parentId === previousParentId) {
      return
    }
    syncDerivedCategoryFields(parentId)
  }
)

const handleIconPreview = () => {
  iconError.value = false
  if (formData.value.categoryIcon) {
    iconPreviewUrl.value = formData.value.categoryIcon
  } else {
    iconPreviewUrl.value = ''
  }
}

const handleIconError = () => {
  iconError.value = true
  iconPreviewUrl.value = ''
}

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()

  await preloadCategoryOptions()

  if (formType.value === 'create') {
    formData.value.status = '1'
  }

  if (id) {
    formLoading.value = true
    try {
      const data = await CategoryTableApi.getCategoryTable(id)
      formData.value = {
        ...data,
        categoryLevel: String(data.categoryLevel),
        status: String(data.status),
        isLeaf: data.isLeaf !== undefined ? String(data.isLeaf) : ''
      }
      if (data.categoryIcon) {
        iconPreviewUrl.value = data.categoryIcon
      }
      if (data.parentId && data.parentId !== 0) {
        const parent = categoryMap.value.get(data.parentId)
        formData.value.parentCategoryName = parent?.categoryName || `类目${data.parentId}`
      } else {
        formData.value.parentCategoryName = '-'
      }
      formData.value.categoryPathNames = buildCategoryPathNames(data.categoryPath, categoryMap.value)
    } finally {
      formLoading.value = false
    }
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    message.error('请检查表单填写是否正确')
    return
  }

  formLoading.value = true
  try {
    const data = {
      ...formData.value,
      categoryLevel: parseInt(formData.value.categoryLevel),
      status: parseInt(formData.value.status),
      isLeaf: formData.value.isLeaf ? parseInt(formData.value.isLeaf) : undefined
    } as unknown as CategoryTable

    if (formType.value === 'create') {
      await CategoryTableApi.createCategoryTable(data)
      message.success(t('common.createSuccess'))
    } else {
      await CategoryTableApi.updateCategoryTable(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } catch {
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    categoryId: undefined,
    categoryName: '',
    parentId: undefined,
    parentCategoryName: '',
    categoryLevel: '',
    categoryPath: '',
    categoryPathNames: '-',
    categoryIcon: '',
    categoryImage: '',
    sortOrder: undefined,
    isLeaf: '',
    status: '1'
  }
  iconPreviewUrl.value = ''
  iconError.value = false
  formRef.value?.resetFields()
}

const preloadCategoryOptions = async () => {
  const data = await CategoryTableApi.getCategoryTableList({ pageNo: 1, pageSize: 999999 })
  const list = data.list || data || []
  allCategoryList.value = list
  categoryMap.value = buildCategoryMap(list)
  categoryTableTree.value = [
    {
      categoryId: 0,
      categoryName: '顶级（无父类目）',
      children: handleTree(list, 'categoryId', 'parentId')
    }
  ]
}
</script>

<style scoped lang="scss">
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.form-fieldset {
  border: 1px dashed #e5e7eb;
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 16px;
  background-color: #fafafa;
}

.basic-info-fieldset {
  background-color: #f5f7fa;
  border-color: #dcdfe6;
}

.form-legend {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding: 0 8px;
  margin-bottom: 16px;
}

.status-field {
  margin-bottom: 16px;

  :deep(.el-form-item__label) {
    font-weight: 600;
  }
}

.path-input {
  :deep(.el-input__wrapper) {
    background-color: #f5f7fa;
  }
}

.icon-preview {
  margin-top: 8px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background-color: #fff;
  overflow: hidden;

  img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }

  &.error {
    background-color: #f5f7fa;
  }

  .placeholder-icon {
    color: #c0c4cc;
    font-size: 16px;
    font-weight: bold;
  }
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 34px;
  font-size: 14px;
}

:deep(.el-tree-select__wrapper) {
  min-height: 34px;
}

@media (prefers-reduced-motion: reduce) {
  * {
    transition: none !important;
    animation: none !important;
  }
}

@media (forced-colors: active) {
  .form-fieldset {
    outline: 2px solid CanvasText;
  }

  :deep(.el-input__wrapper:focus-within),
  :deep(.el-select__wrapper.is-focused) {
    outline: 2px solid CanvasText;
    outline-offset: 2px;
  }

  .el-button:focus {
    outline: 2px solid CanvasText;
    outline-offset: 2px;
  }
}
</style>
