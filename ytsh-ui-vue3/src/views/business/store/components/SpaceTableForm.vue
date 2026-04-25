<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="100px"
    v-loading="formLoading"
  >
     <el-form-item label="房屋面积" prop="buildingArea">
      <el-input v-model="formData.buildingArea" placeholder="请输入房屋面积">
        <template #suffix>㎡</template>
      </el-input>
    </el-form-item>
    <el-form-item label="冷库面积" prop="coldStorageArea">
      <el-input v-model="formData.coldStorageArea" placeholder="请输入冷库面积">
        <template #suffix>㎡</template>
      </el-input>
    </el-form-item>
  </el-form>
</template>
<script setup lang="ts">
import { TableApi } from '@/api/business/store'

const props = defineProps<{
  storeId: number | string // 门店ID（主表的关联字段，支持String和Number类型
}>()
const formLoading = ref(false) // 表单的加载中
const formData = ref<any>({})
const formRules = reactive({
})
const formRef = ref() // 表单 Ref

/** 监听主表的关联字段的变化，加载对应的子表数据 */
watch(
  () => props.storeId,
  async (val) => {
    // 1. 重置表单
    formData.value = {
      storeSpaceId: undefined,
      storeId: undefined,
      buildingArea: undefined,
      coldStorageArea: undefined,
    }
    // 2. val 非空，则加载数据
    if (!val) {
      return;
    }
    try {
      formLoading.value = true
      const data = await TableApi.getSpaceTableByStoreId(val)
      if (!data) {
        return
      }
      formData.value = data
    } finally {
      formLoading.value = false
    }
  },
  { immediate: true }
)

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