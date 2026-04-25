<template>
  <Dialog v-model="dialogVisible" title="类目导入" width="400">
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :action="importUrl + '?updateSupport=' + updateSupport"
      :auto-upload="false"
      :disabled="formLoading"
      :headers="uploadHeaders"
      :limit="1"
      :on-error="submitFormError"
      :on-exceed="handleExceed"
      :on-success="submitFormSuccess"
      accept=".xlsx, .xls"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <div class="el-upload__tip">
            <el-checkbox v-model="updateSupport" />
            是否更新已经存在的类目数据
          </div>
          <span>仅允许导入 xls、xlsx 格式文件。</span>
          <el-link
            :underline="false"
            style="font-size: 12px; vertical-align: baseline"
            type="primary"
            @click="importTemplate"
          >
            下载模板
          </el-link>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { CategoryTableApi } from '@/api/business/category'
import { getAccessToken, getTenantId } from '@/utils/auth'
import download from '@/utils/download'

defineOptions({ name: 'CategoryTableImportForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const uploadRef = ref()
const importUrl =
  import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL + '/business/category-table/import'
const uploadHeaders = ref()
const fileList = ref([])
const updateSupport = ref(0)

const open = () => {
  dialogVisible.value = true
  updateSupport.value = 0
  fileList.value = []
  resetForm()
}
defineExpose({ open })

const submitForm = async () => {
  if (fileList.value.length == 0) {
    message.error('请上传文件')
    return
  }
  uploadHeaders.value = {
    Authorization: 'Bearer ' + getAccessToken(),
    'tenant-id': getTenantId()
  }
  formLoading.value = true
  uploadRef.value!.submit()
}

const emits = defineEmits(['success'])
const submitFormSuccess = (response: any) => {
  if (response.code !== 0) {
    message.error(response.msg)
    resetForm()
    return
  }
  const data = response.data
  let text = '上传成功数量：' + data.createCategoryNames.length + ';'
  for (let categoryName of data.createCategoryNames) {
    text += '< ' + categoryName + ' >'
  }
  text += '更新成功数量：' + data.updateCategoryNames.length + ';'
  for (const categoryName of data.updateCategoryNames) {
    text += '< ' + categoryName + ' >'
  }
  text += '更新失败数量：' + Object.keys(data.failureCategoryNames).length + ';'
  for (const categoryName in data.failureCategoryNames) {
    text += '< ' + categoryName + ': ' + data.failureCategoryNames[categoryName] + ' >'
  }
  message.alert(text)
  formLoading.value = false
  dialogVisible.value = false
  emits('success')
}

const submitFormError = (): void => {
  message.error('上传失败，请您重新上传！')
  formLoading.value = false
}

const resetForm = async (): Promise<void> => {
  formLoading.value = false
  await nextTick()
  uploadRef.value?.clearFiles()
}

const handleExceed = (): void => {
  message.error('最多只能上传一个文件！')
}

const importTemplate = async () => {
  const res = await CategoryTableApi.importCategoryTableTemplate()
  download.excel(res, '商品类目导入模板.xls')
}
</script>
