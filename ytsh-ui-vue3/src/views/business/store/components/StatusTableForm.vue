<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="100px"
    v-loading="formLoading"
  >
     <el-form-item label="当前状态" prop="currentStatus">
      <el-radio-group v-model="formData.currentStatus">
        <el-radio :value="'NORMAL'">正常</el-radio>
        <el-radio :value="'CLOSED'">关闭</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="开业日期" prop="openDate">
      <el-date-picker
        v-model="formData.openDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="选择开业日期"
        class="!w-100%"
      />
    </el-form-item>
    <el-form-item label="签约日期" prop="signDate">
      <el-date-picker
        v-model="formData.signDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="选择签约日期"
        class="!w-100%"
      />
    </el-form-item>
  </el-form>
</template>
<script setup lang="ts">
import { TableApi } from '@/api/business/store'
import { emptyToNull, normalizeDate } from './formUtils'

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
      storeBusinessStatusId: undefined,
      storeId: undefined,
      currentStatus: undefined,
      openDate: undefined,
      signDate: undefined,
    }
    
    // 2. val 非空，则加载数据
    if (!val) {
      return
    }
    
    try {
      formLoading.value = true
      const data = await TableApi.getStatusTableByStoreId(val)
      if (!data) {
        return
      }
      data.openDate = normalizeDate(data.openDate)
      data.signDate = normalizeDate(data.signDate)
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
  return emptyToNull(formData.value, ['currentStatus', 'openDate', 'signDate'])
}

defineExpose({ validate, getData })
</script>