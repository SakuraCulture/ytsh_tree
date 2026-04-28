<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="100px"
    v-loading="formLoading"
  >
     <el-form-item label="经营方式" prop="businessMode">
      <el-select v-model="formData.businessMode" placeholder="请选择经营方式" class="!w-100%">
        <el-option label="直营" value="DIRECT" />
        <el-option label="代理" value="AGENCY" />
        <el-option label="自营" value="SELF" />
        <el-option label="合资" value="JOINT" />
      </el-select>
    </el-form-item>
    <el-form-item label="门店类型" prop="storeType">
      <el-select v-model="formData.storeType" placeholder="请选择门店类型" class="!w-100%">
        <el-option label="线上" value="ONLINE" />
        <el-option label="线上线下" value="O2O" />
      </el-select>
    </el-form-item>
  </el-form>
</template>
<script setup lang="ts">
import { TableApi } from '@/api/business/store'
import { emptyToNull } from './formUtils'

const props = defineProps<{
  storeId: number | string // 门店ID（主表的关联字段，支持String和Number类型）
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
      affiliationId: undefined,
      storeId: undefined,
      businessMode: undefined,
      storeType: undefined,
    }
    // 2. val 非空，则加载数据
    if (!val) {
      return;
    }
    try {
      formLoading.value = true
      const data = await TableApi.getAffiliationTableByStoreId(val)
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
  return emptyToNull(formData.value, ['businessMode', 'storeType'])
}

defineExpose({ validate, getData })
</script>