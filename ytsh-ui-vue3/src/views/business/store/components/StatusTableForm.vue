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
      
      // 临时调试：查看后端返回的原始数据
      // console.log('[StatusTableForm] 后端返回原始数据:', {
      //   data: data,
      //   openDate原始: data.openDate,
      //   openDate类型: typeof data.openDate,
      //   signDate原始: data.signDate,
      //   signDate类型: typeof data.signDate,
      // })
      
      // 3. 处理日期格式，确保兼容 el-date-picker
      // 支持多种后端返回格式：ISO 日期、时间戳等
      if (data.openDate) {
        const normalized = normalizeDate(data.openDate)
        // console.log('[StatusTableForm] openDate 标准化:', {
        //   原始: data.openDate,
        //   标准化后: normalized
        // })
        data.openDate = normalized
      }
      if (data.signDate) {
        const normalized = normalizeDate(data.signDate)
        // console.log('[StatusTableForm] signDate 标准化:', {
        //   原始: data.signDate,
        //   标准化后: normalized
        // })
        data.signDate = normalized
      }
      
      formData.value = data
      
      // 调试：查看赋值后的 formData
      // console.log('[StatusTableForm] formData 赋值后:', {
      //   formData: formData.value,
      //   openDate: formData.value.openDate,
      //   signDate: formData.value.signDate,
      // })
    } finally {
      formLoading.value = false
    }
  },
  { immediate: true }
)

/**
 * 标准化日期格式
 * 确保日期字符串符合 YYYY-MM-DD 格式
 * 处理时区和各种输入格式
 */
const normalizeDate = (date: any): string | undefined => {
  if (!date) return undefined
  
  // 如果已经是 YYYY-MM-DD 格式，直接返回
  if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
    return date
  }
  
  // 如果是 ISO 格式（带时间），提取日期部分
  if (typeof date === 'string' && date.includes('T')) {
    return date.split('T')[0]
  }
  
  // 如果是时间戳，转换为日期（考虑本地时区）
  if (typeof date === 'number') {
    // 假设是毫秒时间戳
    const d = new Date(date)
    if (!isNaN(d.getTime())) {
      // 使用本地时区的日期部分
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
  }
  
  // 如果是 Date 对象
  if (date instanceof Date) {
    if (!isNaN(date.getTime())) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
  }
  
  // 尝试作为字符串解析（使用本地时区）
  if (typeof date === 'string') {
    // 尝试多种日期格式
    const formats = [
      /^(\d{4})-(\d{1,2})-(\d{1,2})$/,           // YYYY-M-D 或 YYYY-MM-DD
      /^(\d{4})\/(\d{1,2})\/(\d{1,2})$/,          // YYYY/M/D 或 YYYY/MM/DD
      /^(\d{1,2})-(\d{1,2})-(\d{4})$/,            // M-D-YYYY 或 MM-DD-YYYY
      /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/,         // M/D/YYYY 或 MM/DD/YYYY
    ]
    
    for (const format of formats) {
      const match = date.match(format)
      if (match) {
        let year, month, day
        if (format === formats[0] || format === formats[1]) {
          // YYYY-MM-DD 或 YYYY/MM/DD
          year = parseInt(match[1])
          month = parseInt(match[2])
          day = parseInt(match[3])
        } else {
          // MM-DD-YYYY 或 MM/DD/YYYY
          month = parseInt(match[1])
          day = parseInt(match[2])
          year = parseInt(match[3])
        }
        
        // 验证日期有效性
        if (month >= 1 && month <= 12 && day >= 1 && day <= 31 && year >= 1900 && year <= 2100) {
          return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
        }
      }
    }
    
    // 如果格式不匹配，尝试使用 Date 解析
    const d = new Date(date)
    if (!isNaN(d.getTime())) {
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
  }
  
  // 无法解析，返回 undefined
  console.warn('[StatusTableForm] 无法解析日期:', date, '类型:', typeof date)
  return undefined
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