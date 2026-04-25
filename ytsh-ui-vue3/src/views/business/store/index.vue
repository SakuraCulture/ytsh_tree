<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <div class="search-section">
      <el-form
        class="query-form-grid"
        :model="queryParams"
        ref="queryFormRef"
        label-width="90px"
      >
        <el-form-item label="门店编码" prop="storeId">
          <el-autocomplete
            v-model="queryParams.storeId"
            :fetch-suggestions="searchStoreIdSuggestions"
            placeholder="请输入门店编码搜索"
            :trigger-on-focus="false"
            clearable
            @select="handleStoreIdSelect"
            @keyup.enter="handleQuery"
            class="!w-100%"
          >
            <template #default="{ item }">
              <div class="flex justify-between">
                <span>{{ item.label }}</span>
                <span class="text-gray-500 text-sm">{{ item.storeName }}</span>
              </div>
            </template>
          </el-autocomplete>
        </el-form-item>
        <el-form-item label="门店名称" prop="storeName">
          <el-autocomplete
            v-model="queryParams.storeName"
            :fetch-suggestions="searchStoreNameSuggestions"
            placeholder="请输入门店名称搜索"
            :trigger-on-focus="false"
            clearable
            @select="handleStoreNameSelect"
            @keyup.enter="handleQuery"
            class="!w-100%"
          >
            <template #default="{ item }">
              <div class="flex justify-between">
                <span>{{ item.label }}</span>
                <span class="text-gray-500 text-sm">{{ item.value }}</span>
              </div>
            </template>
          </el-autocomplete>
        </el-form-item>
        <el-form-item label="行政区划" prop="regionCode">
        <AreaSelect
          v-model="queryParams.regionCode"
          :level="AreaLevelEnum.DISTRICT"
          placeholder="请选择行政区划"
          clearable
          class="!w-100%"
        />
      </el-form-item>
        <el-form-item label="门店区域" prop="area">
          <el-select
            v-model="queryParams.area"
            placeholder="请选择区域"
            clearable
            class="!w-100%"
          >
            <el-option label="华东" value="EAST" />
            <el-option label="华北" value="NORTH" />
            <el-option label="华南" value="SOUTH" />
            <el-option label="华西" value="WEST" />
            <el-option label="华中" value="CENTRAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="storeStatus">
          <el-select
            v-model="queryParams.storeStatus"
            placeholder="请选择状态"
            clearable
            class="!w-100%"
          >
            <el-option label="停用" :value="0" />
            <el-option label="正常" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime">
          <el-date-picker
            v-model="queryParams.createTime"
            value-format="YYYY-MM-DD HH:mm:ss"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
            class="!w-100%"
          />
        </el-form-item>
      </el-form>
      <!-- 操作按钮栏 -->
      <div class="actions-bar">
        <div class="actions-left">
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-5px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
        </div>
        <div class="actions-right">
          <el-button
            type="primary"
            @click="openForm('create')"
            v-hasPermi="['business:table:create']"
          >
            <Icon icon="ep:plus" class="mr-5px" /> 新增
          </el-button>
          <div class="action-group">
            <el-button type="primary" plain @click="handleDownloadTemplate">
              <Icon icon="ep:download" class="mr-5px" /> 下载模板
            </el-button>
            <el-upload
              accept=".xlsx, .xls, .csv"
              :show-file-list="false"
              :before-upload="handleImport"
            >
              <el-button type="primary" plain>
                <Icon icon="ep:upload" class="mr-5px" /> 导入
              </el-button>
            </el-upload>
            <el-button
              type="success"
              plain
              @click="handleExport"
              :loading="exportLoading"
              v-hasPermi="['business:table:export']"
            >
              <Icon icon="ep:download" class="mr-5px" /> 导出
            </el-button>
          </div>
          <div class="action-group">
            <el-button
              type="danger"
              plain
              :disabled="isEmpty(checkedIds)"
              @click="handleDeleteBatch"
              v-hasPermi="['business:table:delete']"
            >
              <Icon icon="ep:delete" class="mr-5px" /> 批量删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table
        row-key="storeId"
        v-loading="loading"
        :data="list"
        :stripe="true"
        :show-overflow-tooltip="true"
        @selection-change="handleRowCheckboxChange"
        @expand-change="handleExpandChange"
    >
    <el-table-column type="expand">
      <template #default="{ row }">
        <div class="expand-content">
          <el-row :gutter="0">
            <el-col :span="12">
              <el-card shadow="hover" class="sub-table-card">
                <template #header>
                  <div class="card-header">
                    <Icon icon="ep:home" class="mr-5px" />
                    <span>空间信息</span>
                  </div>
                </template>
                <div v-if="expandData[row.storeId]?.space" class="sub-table-content">
                  <div class="detail-item">
                    <span class="label">房屋面积:</span>
                    <span>{{ expandData[row.storeId].space.buildingArea }}㎡</span>
                  </div>
                  <div class="detail-item">
                    <span class="label">冷库面积:</span>
                    <span>{{ expandData[row.storeId].space.coldStorageArea }}㎡</span>
                  </div>
                </div>
                <div v-else class="no-data">暂无数据</div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover" class="sub-table-card">
                <template #header>
                  <div class="card-header">
                    <Icon icon="ep:collection" class="mr-5px" />
                    <span>归属信息</span>
                  </div>
                </template>
                <div v-if="expandData[row.storeId]?.affiliation" class="sub-table-content">
                  <div class="detail-item">
                    <span class="label">经营方式:</span>
                    <span>{{ getBusinessModeText(expandData[row.storeId].affiliation.businessMode) }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="label">门店类型:</span>
                    <span>{{ getStoreTypeText(expandData[row.storeId].affiliation.storeType) }}</span>
                  </div>
                </div>
                <div v-else class="no-data">暂无数据</div>
              </el-card>
            </el-col>
          </el-row>
          <el-row :gutter="0">
            <el-col :span="12">
              <el-card shadow="hover" class="sub-table-card">
                <template #header>
                  <div class="card-header">
                    <Icon icon="ep:success" class="mr-5px" />
                    <span>状态信息</span>
                  </div>
                </template>
                <div v-if="expandData[row.storeId]?.status" class="sub-table-content">
                  <div class="detail-item">
                    <span class="label">当前状态:</span>
                    <el-tag :type="expandData[row.storeId].status.currentStatus === 'NORMAL' ? 'success' : 'info'">
                      {{ expandData[row.storeId].status.currentStatus === 'NORMAL' ? '正常' : '关闭' }}
                    </el-tag>
                  </div>
                  <div class="detail-item">
                    <span class="label">开业日期:</span>
                    <span>{{ expandData[row.storeId].status.openDate }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="label">签约日期:</span>
                    <span>{{ expandData[row.storeId].status.signDate }}</span>
                  </div>
                </div>
                <div v-else class="no-data">暂无数据</div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover" class="sub-table-card">
                <template #header>
                  <div class="card-header">
                    <Icon icon="ep:user" class="mr-5px" />
                    <span>加盟商信息</span>
                  </div>
                </template>
                <div v-if="expandData[row.storeId]?.franchisee" class="sub-table-content">
                  <div class="detail-item">
                    <span class="label">加盟商名称:</span>
                    <span>{{ expandData[row.storeId].franchisee.franchiseeName }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="label">联系方式:</span>
                    <span>{{ expandData[row.storeId].franchisee.franchiseePhone }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="label">合同期间:</span>
                    <span>{{ expandData[row.storeId].franchisee.contractStart }} ~ {{ expandData[row.storeId].franchisee.contractEnd }}</span>
                  </div>
                </div>
                <div v-else class="no-data">暂无数据</div>
              </el-card>
            </el-col>
          </el-row>
          <el-row :gutter="0">
            <el-col :span="24">
              <el-card shadow="hover" class="sub-table-card">
                <template #header>
                  <div class="card-header">
                    <Icon icon="ep:phone" class="mr-5px" />
                    <span>联系人信息</span>
                  </div>
                </template>
                <div v-if="expandData[row.storeId]?.contacts?.length > 0" class="sub-table-content">
                  <div v-for="(contact, index) in expandData[row.storeId].contacts" :key="index" class="contact-item">
                    <div class="detail-item">
                      <span class="label">姓名:</span>
                      <span>{{ contact.contactName }}</span>
                      <el-tag v-if="contact.isPrimary === 1" type="warning" size="small" class="ml-5px">主要</el-tag>
                    </div>
                    <div class="detail-item">
                      <span class="label">电话:</span>
                      <span>{{ contact.phone }}</span>
                    </div>
                    <div class="detail-item contact-item-divider" v-if="index < expandData[row.storeId].contacts.length - 1"></div>
                  </div>
                </div>
                <div v-else class="no-data">暂无数据</div>
              </el-card>
            </el-col>
          </el-row>
          <el-row :gutter="0">
            <el-col :span="24">
              <el-card shadow="hover" class="sub-table-card">
                <template #header>
                  <div class="card-header">
                    <Icon icon="ep:platform" class="mr-5px" />
                    <span>平台信息</span>
                  </div>
                </template>
                <div v-if="expandData[row.storeId]?.platforms?.length > 0" class="sub-table-content">
                  <div v-for="(platform, index) in expandData[row.storeId].platforms" :key="index" class="contact-item">
                    <div class="detail-item">
                      <span class="label">平台:</span>
                      <span class="font-semibold">{{ platform.platformName || '-' }}</span>
                      <el-tag :type="platform.status === 1 ? 'success' : 'info'" size="small" class="ml-5px">
                        {{ platform.status === 1 ? '正常' : '停用' }}
                      </el-tag>
                    </div>
                    <div class="detail-item">
                      <span class="label">平台门店:</span>
                      <span>{{ platform.platformStoreName || '-' }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">代理商类型:</span>
                      <span>{{ platform.agentType || '-' }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">佣金比例:</span>
                      <span>{{ platform.commissionRate != null ? platform.commissionRate + '%' : '-' }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">结算账户:</span>
                      <span>{{ platform.settlementAccount || '-' }}</span>
                    </div>
                    <div class="detail-item contact-item-divider" v-if="index < expandData[row.storeId].platforms.length - 1"></div>
                  </div>
                </div>
                <div v-else class="no-data">暂无数据</div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </template>
    </el-table-column>
    <el-table-column type="selection" width="55" />
      <el-table-column label="门店编码" align="center" prop="storeId" width="150" />
      <el-table-column label="门店名称" align="center" prop="storeName" min-width="150" />
      <el-table-column label="行政区划" align="center" prop="regionName" width="180" />
      <el-table-column label="详细地址" align="center" prop="address" min-width="200" />
      <el-table-column label="区域" align="center" prop="area" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.area === 'EAST'" type="success">华东</el-tag>
          <el-tag v-else-if="scope.row.area === 'NORTH'" type="warning">华北</el-tag>
          <el-tag v-else-if="scope.row.area === 'SOUTH'" type="info">华南</el-tag>
          <el-tag v-else-if="scope.row.area === 'WEST'" type="danger">华西</el-tag>
          <el-tag v-else-if="scope.row.area === 'CENTRAL'">华中</el-tag>
          <span v-else>{{ scope.row.area }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="storeStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.storeStatus === 1 ? 'success' : 'info'">
            {{ scope.row.storeStatus === 1 ? '正常' : '停用' }}
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
      <el-table-column label="操作" align="center" fixed="right" width="110">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.storeId)"
            v-hasPermi="['business:table:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.storeId)"
            v-hasPermi="['business:table:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <TableForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { isEmpty } from '@/utils/is'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { AreaLevelEnum } from '@/utils/constants'
import { TableApi, Table } from '@/api/business/store'
import TableForm from './TableForm.vue'
import AreaSelect from '@/components/FormCreate/src/components/AreaSelect.vue'

/** 门店 列表 */
defineOptions({ name: 'Table' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<Table[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined,
  storeName: undefined,
  regionCode: undefined,
  area: undefined,
  storeStatus: undefined,
  createTime: []
})
const queryFormRef = ref()
const exportLoading = ref(false)

// 门店名称搜索建议
const storeNameLoading = ref(false)
const storeNameList = ref<Table[]>([])

const searchStoreIdSuggestions = async (queryString: string, cb: (arg: any) => void) => {
  if (!queryString) {
    cb([])
    return
  }
  try {
    const data = await TableApi.getTableSimpleList(queryString)
    const suggestions = (data || []).map((item: Table) => ({
      value: item.storeId,
      label: item.storeId,
      storeName: item.storeName
    }))
    cb(suggestions)
  } finally {
  }
}

const handleStoreIdSelect = (item: any) => {
  // 选中时自动设置
}

const searchStoreNameSuggestions = async (queryString: string, cb: (arg: any) => void) => {
  if (!queryString) {
    cb([])
    return
  }
  try {
    const data = await TableApi.getTableSimpleList(queryString)
    const suggestions = (data || []).map((item: Table) => ({
      value: item.storeName,
      label: item.storeName,
      storeId: item.storeId
    }))
    cb(suggestions)
  } finally {
  }
}

const handleStoreNameSelect = (item: any) => {
  // 选中时自动设置
}

const getList = async () => {
  loading.value = true
  try {
    const searchParams = { ...queryParams }
    // 处理行政区划：只取最小一级ID
    if (searchParams.regionCode && Array.isArray(searchParams.regionCode) && searchParams.regionCode.length > 0) {
      searchParams.regionCode = searchParams.regionCode[searchParams.regionCode.length - 1]
    }
    const data = await TableApi.getTablePage(searchParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const formRef = ref()
const openForm = (type: string, id?: number | string) => {
  formRef.value.open(type, id)
}

const handleDelete = async (id: number | string) => {
  try {
    await message.delConfirm()
    await TableApi.deleteTable(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleDeleteBatch = async () => {
  try {
    await message.delConfirm()
    await TableApi.deleteTableList(checkedIds.value)
    checkedIds.value = []
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const checkedIds = ref<number[] | string[]>([])
const handleRowCheckboxChange = (records: Table[]) => {
  checkedIds.value = records.map((item) => item.storeId!)
}

// 展开行数据
const expandData = ref<Record<string, any>>({})

const handleExpandChange = (row: any, expandedRows: any[]) => {
  if (expandedRows.includes(row)) {
    // 展开时加载数据
    loadExpandData(row.storeId)
  }
}

const loadExpandData = async (storeId: string) => {
  if (expandData.value[storeId]) {
    return
  }
  try {
    const [space, affiliation, status, franchisee, contacts, platforms] = await Promise.all([
      TableApi.getSpaceTableByStoreId(storeId),
      TableApi.getAffiliationTableByStoreId(storeId),
      TableApi.getStatusTableByStoreId(storeId),
      TableApi.getFranchiseeTableByStoreId(storeId),
      TableApi.getContactTableListByStoreId(storeId),
      TableApi.getPlatformTableListByStoreId(storeId)
    ])
    expandData.value[storeId] = {
      space,
      affiliation,
      status,
      franchisee,
      contacts,
      platforms
    }
  } catch {
  }
}

const getBusinessModeText = (mode: string) => {
  const map: Record<string, string> = {
    'DIRECT': '直营',
    'AGENCY': '代理',
    'SELF': '自营',
    'JOINT': '联营'
  }
  return map[mode] || mode
}

const getStoreTypeText = (type: string) => {
  const map: Record<string, string> = {
    'ONLINE': '线上',
    'O2O': 'O2O'
  }
  return map[type] || type
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const searchParams = { ...queryParams }
    if (searchParams.regionCode && Array.isArray(searchParams.regionCode) && searchParams.regionCode.length > 0) {
      searchParams.regionCode = searchParams.regionCode[searchParams.regionCode.length - 1]
    }
    const data = await TableApi.exportTable(searchParams)
    download.excel(data, '门店.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const handleDownloadTemplate = async () => {
  try {
    const data = await TableApi.getImportTemplate()
    download.excel(data, '门店导入模板.xls')
  } catch {}
}

const handleImport = async (file: File) => {
  try {
    await message.importConfirm()
    const res = await TableApi.importTable(file, true)
    if (res.createStoreNames?.length > 0) {
      message.success(`成功创建 ${res.createStoreNames.length} 条数据`)
    }
    if (res.updateStoreNames?.length > 0) {
      message.success(`成功更新 ${res.updateStoreNames.length} 条数据`)
    }
    if (res.failureStoreNames && Object.keys(res.failureStoreNames).length > 0) {
      const failureText = Object.entries(res.failureStoreNames)
        .map(([name, reason]) => `${name}: ${reason}`)
        .join('\n')
      message.warning(`导入失败：\n${failureText}`)
    }
    await getList()
  } catch {}
  return false
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.search-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 24px;
}

.query-form-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px 24px;
}

.query-form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.actions-left,
.actions-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 16px;
  border-left: 1px solid #e5e7eb;
  margin-left: 4px;
}

@media (max-width: 1200px) {
  .query-form-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .query-form-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .actions-bar {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .actions-left,
  .actions-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .action-group {
    border-left: none;
    padding-left: 0;
    margin-left: 0;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .query-form-grid {
    grid-template-columns: 1fr;
  }
}

.expand-content {
  padding: 16px;
}

.expand-content :deep(.el-row) {
  margin: 0 -12px;
}

.expand-content :deep(.el-col) {
  padding: 0 12px;
  margin-bottom: 16px;
}

.expand-content :deep(.el-row:last-child .el-col) {
  margin-bottom: 0;
}

.sub-table-card {
  margin-bottom: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sub-table-card :deep(.el-card__body) {
  flex: 1;
  min-height: 80px;
}

.sub-table-card :deep(.el-card__header) {
  padding: 12px 16px;
}

.card-header {
  display: flex;
  align-items: center;
  font-weight: 600;
  font-size: 14px;
}

.sub-table-content {
  padding: 8px 0;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-item .label {
  font-weight: 500;
  color: #666;
  min-width: 90px;
}

.no-data {
  color: #999;
  font-size: 13px;
  padding: 24px 0;
  text-align: center;
}

.contact-item {
  padding: 8px 0;
}

.contact-item-divider {
  border-bottom: 1px dashed #e5e7eb;
  margin-bottom: 8px;
}
</style>
