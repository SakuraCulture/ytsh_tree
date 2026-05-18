<template>
  <el-dialog v-model="dialogVisible" title="门店库存导入" width="620px" destroy-on-close>
    <div class="import-tip">模板要求：ERP门店编码、SKU编码和完整库存字段必填。</div>

    <el-upload
      :auto-upload="false"
      :limit="1"
      :file-list="fileList"
      accept=".xls,.xlsx"
      @change="handleFileChange"
      @remove="handleRemove"
    >
      <template #trigger>
        <el-button type="primary">选择文件</el-button>
      </template>
    </el-upload>

    <el-alert v-if="lastResult" type="success" :closable="false" class="result-alert">
      <template #title>
        <span>
          导入完成：正式 {{ lastResult.formalSuccessCount || 0 }}，影子 {{ lastResult.shadowSuccessCount || 0 }}，治理
          {{ lastResult.governanceCount || 0 }}，失败 {{ lastResult.failureCount || 0 }}
        </span>
      </template>
    </el-alert>

    <el-table
      v-if="(lastResult?.failureList || []).length"
      :data="lastResult?.failureList || []"
      border
      stripe
      size="small"
      style="width: 100%"
      :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
    >
      <el-table-column prop="rowNo" label="行号" width="80" align="center" />
      <el-table-column prop="skuCode" label="SKU编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="message" label="失败原因" min-width="220" show-overflow-tooltip />
    </el-table>

    <template #footer>
      <el-button @click="handleDownloadTemplate">下载模板</el-button>
      <el-button type="primary" :loading="uploading" @click="handleSubmit">开始导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadUserFile } from 'element-plus'

import {
  downloadInventoryImportTemplate,
  importStoreInventory,
  type EleStoreInventoryImportRespVO
} from '@/api/ele/storeInventory'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const uploading = ref(false)
const selectedFile = ref<File>()
const fileList = ref<UploadUserFile[]>([])
const lastResult = ref<EleStoreInventoryImportRespVO>()

watch(dialogVisible, (value) => {
  if (!value) {
    selectedFile.value = undefined
    fileList.value = []
    lastResult.value = undefined
  }
})

const handleFileChange = (uploadFile: UploadFile) => {
  selectedFile.value = uploadFile.raw
  fileList.value = uploadFile.raw
    ? [
        {
          name: uploadFile.name,
          url: uploadFile.url,
          status: uploadFile.status,
          uid: uploadFile.uid
        }
      ]
    : []
}

const handleRemove = () => {
  selectedFile.value = undefined
  fileList.value = []
}

const handleDownloadTemplate = async () => {
  try {
    await downloadInventoryImportTemplate()
  } catch (error: any) {
    ElMessage.error(error?.message || '下载模板失败')
  }
}

const handleSubmit = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择导入文件')
    return
  }
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  uploading.value = true
  try {
    const result = await importStoreInventory(formData)
    lastResult.value = result
    ElMessage.success(
      `导入完成：正式 ${result.formalSuccessCount || 0}，影子 ${result.shadowSuccessCount || 0}，治理 ${result.governanceCount || 0}，失败 ${result.failureCount || 0}`
    )
    emit('success')
  } catch (error: any) {
    ElMessage.error(error?.message || '导入库存失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped lang="scss">
.import-tip {
  margin-bottom: 16px;
  color: #64748b;
  font-size: 13px;
}

.result-alert {
  margin: 16px 0;
}
</style>
