<template>
  <div class="app-container">
    <div class="pull-card">
      <div class="pull-card-header">
        <span class="header-title">📦 订单同步</span>
        <el-divider direction="vertical" />
        <span class="header-title">🧾 账单同步</span>
      </div>
      <div class="pull-card-body">
        <el-tabs v-model="activeSyncTab" class="sync-tabs">
          <el-tab-pane label="订单同步" name="order">
            <div class="settings-bar">
          <div class="setting-group">
            <span class="setting-label">拉取模式</span>
            <el-radio-group v-model="pullMode" class="mode-radio" size="small">
              <el-radio-button label="single">按门店</el-radio-button>
              <el-radio-button label="all">全部门店</el-radio-button>
            </el-radio-group>
          </div>
          <div class="setting-group">
            <span class="setting-label">定时同步</span>
            <el-switch
              v-if="pullMode === 'all'"
              v-model="scheduleEnabled"
              inline-prompt
              active-text="开"
              inactive-text="关"
              size="small"
            />
            <span v-else class="setting-disabled-text">—</span>
          </div>
          <div class="setting-group">
            <span class="setting-label">实时推送</span>
            <el-switch
              v-model="pushSetting.orderPushEnabled"
              inline-prompt
              active-text="开"
              inactive-text="关"
              size="small"
              @change="handlePushSettingChange"
            />
          </div>
        </div>
        <div class="form-row" v-if="pullMode === 'single'">
          <div class="form-item">
            <span class="form-label">选择门店</span>
            <el-select
              v-model="selectedStoreId"
              placeholder="请选择门店"
              clearable
              filterable
              :loading="storeLoading"
              class="store-select"
            >
              <el-option
                v-for="store in storeList"
                :key="store.platformStoreId || store.storeName"
                :label="store.storeName"
                :value="store.platformStoreId || store.storeName"
              >
                <span
                  :class="{
                    'store-open': store.storeStatus === 1,
                    'store-closed': store.storeStatus === 0
                  }"
                >
                  {{ store.storeName }}
                </span>
              </el-option>
            </el-select>
          </div>
        </div>
        <div class="form-row" v-if="pullMode === 'single'">
          <div class="form-item">
            <span class="form-label">拉取日期</span>
            <el-radio-group v-model="dateType" class="date-radio">
              <el-radio-button label="today">当日</el-radio-button>
              <el-radio-button label="custom">自定义</el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-if="dateType === 'custom'"
              v-model="customDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              class="date-picker"
            />
          </div>
        </div>
        <div class="form-row" v-if="pullMode === 'all'">
          <div class="form-item">
            <span class="form-label">开始日期</span>
            <el-date-picker
              v-model="startDate"
              type="date"
              placeholder="选择开始日期"
              value-format="YYYY-MM-DD"
              class="date-picker"
            />
          </div>
          <div class="form-item">
            <span class="form-label">结束日期</span>
            <el-date-picker
              v-model="endDate"
              type="date"
              placeholder="选择结束日期（可选）"
              value-format="YYYY-MM-DD"
              class="date-picker"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-item btn-item">
            <el-button
              type="primary"
              size="large"
              :loading="pullLoading"
              :disabled="!canPull"
              @click="handlePull"
            >
              {{ pullMode === 'all' ? '立即拉取全部门店' : '立即拉取门店订单' }}
            </el-button>
          </div>
        </div>
        <div class="detail-sections" v-if="pullMode === 'all'">
          <div class="schedule-section" v-if="scheduleEnabled">
            <div class="form-row">
              <div class="form-item">
                <span class="form-label">定时模式</span>
                <el-radio-group v-model="scheduleType" class="schedule-mode-radio" size="small">
                  <el-radio-button label="time">指定时间</el-radio-button>
                  <el-radio-button label="dayOfMonth">指定天数</el-radio-button>
                  <el-radio-button label="weekDay">指定星期</el-radio-button>
                  <el-radio-button label="interval">间隔时间</el-radio-button>
                </el-radio-group>
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'time'">
              <div class="form-item">
                <span class="form-label">执行时间</span>
                <el-time-picker
                  v-model="newTimePoint"
                  format="HH:mm:ss"
                  value-format="HH:mm:ss"
                  placeholder="选择时间"
                  class="time-picker"
                  size="small"
                />
                <el-button @click="addTimePoint" :disabled="!newTimePoint" size="small"
                  >添加</el-button
                >
                <div class="time-points-list" v-if="scheduleTimePoints.length > 0">
                  <el-tag
                    v-for="(time, index) in scheduleTimePoints"
                    :key="index"
                    closable
                    @close="removeTimePoint(index)"
                    class="time-tag"
                    size="small"
                  >
                    {{ time }}
                  </el-tag>
                </div>
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'dayOfMonth'">
              <div class="form-item">
                <span class="form-label">选择天数</span>
                <el-date-picker
                  v-model="selectedMonthDays"
                  type="dates"
                  placeholder="点击日历选择多个日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  class="month-day-picker"
                  size="small"
                  :clearable="false"
                />
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'dayOfMonth'">
              <div class="form-item">
                <span class="form-label">执行时间</span>
                <el-time-picker
                  v-model="dayOfMonthTime"
                  format="HH:mm:ss"
                  value-format="HH:mm:ss"
                  class="time-picker"
                  size="small"
                />
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'weekDay'">
              <div class="form-item">
                <span class="form-label">选择星期</span>
                <el-select
                  v-model="selectedWeekDays"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="选择星期"
                  class="week-select"
                  size="small"
                >
                  <el-option
                    v-for="opt in weekDaysOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'weekDay'">
              <div class="form-item">
                <span class="form-label">执行时间</span>
                <el-time-picker
                  v-model="weekDayTime"
                  format="HH:mm:ss"
                  value-format="HH:mm:ss"
                  class="time-picker"
                  size="small"
                />
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'interval'">
              <div class="form-item">
                <span class="form-label">开始时间</span>
                <el-time-picker
                  v-model="intervalStartTime"
                  format="HH:mm:ss"
                  value-format="HH:mm:ss"
                  placeholder="选择开始时间"
                  class="time-picker"
                  size="small"
                />
              </div>
            </div>
            <div class="form-row" v-if="scheduleType === 'interval'">
              <div class="form-item">
                <span class="form-label">间隔时间</span>
                <el-select
                  v-model="intervalHours"
                  placeholder="选择间隔"
                  class="interval-select"
                  size="small"
                >
                  <el-option
                    v-for="opt in intervalHoursOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </div>
            </div>
            <div class="form-row" v-if="generatedCron">
              <div class="form-item">
                <span class="form-label">CRON表达式</span>
                <span class="cron-text">{{ generatedCron }}</span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-item btn-item">
                <el-button
                  type="success"
                  :loading="scheduleSaving"
                  @click="saveScheduleConfig"
                  size="small"
                >
                  保存定时配置
                </el-button>
              </div>
            </div>
            <div class="schedule-tip">💡 按设定规则自动拉取全部门店订单</div>
          </div>

          <div class="push-section" v-if="pushSetting.orderPushEnabled">
            <div class="form-row">
              <div class="form-item">
                <span class="form-label">推送状态</span>
                <el-select
                  v-model="pushSetting.orderPushTypesArray"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="选择推送状态"
                  @change="handlePushSettingChange"
                  class="push-status-select"
                  size="small"
                >
                  <el-option label="已支付(1)" value="1" />
                  <el-option label="已接单(2)" value="2" />
                  <el-option label="已拣货(3)" value="3" />
                  <el-option label="已打包(4)" value="4" />
                  <el-option label="已发货(5)" value="5" />
                  <el-option label="交易成功(6)" value="6" />
                  <el-option label="交易关闭(-1)" value="-1" />
                </el-select>
              </div>
              <div class="form-item" style="margin-left: 16px">
                <span class="form-label">桌面通知</span>
                <el-switch
                  v-model="pushSetting.orderPushDesktop"
                  inline-prompt
                  active-text="开"
                  inactive-text="关"
                  size="small"
                  @change="handleDesktopNotificationChange"
                />
              </div>
            </div>
            <div class="push-status-tip">💡 订单状态变更时将实时推送通知</div>
          </div>
        </div>
          </el-tab-pane>

          <el-tab-pane label="账单同步" name="bill">
            <div class="bill-sync-card">
              <div class="bill-header">
                <span class="bill-header-title">账单同步设置</span>
              </div>
              <div class="bill-settings">
                <div class="setting-row">
                  <div class="setting-group">
                    <span class="setting-label">同步模式</span>
                    <el-radio-group v-model="billSyncMode" class="bill-mode-radio" size="small">
                      <el-radio-button label="single">指定门店</el-radio-button>
                      <el-radio-button label="all">全部门店</el-radio-button>
                    </el-radio-group>
                  </div>
                </div>

                <div class="setting-row" v-if="billSyncMode === 'single'">
                  <div class="setting-group">
                    <span class="setting-label">选择门店</span>
                    <el-select
                      v-model="selectedBillStoreId"
                      placeholder="请选择门店"
                      clearable
                      filterable
                      :loading="storeLoading"
                      class="bill-store-select"
                    >
                      <el-option
                        v-for="store in storeList"
                        :key="store.platformStoreId || store.storeName"
                        :label="store.storeName"
                        :value="store.platformStoreId || store.storeName"
                      />
                    </el-select>
                  </div>
                  <div class="setting-group">
                    <span class="setting-label">账单日期</span>
                    <el-date-picker
                      v-model="billSyncDate"
                      type="date"
                      placeholder="选择账单日期"
                      value-format="YYYY-MM-DD"
                      class="bill-date-picker"
                    />
                  </div>
                  <div class="setting-group btn-group">
                    <el-button
                      type="primary"
                      :loading="billSyncLoading"
                      :disabled="!canBillSyncSingle"
                      @click="handleBillSyncSingle"
                    >
                      同步门店账单
                    </el-button>
                  </div>
                </div>

                <div class="setting-row" v-if="billSyncMode === 'all'">
                  <div class="setting-group">
                    <span class="setting-label">开始日期</span>
                    <el-date-picker
                      v-model="billSyncStartDate"
                      type="date"
                      placeholder="选择开始日期"
                      value-format="YYYY-MM-DD"
                      class="bill-date-picker"
                    />
                  </div>
                  <div class="setting-group">
                    <span class="setting-label">结束日期</span>
                    <el-date-picker
                      v-model="billSyncEndDate"
                      type="date"
                      placeholder="选择结束日期"
                      value-format="YYYY-MM-DD"
                      class="bill-date-picker"
                    />
                  </div>
                  <div class="setting-group btn-group">
                    <el-button
                      type="primary"
                      :loading="billSyncLoading"
                      :disabled="!canBillSyncAll"
                      @click="handleBillSyncAll"
                    >
                      同步全部账单
                    </el-button>
                  </div>
                </div>

                <div class="bill-sync-tip">
                  💡 账单同步按门店+日期维度拉取，接口QPS限制为20，内部已做限流处理
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <div class="sync-progress-overlay" v-if="showBatchSyncProgress">
      <div class="sync-progress-container">
        <div class="sync-header">
          <div class="sync-header-left">
            <span class="sync-icon">{{
              batchProgress.syncStatus === 'COMPLETED'
                ? '✅'
                : batchProgress.syncStatus === 'FAILED'
                  ? '❌'
                  : '🔄'
            }}</span>
            <span class="sync-title">{{ syncProgressTitle }}</span>
          </div>
          <el-button
            v-if="!batchProgress.isSyncing"
            type="primary"
            size="small"
            @click="showBatchSyncProgress = false"
          >
            关闭
          </el-button>
        </div>

        <div v-if="batchProgress.isSyncing" class="sync-detail">
          <el-progress
            :percentage="syncProgressPercent"
            :status="syncProgressPercent === 100 ? 'success' : ''"
            :stroke-width="12"
            class="sync-progress-bar"
          />
          <div class="sync-stats">
            <div class="stat-item">
              <span class="stat-label">门店进度</span>
              <span class="stat-value"
                >{{ batchProgress.completedStores }}/{{ batchProgress.totalStores }}</span
              >
            </div>
            <div class="stat-item">
              <span class="stat-label">已拉取订单</span>
              <span class="stat-value">{{ batchProgress.totalOrders }}</span>
            </div>
            <div class="stat-item success">
              <span class="stat-label">成功门店</span>
              <span class="stat-value">{{ batchProgress.successStores }}</span>
            </div>
            <div class="stat-item failed">
              <span class="stat-label">失败门店</span>
              <span class="stat-value">{{ batchProgress.failedStores }}</span>
            </div>
          </div>
          <div v-if="batchProgress.currentSyncingStores.length > 0" class="syncing-info">
            <el-icon class="syncing-icon"><Loading /></el-icon>
            <span>正在同步: {{ batchProgress.currentSyncingStores.slice(0, 3).join('、') }}</span>
            <span v-if="batchProgress.currentSyncingStores.length > 3"
              >等{{ batchProgress.currentSyncingCount }}家门店</span
            >
          </div>
        </div>

        <div v-else class="sync-complete">
          <div class="sync-stats summary-stats">
            <div class="stat-card-item">
              <div class="stat-card-inner">
                <div class="stat-card-label">门店总数</div>
                <div class="stat-card-value">{{ batchProgress.totalStores }}</div>
              </div>
            </div>
            <div class="stat-card-item">
              <div class="stat-card-inner success-card">
                <div class="stat-card-label">成功门店</div>
                <div class="stat-card-value success-value">{{ batchProgress.successStores }}</div>
              </div>
            </div>
            <div class="stat-card-item">
              <div class="stat-card-inner">
                <div class="stat-card-label">拉取订单</div>
                <div class="stat-card-value">{{ batchProgress.totalOrders }}</div>
              </div>
            </div>
            <div class="stat-card-item">
              <div class="stat-card-inner failed-card" v-if="batchProgress.failedStores > 0">
                <div class="stat-card-label">失败门店</div>
                <div class="stat-card-value failed-value">{{ batchProgress.failedStores }}</div>
              </div>
            </div>
          </div>

          <div
            v-if="batchProgress.failedStores > 0 && batchProgress.failedStoreDetails.length > 0"
            class="failed-stores-section"
          >
            <div class="failed-section-title">
              <el-icon><WarningFilled /></el-icon>
              失败门店详情
            </div>
            <div class="failed-store-list">
              <div
                v-for="(store, index) in batchProgress.failedStoreDetails"
                :key="index"
                class="failed-store-item"
              >
                <span class="failed-store-name">{{
                  store.storeName || store.platformStoreId
                }}</span>
                <span v-if="store.platformStoreId" class="failed-store-id"
                  >({{ store.platformStoreId }})</span
                >
                <span v-if="store.errorMsg" class="failed-store-error">{{ store.errorMsg }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="table-container">
      <div class="section-header">
        <span class="section-title">同步日志</span>
      </div>
      <el-table
        :data="syncLogList"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#334155', fontWeight: '700' }"
      >
        <el-table-column
          prop="syncBatchId"
          label="同步批次"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column prop="storeName" label="门店名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="syncMode" label="同步模式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.syncMode === 'MULTI' ? 'success' : 'info'" size="small">
              {{ row.syncMode === 'MULTI' ? '多线程' : '单线程' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="threadCount" label="线程数" width="80" align="center" />
        <el-table-column prop="totalPulled" label="API拉取" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.totalPulled || row.syncCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="syncCount" label="同步订单数" width="110" align="center">
          <template #default="{ row }">
            <span>{{ row.syncCount ?? '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expectedTotal" label="预期总数" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.expectedTotal ?? '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="savedTotal" label="实际落库" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.discrepancyRate > 0 }">{{
              row.savedTotal ?? '--'
            }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="discrepancyRate" label="差异率" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.discrepancyRate !== null && row.discrepancyRate !== undefined"
              :type="
                row.discrepancyRate === 0
                  ? 'success'
                  : row.discrepancyRate > 20
                    ? 'danger'
                    : 'warning'
              "
              size="small"
            >
              {{ row.discrepancyRate }}%
            </el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="dataIntegrity" label="完整性" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.dataIntegrity === 1" type="success" size="small">完整</el-tag>
            <el-tag v-else-if="row.dataIntegrity === 2" type="warning" size="small">部分</el-tag>
            <el-tag v-else-if="row.dataIntegrity === 3" type="danger" size="small">严重</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="successCount" label="成功" width="90" align="center">
          <template #default="{ row }">
            <span class="text-success">{{ row.successCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="failCount" label="失败" width="90" align="center">
          <template #default="{ row }">
            <span class="text-danger">{{ row.failCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="同步耗时" width="100" align="center">
          <template #default="{ row }">
            <span>{{ calculateDuration(row) }}秒</span>
          </template>
        </el-table-column>
        <el-table-column label="同步开始时间" min-width="170">
          <template #default="{ row }">
            <span>{{ formatTimestamp(row.syncStartTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="同步结束时间" min-width="170">
          <template #default="{ row }">
            <span>{{ formatTimestamp(row.syncEndTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="错误代码" width="200" align="center">
          <template #default="{ row }">
            <div class="error-codes-cell">
              <el-tag v-if="row.pullErrorCode" type="danger" size="small" class="error-code-tag">
                {{ getErrorInfo(row.pullErrorCode).icon
                }}{{ getErrorInfo(row.pullErrorCode).label }}
              </el-tag>
              <el-tag v-if="row.saveErrorCode" type="warning" size="small" class="error-code-tag">
                {{ getErrorInfo(row.saveErrorCode).icon
                }}{{ getErrorInfo(row.saveErrorCode).label }}
              </el-tag>
              <el-tag
                v-if="row.reconciliationErrorCode"
                type="info"
                size="small"
                class="error-code-tag"
              >
                {{ getErrorInfo(row.reconciliationErrorCode).icon
                }}{{ getErrorInfo(row.reconciliationErrorCode).label }}
              </el-tag>
              <span v-if="!row.pullErrorCode && !row.saveErrorCode && !row.reconciliationErrorCode"
                >--</span
              >
            </div>
          </template>
        </el-table-column>
        <el-table-column label="失败详情" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span
              v-if="
                (row.failCount > 0 ||
                  row.pullErrorCode ||
                  row.saveErrorCode ||
                  row.reconciliationErrorCode) &&
                row.errorMsg
              "
              class="error-msg"
              @click="showErrorDetail(row)"
            >
              {{ row.errorMsg }}
            </span>
            <span v-else>--</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @pagination="getSyncLogs"
      />
    </div>

    <el-dialog title="失败详情" v-model="errorDialogVisible" width="600px" destroy-on-close>
      <div class="error-detail">
        <pre>{{ currentError }}</pre>
      </div>
      <template #footer>
        <el-button @click="errorDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      title="同步错误详情"
      v-model="errorDetailDialogVisible"
      width="800px"
      destroy-on-close
    >
      <div v-if="errorDetailData" class="error-detail-dialog">
        <div class="error-detail-header">
          <span>门店: {{ errorDetailData.storeName }}</span>
          <span>时间: {{ errorDetailData.syncStartTime }}</span>
        </div>

        <el-tabs v-model="activeErrorTab" class="error-tabs">
          <el-tab-pane label="对账数据" name="reconciliation">
            <div class="reconciliation-panel">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="API查询总数"
                  >{{ errorDetailData.reconciliation?.expectedTotal || 0 }}条</el-descriptions-item
                >
                <el-descriptions-item label="实际落库数"
                  >{{ errorDetailData.reconciliation?.savedTotal || 0 }}条</el-descriptions-item
                >
                <el-descriptions-item label="差异率"
                  >{{ errorDetailData.reconciliation?.discrepancyRate || 0 }}%</el-descriptions-item
                >
                <el-descriptions-item label="重试次数"
                  >{{ errorDetailData.reconciliation?.retryCount || 0 }}次</el-descriptions-item
                >
              </el-descriptions>
              <div v-if="errorDetailData.reconciliation?.apiStatusCounts" class="status-compare">
                <h4>各状态对比</h4>
                <el-table :data="statusCompareData" border size="small">
                  <el-table-column prop="statusName" label="状态" width="100" />
                  <el-table-column prop="apiCount" label="API数" width="80" align="center" />
                  <el-table-column prop="savedCount" label="落库数" width="80" align="center" />
                  <el-table-column prop="diff" label="差异" width="80" align="center" />
                </el-table>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane
            :label="`拉取错误${errorDetailData.pullError?.code ? '(1)' : ''}`"
            name="pull"
          >
            <div v-if="errorDetailData.pullError?.code" class="error-card">
              <el-alert
                :title="
                  getErrorInfo(errorDetailData.pullError.code).icon +
                  ' ' +
                  getErrorInfo(errorDetailData.pullError.code).label
                "
                :type="getErrorInfo(errorDetailData.pullError.code).color as any"
                :description="getErrorInfo(errorDetailData.pullError.code).suggestion"
                show-icon
                :closable="false"
              />
              <pre v-if="errorDetailData.pullError.detail" class="error-json">{{
                JSON.stringify(errorDetailData.pullError.detail, null, 2)
              }}</pre>
            </div>
            <el-empty v-else description="无拉取错误" />
          </el-tab-pane>

          <el-tab-pane
            :label="`落库错误${errorDetailData.saveError?.code ? '(1)' : ''}`"
            name="save"
          >
            <div v-if="errorDetailData.saveError?.code" class="error-card">
              <el-alert
                :title="
                  getErrorInfo(errorDetailData.saveError.code).icon +
                  ' ' +
                  getErrorInfo(errorDetailData.saveError.code).label
                "
                :type="getErrorInfo(errorDetailData.saveError.code).color as any"
                :description="getErrorInfo(errorDetailData.saveError.code).suggestion"
                show-icon
                :closable="false"
              />
              <pre v-if="errorDetailData.saveError.detail" class="error-json">{{
                JSON.stringify(errorDetailData.saveError.detail, null, 2)
              }}</pre>
            </div>
            <el-empty v-else description="无落库错误" />
          </el-tab-pane>

          <el-tab-pane
            :label="`对账错误${errorDetailData.reconciliationError?.code ? '(1)' : ''}`"
            name="recon"
          >
            <div v-if="errorDetailData.reconciliationError?.code" class="error-card">
              <el-alert
                :title="
                  getErrorInfo(errorDetailData.reconciliationError.code).icon +
                  ' ' +
                  getErrorInfo(errorDetailData.reconciliationError.code).label
                "
                :type="getErrorInfo(errorDetailData.reconciliationError.code).color as any"
                :description="getErrorInfo(errorDetailData.reconciliationError.code).suggestion"
                show-icon
                :closable="false"
              />
              <pre v-if="errorDetailData.reconciliationError.detail" class="error-json">{{
                JSON.stringify(errorDetailData.reconciliationError.detail, null, 2)
              }}</pre>
            </div>
            <el-empty v-else description="无对账错误" />
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="errorDetailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="errorDetailData?.reconciliation?.discrepancyRate > 0"
          type="warning"
          @click="triggerCompensation"
          :loading="compensationLoading"
        >
          触发补偿
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      :title="alertDialogType === 'critical' ? '🚨 严重告警' : '⚠️ 超时警告'"
      v-model="alertDialogVisible"
      :width="alertDialogWidth"
      destroy-on-close
      :class="alertDialogType === 'critical' ? 'critical-alert-dialog' : 'warning-alert-dialog'"
    >
      <div class="alert-detail">
        <p class="alert-summary-text">
          {{
            alertDialogType === 'critical'
              ? '以下订单已超过5天未完结，请尽快处理！'
              : '以下订单已超过3天未完结，请关注处理进度。'
          }}
        </p>
        <el-table :data="alertList" border stripe class="alert-table">
          <el-table-column prop="orderId" label="订单号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="erpStoreCode" label="门店编码" width="120" align="center" />
          <el-table-column
            prop="platformStoreId"
            label="平台门店ID"
            width="130"
            show-overflow-tooltip
          />
          <el-table-column prop="orderStatus" label="当前状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.orderStatus)" size="small">
                {{ getStatusName(row.orderStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTimeStr" label="创建时间" width="160" align="center" />
          <el-table-column prop="daysElapsed" label="已过天数" width="90" align="center">
            <template #default="{ row }">
              <span :class="row.daysElapsed >= 5 ? 'days-critical' : 'days-warning'">
                {{ row.daysElapsed }}天
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="handleDismissAlerts" type="primary">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Loading, WarningFilled, Document, Check, Refresh } from '@element-plus/icons-vue'
import {
  pullOrdersByRange,
  getSyncLogPage,
  getSyncScheduleConfig,
  updateSyncScheduleConfig,
  getSyncProgress,
  getBatchSyncProgress,
  getErrorDetail,
  type SyncScheduleConfigReqVO
} from '@/api/ele/orderSync'
import {
  syncBillsByDate,
  syncBillsByDateRange,
  syncBillByStore as syncBillByStoreApi,
  getBillInfo,
  getBillSummary
} from '@/api/ele/billSync'
import { getOrderPushSetting, updateOrderPushSetting } from '@/api/ele/orderPush'
import {
  getUnshownAlerts,
  markAllAlertsAsShown,
  type OrderTrackingAlertVO
} from '@/api/ele/orderTracking'
import { TableApi } from '@/api/business/store'
import Pagination from '@/components/Pagination/index.vue'

const pullMode = ref<'single' | 'all'>('single')
const activeSyncTab = ref<'order' | 'bill'>('order')
const storeList = ref<any[]>([])
const storeLoading = ref(false)
const selectedStoreId = ref<string | null>(null)
const billSyncMode = ref<'single' | 'all'>('all')
const selectedBillStoreId = ref<string | null>(null)
const billSyncDate = ref<string | null>(null)
const billSyncStartDate = ref<string | null>(null)
const billSyncEndDate = ref<string | null>(null)
const billSyncLoading = ref(false)
const dateType = ref<'today' | 'custom'>('today')
const customDate = ref<string | null>(null)
const startDate = ref<string | null>(null)
const endDate = ref<string | null>(null)
const pullLoading = ref(false)
const loading = ref(true)
const total = ref(0)
const syncLogList = ref<any[]>([])
const errorDialogVisible = ref(false)
const currentError = ref('')

const showBatchSyncProgress = ref(false)
const batchProgress = ref({
  isSyncing: false,
  syncStatus: 'IDLE',
  totalStores: 0,
  completedStores: 0,
  successStores: 0,
  failedStores: 0,
  currentSyncingCount: 0,
  currentSyncingStores: [] as string[],
  failedStoreDetails: [] as { platformStoreId: string; storeName?: string; errorMsg?: string }[],
  startTime: 0,
  totalOrders: 0,
  successOrders: 0,
  failOrders: 0,
  apiStatusCounts: {} as Record<number, number>,
  savedStatusCounts: {} as Record<number, number>,
  pageCounts: {} as Record<number, number>,
  totalApiCount: 0,
  totalSavedCount: 0,
  discrepancyRate: 0,
  reconciliationStatus: 'PENDING' as string,
  retryCount: 0,
  pullErrors: [] as any[],
  saveErrors: [] as any[],
  reconciliationErrors: [] as any[],
  currentStatus: null as number | null,
  currentPage: 0,
  elapsedSeconds: 0
})
const batchSyncPollingTimer = ref<ReturnType<typeof setInterval> | null>(null)

const errorDetailDialogVisible = ref(false)
const currentSyncLogId = ref<number | null>(null)
const errorDetailData = ref<any>(null)
const activeErrorTab = ref<string>('reconciliation')
const compensationLoading = ref(false)

const STATUS_NAMES: Record<string, string> = {
  '0': '待支付',
  '1': '已支付',
  '2': '已接单',
  '3': '已拣货',
  '4': '已打包',
  '5': '已发货',
  '6': '交易成功',
  '-1': '交易关闭',
  '-2': '已取消',
  '-3': '部分退款',
  '-4': '全额退款',
  '-5': '退款中'
}

const ERROR_CODE_MAP: Record<
  string,
  { label: string; color: string; icon: string; suggestion: string }
> = {
  PULL_API_TIMEOUT: {
    label: 'API调用超时',
    color: 'warning',
    icon: '⏱️',
    suggestion: '系统将自动重试'
  },
  PULL_API_RATE_LIMIT: {
    label: 'API限流',
    color: 'info',
    icon: '🚦',
    suggestion: '请求频率过高，请稍后重试'
  },
  PULL_PAGE_INCOMPLETE: {
    label: '分页不完整',
    color: 'error',
    icon: '📄',
    suggestion: 'API返回数据不完整，已触发二次拉取'
  },
  PULL_STATUS_MISSING: {
    label: '状态订单缺失',
    color: 'error',
    icon: '📋',
    suggestion: '某状态订单未拉取完整'
  },
  PULL_NETWORK_ERROR: {
    label: '网络错误',
    color: 'warning',
    icon: '🌐',
    suggestion: '网络连接异常，系统将重试'
  },
  PULL_AUTH_ERROR: {
    label: '认证失败',
    color: 'error',
    icon: '🔐',
    suggestion: 'API认证失败，请联系技术支持'
  },
  PULL_DATA_FORMAT_ERROR: {
    label: '数据格式错误',
    color: 'error',
    icon: '📝',
    suggestion: 'API返回数据格式异常'
  },
  SAVE_DUPLICATE_KEY: {
    label: '重复订单',
    color: 'info',
    icon: '🔄',
    suggestion: '订单已存在，已自动跳过'
  },
  SAVE_DATA_TRUNCATION: {
    label: '数据截断',
    color: 'warning',
    icon: '✂️',
    suggestion: '字段长度超出限制'
  },
  SAVE_CONSTRAINT_VIOLATION: {
    label: '约束违反',
    color: 'error',
    icon: '🚫',
    suggestion: '数据库约束冲突'
  },
  SAVE_TRANSACTION_TIMEOUT: {
    label: '事务超时',
    color: 'warning',
    icon: '⏰',
    suggestion: '数据库事务执行超时'
  },
  SAVE_CONNECTION_ERROR: {
    label: '数据库连接错误',
    color: 'error',
    icon: '🔌',
    suggestion: '数据库连接异常'
  },
  SAVE_BATCH_ERROR: {
    label: '批量插入失败',
    color: 'warning',
    icon: '📦',
    suggestion: '批量操作部分失败'
  },
  RECON_DISCREPANCY: {
    label: '数据不一致',
    color: 'error',
    icon: '⚠️',
    suggestion: 'API与数据库数据不一致，已触发补偿拉取'
  },
  RECON_STATUS_MISMATCH: {
    label: '状态分布不匹配',
    color: 'warning',
    icon: '📊',
    suggestion: '各状态订单数量不匹配'
  },
  RECON_TIME_RANGE_ERROR: {
    label: '时间范围异常',
    color: 'warning',
    icon: '🕐',
    suggestion: '订单时间超出同步时间窗口'
  },
  RECON_RETRY_EXHAUSTED: {
    label: '重试次数用尽',
    color: 'error',
    icon: '🔁',
    suggestion: '补偿3次后仍不一致，请人工介入'
  },
  RECON_API_DATA_CHANGED: {
    label: 'API数据变更',
    color: 'warning',
    icon: '🔄',
    suggestion: '对账期间API数据发生变化'
  }
}

function getErrorInfo(errorCode: string) {
  return (
    ERROR_CODE_MAP[errorCode] || {
      label: '未知错误',
      color: 'error',
      icon: '❓',
      suggestion: '请联系技术支持'
    }
  )
}

const scheduleEnabled = ref(false)
const currentCron = ref('')
const scheduleSaving = ref(false)

const scheduleType = ref<'time' | 'dayOfMonth' | 'weekDay' | 'interval'>('time')
const scheduleTimePoints = ref<string[]>([])
const newTimePoint = ref<string | null>(null)
const selectedMonthDays = ref<string[]>([])
const selectedDaysOfMonth = ref<number[]>([])
const dayOfMonthTime = ref('00:00:00')
const selectedWeekDays = ref<number[]>([])
const weekDayTime = ref('09:00:00')
const intervalStartTime = ref('00:00:00')
const intervalHours = ref<number>(1)

const daysOfMonthOptions = Array.from({ length: 31 }, (_, i) => ({
  label: `${i + 1}号`,
  value: i + 1
}))

const intervalHoursOptions = [
  { label: '1小时', value: 1 },
  { label: '2小时', value: 2 },
  { label: '3小时', value: 3 },
  { label: '5小时', value: 5 },
  { label: '12小时', value: 12 },
  { label: '24小时', value: 24 }
]

const weekDaysOptions = [
  { label: '周一', value: 2 },
  { label: '周二', value: 3 },
  { label: '周三', value: 4 },
  { label: '周四', value: 5 },
  { label: '周五', value: 6 },
  { label: '周六', value: 7 },
  { label: '周日', value: 1 }
]

const currentTaskId = ref<string | null>(null)
const pollingTimer = ref<ReturnType<typeof setInterval> | null>(null)
const MAX_POLL_COUNT = 120
const POLL_INTERVAL = 1000

const pushSetting = ref({
  orderPushEnabled: true,
  orderPushTypes: '',
  orderPushTypesArray: [] as string[],
  orderPushDesktop: false
})

const websocket = ref<WebSocket | null>(null)

const alertDialogVisible = ref(false)
const alertDialogType = ref<'warning' | 'critical'>('warning')
const alertList = ref<OrderTrackingAlertVO[]>([])
const alertPollingTimer = ref<ReturnType<typeof setInterval> | null>(null)

const alertDialogWidth = computed(() => {
  return alertList.value.length > 5 ? '90%' : '800px'
})

const queryParams = ref({
  pageNum: 1,
  pageSize: 20,
  platformStoreId: null as string | null
})

const canPull = computed(() => {
  if (pullMode.value === 'single') {
    return !!selectedStoreId.value
  }
  return !!startDate.value
})

const canBillSyncSingle = computed(() => {
  return !!selectedBillStoreId.value && !!billSyncDate.value
})

const canBillSyncAll = computed(() => {
  return !!billSyncStartDate.value && !!billSyncEndDate.value
})

const syncProgressPercent = computed(() => {
  if (!batchProgress.value.totalStores) return 0
  return Math.round((batchProgress.value.completedStores / batchProgress.value.totalStores) * 100)
})

const syncProgressTitle = computed(() => {
  if (batchProgress.value.syncStatus === 'COMPLETED') {
    return '拉取完成'
  }
  if (batchProgress.value.syncStatus === 'FAILED') {
    return '拉取失败'
  }
  return `正在拉取门店订单 (${batchProgress.value.completedStores}/${batchProgress.value.totalStores})`
})

watch(
  () => selectedMonthDays.value,
  (dates: string[]) => {
    selectedDaysOfMonth.value = [...new Set(dates.map((d) => new Date(d).getDate()))].sort(
      (a, b) => a - b
    )
  },
  { deep: true }
)

const generatedCron = computed(() => {
  if (!scheduleEnabled.value) return ''

  if (scheduleType.value === 'time') {
    if (scheduleTimePoints.value.length === 0) return ''
    const hours = [...new Set(scheduleTimePoints.value.map((t) => t.split(':')[0]))].join(',')
    return `0 0 ${hours} * * ?`
  }

  if (scheduleType.value === 'dayOfMonth') {
    if (selectedDaysOfMonth.value.length === 0) return ''
    const [h, m, s] = dayOfMonthTime.value.split(':')
    const days = selectedDaysOfMonth.value.join(',')
    return `${s} ${m} ${h} ${days} * ?`
  }

  if (scheduleType.value === 'weekDay') {
    if (selectedWeekDays.value.length === 0) return ''
    const [h, m, s] = weekDayTime.value.split(':')
    const weeks = selectedWeekDays.value.join(',')
    return `${s} ${m} ${h} ? * ${weeks}`
  }

  if (scheduleType.value === 'interval') {
    if (!intervalStartTime.value) return ''
    const [h, m, s] = intervalStartTime.value.split(':')
    return `0 ${m} ${h}/${intervalHours.value} * * ?`
  }

  return ''
})

const getDayTimestamps = (dateStr?: string) => {
  let date: Date
  if (dateStr) {
    const [y, m, d] = dateStr.split('-').map(Number)
    date = new Date(y, m - 1, d)
  } else {
    date = new Date()
    date.setHours(0, 0, 0, 0)
  }
  const start = Math.floor(date.getTime() / 1000)
  const end = start + 86399
  return { start, end }
}

const getDayStartTimestamp = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  return Math.floor(date.getTime() / 1000)
}

const getDayEndTimestamp = (dateStr: string) => {
  const [y, m, d] = dateStr.split('-').map(Number)
  const date = new Date(y, m - 1, d, 23, 59, 59)
  return Math.floor(date.getTime() / 1000)
}

const getPullTimeRange = () => {
  if (pullMode.value === 'all') {
    const startTs = getDayStartTimestamp(startDate.value!)
    const endTs = endDate.value ? getDayEndTimestamp(endDate.value) : Math.floor(Date.now() / 1000)
    return { start: startTs, end: endTs }
  }
  return dateType.value === 'today'
    ? getNowDayTimestamps()
    : getDayTimestamps(customDate.value || undefined)
}

const getNowDayTimestamps = () => {
  const now = new Date()
  const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const start = Math.floor(startOfDay.getTime() / 1000)
  const nowSec = Math.floor(now.getTime() / 1000)
  return { start, end: nowSec }
}

const normalizeTimestamp = (ts: number | null | undefined): number | null => {
  if (!ts) return null
  return ts > 100000000000 ? ts : ts * 1000
}

const getStoreNameById = (platformStoreId: string) => {
  const store = storeList.value.find((s) => s.platformStoreId === platformStoreId)
  return store ? store.storeName : platformStoreId || '--'
}

const loadStoreList = async () => {
  storeLoading.value = true
  try {
    const res = await TableApi.getTableAllSimpleList(1)
    const rawData = (res as any)?.data || res
    const list = Array.isArray(rawData) ? rawData : []
    // console.log('门店列表加载成功:', list.length, '条数据', list)
    storeList.value = list.sort((a: any, b: any) => {
      const aStatus = a.storeStatus ?? 1
      const bStatus = b.storeStatus ?? 1
      return bStatus - aStatus
    })
  } catch (error) {
    // console.error('门店列表加载失败:', error)
    storeList.value = []
  } finally {
    storeLoading.value = false
  }
}

const handlePaginationChange = ({ page, limit }: { page: number; limit: number }) => {
  queryParams.value.pageNum = page
  queryParams.value.pageSize = limit
  getSyncLogs()
}

const getSyncLogs = async () => {
  loading.value = true

  const requestParams: any = {
    pageNo: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize
  }

  if (pullMode.value === 'single' && selectedStoreId.value) {
    requestParams.platformStoreId = selectedStoreId.value
  }

  try {
    const response = await getSyncLogPage(requestParams)
    const list = (response as any)?.list || (response as any)?.data?.list || []
    const totalVal = (response as any)?.total ?? (response as any)?.data?.total ?? 0
    syncLogList.value = list
    total.value = totalVal
  } catch (error: any) {
    // console.error('同步日志查询失败:', error)
    syncLogList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const startPolling = (taskId: string) => {
  stopPolling()
  currentTaskId.value = taskId
  showBatchSyncProgress.value = true
  batchProgress.value = {
    isSyncing: true,
    syncStatus: 'RUNNING',
    totalStores: 1,
    completedStores: 0,
    successStores: 0,
    failedStores: 0,
    currentSyncingCount: 1,
    currentSyncingStores: selectedStoreId.value ? [getStoreNameById(selectedStoreId.value)] : [],
    failedStoreDetails: [],
    startTime: Date.now(),
    totalOrders: 0,
    successOrders: 0,
    failOrders: 0
  }

  let pollCount = 0
  const startMs = Date.now()

  pollingTimer.value = setInterval(async () => {
    pollCount++
    try {
      const progress = await getSyncProgress(taskId)
      const elapsed = Math.floor((Date.now() - startMs) / 1000)

      if (progress && (progress.status === 'SUCCESS' || progress.status === 'FAILED')) {
        stopPolling()
        const isSuccess = progress.status === 'SUCCESS'
        batchProgress.value = {
          isSyncing: false,
          syncStatus: isSuccess ? 'COMPLETED' : 'FAILED',
          totalStores: 1,
          completedStores: 1,
          successStores: isSuccess ? 1 : 0,
          failedStores: isSuccess ? 0 : 1,
          currentSyncingCount: 0,
          currentSyncingStores: [],
          failedStoreDetails: isSuccess
            ? []
            : [
                {
                  platformStoreId: progress.platformStoreId || '',
                  storeName: progress.platformStoreId
                    ? getStoreNameById(progress.platformStoreId)
                    : '',
                  errorMsg: progress.errorMessage || '同步失败'
                }
              ],
          startTime: Date.now() - elapsed * 1000,
          totalOrders: progress.pulledCount || 0,
          successOrders: isSuccess ? progress.pulledCount || 0 : 0,
          failOrders: isSuccess ? 0 : 1
        }

        setTimeout(() => {
          showBatchSyncProgress.value = false
        }, 5000)

        if (isSuccess) {
          ElMessage.success('门店订单同步完成')
        } else {
          ElMessage.error(progress.errorMessage || '门店订单同步失败')
        }
        await getSyncLogs()
        currentTaskId.value = null
        return
      }

      batchProgress.value = {
        ...batchProgress.value,
        isSyncing: true,
        totalOrders: progress?.pulledCount || 0,
        successOrders: progress?.pulledCount || 0,
        failOrders: 0
      }

      if (pollCount >= MAX_POLL_COUNT) {
        stopPolling()
        batchProgress.value = {
          isSyncing: false,
          syncStatus: 'FAILED',
          totalStores: 1,
          completedStores: 1,
          successStores: 0,
          failedStores: 1,
          currentSyncingCount: 0,
          currentSyncingStores: [],
          failedStoreDetails: [],
          startTime: Date.now() - elapsed * 1000,
          totalOrders: 0,
          successOrders: 0,
          failOrders: 0
        }

        setTimeout(() => {
          showBatchSyncProgress.value = false
        }, 5000)

        ElMessage.warning('同步任务超时，请稍后在同步日志中查看结果')
        getSyncLogs()
      }
    } catch {
      if (pollCount >= MAX_POLL_COUNT) {
        stopPolling()
        batchProgress.value.isSyncing = false
        batchProgress.value.syncStatus = 'FAILED'

        setTimeout(() => {
          showBatchSyncProgress.value = false
        }, 5000)

        getSyncLogs()
      }
    }
  }, POLL_INTERVAL)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

const isBatchProgressRunning = (data: any): boolean => {
  if (!data) return false
  const syncStatus = String(data.syncStatus || '').toUpperCase()
  const totalStores = Number(data.totalStores || 0)
  const completedStores = Number(data.completedStores || 0)
  const currentSyncingCount = Number(data.currentSyncingCount || 0)

  return (
    data.isSyncing === true ||
    syncStatus === 'RUNNING' ||
    syncStatus === 'SYNCING' ||
    syncStatus === 'PROCESSING' ||
    currentSyncingCount > 0 ||
    (totalStores > 0 && completedStores < totalStores)
  )
}

const toBatchProgress = (data: any, fallbackStatus = 'IDLE') => ({
  isSyncing: isBatchProgressRunning(data),
  syncStatus: data?.syncStatus || fallbackStatus,
  totalStores: data?.totalStores ?? 0,
  completedStores: data?.completedStores ?? 0,
  successStores: data?.successStores ?? 0,
  failedStores: data?.failedStores ?? 0,
  currentSyncingCount: data?.currentSyncingCount ?? 0,
  currentSyncingStores: data?.currentSyncingStores ?? [],
  failedStoreDetails: batchProgress.value.failedStoreDetails || [],
  startTime: data?.startTime ?? 0,
  totalOrders: data?.totalOrders ?? 0,
  successOrders: data?.successOrders ?? 0,
  failOrders: data?.failOrders ?? 0
})

const startBatchSyncPolling = () => {
  stopBatchSyncPolling()
  batchSyncPollingTimer.value = setInterval(async () => {
    try {
      const res = await getBatchSyncProgress()
      if (res) {
        const data = (res as any)?.data || res
        batchProgress.value = toBatchProgress(data)

        if (!isBatchProgressRunning(data) || data?.syncStatus === 'COMPLETED' || data?.syncStatus === 'FAILED') {
          stopBatchSyncPolling()

          if (batchProgress.value.failedStores > 0) {
            try {
              const logsRes = await getSyncLogPage({
                pageNo: 1,
                pageSize: 100,
                status: 0
              })
              const logs = (logsRes as any)?.list || (logsRes as any)?.data?.list || []
              batchProgress.value.failedStoreDetails = logs
                .filter((log: any) => log.failCount > 0 || log.status === 0)
                .map((log: any) => ({
                  platformStoreId: log.platformStoreId || '',
                  storeName: log.storeName || '',
                  errorMsg: log.errorMsg || '同步失败'
                }))
            } catch {
              // 静默处理
            }
          }

          if (data?.syncStatus === 'COMPLETED' || batchProgress.value.completedStores > 0) {
            showBatchSyncProgress.value = true
            setTimeout(() => {
              showBatchSyncProgress.value = false
            }, 5000)

            ElMessage.success(
              `拉取完成！成功${batchProgress.value.successStores}家，失败${batchProgress.value.failedStores}家`
            )
          } else if (data?.syncStatus === 'FAILED') {
            ElMessage.error(
              `拉取失败！成功${batchProgress.value.successStores}家，失败${batchProgress.value.failedStores}家`
            )
          }
          await getSyncLogs()
        }
      }
    } catch (e) {
      // Silent fail for polling errors
    }
  }, 1000)
}

const stopBatchSyncPolling = () => {
  if (batchSyncPollingTimer.value) {
    clearInterval(batchSyncPollingTimer.value)
    batchSyncPollingTimer.value = null
  }
}

const handleBillSyncSingle = async () => {
  if (!selectedBillStoreId.value || !billSyncDate.value) {
    ElMessage.warning('请选择门店和账单日期')
    return
  }

  const store = storeList.value.find(s => s.platformStoreId === selectedBillStoreId.value)
  if (!store) {
    ElMessage.warning('门店信息不存在')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要同步门店「${store.storeName}」${billSyncDate.value}的账单吗？`,
      '确认同步账单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  billSyncLoading.value = true
  try {
    await syncBillByStoreApi(store.settlementAccount || '', selectedBillStoreId.value, billSyncDate.value)
    ElMessage.success('门店账单同步任务已提交')
  } catch (error: any) {
    ElMessage.error(error?.message || '账单同步任务提交失败')
  } finally {
    billSyncLoading.value = false
  }
}

const handleBillSyncAll = async () => {
  if (!billSyncStartDate.value || !billSyncEndDate.value) {
    ElMessage.warning('请选择开始日期和结束日期')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要同步全部门店 ${billSyncStartDate.value} ~ ${billSyncEndDate.value} 的账单吗？\n（可能需要较长时间，请耐心等待）`,
      '确认同步账单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  billSyncLoading.value = true
  try {
    await syncBillsByDateRange(billSyncStartDate.value, billSyncEndDate.value)
    ElMessage.success('全部门店账单同步任务已提交')
  } catch (error: any) {
    ElMessage.error(error?.message || '账单同步任务提交失败')
  } finally {
    billSyncLoading.value = false
  }
}

const handlePull = async () => {
  if (pullMode.value === 'single' && !selectedStoreId.value) {
    ElMessage.warning('请选择要拉取的门店')
    return
  }
  if (pullMode.value === 'all' && !startDate.value) {
    ElMessage.warning('请选择开始日期')
    return
  }

  const timeRange = getPullTimeRange()

  const confirmMsg =
    pullMode.value === 'all'
      ? '确定要拉取全部门店的订单吗？'
      : `确定要拉取门店「${getStoreNameById(selectedStoreId.value!)}」的订单吗？`

  try {
    await ElMessageBox.confirm(confirmMsg, '确认拉取订单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  pullLoading.value = true

  try {
    if (pullMode.value === 'all') {
      showBatchSyncProgress.value = true
      batchProgress.value = {
        isSyncing: true,
        syncStatus: 'RUNNING',
        totalStores: 0,
        completedStores: 0,
        successStores: 0,
        failedStores: 0,
        currentSyncingCount: 0,
        currentSyncingStores: [],
        startTime: 0
      }

      try {
        const result = await pullOrdersByRange({
          startTime: timeRange.start,
          endTime: timeRange.end
        })
        const data = (result as any)?.data || result || {}

        if (data && data.totalCount !== undefined) {
          const completed = data.completed === true
          batchProgress.value = {
            isSyncing: false,
            syncStatus: completed ? 'COMPLETED' : 'FAILED',
            totalStores: data.totalCount || 0,
            completedStores: data.totalCount || 0,
            successStores: data.successCount || 0,
            failedStores: data.failCount || 0,
            currentSyncingCount: 0,
            currentSyncingStores: [],
            failedStoreDetails: (data.failedStores || []).map((storeId: string) => ({
              platformStoreId: storeId,
              storeName: getStoreNameById(storeId),
              errorMsg: '同步失败'
            })),
            startTime: Date.now(),
            totalOrders: data.totalCount || 0,
            successOrders: data.successCount || 0,
            failOrders: data.failCount || 0
          }

          if (completed) {
            ElMessage.success(
              `拉取完成！成功${batchProgress.value.successStores}家，失败${batchProgress.value.failedStores}家`
            )
            await getSyncLogs()
            setTimeout(() => {
              showBatchSyncProgress.value = false
            }, 5000)
          } else {
            ElMessage.error(
              `拉取失败！成功${batchProgress.value.successStores}家，失败${batchProgress.value.failedStores}家`
            )
          }
        } else {
          batchProgress.value.syncStatus = 'FAILED'
          batchProgress.value.isSyncing = false
          ElMessage.error('订单同步返回数据异常')
        }
      } catch (e) {
        batchProgress.value.syncStatus = 'FAILED'
        batchProgress.value.isSyncing = false
        ElMessage.error('订单同步任务提交失败')
      }

      if (batchProgress.value.isSyncing) {
        startBatchSyncPolling()
        ElMessage.info('订单同步任务已提交，正在等待结果...')
      }
    } else {
      const result = await pullOrdersByRange({
        platformStoreId: selectedStoreId.value!,
        startTime: timeRange.start,
        endTime: timeRange.end
      })
      const data = (result as any)?.data || result || {}

      if (data && data.totalCount !== undefined) {
        batchProgress.value = {
          isSyncing: false,
          syncStatus: data.completed ? 'COMPLETED' : 'FAILED',
          totalStores: 1,
          completedStores: 1,
          successStores: data.successCount || 0,
          failedStores: data.failCount || 0,
          currentSyncingCount: 0,
          currentSyncingStores: [],
          startTime: Date.now(),
          totalOrders: data.totalCount || 0,
          successOrders: data.successCount || 0,
          failOrders: data.failCount || 0
        }

        if (data.completed) {
          ElMessage.success('门店订单同步完成')
        } else {
          ElMessage.error(`门店订单同步失败，失败门店: ${(data.failedStores || []).join(', ')}`)
        }
        await getSyncLogs()
      } else {
        ElMessage.error('门店订单同步返回数据异常')
      }
    }
  } catch (error: any) {
    batchProgress.value = {
      isSyncing: false,
      syncStatus: 'FAILED',
      totalStores: 1,
      completedStores: 1,
      successStores: 0,
      failedStores: 1,
      currentSyncingCount: 0,
      currentSyncingStores: [],
      startTime: Date.now(),
      totalOrders: 0,
      successOrders: 0,
      failOrders: 0
    }
    ElMessage.error(error?.message || '订单拉取失败')
  } finally {
    pullLoading.value = false
  }
}

const calculateDuration = (row: any) => {
  if (row.syncStartTime && row.syncEndTime) {
    let startTs = Number(row.syncStartTime)
    let endTs = Number(row.syncEndTime)
    if (startTs < 100000000000) startTs *= 1000
    if (endTs < 100000000000) endTs *= 1000
    const durationMs = endTs - startTs
    if (durationMs > 0 && durationMs < 86400000) {
      return (durationMs / 1000).toFixed(1)
    }
  }
  return '--'
}

const formatTimestamp = (timestamp: number | null | undefined) => {
  if (!timestamp) return '--'
  const ts = normalizeTimestamp(timestamp)
  if (!ts) return '--'
  const date = new Date(ts)
  if (isNaN(date.getTime())) return '--'
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hours = `${date.getHours()}`.padStart(2, '0')
  const minutes = `${date.getMinutes()}`.padStart(2, '0')
  const seconds = `${date.getSeconds()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const showErrorDetail = (row: any) => {
  if (typeof row === 'string') {
    currentError.value = row
    errorDialogVisible.value = true
    return
  }

  if (row.id) {
    currentSyncLogId.value = row.id
    loadErrorDetail(row)
  } else {
    currentError.value = row.errorMsg || '未知错误'
    errorDialogVisible.value = true
  }
}

const loadErrorDetail = async (row: any) => {
  try {
    errorDetailData.value = {
      storeName: row.storeName || '--',
      syncStartTime: row.syncStartTime ? formatTimestamp(row.syncStartTime) : '--',
      reconciliation: {
        expectedTotal: row.expectedTotal || 0,
        savedTotal: row.savedTotal || 0,
        discrepancyRate: row.discrepancyRate || 0,
        retryCount: row.retryCount || 0,
        apiStatusCounts: row.apiStatusCounts ? JSON.parse(row.apiStatusCounts) : {},
        savedStatusCounts: row.savedStatusCounts ? JSON.parse(row.savedStatusCounts) : {}
      },
      pullError: row.pullErrorCode
        ? {
            code: row.pullErrorCode,
            detail: row.pullErrorDetail ? JSON.parse(row.pullErrorDetail) : null
          }
        : { code: null, detail: null },
      saveError: row.saveErrorCode
        ? {
            code: row.saveErrorCode,
            detail: row.saveErrorDetail ? JSON.parse(row.saveErrorDetail) : null
          }
        : { code: null, detail: null },
      reconciliationError: row.reconciliationErrorCode
        ? {
            code: row.reconciliationErrorCode,
            detail: row.reconciliationErrorDetail ? JSON.parse(row.reconciliationErrorDetail) : null
          }
        : { code: null, detail: null }
    }
    activeErrorTab.value = 'reconciliation'
    errorDetailDialogVisible.value = true
  } catch (error) {
    // console.error('加载错误详情失败:', error)
    currentError.value = row.errorMsg || '加载详情失败'
    errorDialogVisible.value = true
  }
}

const triggerCompensation = async () => {
  if (!errorDetailData.value) return

  compensationLoading.value = true
  try {
    await pullOrdersByRange({
      platformStoreId: errorDetailData.value.storeName || '',
      startTime: Math.floor(new Date(errorDetailData.value.syncStartTime).getTime() / 1000),
      endTime: Math.floor(Date.now() / 1000)
    })

    ElMessage.success('补偿任务已提交，正在重新拉取订单')
    errorDetailDialogVisible.value = false
    await getSyncLogs()
  } catch (error) {
    // console.error('触发补偿失败:', error)
    ElMessage.error('触发补偿失败')
  } finally {
    compensationLoading.value = false
  }
}

const statusCompareData = computed(() => {
  if (!errorDetailData.value?.reconciliation?.apiStatusCounts) return []

  const apiCounts = errorDetailData.value.reconciliation.apiStatusCounts || {}
  const savedCounts = errorDetailData.value.reconciliation.savedStatusCounts || {}

  const allStatuses = new Set([...Object.keys(apiCounts), ...Object.keys(savedCounts)])

  return Array.from(allStatuses).map((status) => {
    const apiCount = apiCounts[status] || 0
    const savedCount = savedCounts[status] || 0
    return {
      statusName: STATUS_NAMES[status] || `状态${status}`,
      apiCount,
      savedCount,
      diff: apiCount - savedCount
    }
  })
})

const addTimePoint = () => {
  if (newTimePoint.value && !scheduleTimePoints.value.includes(newTimePoint.value)) {
    scheduleTimePoints.value.push(newTimePoint.value)
    scheduleTimePoints.value.sort()
  }
  newTimePoint.value = null
}

const removeTimePoint = (index: number) => {
  scheduleTimePoints.value.splice(index, 1)
}

const loadScheduleConfig = async () => {
  try {
    const res = await getSyncScheduleConfig()
    const data = (res as any)?.data || res || {}
    if (data.exists) {
      scheduleEnabled.value = !!data.enabled
      scheduleType.value = data.scheduleType || 'time'
      if (data.scheduleType === 'time') {
        scheduleTimePoints.value = data.timePoints || []
      } else if (data.scheduleType === 'dayOfMonth') {
        selectedDaysOfMonth.value = data.daysOfMonth || []
        dayOfMonthTime.value = data.dayOfMonthTime || '00:00:00'
      } else if (data.scheduleType === 'weekDay') {
        selectedWeekDays.value = data.weekDays || []
        weekDayTime.value = data.weekDayTime || '09:00:00'
      } else if (data.scheduleType === 'interval') {
        intervalStartTime.value = data.intervalStartTime || '00:00:00'
        intervalHours.value = data.intervalHours || 1
      }
      currentCron.value = data.cronExpression || ''
    } else {
      scheduleEnabled.value = false
      scheduleType.value = 'time'
      scheduleTimePoints.value = []
      selectedDaysOfMonth.value = []
      selectedWeekDays.value = []
      dayOfMonthTime.value = '00:00:00'
      weekDayTime.value = '09:00:00'
      intervalStartTime.value = '00:00:00'
      intervalHours.value = 1
      currentCron.value = ''
      await updateSyncScheduleConfig({
        enabled: false,
        scheduleType: 'time',
        cronExpression: '',
        timePoints: [],
        dayOfMonthTime: '00:00:00',
        weekDays: [],
        weekDayTime: '09:00:00'
      })
    }
  } catch {
    scheduleEnabled.value = false
    scheduleType.value = 'time'
  }
}

const validateScheduleConfig = (): boolean => {
  if (scheduleType.value === 'time' && scheduleTimePoints.value.length === 0) {
    ElMessage.warning('请至少添加一个执行时间')
    return false
  }
  if (scheduleType.value === 'dayOfMonth' && selectedDaysOfMonth.value.length === 0) {
    ElMessage.warning('请至少选择一天')
    return false
  }
  if (scheduleType.value === 'weekDay' && selectedWeekDays.value.length === 0) {
    ElMessage.warning('请至少选择一个星期')
    return false
  }
  if (scheduleType.value === 'interval') {
    if (!intervalStartTime.value) {
      ElMessage.warning('请设置开始时间')
      return false
    }
    if (!intervalHours.value) {
      ElMessage.warning('请选择间隔时间')
      return false
    }
  }
  return true
}

const saveScheduleConfig = async () => {
  if (!scheduleEnabled.value) {
    await updateSyncScheduleConfig({
      enabled: false,
      scheduleType: scheduleType.value,
      cronExpression: '',
      timePoints: [],
      daysOfMonth: [],
      dayOfMonthTime: dayOfMonthTime.value,
      weekDays: [],
      weekDayTime: weekDayTime.value,
      intervalStartTime: intervalStartTime.value,
      intervalHours: intervalHours.value
    })
    ElMessage.success('定时同步已关闭')
    await loadScheduleConfig()
    return
  }

  if (!validateScheduleConfig()) return

  const cron = generatedCron.value
  if (!cron) {
    ElMessage.warning('请完善定时任务配置')
    return
  }

  scheduleSaving.value = true
  try {
    const config: SyncScheduleConfigReqVO = {
      enabled: scheduleEnabled.value,
      scheduleType: scheduleType.value,
      cronExpression: cron,
      timePoints: scheduleTimePoints.value.length > 0 ? scheduleTimePoints.value : undefined,
      daysOfMonth: selectedDaysOfMonth.value.length > 0 ? selectedDaysOfMonth.value : undefined,
      dayOfMonthTime: dayOfMonthTime.value,
      weekDays: selectedWeekDays.value.length > 0 ? selectedWeekDays.value : undefined,
      weekDayTime: weekDayTime.value,
      intervalStartTime: scheduleType.value === 'interval' ? intervalStartTime.value : undefined,
      intervalHours: scheduleType.value === 'interval' ? intervalHours.value : undefined
    }
    await updateSyncScheduleConfig(config)
    ElMessage.success('定时同步配置已保存')
    await loadScheduleConfig()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存定时配置失败')
  } finally {
    scheduleSaving.value = false
  }
}

const checkBatchSyncStatusOnMount = async () => {
  try {
    const res = await getBatchSyncProgress()
    const data = (res as any)?.data || res || {}

    if (isBatchProgressRunning(data)) {
      showBatchSyncProgress.value = true
      batchProgress.value = toBatchProgress(data, 'RUNNING')
      startBatchSyncPolling()
    }
  } catch (err) {
    // console.warn('检查同步状态失败（请确认后端服务已重启）:', err)
  }
}

const loadPushSetting = async () => {
  try {
    const res = await getOrderPushSetting()
    if (res.code === 0 && res.data) {
      pushSetting.value = {
        orderPushEnabled: res.data.orderPushEnabled ?? true,
        orderPushTypes: res.data.orderPushTypes || '',
        orderPushTypesArray: res.data.orderPushTypes ? res.data.orderPushTypes.split(',') : [],
        orderPushDesktop: res.data.orderPushDesktop ?? false
      }
    }
  } catch (e) {
    // console.error('加载推送设置失败:', e)
  }
}

const handlePushSettingChange = async () => {
  try {
    await updateOrderPushSetting({
      orderPushEnabled: pushSetting.value.orderPushEnabled,
      orderPushTypes: pushSetting.value.orderPushTypesArray.join(','),
      orderPushDesktop: pushSetting.value.orderPushDesktop
    })
  } catch (e: any) {
    ElMessage.error(e?.message || '保存推送设置失败')
  }
}

const handleDesktopNotificationChange = async () => {
  if (pushSetting.value.orderPushDesktop) {
    if (!('Notification' in window)) {
      ElMessage.warning('您的浏览器不支持桌面通知')
      pushSetting.value.orderPushDesktop = false
      return
    }

    const permission = await Notification.requestPermission()
    if (permission !== 'granted') {
      ElMessage.warning('桌面通知权限被拒绝，请在浏览器设置中开启')
      pushSetting.value.orderPushDesktop = false
    }
  }

  await handlePushSettingChange()
}

const connectWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/admin-api/ws`

  try {
    websocket.value = new WebSocket(wsUrl)

    websocket.value.onopen = () => {
      // console.log('WebSocket连接成功')
    }

    websocket.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'order-status-change') {
          handleOrderStatusPush(data)
        }
      } catch (e) {
        // console.error('解析WebSocket消息失败:', e)
      }
    }

    websocket.value.onerror = (error) => {
      // console.error('WebSocket错误:', error)
    }

    websocket.value.onclose = () => {
      // console.log('WebSocket连接关闭，3秒后重连...')
      setTimeout(connectWebSocket, 3000)
    }
  } catch (e) {
    // console.warn('WebSocket连接失败（如未配置WebSocket服务可忽略）:', e)
  }
}

const disconnectWebSocket = () => {
  if (websocket.value) {
    websocket.value.close()
    websocket.value = null
  }
}

const handleOrderStatusPush = (data: any) => {
  const statusNames: Record<string, string> = {
    '1': '已支付',
    '2': '已接单',
    '3': '已拣货',
    '4': '已打包',
    '5': '已发货',
    '6': '交易成功',
    '-1': '交易关闭'
  }

  const newStatusName = statusNames[String(data.newStatus)] || '未知状态'

  ElNotification({
    title: '订单状态变更',
    message: `订单 ${data.orderId} 状态已变更为 ${newStatusName}`,
    type: 'info',
    duration: 5000
  })

  if (data.desktopEnabled && 'Notification' in window && Notification.permission === 'granted') {
    new Notification('订单状态变更', {
      body: `订单 ${data.orderId} 状态已变更为 ${newStatusName}`,
      icon: '/favicon.ico'
    })
  }

  getSyncLogs()
}

const getStatusName = (status: number | string): string => {
  const map: Record<string, string> = {
    '1': '已支付',
    '2': '已接单',
    '3': '已拣货',
    '4': '已打包',
    '5': '已发货',
    '6': '交易成功',
    '-1': '交易关闭'
  }
  return map[String(status)] || `状态${status}`
}

const getStatusTagType = (status: number): string => {
  if (status === 6) return 'success'
  if (status === -1) return 'info'
  if (status >= 3) return 'warning'
  return ''
}

const checkOrderAlerts = async () => {
  try {
    const res = await getUnshownAlerts()
    const data = (res as any)?.data || res
    if (data && data.length > 0) {
      const hasCritical = data.some((item: OrderTrackingAlertVO) => item.alertLevel === 'CRITICAL')
      alertList.value = data
      alertDialogType.value = hasCritical ? 'critical' : 'warning'
      alertDialogVisible.value = true
      markAllAlertsAsShown()
    }
  } catch {
    // 静默处理
  }
}

const startAlertPolling = () => {
  stopAlertPolling()
  alertPollingTimer.value = setInterval(() => {
    if (!alertDialogVisible.value) {
      checkOrderAlerts()
    }
  }, 60000)
}

const stopAlertPolling = () => {
  if (alertPollingTimer.value) {
    clearInterval(alertPollingTimer.value)
    alertPollingTimer.value = null
  }
}

const handleDismissAlerts = async () => {
  alertDialogVisible.value = false
  alertList.value = []
}

onMounted(async () => {
  loadStoreList()
  getSyncLogs()
  loadScheduleConfig()
  checkBatchSyncStatusOnMount()
  await loadPushSetting()
  connectWebSocket()
  startAlertPolling()
  checkOrderAlerts()
})

onUnmounted(() => {
  stopPolling()
  stopBatchSyncPolling()
  stopAlertPolling()
  disconnectWebSocket()
})
</script>

<style lang="scss" scoped>
.app-container {
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
  padding: 24px;
  position: relative;
}

.pull-card {
  background: white;
  border-radius: 16px;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.05),
    0 1px 3px rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(226, 232, 240, 0.6);
  margin-bottom: 20px;
  overflow: hidden;

  .pull-card-header {
    padding: 16px 24px;
    border-bottom: 1px solid rgba(226, 232, 240, 0.6);
    background: #f8fafc;
    display: flex;
    align-items: center;

    .header-title {
      font-size: 16px;
      font-weight: 700;
      color: #1e293b;
    }
  }

  .pull-card-body {
    padding: 20px 24px;

    .sync-tabs {
      :deep(.el-tabs__header) {
        margin-bottom: 16px;
      }

      :deep(.el-tabs__item) {
        font-size: 15px;
        font-weight: 600;
        color: #64748b;

        &.is-active {
          color: #6366f1;
        }
      }

      :deep(.el-tabs__active-bar) {
        background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
      }
    }

    .bill-sync-card {
      .bill-header {
        margin-bottom: 16px;

        .bill-header-title {
          font-size: 14px;
          font-weight: 600;
          color: #1e293b;
        }
      }

      .bill-settings {
        .setting-row {
          display: flex;
          align-items: center;
          margin-bottom: 16px;
          gap: 16px;
          flex-wrap: wrap;

          .setting-group {
            display: flex;
            align-items: center;
            gap: 8px;

            .setting-label {
              font-size: 13px;
              color: #64748b;
              font-weight: 500;
              white-space: nowrap;
            }
          }

          .btn-group {
            margin-left: auto;
          }
        }

        .bill-sync-tip {
          margin-top: 12px;
          font-size: 12px;
          color: #94a3b8;
          padding: 8px 12px;
          background: #f8fafc;
          border-radius: 6px;
          line-height: 1.6;
        }
      }
    }

    .bill-mode-radio {
      :deep(.el-radio-button__inner) {
        padding: 8px 16px;
        font-size: 14px;
        background: #f8fafc;
        border-color: #e2e8f0;
        color: #64748b;
        transition: all 0.2s ease;
      }

      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
        border-color: #6366f1;
        color: #fff;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
      }
    }

    .bill-store-select {
      width: 240px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #6366f1;
        }
      }
    }

    .bill-date-picker {
      width: 160px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #0ea5e9;
        }
      }
    }

    .settings-bar {
      display: flex;
      align-items: center;
      gap: 32px;
      padding: 12px 16px;
      background: #f8fafc;
      border-radius: 8px;
      margin-bottom: 16px;
      flex-wrap: wrap;

      .setting-group {
        display: flex;
        align-items: center;
        gap: 8px;

        .setting-label {
          font-size: 12px;
          color: #64748b;
          font-weight: 500;
          white-space: nowrap;
        }

        .setting-disabled-text {
          color: #cbd5e1;
          font-size: 14px;
        }
      }
    }

    .detail-sections {
      margin-top: 8px;

      .schedule-section,
      .push-section {
        padding: 12px 16px;
        background: #fff;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        margin-bottom: 12px;
      }

      .push-section .form-row {
        margin-bottom: 8px;
      }

      .push-section .form-row:last-child {
        margin-bottom: 0;
      }
    }

    .form-row {
      display: flex;
      align-items: center;
      margin-bottom: 20px;
      gap: 16px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .form-item {
      display: flex;
      align-items: center;
      gap: 12px;

      .form-label {
        font-size: 13px;
        color: #64748b;
        white-space: nowrap;
        font-weight: 500;
        min-width: 70px;
      }
    }

    .mode-radio {
      :deep(.el-radio-button__inner) {
        padding: 8px 16px;
        font-size: 14px;
        background: #f8fafc;
        border-color: #e2e8f0;
        color: #64748b;
        transition: all 0.2s ease;
      }

      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
        border-color: #6366f1;
        color: #fff;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
      }
    }

    .date-radio {
      :deep(.el-radio-button__inner) {
        padding: 6px 14px;
        font-size: 13px;
        background: #f8fafc;
        border-color: #e2e8f0;
        color: #64748b;
        transition: all 0.2s ease;
      }

      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
        border-color: #0ea5e9;
        color: #fff;
        box-shadow: 0 4px 12px rgba(14, 165, 233, 0.35);
      }
    }

    .store-select {
      width: 240px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #6366f1;
        }
      }

      :deep(.el-input__inner) {
        color: #334155;
      }
    }

    .date-picker {
      width: 160px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #0ea5e9;
        }
      }
    }

    .schedule-mode-radio {
      :deep(.el-radio-button__inner) {
        padding: 6px 14px;
        font-size: 13px;
        background: #f8fafc;
        border-color: #e2e8f0;
        color: #64748b;
        transition: all 0.2s ease;
      }

      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
        border-color: #8b5cf6;
        color: #fff;
        box-shadow: 0 4px 12px rgba(139, 92, 246, 0.35);
      }
    }

    .time-picker {
      width: 140px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #8b5cf6;
        }
      }
    }

    .time-points-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 8px;

      .time-tag {
        margin-right: 0;
      }
    }

    .day-mode-form-item {
      flex-direction: column;
      align-items: flex-start;

      .day-selector {
        display: flex;
        flex-direction: column;
        gap: 10px;
        width: 100%;
        max-width: 560px;

        .day-actions {
          display: flex;
          align-items: center;
          gap: 8px;

          .selected-count {
            font-size: 12px;
            color: #64748b;
            margin-left: 4px;
          }
        }

        .day-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;

          .day-chip {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 56px;
            padding: 6px 12px;
            border-radius: 20px;
            background: #f1f5f9;
            color: #475569;
            font-size: 13px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.15s ease;
            user-select: none;
            border: 1px solid transparent;

            &:hover {
              background: #e2e8f0;
              transform: translateY(-1px);
            }

            &.selected {
              background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
              color: #fff;
              border-color: #8b5cf6;
              box-shadow: 0 2px 8px rgba(139, 92, 246, 0.3);
            }
          }
        }
      }
    }

    .month-day-picker {
      width: 260px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #8b5cf6;
        }
      }
    }

    :deep(.el-picker__popper) {
      .el-date-picker {
        .el-picker-panel__content {
          tr td .el-date-table-cell__text {
            border-radius: 4px;
          }

          tr td.in-range .el-date-table-cell__text {
            background: rgba(59, 130, 246, 0.1);
          }

          tr td.selected .el-date-table-cell__text {
            background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
            color: #fff !important;
            font-weight: 700;
            box-shadow: 0 2px 6px rgba(59, 130, 246, 0.4);

            &::after {
              content: '✔';
              position: absolute;
              top: -2px;
              right: 1px;
              font-size: 10px;
              color: #fff;
              opacity: 0.9;
            }
          }

          tr td.today .el-date-table-cell__text {
            border: 1px solid #3b82f6;
            color: #3b82f6;
          }
        }
      }
    }

    .selected-days-preview {
      margin-top: 8px;
      font-size: 12px;
      color: #64748b;
      line-height: 1.6;
      max-width: 400px;
      word-break: break-all;
    }

    .week-select {
      width: 300px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #8b5cf6;
        }
      }
    }

    .btn-item {
      margin-top: 8px;

      .el-button {
        min-width: 200px;
        background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
        border: none;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
        transition: all 0.2s ease;
        border-radius: 8px;
        font-size: 15px;
        font-weight: 600;

        &:hover:not(:disabled) {
          transform: translateY(-1px);
          box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
        }

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      }

      .el-button--success {
        min-width: 140px;
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);

        &:hover:not(:disabled) {
          box-shadow: 0 6px 20px rgba(16, 185, 129, 0.45);
        }
      }
    }

    .interval-select {
      width: 160px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #8b5cf6;
        }
      }
    }

    .cron-text {
      font-size: 13px;
      color: #64748b;
      font-family: 'Courier New', monospace;
      background: #f1f5f9;
      padding: 4px 10px;
      border-radius: 6px;
      border: 1px solid #e2e8f0;
    }

    .schedule-tip {
      margin-top: 8px;
      font-size: 12px;
      color: #94a3b8;
      line-height: 1.6;
    }

    .push-status-select {
      width: 260px;

      :deep(.el-input__wrapper) {
        background: #f8fafc;
        border-color: #e2e8f0;
        box-shadow: none;
        border-radius: 8px;

        &:hover,
        &:focus-within {
          border-color: #8b5cf6;
        }
      }
    }

    .push-status-tip {
      margin-top: 8px;
      font-size: 12px;
      color: #94a3b8;
      line-height: 1.6;
    }
  }

  :deep(.el-divider) {
    margin: 20px 0 16px;

    .divider-text {
      font-size: 13px;
      color: #94a3b8;
      font-weight: 500;
    }
  }

  :deep(.el-switch) {
    --el-switch-on-color: #10b981;
  }
}

.table-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 1px 2px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.section-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);

  .section-title {
    font-size: 16px;
    font-weight: 700;
    color: #1e293b;
    display: flex;
    align-items: center;
    gap: 10px;

    &::before {
      content: '';
      width: 4px;
      height: 20px;
      background: linear-gradient(180deg, #6366f1, #4f46e5);
      border-radius: 2px;
    }
  }
}

.text-success {
  color: #10b981;
  font-weight: 600;
}

.text-danger {
  color: #ef4444;
  font-weight: 600;
}

.error-msg {
  color: #ef4444;
  cursor: pointer;
  transition: color 0.2s ease;

  &:hover {
    color: #dc2626;
    text-decoration: underline;
  }
}

.error-codes-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;

  .error-code-tag {
    margin: 0;
  }
}

.el-table {
  font-size: 14px;

  :deep(.el-table__header-wrapper) {
    .el-table__header th {
      background-color: #f8fafc !important;
      color: #334155;
      font-weight: 700;
      text-align: center;
      border-bottom: 1px solid #e2e8f0 !important;
    }
  }

  :deep(.el-table__body-wrapper) {
    .el-table__row td {
      background: white !important;
      border-bottom: 1px solid #f1f5f9 !important;
      color: #475569;
    }

    .el-table__row:hover {
      background: #f8fafc !important;
    }
  }
}

.el-dialog {
  :deep(.el-dialog__header) {
    padding: 18px 24px;
    border-bottom: 1px solid #e2e8f0;
  }

  :deep(.el-dialog__title) {
    font-size: 17px;
    font-weight: 700;
    color: #1e293b;
  }

  :deep(.el-dialog__body) {
    padding: 24px;
    max-height: 60vh;
    overflow-y: auto;
    background: #f8fafc;
  }

  :deep(.el-dialog__footer) {
    padding: 14px 24px;
    border-top: 1px solid #e2e8f0;
  }
}

.error-detail {
  pre {
    white-space: pre-wrap;
    word-wrap: break-word;
    font-size: 14px;
    line-height: 1.6;
    color: #ef4444;
    padding: 16px;
    background: #fef2f2;
    border-radius: 8px;
    border: 1px solid #fecaca;
  }
}

.sync-progress-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);

  .sync-progress-container {
    background: white;
    border-radius: 16px;
    padding: 28px;
    width: 520px;
    max-width: 90vw;
    max-height: 80vh;
    overflow-y: auto;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
    animation: slideUp 0.3s ease;

    .sync-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 24px;

      .sync-header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        .sync-icon {
          font-size: 28px;
        }

        .sync-title {
          font-size: 18px;
          font-weight: 700;
          color: #1e293b;
        }
      }
    }

    .sync-detail {
      .sync-progress-bar {
        margin-bottom: 20px;
      }

      .sync-stats {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
        padding: 16px;
        background: #f8fafc;
        border-radius: 12px;
        margin-bottom: 16px;

        .stat-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 4px;
          padding: 10px;
          background: white;
          border-radius: 8px;

          .stat-label {
            font-size: 12px;
            color: #64748b;
          }

          .stat-value {
            font-size: 20px;
            font-weight: 700;
            color: #1e293b;
          }

          &.success {
            .stat-value {
              color: #10b981;
            }
          }

          &.failed {
            .stat-value {
              color: #ef4444;
            }
          }
        }
      }

      .syncing-info {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 16px;
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        border-radius: 8px;
        font-size: 13px;
        color: #1e40af;
        line-height: 1.6;

        .syncing-icon {
          animation: spin 1s linear infinite;
        }
      }
    }

    .sync-complete {
      .summary-stats {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 12px;
        padding: 0;
        background: transparent;
        border-radius: 0;
        margin-bottom: 0;

        .stat-card-item {
          .stat-card-inner {
            padding: 16px 12px;
            background: #f8fafc;
            border-radius: 12px;
            text-align: center;
            transition: transform 0.2s ease;

            &:hover {
              transform: translateY(-2px);
            }

            &.success-card {
              background: #f0fdf4;
              border: 1px solid #bbf7d0;
            }

            &.failed-card {
              background: #fef2f2;
              border: 1px solid #fecaca;
            }

            .stat-card-label {
              font-size: 12px;
              color: #64748b;
              margin-bottom: 8px;
            }

            .stat-card-value {
              font-size: 24px;
              font-weight: 700;
              color: #1e293b;

              &.success-value {
                color: #10b981;
              }

              &.failed-value {
                color: #ef4444;
              }
            }
          }
        }
      }

      .failed-stores-section {
        margin-top: 20px;

        .failed-section-title {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 14px;
          font-weight: 600;
          color: #ef4444;
          margin-bottom: 12px;

          .el-icon {
            font-size: 16px;
          }
        }

        .failed-store-list {
          max-height: 250px;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: 8px;

          .failed-store-item {
            padding: 12px;
            background: #fef2f2;
            border: 1px solid #fecaca;
            border-radius: 8px;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            align-items: center;
            font-size: 13px;

            .failed-store-name {
              color: #dc2626;
              font-weight: 600;
            }

            .failed-store-id {
              color: #94a3b8;
            }

            .failed-store-error {
              color: #ef4444;
              font-size: 12px;
              flex-basis: 100%;
              padding-left: 4px;
            }
          }
        }
      }
    }
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

:deep(.el-pagination) {
  margin-top: 24px;
  justify-content: center;

  .el-pager li {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
    border-radius: 8px;

    &:hover {
      color: #6366f1;
      border-color: #6366f1;
    }

    &.is-active {
      background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
      border-color: #6366f1;
      color: #fff;
    }
  }

  button {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
  }
}

@media (max-width: 768px) {
  .pull-card .pull-card-body .form-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .pull-progress-card .progress-summary {
    grid-template-columns: repeat(2, 1fr);
  }
}

.alert-detail {
  .alert-summary-text {
    font-size: 14px;
    color: #64748b;
    margin-bottom: 16px;
    padding: 12px;
    background: #f8fafc;
    border-radius: 8px;
    border-left: 4px solid #f59e0b;
  }

  .alert-table {
    :deep(.el-table__header-wrapper) {
      .el-table__header th {
        background-color: #f8fafc !important;
        color: #334155;
        font-weight: 700;
      }
    }
  }

  .days-critical {
    color: #dc2626;
    font-weight: 700;
    font-size: 14px;
  }

  .days-warning {
    color: #f59e0b;
    font-weight: 600;
  }
}

:deep(.critical-alert-dialog) {
  .el-dialog__header {
    background: linear-gradient(135deg, #dc2626, #ef4444);
    padding: 18px 24px;
    border-bottom: none;

    .el-dialog__title {
      color: #fff !important;
      font-size: 18px;
      font-weight: 700;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: #fff;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }
}

:deep(.warning-alert-dialog) {
  .el-dialog__header {
    background: linear-gradient(135deg, #f59e0b, #fbbf24);
    padding: 18px 24px;
    border-bottom: none;

    .el-dialog__title {
      color: #fff !important;
      font-size: 18px;
      font-weight: 700;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: #fff;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }
}
</style>
