<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="820px">
    <el-form label-width="100px" v-loading="loading">
      <el-form-item label="标签搜索">
        <el-input v-model="keyword" placeholder="请输入标签名称或编码" clearable />
      </el-form-item>
      <el-form-item label="标签选择">
        <el-checkbox-group v-model="selectedTagIds" class="tag-grid">
          <el-checkbox
            v-for="item in filteredTagList"
            :key="item.tagValueId"
            :label="item.tagValueId"
          >
            {{ item.tagValueName }}（{{ item.tagValueCode }}）
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="loading" @click="submitForm">保 存</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { SpuTableApi, type ProductSpuTagRespVO } from '@/api/business/product'
import { TagValueApi, type TagSelectableValue } from '@/api/business/tag/value'
import { collectSelectedTagIds } from './productTagLogic'

defineOptions({ name: 'ProductSpuTagForm' })

const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('管理标签')
const loading = ref(false)
const keyword = ref('')
const productSpuId = ref<number>()
const selectableTagList = ref<TagSelectableValue[]>([])
const selectedTagIds = ref<number[]>([])

const emit = defineEmits(['success'])

const filteredTagList = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) {
    return selectableTagList.value
  }
  return selectableTagList.value.filter((item) => {
    return item.tagValueName.toLowerCase().includes(value) || item.tagValueCode.toLowerCase().includes(value)
  })
})

const open = async (spuId: number) => {
  dialogVisible.value = true
  dialogTitle.value = '管理标签'
  productSpuId.value = spuId
  keyword.value = ''
  selectedTagIds.value = []
  loading.value = true
  try {
    const [tagPool, currentTags] = await Promise.all([
      TagValueApi.getTagValueListForObject('SPU'),
      SpuTableApi.getProductSpuTagList(spuId)
    ])
    selectableTagList.value = tagPool
    selectedTagIds.value = extractManualTagIds(currentTags)
  } finally {
    loading.value = false
  }
}

defineExpose({ open })

const submitForm = async () => {
  if (!productSpuId.value) {
    return
  }
  loading.value = true
  try {
    await SpuTableApi.saveProductSpuManualTags({
      productSpuId: productSpuId.value,
      tagValueIds: collectSelectedTagIds(selectedTagIds.value)
    })
    message.success('保存成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}

const extractManualTagIds = (tags: ProductSpuTagRespVO[]) => {
  return tags
    .filter((item) => item.sources.includes('MANUAL'))
    .map((item) => item.tagValueId)
}
</script>

<style scoped>
.tag-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.tag-grid :deep(.el-checkbox) {
  margin-right: 0;
}
</style>
