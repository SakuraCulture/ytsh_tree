<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="100px"
    v-loading="formLoading"
  >
     <el-form-item label="加盟商名称" prop="franchiseeName">
      <el-input v-model="formData.franchiseeName" placeholder="请输入加盟商名称" />
    </el-form-item>
    <el-form-item label="加盟联系方式" prop="franchiseePhone">
      <el-input v-model="formData.franchiseePhone" placeholder="请输入加盟联系方式" />
    </el-form-item>
    <el-form-item label="加盟费" prop="franchiseeFee">
      <el-input v-model="formData.franchiseeFee" placeholder="请输入加盟费" />
    </el-form-item>
    <el-form-item label="保证金" prop="securityDeposit">
      <el-input v-model="formData.securityDeposit" placeholder="请输入保证金" />
    </el-form-item>
    <el-form-item label="合同开始日期" prop="contractStart">
      <el-date-picker
        v-model="formData.contractStart"
        type="date"
        placeholder="请选择合同开始日期"
        value-format="YYYY-MM-DD"
        class="!w-100%"
      />
    </el-form-item>
    <el-form-item label="合同结束日期" prop="contractEnd">
      <el-date-picker
        v-model="formData.contractEnd"
        type="date"
        placeholder="请选择合同结束日期"
        value-format="YYYY-MM-DD"
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
      franchiseeId: undefined,
      storeId: undefined,
      franchiseeName: undefined,
      franchiseePhone: undefined,
      franchiseeFee: undefined,
      securityDeposit: undefined,
      contractStart: undefined,
      contractEnd: undefined,
    }
    // 2. val 非空，则加载数据
    if (!val) {
      return
    }
    try {
      formLoading.value = true
      const data = await TableApi.getFranchiseeTableByStoreId(val)
      if (!data) {
        return
      }
      data.contractStart = normalizeDate(data.contractStart)
      data.contractEnd = normalizeDate(data.contractEnd)
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
  return emptyToNull(formData.value, ['franchiseeName', 'franchiseePhone', 'franchiseeFee', 'securityDeposit', 'contractStart', 'contractEnd'])
}

defineExpose({ validate, getData })
</script>