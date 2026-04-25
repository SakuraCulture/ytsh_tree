<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    v-loading="formLoading"
    label-width="0px"
    :inline-message="true"
  >
    <el-table :data="formData" class="-mt-10px">
      <el-table-column label="序号" type="index" width="100" />
      <el-table-column label="SKU编码" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productSkuCode`" :rules="formRules.productSkuCode" class="mb-0px!">
            <el-input v-model="row.productSkuCode" placeholder="请输入SKU编码" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="SKU名称" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productSkuName`" :rules="formRules.productSkuName" class="mb-0px!">
            <el-input v-model="row.productSkuName" placeholder="请输入SKU名称" />
          </el-form-item>
        </template>
      </el-table-column>
       <el-table-column label="主EAN码(13位)" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productSkuEan`" :rules="formRules.productSkuEan" class="mb-0px!">
            <el-input v-model="row.productSkuEan" placeholder="请输入主EAN码(13位)" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="重量" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productWeight`" :rules="formRules.productWeight" class="mb-0px!">
            <el-input v-model="row.productWeight" placeholder="请输入重量" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="重量单位" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productWeightUnit`" :rules="formRules.productWeightUnit" class="mb-0px!">
            <el-input v-model="row.productWeightUnit" placeholder="请输入重量单位" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="长度(cm)" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productLength`" :rules="formRules.productLength" class="mb-0px!">
            <el-input v-model="row.productLength" placeholder="请输入长度(cm)" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="宽度(cm)" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productWidth`" :rules="formRules.productWidth" class="mb-0px!">
            <el-input v-model="row.productWidth" placeholder="请输入宽度(cm)" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="高度(cm)" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productHeight`" :rules="formRules.productHeight" class="mb-0px!">
            <el-input v-model="row.productHeight" placeholder="请输入高度(cm)" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="基准成本价" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productCostPrice`" :rules="formRules.productCostPrice" class="mb-0px!">
            <el-input v-model="row.productCostPrice" placeholder="请输入基准成本价" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="基准零售价" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productRetailPrice`" :rules="formRules.productRetailPrice" class="mb-0px!">
            <el-input v-model="row.productRetailPrice" placeholder="请输入基准零售价" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="SKU主图URL" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productImageUrl`" :rules="formRules.productImageUrl" class="mb-0px!">
            <el-input v-model="row.productImageUrl" placeholder="请输入SKU主图URL" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.productSkuStatus`" :rules="formRules.productSkuStatus" class="mb-0px!">
            <el-radio-group v-model="row.productSkuStatus" class="status-radio-group">
              <el-radio :value="1">上架</el-radio>
              <el-radio :value="0">下架</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" label="操作" width="60">
        <template #default="{ $index }">
          <el-button @click="handleDelete($index)" link>—</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-form>
  <el-row justify="center" class="mt-3">
    <el-button @click="handleAdd" round>+ 添加SKU商品主数据</el-button>
  </el-row>
</template>
<script setup lang="ts">
import { SpuTableApi } from '@/api/business/product'

const props = defineProps<{
  productSpuId?: number
}>()
const formLoading = ref(false) // 表单的加载中
const formData = ref<any[]>([])
const formRules = reactive({
})
const formRef = ref() // 表单 Ref

/** 监听主表的关联字段的变化，加载对应的子表数据 */
watch(
  () => props.productSpuId,
  async (val) => {
    // 1. 重置表单
    formData.value = []
    // 2. val 非空，则加载数据
    if (!val) {
      return;
    }
    try {
      formLoading.value = true
      formData.value = await SpuTableApi.getSkuTableListByProductSpuId(val)
    } finally {
      formLoading.value = false
    }
  },
  { immediate: true }
)

/** 新增按钮操作 */
const handleAdd = () => {
  const row = {
    productSkuId: undefined,
    productSkuCode: undefined,
    productSkuName: undefined,
    productSpuId: undefined,
    productSkuEan: undefined,
    productWeight: undefined,
    productWeightUnit: undefined,
    productLength: undefined,
    productWidth: undefined,
    productHeight: undefined,
    productCostPrice: undefined,
    productRetailPrice: undefined,
    productImageUrl: undefined,
    productSkuStatus: 1
  }
  row.productSpuId = props.productSpuId as any
  formData.value.push(row)
}

/** 删除按钮操作 */
const handleDelete = (index) => {
  formData.value.splice(index, 1)
}

/** 表单校验 */
const validate = () => {
  return formRef.value.validate()
}

/** 表单值 */
const getData = () => {
  return formData.value
}

defineExpose({ validate, getData })
</script>

<style lang="scss" scoped>
.status-radio-group {
  display: flex;
  flex-direction: row;
  align-items: center;
  :deep(.el-radio) {
    margin-right: 12px;
  }
}
</style>