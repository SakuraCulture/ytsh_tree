<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      label-width="80px"
      class="-mb-15px"
    >
      <el-form-item label="对象域" prop="domainType">
        <el-select
          v-model="queryParams.domainType"
          placeholder="请选择对象域"
          clearable
          class="!w-240px"
          @change="handleDomainTypeFilterChange"
        >
          <el-option label="商品" value="PRODUCT" />
          <el-option label="门店" value="STORE" />
          <el-option label="会员" value="MEMBER" />
        </el-select>
      </el-form-item>
      <el-form-item label="层级" prop="level">
        <el-select v-model="queryParams.level" placeholder="请选择层级" clearable class="!w-240px">
          <el-option label="一级" :value="1" />
          <el-option label="二级" :value="2" />
          <el-option label="三级" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="父级维度" prop="parentId">
        <el-tree-select
          v-model="queryParams.parentId"
          :data="dimensionTreeOptions"
          :props="{ label: 'name', value: 'id', children: 'children' }"
          check-strictly
          default-expand-all
          clearable
          filterable
          class="!w-240px"
          placeholder="请选择父级维度"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" /> 搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['business:tag-dimension:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      row-key="id"
      :default-expand-all="true"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column label="维度名称" prop="name" min-width="220" show-overflow-tooltip />
      <el-table-column label="维度编码" prop="code" min-width="180" show-overflow-tooltip />
      <el-table-column label="对象域" align="center" min-width="100">
        <template #default="scope">
          {{ formatDomainType(scope.row.domainType) }}
        </template>
      </el-table-column>
      <el-table-column label="层级" align="center" width="100">
        <template #default="scope">
          <el-tag :type="levelTagTypeMap[scope.row.level] || 'info'">
            {{ formatLevel(scope.row.level) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="90" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" prop="description" min-width="220" show-overflow-tooltip />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['business:tag-dimension:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['business:tag-dimension:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <TagDimensionForm ref="formRef" @success="handleFormSuccess" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { handleTree } from '@/utils/tree'
import {
  TagDimensionApi,
  type TagDimension,
  type TagDimensionListReqVO
} from '@/api/business/tag/dimension'
import { filterTreeOptionsByDomain } from './dimensionFormLogic'
import TagDimensionForm from './TagDimensionForm.vue'

defineOptions({ name: 'BusinessTagDimension' })

const { t } = useI18n()
const message = useMessage()

const DOMAIN_TYPE_LABEL_MAP: Record<string, string> = {
  PRODUCT: '商品',
  STORE: '门店',
  MEMBER: '会员'
}

const LEVEL_LABEL_MAP: Record<number, string> = {
  1: '一级',
  2: '二级',
  3: '三级'
}

const levelTagTypeMap: Record<number, 'success' | 'warning' | 'info'> = {
  1: 'success',
  2: 'warning',
  3: 'info'
}

const loading = ref(false)
const list = ref<TagDimension[]>([])
const allDimensions = ref<TagDimension[]>([])
const dimensionTreeOptions = ref<Array<{ id: number; name: string; children?: TagDimension[] }>>([])
const queryFormRef = ref()
const queryParams = reactive<TagDimensionListReqVO>({
  domainType: undefined,
  level: undefined,
  parentId: undefined
})

const buildTreeOptions = (dimensions: TagDimension[], domainType?: string) => {
  if (domainType) {
    dimensionTreeOptions.value = filterTreeOptionsByDomain(dimensions, domainType)
    return
  }
  const root = {
    id: 0,
    name: '顶级（无父级）',
    children: handleTree(dimensions.map((item) => ({ ...item })), 'id', 'parentId')
  }
  dimensionTreeOptions.value = [root]
}

const getList = async () => {
  loading.value = true
  try {
    const data = await TagDimensionApi.getTagDimensionList(queryParams)
    const result = Array.isArray(data) ? data : data?.list || []
    list.value = handleTree(result, 'id', 'parentId')
  } finally {
    loading.value = false
  }
}

const getAllDimensions = async () => {
  const data = await TagDimensionApi.getTagDimensionList()
  const result = Array.isArray(data) ? data : data?.list || []
  allDimensions.value = result
  buildTreeOptions(result, queryParams.domainType)
}

const handleDomainTypeFilterChange = () => {
  queryParams.parentId = undefined
  buildTreeOptions(allDimensions.value, queryParams.domainType)
}

const handleQuery = async () => {
  await getList()
}

const resetQuery = async () => {
  queryFormRef.value?.resetFields()
  buildTreeOptions(allDimensions.value)
  await getList()
}

const formRef = ref()
const openForm = (type: 'create' | 'update', id?: number) => {
  formRef.value.open(type, id, allDimensions.value)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await TagDimensionApi.deleteTagDimension(id)
    message.success(t('common.delSuccess'))
    await Promise.all([getList(), getAllDimensions()])
  } catch {}
}

const handleFormSuccess = async () => {
  await Promise.all([getList(), getAllDimensions()])
}

const formatDomainType = (domainType?: string) => {
  if (!domainType) {
    return '-'
  }
  return DOMAIN_TYPE_LABEL_MAP[domainType] || domainType
}

const formatLevel = (level?: number) => {
  if (!level) {
    return '-'
  }
  return LEVEL_LABEL_MAP[level] || String(level)
}

onMounted(async () => {
  await Promise.all([getList(), getAllDimensions()])
})
</script>
