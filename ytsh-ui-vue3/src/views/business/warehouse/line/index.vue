<template>
  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="94px"
    >
      <el-form-item label="仓库" prop="warehouseId">
        <el-select
          v-model="queryParams.warehouseId"
          placeholder="请选择仓库"
          filterable
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="item in warehouseList"
            :key="item.warehouseId"
            :label="`${item.warehouseId} - ${item.warehouseName}`"
            :value="item.warehouseId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="线路编码" prop="lineCode">
        <el-input
          v-model="queryParams.lineCode"
          placeholder="请输入线路编码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="线路名称" prop="lineName">
        <el-input
          v-model="queryParams.lineName"
          placeholder="请输入线路名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="lineStatus">
        <el-select
          v-model="queryParams.lineStatus"
          placeholder="请选择状态"
          clearable
          class="!w-240px"
        >
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
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
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['business:warehouse-line:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button type="primary" plain @click="handleDownloadTemplate" class="mr-10px">
          <Icon icon="ep:download" class="mr-5px" /> 下载模板
        </el-button>
        <el-upload
          accept=".xlsx, .xls"
          :show-file-list="false"
          :before-upload="handleImport"
          class="mr-10px"
          v-hasPermi="['business:warehouse-line:import']"
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
          v-hasPermi="['business:warehouse-line:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="仓库" align="center" prop="warehouseName" min-width="140" />
      <el-table-column label="线路编码" align="center" prop="lineCode" width="140" />
      <el-table-column label="线路名称" align="center" prop="lineName" min-width="160" />
      <el-table-column label="可下单星期" align="center" min-width="180">
        <template #default="scope">
          {{ formatWeekdays(scope.row.orderWeekdays) }}
        </template>
      </el-table-column>
      <el-table-column label="参与门店数" align="center" prop="storeCount" width="110" />
      <el-table-column label="状态" align="center" prop="lineStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.lineStatus === 1 ? 'success' : 'info'">
            {{ scope.row.lineStatus === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="180" />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.lineId)"
            v-hasPermi="['business:warehouse-line:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.lineId)"
            v-hasPermi="['business:warehouse-line:delete']"
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

  <LineForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import {
  WarehouseLineApi,
  type WarehouseLinePageReqVO,
  type WarehouseLineRespVO
} from '@/api/business/warehouse/line'
import { WarehouseApi, type WarehouseSimpleRespVO } from '@/api/business/warehouse/warehouse'
import LineForm from './LineForm.vue'

defineOptions({ name: 'BusinessWarehouseLine' })

const weekdayTextMap: Record<number, string> = {
  1: '周一',
  2: '周二',
  3: '周三',
  4: '周四',
  5: '周五',
  6: '周六',
  7: '周日'
}

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<WarehouseLineRespVO[]>([])
const total = ref(0)
const warehouseList = ref<WarehouseSimpleRespVO[]>([])
const queryParams = reactive<WarehouseLinePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  warehouseId: undefined,
  lineCode: undefined,
  lineName: undefined,
  lineStatus: undefined,
  createTime: undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)

const formatWeekdays = (value?: number[]) =>
  value?.length ? value.map((item) => weekdayTextMap[item]).join('、') : '-'

const getList = async () => {
  loading.value = true
  try {
    const data = await WarehouseLineApi.getPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const loadWarehouseList = async () => {
  warehouseList.value = (await WarehouseApi.getWarehouseSimpleList()) || []
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const formRef = ref()
const openForm = (type: 'create' | 'update', lineId?: number) => {
  formRef.value.open(type, lineId)
}

const handleDelete = async (lineId: number) => {
  try {
    await message.delConfirm()
    await WarehouseLineApi.delete(lineId)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await WarehouseLineApi.export(queryParams)
    download.excel(data, '仓库线路.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const handleDownloadTemplate = async () => {
  try {
    const data = await WarehouseLineApi.getImportTemplate()
    download.excel(data, '仓库线路导入模板.xls')
  } catch {}
}

const handleImport = async (rawFile: File) => {
  try {
    const data = new FormData()
    data.append('file', rawFile)
    const res = await WarehouseLineApi.importExcel(data)
    if (res.failureRows && Object.keys(res.failureRows).length > 0) {
      const failureText = Object.entries(res.failureRows)
        .map(([name, reason]: [string, string]) => `${name}: ${reason}`)
        .join('\n')
      message.warning(`导入失败：\n${failureText}`)
    } else {
      message.success(`导入成功：创建 ${res.createCount || 0}，更新 ${res.updateCount || 0}`)
    }
    await getList()
  } catch {}
  return false
}

onMounted(async () => {
  await Promise.all([getList(), loadWarehouseList()])
})
</script>
