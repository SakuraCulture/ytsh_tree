<template>
  <Dialog v-model="dialogVisible" title="标签值导入" width="420px">
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :auto-upload="false"
      :disabled="formLoading"
      :limit="1"
      :on-exceed="handleExceed"
      accept=".xlsx, .xls"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <div class="el-upload__tip mb-8px">
            <el-checkbox v-model="updateSupport" /> 是否更新已经存在的标签值数据
          </div>
          <div v-if="domainType" class="mb-8px">当前页面筛选对象域：{{ domainType }}（仅展示，不影响导入内容）</div>
          <span>仅允许导入 xls、xlsx 格式文件。</span>
          <el-link
            :underline="false"
            style="font-size: 12px; vertical-align: baseline"
            type="primary"
            @click="downloadTemplate"
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

<script setup lang="ts">
import { TagValueApi } from '@/api/business/tag/value'
import download from '@/utils/download'

defineOptions({ name: 'BusinessTagValueImportForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const uploadRef = ref()
const fileList = ref<any[]>([])
const updateSupport = ref(false)
const domainType = ref<string | undefined>()

const emit = defineEmits(['success'])

const open = (currentDomainType?: string) => {
  dialogVisible.value = true
  updateSupport.value = false
  domainType.value = currentDomainType
  fileList.value = []
  resetForm()
}

defineExpose({ open })

const submitForm = async () => {
  const file = fileList.value[0]?.raw
  if (!file) {
    message.error('请上传文件')
    return
  }
  formLoading.value = true
  try {
    const data = await TagValueApi.importTagValue(file, updateSupport.value)
    const createNames = data.createNames || []
    const updateNames = data.updateNames || []
    const failureNames = data.failureNames || {}
    let text = `创建成功 ${createNames.length} 条；更新成功 ${updateNames.length} 条；失败 ${Object.keys(failureNames).length} 条`
    if (createNames.length) {
      text += `\n创建：${createNames.join('、')}`
    }
    if (updateNames.length) {
      text += `\n更新：${updateNames.join('、')}`
    }
    if (Object.keys(failureNames).length) {
      text += `\n失败：${Object.entries(failureNames)
        .map(([name, reason]) => `${name}: ${reason}`)
        .join('；')}`
    }
    message.alert(text)
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = async () => {
  formLoading.value = false
  await nextTick()
  uploadRef.value?.clearFiles()
}

const handleExceed = () => {
  message.error('最多只能上传一个文件！')
}

const downloadTemplate = async () => {
  const data = await TagValueApi.getImportTemplate()
  download.excel(data, '标签值导入模板.xls')
}
</script>
