<template>
  <ContentWrap>
    <div class="page-container">
      <!-- 搜索区域 -->
      <div class="search-section">
        <el-form
          ref="queryFormRef"
          :model="queryParams"
          :inline="true"
          label-width="90px"
          class="search-form"
          @submit.prevent
        >
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="门店ID" prop="storeId">
                <el-input
                  v-model="queryParams.storeId"
                  placeholder="请输入门店ID"
                  clearable
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="订单号" prop="orderId">
                <el-input
                  v-model="queryParams.orderId"
                  placeholder="请输入内部订单号"
                  clearable
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="平台订单号" prop="channelOrderId">
                <el-input
                  v-model="queryParams.channelOrderId"
                  placeholder="请输入平台订单号"
                  clearable
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="业务类型" prop="bizType">
                <el-input
                  v-model="queryParams.bizType"
                  placeholder="请输入业务类型"
                  clearable
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="失败阶段" prop="failStage">
                <el-input
                  v-model="queryParams.failStage"
                  placeholder="请输入失败阶段"
                  clearable
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="处理状态" prop="processStatus">
                <el-select
                  v-model="queryParams.processStatus"
                  placeholder="请选择处理状态"
                  clearable
                  style="width: 100%"
                >
                  <el-option label="待重试" value="PENDING_RETRY" />
                  <el-option label="重试中" value="RETRYING" />
                  <el-option label="重试成功" value="SUCCESS" />
                  <el-option label="重试失败" value="FAILED" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-form-item label="失败时间">
                <el-date-picker
                  v-model="timeRange"
                  type="datetimerange"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  start-placeholder="开始时间"
                  end-placeholder="结束时间"
                  class="!w-100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <!-- 定时器控制区域 -->
        <div class="timer-section">
          <el-divider>定时自动拉取</el-divider>
          <el-form :inline="true" class="timer-form">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="timerStartTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择开始时间"
                :disabled-date="disabledStartDate"
              />
            </el-form-item>
            <el-form-item label="间隔时间">
              <el-select v-model="timerInterval" placeholder="选择间隔" style="width: 140px">
                <el-option label="5分钟" :value="5 * 60 * 1000" />
                <el-option label="10分钟" :value="10 * 60 * 1000" />
                <el-option label="15分钟" :value="15 * 60 * 1000" />
                <el-option label="30分钟" :value="30 * 60 * 1000" />
                <el-option label="1小时" :value="60 * 60 * 1000" />
                <el-option label="2小时" :value="2 * 60 * 60 * 1000" />
                <el-option label="3小时" :value="3 * 60 * 60 * 1000" />
                <el-option label="5小时" :value="5 * 60 * 60 * 1000" />
                <el-option label="12小时" :value="12 * 60 * 60 * 1000" />
                <el-option label="24小时" :value="24 * 60 * 60 * 1000" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button 
                v-if="!timerRunning" 
                type="warning" 
                plain 
                @click="startTimer"
                :disabled="!timerStartTime || !timerInterval"
              >
                <Icon icon="ep:video-play" class="mr-5px" />启动定时器
              </el-button>
              <el-button v-else type="danger" plain @click="stopTimer">
                <Icon icon="ep:video-pause" class="mr-5px" />停止定时器
              </el-button>
            </el-form-item>
            <el-form-item v-if="timerRunning">
              <el-tag type="success" effect="dark">
                <Icon icon="ep:clock" class="mr-5px" />
                运行中 | 下次执行: {{ nextRunTime }}
              </el-tag>
            </el-form-item>
          </el-form>
        </div>

        <!-- 操作按钮区 -->
        <div class="action-buttons">
          <div class="primary-actions">
            <el-button type="primary" @click="handleQuery">
              <Icon icon="ep:search" class="mr-5px" />搜索
            </el-button>
            <el-button @click="resetQuery">
              <Icon icon="ep:refresh" class="mr-5px" />重置
            </el-button>
          </div>
          <div class="secondary-actions">
            <el-button type="danger" plain @click="handleBatchRetry">
              <Icon icon="ep:refresh-right" class="mr-5px" />批量重试失败
            </el-button>
          </div>
        </div>

        <!-- 批量重试时间范围 -->
        <el-divider class="section-divider">按时间范围批量重试</el-divider>
        <div class="batch-retry-section">
          <el-form :inline="true" class="batch-form">
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="batchRetryTimeRange"
                type="datetimerange"
                value-format="YYYY-MM-DD HH:mm:ss"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                class="batch-date-picker"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="success" plain @click="handleBatchRetryByTimeRange">
                <Icon icon="ep:refresh-right" class="mr-5px" />执行批量重试
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <div class="table-container">
      <!-- 表格工具栏 -->
      <div class="table-toolbar">
        <div class="toolbar-left">
          <span class="table-title">失败记录列表</span>
          <span class="table-count" v-if="total > 0">（共 {{ total }} 条）</span>
        </div>
        <div class="toolbar-right">
          <el-popover trigger="click" placement="bottom-end" width="200" class="column-popover">
            <template #reference>
              <el-button class="column-selector-btn">
                <Icon icon="ep:operation" class="mr-5px" />
                列设置
              </el-button>
            </template>
            <div class="column-selector">
              <div class="selector-header">
                <span>选择显示列</span>
                <el-button link type="primary" size="small" @click="resetColumns"> 重置 </el-button>
              </div>
              <el-divider style="margin: 8px 0" />
              <div class="column-list">
                <el-checkbox-group v-model="selectedColumnProps" @change="onColumnChange">
                  <div v-for="col in columnOptions" :key="col.prop" class="column-item">
                    <el-checkbox :value="col.prop">{{ col.label }}</el-checkbox>
                  </div>
                </el-checkbox-group>
              </div>
            </div>
          </el-popover>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="list"
        stripe
        @selection-change="handleSelectionChange"
        class="main-table"
      >
        <el-table-column type="selection" width="55" fixed="left" />

        <el-table-column
          v-if="visibleColumns.includes('id')"
          label="ID"
          align="center"
          prop="id"
          width="90"
        />

        <el-table-column
          v-if="visibleColumns.includes('platformType')"
          label="平台类型"
          align="center"
          prop="platformType"
          width="100"
        />

        <el-table-column
          v-if="visibleColumns.includes('storeId')"
          label="门店ID"
          align="center"
          prop="storeId"
          width="100"
        />

        <el-table-column
          v-if="visibleColumns.includes('platformStoreId')"
          label="平台门店ID"
          align="center"
          prop="platformStoreId"
          width="120"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('merchantCode')"
          label="商家编码"
          align="center"
          prop="merchantCode"
          width="120"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('erpStoreCode')"
          label="ERP门店编码"
          align="center"
          prop="erpStoreCode"
          width="130"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('orderId')"
          label="内部订单号"
          align="center"
          prop="orderId"
          min-width="180"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('channelOrderId')"
          label="平台订单号"
          align="center"
          prop="channelOrderId"
          min-width="180"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('bizType')"
          label="业务类型"
          align="center"
          prop="bizType"
          width="120"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('failStage')"
          label="失败阶段"
          align="center"
          prop="failStage"
          width="120"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('failCode')"
          label="错误码"
          align="center"
          prop="failCode"
          width="120"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('failMessage')"
          label="失败信息"
          align="center"
          prop="failMessage"
          min-width="220"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('requestParam')"
          label="请求参数"
          align="center"
          width="100"
        >
          <template #default="scope">
            <el-button link type="primary" @click="showDetail('请求参数', scope.row.requestParam)">
              查看
            </el-button>
          </template>
        </el-table-column>

        <el-table-column
          v-if="visibleColumns.includes('responseContent')"
          label="返回内容"
          align="center"
          width="100"
        >
          <template #default="scope">
            <el-button
              link
              type="primary"
              @click="showDetail('返回内容', scope.row.responseContent)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>

        <el-table-column
          v-if="visibleColumns.includes('retryCount')"
          label="重试次数"
          align="center"
          width="110"
        >
          <template #default="scope">
            {{ scope.row.retryCount ?? 0 }}/{{ scope.row.maxRetryCount ?? 0 }}
          </template>
        </el-table-column>

        <el-table-column
          v-if="visibleColumns.includes('taskId')"
          label="任务ID"
          align="center"
          prop="taskId"
          width="140"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('processStatus')"
          label="处理状态"
          align="center"
          prop="processStatus"
          width="120"
        >
          <template #default="scope">
            <el-tag v-if="scope.row.processStatus === 'SUCCESS'" type="success">SUCCESS</el-tag>
            <el-tag v-else-if="scope.row.processStatus === 'FAILED'" type="danger">FAILED</el-tag>
            <el-tag v-else type="info">{{ scope.row.processStatus || '-' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column
          v-if="visibleColumns.includes('remark')"
          label="备注"
          align="center"
          prop="remark"
          min-width="160"
          show-overflow-tooltip
        />

        <el-table-column
          v-if="visibleColumns.includes('createTime')"
          label="创建时间"
          align="center"
          prop="createTime"
          :formatter="dateFormatter"
          width="180"
        />

        <el-table-column
          v-if="visibleColumns.includes('updateTime')"
          label="更新时间"
          align="center"
          prop="updateTime"
          :formatter="dateFormatter"
          width="180"
        />

        <el-table-column label="操作" align="center" fixed="right" width="100">
          <template #default="scope">
            <el-button link type="primary" @click="handleRetry(scope.row)">
              <Icon icon="ep:refresh-right" class="mr-5px" />重试
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
    </div>
  </ContentWrap>

  <el-dialog v-model="detailVisible" :title="detailTitle" width="60%">
    <pre class="detail-content">
      {{ formatJson(detailContent) }}
    </pre>
  </el-dialog>
</template>

<script lang="ts" setup>
import dayjs from 'dayjs'
import { dateFormatter } from '@/utils/formatTime'
import * as FailRecordApi from '@/api/ele/failRecord'
import type { FormInstance } from 'element-plus'
import { ElMessageBox } from 'element-plus'

defineOptions({ name: 'EleOrderFailRecord' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<FailRecordApi.EleFailRecordRespVO[]>([])
const selectedIds = ref<number[]>([])
const timeRange = ref<string[]>([])
const batchRetryTimeRange = ref<string[]>([])
const queryFormRef = ref<FormInstance>()
const detailVisible = ref(false)
const detailTitle = ref('')
const detailContent = ref('')

const timerStartTime = ref<string | null>(null)
const timerInterval = ref<number | null>(null)
const timerRunning = ref(false)
const timerId = ref<number | null>(null)
const nextRunTime = ref<string>('')

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  storeId: undefined as string | undefined,
  orderId: undefined as string | undefined,
  channelOrderId: undefined as string | undefined,
  bizType: undefined as string | undefined,
  failStage: undefined as string | undefined,
  processStatus: undefined as string | undefined
})

// 列选择器配置
const columnOptions = ref([
  { prop: 'id', label: 'ID', visible: true },
  { prop: 'platformType', label: '平台类型', visible: true },
  { prop: 'storeId', label: '门店ID', visible: true },
  { prop: 'platformStoreId', label: '平台门店ID', visible: false },
  { prop: 'merchantCode', label: '商家编码', visible: false },
  { prop: 'erpStoreCode', label: 'ERP门店编码', visible: false },
  { prop: 'orderId', label: '内部订单号', visible: true },
  { prop: 'channelOrderId', label: '平台订单号', visible: true },
  { prop: 'bizType', label: '业务类型', visible: true },
  { prop: 'failStage', label: '失败阶段', visible: true },
  { prop: 'failCode', label: '错误码', visible: false },
  { prop: 'failMessage', label: '失败信息', visible: true },
  { prop: 'requestParam', label: '请求参数', visible: false },
  { prop: 'responseContent', label: '返回内容', visible: false },
  { prop: 'retryCount', label: '重试次数', visible: true },
  { prop: 'taskId', label: '任务ID', visible: false },
  { prop: 'processStatus', label: '处理状态', visible: true },
  { prop: 'remark', label: '备注', visible: false },
  { prop: 'createTime', label: '创建时间', visible: true },
  { prop: 'updateTime', label: '更新时间', visible: false }
])

const visibleColumns = ref<string[]>([])
const selectedColumnProps = ref<string[]>([])

const initVisibleColumns = () => {
  selectedColumnProps.value = columnOptions.value
    .filter((col) => col.visible)
    .map((col) => col.prop)
  visibleColumns.value = [...selectedColumnProps.value]
}

const onColumnChange = (selected: string[]) => {
  columnOptions.value.forEach((col) => {
    col.visible = selected.includes(col.prop)
  })
  visibleColumns.value = [...selected]
}

// 重置列设置
const resetColumns = () => {
  columnOptions.value.forEach((col) => {
    col.visible =
      col.prop !== 'platformStoreId' &&
      col.prop !== 'merchantCode' &&
      col.prop !== 'erpStoreCode' &&
      col.prop !== 'failCode' &&
      col.prop !== 'requestParam' &&
      col.prop !== 'responseContent' &&
      col.prop !== 'taskId' &&
      col.prop !== 'remark' &&
      col.prop !== 'updateTime'
  })
  selectedColumnProps.value = columnOptions.value
    .filter((col) => col.visible)
    .map((col) => col.prop)
  visibleColumns.value = [...selectedColumnProps.value]
}

const buildQueryParams = (): FailRecordApi.EleFailRecordReqVO => {
  const [startTime, endTime] = timeRange.value || []
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    storeId: queryParams.storeId ? Number(queryParams.storeId) : undefined,
    orderId: queryParams.orderId || undefined,
    channelOrderId: queryParams.channelOrderId || undefined,
    bizType: queryParams.bizType || undefined,
    failStage: queryParams.failStage || undefined,
    processStatus: queryParams.processStatus || undefined,
    startTime: startTime ? dayjs(startTime).unix() : undefined,
    endTime: endTime ? dayjs(endTime).unix() : undefined
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await FailRecordApi.getFailRecordPage(buildQueryParams())
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

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  timeRange.value = []
  selectedIds.value = []
  handleQuery()
}

const handleSelectionChange = (rows: FailRecordApi.EleFailRecordRespVO[]) => {
  selectedIds.value = rows.map((row) => row.id).filter((id): id is number => id !== undefined)
}

const handleRetry = async (row: FailRecordApi.EleFailRecordRespVO) => {
  if (!row.id) {
    message.warning('缺少失败记录 ID，无法重试')
    return
  }
  let overwrite = false
  try {
    await ElMessageBox.confirm(
      '订单拉取成功后，如果数据库中已存在该订单，是否覆盖更新？',
      '重试失败记录',
      {
        distinguishCancelAndClose: true,
        confirmButtonText: '覆盖已有订单',
        cancelButtonText: '跳过已有订单',
        type: 'warning'
      }
    )
    overwrite = true
  } catch (action) {
    if (action === 'cancel') {
      overwrite = false
    } else {
      return
    }
  }
  try {
    if (overwrite) {
      await FailRecordApi.retryFailRecordWithOverwrite(row.id)
      message.success('重试成功（覆盖模式）')
    } else {
      await FailRecordApi.retryFailRecord(row.id)
      message.success('重试成功（跳过模式）')
    }
  } catch (e: any) {
    const errMsg = e?.msg || e?.message || '重试失败，请检查该记录的订单号和门店信息是否完整'
    message.error(errMsg)
  }
  await getList()
}

const handleBatchRetry = async () => {
  let overwrite = false
  try {
    await ElMessageBox.confirm(
      '将批量重试所有处理状态为 FAILED 的记录。如果数据库中已存在对应订单，是否覆盖更新？',
      '批量重试失败记录',
      {
        distinguishCancelAndClose: true,
        confirmButtonText: '覆盖已有订单',
        cancelButtonText: '跳过已有订单',
        type: 'warning'
      }
    )
    overwrite = true
  } catch (action) {
    if (action === 'cancel') {
      overwrite = false
    } else {
      return
    }
  }
  try {
    const allFailed = await FailRecordApi.getAllFailedIds()
    if (allFailed.length === 0) {
      message.info('没有需要重试的失败记录')
      return
    }
    await FailRecordApi.batchRetryFailRecord(allFailed)
    message.success(
      `批量重试任务已提交（${overwrite ? '覆盖' : '跳过'}模式，共 ${allFailed.length} 条）`
    )
  } catch (e: any) {
    const errMsg = e?.msg || e?.message || '批量重试失败'
    message.error(errMsg)
  }
  await getList()
}

const handleBatchRetryByTimeRange = async () => {
  if (!batchRetryTimeRange.value || batchRetryTimeRange.value.length < 2) {
    message.warning('请选择时间范围')
    return
  }
  const [startTimeStr, endTimeStr] = batchRetryTimeRange.value
  const startTime = dayjs(startTimeStr).valueOf()
  const endTime = dayjs(endTimeStr).valueOf()

  if (startTime >= endTime) {
    message.warning('开始时间必须小于结束时间')
    return
  }

  try {
    await ElMessageBox.confirm(
      '将批量重试指定时间范围内的 FAILED 记录。如果数据库中已存在对应订单，是否覆盖更新？',
      '按时间范围批量重试',
      {
        distinguishCancelAndClose: true,
        confirmButtonText: '覆盖已有订单',
        cancelButtonText: '跳过已有订单',
        type: 'warning'
      }
    )
    const count = await FailRecordApi.retryFailRecordsByTimeRange(startTime, endTime, true)
    message.success(`按时间范围重试任务已提交（覆盖模式，成功 ${count} 条）`)
  } catch (action) {
    if (action === 'cancel') {
      const count = await FailRecordApi.retryFailRecordsByTimeRange(startTime, endTime, false)
      message.success(`按时间范围重试任务已提交（跳过模式，成功 ${count} 条）`)
    } else {
      return
    }
  }
  await getList()
}

const showDetail = (title: string, content: string) => {
  detailTitle.value = title
  detailContent.value = content || '无数据'
  detailVisible.value = true
}

const formatJson = (content: string) => {
  if (!content) return ''
  try {
    return JSON.stringify(JSON.parse(content), null, 2)
  } catch {
    return content
  }
}

const disabledStartDate = (time: Date) => {
  return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

const calculateNextRunTime = (currentTime: Date) => {
  const next = new Date(currentTime.getTime() + timerInterval.value!)
  nextRunTime.value = dayjs(next).format('YYYY-MM-DD HH:mm:ss')
}

const executeTimerTask = () => {
  getList()
  calculateNextRunTime(new Date())
}

const startTimer = () => {
  if (!timerStartTime.value || !timerInterval.value) {
    message.warning('请设置开始时间和间隔时间')
    return
  }
  
  const startTime = dayjs(timerStartTime.value).valueOf()
  const now = Date.now()
  const delay = Math.max(0, startTime - now)
  
  message.success(`定时器已启动，将在 ${dayjs(startTime).format('YYYY-MM-DD HH:mm:ss')} 开始执行`)
  
  timerId.value = window.setTimeout(() => {
    executeTimerTask()
    timerId.value = window.setInterval(executeTimerTask, timerInterval.value!)
    timerRunning.value = true
  }, delay)
}

const stopTimer = () => {
  if (timerId.value) {
    clearTimeout(timerId.value)
    clearInterval(timerId.value)
    timerId.value = null
  }
  timerRunning.value = false
  nextRunTime.value = ''
  message.info('定时器已停止')
}

onMounted(() => {
  initVisibleColumns()
  getList()
})

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
.page-container {
  padding: 8px 0;
}

.search-section {
  background: #fff;
  border-radius: 4px;
}

.search-form {
  margin-bottom: 16px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.search-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: #606266;
}

.search-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: all 0.2s;
}

.search-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

.search-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  margin-top: 8px;
  border-top: 1px solid #ebeef5;
}

.primary-actions {
  display: flex;
  gap: 8px;
}

.secondary-actions {
  display: flex;
  gap: 8px;
}

.section-divider {
  margin: 16px 0;
  font-size: 13px;
  color: #909399;
}

.batch-retry-section {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.timer-section {
  background: #fffbeb;
  padding: 16px;
  border-radius: 4px;
  margin: 16px 0;
  border: 1px solid #f5dab1;
}

.timer-form {
  margin-bottom: 0;
}

.timer-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.timer-section :deep(.el-divider) {
  margin: 0 0 16px 0;
  font-size: 13px;
  color: #e6a23c;
}

.batch-form {
  margin-bottom: 0;
}

.batch-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.batch-date-picker {
  width: 360px;
}

.table-container {
  padding: 8px 0;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.toolbar-left {
  display: flex;
  align-items: baseline;
}

.table-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.table-count {
  font-size: 13px;
  color: #909399;
  margin-left: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.column-selector-btn {
  border-color: #dcdfe6;
  color: #606266;
}

.column-selector-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.column-selector {
  max-height: 400px;
  overflow-y: auto;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.column-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.column-item {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.column-item:hover {
  background-color: #f5f7fa;
}

.column-label {
  margin-left: 8px;
  font-size: 13px;
  color: #606266;
}

.main-table {
  border-radius: 4px;
  overflow: hidden;
}

.main-table :deep(.el-table__header-wrapper) {
  background-color: #fafafa;
}

.main-table :deep(.el-table th) {
  background-color: #fafafa;
  color: #303133;
  font-weight: 500;
}

.main-table :deep(.el-table__body tr:hover > td) {
  background-color: #f5f7fa;
}

.detail-content {
  max-height: 500px;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
}

@media (max-width: 768px) {
  .action-buttons {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .batch-date-picker {
    width: 100%;
  }

  .table-toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .toolbar-right {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
