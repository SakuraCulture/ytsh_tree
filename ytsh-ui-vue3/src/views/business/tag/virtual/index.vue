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
        <el-select v-model="queryParams.domainType" placeholder="请选择对象域" clearable class="!w-240px">
          <el-option label="商品" value="PRODUCT" />
          <el-option label="门店" value="STORE" />
          <el-option label="会员" value="MEMBER" />
        </el-select>
      </el-form-item>
      <el-form-item label="虚拟标签名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入虚拟标签名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="虚拟标签编码" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入虚拟标签编码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
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
          v-hasPermi="['business:tag-virtual:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="ID" align="center" prop="id" width="90" />
      <el-table-column label="对象域" align="center" width="100">
        <template #default="scope">
          {{ formatDomainType(scope.row.domainType) }}
        </template>
      </el-table-column>
      <el-table-column label="虚拟标签名称" prop="name" min-width="180" />
      <el-table-column label="虚拟标签编码" prop="code" min-width="180" />
      <el-table-column label="表达式摘要" prop="expressionSummary" min-width="220" />
      <el-table-column label="使用场景" prop="usageScenario" min-width="180" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
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
            v-hasPermi="['business:tag-virtual:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['business:tag-virtual:delete']"
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

  <TagVirtualForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { TagVirtualApi, type TagVirtual, type TagVirtualPageReqVO } from '@/api/business/tag/virtual'
import TagVirtualForm from './TagVirtualForm.vue'

defineOptions({ name: 'BusinessTagVirtual' })

const { t } = useI18n()
const message = useMessage()

const DOMAIN_TYPE_LABEL_MAP: Record<string, string> = {
  PRODUCT: '商品',
  STORE: '门店',
  MEMBER: '会员'
}

const loading = ref(false)
const list = ref<TagVirtual[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive<TagVirtualPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  domainType: undefined,
  name: undefined,
  code: undefined,
  status: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await TagVirtualApi.getTagVirtualPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = async () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  await getList()
}

const formRef = ref()
const openForm = (type: 'create' | 'update', id?: number) => {
  formRef.value.open(type, id, queryParams.domainType)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await TagVirtualApi.deleteTagVirtual(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const formatDomainType = (domainType?: string) => {
  if (!domainType) {
    return '-'
  }
  return DOMAIN_TYPE_LABEL_MAP[domainType] || domainType
}

onMounted(async () => {
  await getList()
})
</script>
