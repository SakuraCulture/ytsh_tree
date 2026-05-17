<template>
  <Dialog title="批量挂标" v-model="dialogVisible" width="820px">
    <el-form label-width="100px" v-loading="loading">
      <el-form-item label="标签搜索">
        <el-input v-model="keyword" placeholder="请输入标签名称或编码" clearable />
      </el-form-item>
      <el-form-item label="标签选择">
        <el-checkbox-group v-model="selectedTagIds" class="tag-grid">
          <el-checkbox v-for="item in filteredTagList" :key="item.tagValueId" :label="item.tagValueId">
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
import { StoreProductApi } from '@/api/business/store-product'
import { TagValueApi, type TagSelectableValue } from '@/api/business/tag/value'
import { collectSelectedTagIds, formatBatchTagResult } from '../product/productTagLogic'

defineOptions({ name: 'StoreProductBatchTagForm' })

const message = useMessage()

const dialogVisible = ref(false)
const loading = ref(false)
const keyword = ref('')
const storeProductIds = ref<string[]>([])
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

const open = async (ids: string[]) => {
  storeProductIds.value = ids
  keyword.value = ''
  selectedTagIds.value = []
  dialogVisible.value = true
  loading.value = true
  try {
    selectableTagList.value = await TagValueApi.getTagValueListForObject('STORE_PRODUCT')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })

const submitForm = async () => {
  if (!storeProductIds.value.length) {
    return
  }
  loading.value = true
  try {
    const resp = await StoreProductApi.saveStoreProductManualTagsBatch({
      storeProductIds: storeProductIds.value,
      tagValueIds: collectSelectedTagIds(selectedTagIds.value)
    })
    message.success(formatBatchTagResult(resp))
    dialogVisible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
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
