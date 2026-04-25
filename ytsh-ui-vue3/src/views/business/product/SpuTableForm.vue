<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="SPU编码" prop="productSpuCode">
            <el-input v-model="formData.productSpuCode" placeholder="请输入SPU编码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="SPU名称" prop="productSpuName">
            <el-input v-model="formData.productSpuName" placeholder="请输入SPU名称" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="品牌" prop="productBrand">
            <el-input v-model="formData.productBrand" placeholder="请输入品牌" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分类" prop="categoryId">
            <el-tree-select
              v-model="formData.categoryId"
              :data="categoryTreeData"
              :props="{ label: 'categoryName', value: 'categoryId', children: 'children' }"
              check-strictly
              default-expand-all
              placeholder="请选择类目"
              class="!w-100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="产地" prop="productOrigin">
            <el-input v-model="formData.productOrigin" placeholder="请输入产地" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="生产商" prop="productManufacturer">
            <el-input v-model="formData.productManufacturer" placeholder="请输入生产商" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="规格模板" prop="productSpecTemplate">
            <el-input v-model="formData.productSpecTemplate" placeholder="请输入规格模板" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="productSpuStatus">
            <el-radio-group v-model="formData.productSpuStatus">
              <el-radio :value="0">下架</el-radio>
              <el-radio :value="1">上架</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <!-- 子表的表单 -->
    <el-tabs v-model="subTabsName">
      <el-tab-pane label="SKU商品主数据" name="skuTable">
        <SkuTableForm ref="skuTableFormRef" :product-spu-id="formData.productSpuId" />
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { SpuTableApi, SpuTable } from '@/api/business/product'
import { CategoryTableApi } from '@/api/business/category'
import { handleTree } from '@/utils/tree'
import SkuTableForm from './components/SkuTableForm.vue'

/** SPU基础分类 表单 */
defineOptions({ name: 'SpuTableForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  productSpuId: undefined,
  productSpuCode: undefined,
  productSpuName: undefined,
  productBrand: undefined,
  categoryId: undefined,
  productOrigin: undefined,
  productManufacturer: undefined,
  productSpecTemplate: undefined,
  productSpuStatus: 0
})
const formRules = reactive({
})
const formRef = ref() // 表单 Ref

/** 子表的表单 */
const subTabsName = ref('skuTable')
const skuTableFormRef = ref()
const categoryTreeData = ref<any[]>([]) // 类目树形数据

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await fetchCategoryTree()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      const data = await SpuTableApi.getSpuTable(id)
      formData.value = data || {}
    } catch (error) {
      console.error('获取数据失败:', error)
      message.error('获取数据失败')
      dialogVisible.value = false
      return
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  await formRef.value.validate()
  // 校验子表单
  try {
    await skuTableFormRef.value.validate()
  } catch (e) {
    subTabsName.value = 'skuTable'
    return
  }
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as SpuTable
    // 拼接子表的数据
    data.skuTables = skuTableFormRef.value.getData()
    // 确保使用 productSpuId 作为主键
    data.productSpuId = formData.value.productSpuId
    if (formType.value === 'create') {
      await SpuTableApi.createSpuTable(data)
      message.success(t('common.createSuccess'))
    } else {
      await SpuTableApi.updateSpuTable(data)
      message.success(t('common.updateSuccess'))
    }
    emit('success')
  } finally {
    formLoading.value = false
  }
  // 关闭弹窗（放在最后确保各种情况都能执行到）
  dialogVisible.value = false
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    productSpuId: undefined,
    productSpuCode: undefined,
    productSpuName: undefined,
    productBrand: undefined,
    categoryId: undefined,
    productOrigin: undefined,
    productManufacturer: undefined,
    productSpecTemplate: undefined,
    productSpuStatus: 0
  }
  formRef.value?.resetFields()
}

/** 获取类目树形数据 */
const fetchCategoryTree = async () => {
  const data = await CategoryTableApi.getCategoryTableList()
  const list = data.list || data || []
  const root = { categoryId: 0, categoryName: '顶级（无父类目）', children: [] }
  root.children = handleTree(list, 'categoryId', 'parentId')
  categoryTreeData.value = [root]
}
</script>