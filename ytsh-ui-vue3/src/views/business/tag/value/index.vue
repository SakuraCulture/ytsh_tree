<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      label-width="88px"
      class="-mb-15px"
    >
      <el-form-item label="对象域" prop="domainType">
        <el-select
          v-model="queryParams.domainType"
          placeholder="请选择对象域"
          clearable
          class="!w-240px"
          @change="handleDomainTypeChange"
        >
          <el-option label="商品" value="PRODUCT" />
          <el-option label="门店" value="STORE" />
          <el-option label="会员" value="MEMBER" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签维度" prop="dimensionId">
        <el-tree-select
          v-model="queryParams.dimensionId"
          :data="dimensionTreeOptions"
          :props="{ label: 'name', value: 'id', children: 'children' }"
          check-strictly
          default-expand-all
          clearable
          filterable
          class="!w-240px"
          placeholder="请选择三级标签维度"
        />
      </el-form-item>
      <el-form-item label="标签值名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入标签值名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="标签值编码" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入标签值编码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="打标方式" prop="tagMethod">
        <el-select v-model="queryParams.tagMethod" placeholder="请选择打标方式" clearable class="!w-240px">
          <el-option v-for="item in tagMethodOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-240px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
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
          v-hasPermi="['business:tag-value:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="primary"
          plain
          @click="openImportForm"
          v-hasPermi="['business:tag-value:import']"
        >
          <Icon icon="ep:upload" class="mr-5px" /> 导入
        </el-button>
        <el-button type="warning" plain @click="downloadTemplate">
          <Icon icon="ep:download" class="mr-5px" /> 模板
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="ID" align="center" prop="id" width="90" />
      <el-table-column label="标签值名称" prop="name" min-width="180" />
      <el-table-column label="标签值编码" prop="code" min-width="180" />
      <el-table-column label="标签维度" align="center" min-width="160">
        <template #default="scope">
          {{ getDimensionName(scope.row.dimensionId) }}
        </template>
      </el-table-column>
      <el-table-column label="打标方式" align="center" width="120">
        <template #default="scope">
          {{ formatTagMethod(scope.row.tagMethod) }}
        </template>
      </el-table-column>
      <el-table-column label="数据来源" prop="dataSource" min-width="150" />
      <el-table-column label="更新频率" prop="updateFrequency" align="center" width="120" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="90" />
      <el-table-column label="逻辑说明" prop="logicDescription" min-width="220" />
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
            v-hasPermi="['business:tag-value:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['business:tag-value:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <TagValueForm ref="formRef" :dimensions="allDimensions" @success="getList" />
  <TagValueImportForm ref="importFormRef" @success="handleImportSuccess" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { TagDimensionApi, type TagDimension } from '@/api/business/tag/dimension'
import { TagValueApi, type TagValue, type TagValuePageReqVO } from '@/api/business/tag/value'
import {
  buildDimensionTreeOptions,
  getDimensionNameMap,
  type DimensionOption
} from './valuePageLogic'
import TagValueForm from './TagValueForm.vue'
import TagValueImportForm from './TagValueImportForm.vue'

defineOptions({ name: 'BusinessTagValue' })

const { t } = useI18n()
const message = useMessage()

const tagMethodOptions = [
  { label: '人工', value: 'MANUAL' },
  { label: '规则', value: 'RULE' },
  { label: '算法', value: 'ALGORITHM' },
  { label: '继承', value: 'INHERIT' }
]

const loading = ref(false)
const list = ref<TagValue[]>([])
const total = ref(0)
const allDimensions = ref<TagDimension[]>([])
const dimensionTreeOptions = ref<DimensionOption[]>([])
const dimensionNameMap = ref(new Map<number, string>())
const queryFormRef = ref()
const queryParams = reactive<TagValuePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  domainType: undefined,
  dimensionId: undefined,
  name: undefined,
  code: undefined,
  tagMethod: undefined,
  status: undefined
})

const loadDimensions = async () => {
  const data = await TagDimensionApi.getTagDimensionList()
  const result = Array.isArray(data) ? data : data?.list || []
  allDimensions.value = result
  dimensionTreeOptions.value = buildDimensionTreeOptions(result, queryParams.domainType)
  dimensionNameMap.value = getDimensionNameMap(result)
}

const getList = async () => {
  loading.value = true
  try {
    const data = await TagValueApi.getTagValuePage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleDomainTypeChange = async () => {
  queryParams.dimensionId = undefined
  await loadDimensions()
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = async () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  await loadDimensions()
  await getList()
}

const formRef = ref()
const openForm = (type: 'create' | 'update', id?: number) => {
  formRef.value.open(type, id, queryParams.domainType)
}

const importFormRef = ref()
const openImportForm = () => {
  importFormRef.value.open(queryParams.domainType)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await TagValueApi.deleteTagValue(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleImportSuccess = async () => {
  await Promise.all([getList(), loadDimensions()])
}

const downloadTemplate = async () => {
  const data = await TagValueApi.getImportTemplate()
  download.excel(data, '标签值导入模板.xls')
}

const formatTagMethod = (tagMethod?: string) => {
  return tagMethodOptions.find((item) => item.value === tagMethod)?.label || tagMethod || '-'
}

const getDimensionName = (dimensionId?: number) => {
  if (!dimensionId) {
    return '-'
  }
  return dimensionNameMap.value.get(dimensionId) || String(dimensionId)
}

onMounted(async () => {
  await Promise.all([loadDimensions(), getList()])
})
</script>
