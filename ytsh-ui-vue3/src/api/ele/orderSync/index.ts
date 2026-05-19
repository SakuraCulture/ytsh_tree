import request from '@/config/axios'

export interface SyncLogPageReqVO {
  platformStoreId?: string
  status?: number
  startTime?: number
  endTime?: number
  pageNo?: number
  pageSize?: number
}

export interface SyncLogItemVO {
  id?: number
  platformStoreId?: string
  merchantCode?: string
  erpStoreCode?: string
  storeName?: string
  syncCount?: number
  successCount?: number
  failCount?: number
  syncStartTime?: number
  syncEndTime?: number
  status?: number
  errorMsg?: string
  createTime?: number
}

export interface SyncLogPageRespVO {
  list?: SyncLogItemVO[]
  total?: number
}

export interface StoreSyncStatsVO {
  platformStoreId?: string
  storeName?: string
  totalSyncCount?: number
  successCount?: number
  failCount?: number
  successRate?: number
  avgDuration?: number
  lastSyncTime?: number
}

export interface SyncProgressVO {
  taskId?: string
  status?: string
  platformStoreId?: string
  merchantCode?: string
  erpStoreCode?: string
  startTime?: number
  endTime?: number
  errorMessage?: string
}

export interface BatchSyncProgressVO {
  isSyncing: boolean
  syncStatus: string
  totalStores: number
  completedStores: number
  successStores: number
  failedStores: number
  currentSyncingCount: number
  currentSyncingStores: string[]
  startTime: number
}

export interface StoreSyncProgressVO {
  batchId?: string
  platformStoreId?: string
  storeName?: string
  status?: string
  apiStatusCounts?: Record<number, number>
  savedStatusCounts?: Record<number, number>
  pageCounts?: Record<number, number>
  totalApiCount?: number
  totalSavedCount?: number
  discrepancyRate?: number
  reconciliationStatus?: string
  retryCount?: number
  pullErrors?: any[]
  saveErrors?: any[]
  reconciliationErrors?: any[]
  startTime?: number
  endTime?: number
  elapsedSeconds?: number
}

export interface SyncErrorDetailVO {
  syncLogId: number
  storeName: string
  syncStartTime: string
  pullError: { code: string; detail: any }
  saveError: { code: string; detail: any }
  reconciliationError: { code: string; detail: any }
  reconciliation: {
    expectedTotal: number
    actualTotal: number
    savedTotal: number
    discrepancyRate: number
    dataIntegrity: number
    retryCount: number
    apiStatusCounts: any
    savedStatusCounts: any
  }
}

export interface SyncScheduleConfigVO {
  exists: boolean
  enabled?: boolean
  scheduleType?: 'time' | 'dayOfMonth' | 'weekDay' | 'interval'
  cronExpression?: string
  timePoints?: string[]
  daysOfMonth?: number[]
  dayOfMonthTime?: string
  weekDays?: number[]
  weekDayTime?: string
  intervalStartTime?: string
  intervalHours?: number
  jobId?: number
  jobStatus?: number
}

export interface SyncScheduleConfigReqVO {
  enabled: boolean
  scheduleType: 'time' | 'dayOfMonth' | 'weekDay' | 'interval'
  cronExpression: string
  timePoints?: string[]
  daysOfMonth?: number[]
  dayOfMonthTime?: string
  weekDays?: number[]
  weekDayTime?: string
  intervalStartTime?: string
  intervalHours?: number
}

export const getSyncScheduleConfig = async () => {
  return await request.get<SyncScheduleConfigVO>({
    url: '/ele/order/sync/schedule-config'
  })
}

export const updateSyncScheduleConfig = async (data: SyncScheduleConfigReqVO) => {
  return await request.put<boolean>({
    url: '/ele/order/sync/schedule-config',
    data
  })
}

export const pullOrdersByRange = async (params: { startTime?: number; endTime?: number; platformStoreId?: string }) => {
  return await request.post<Map<string, any>>({
    url: '/ele/order-sync/sync-all',
    params
  })
}

export const triggerCompensation = async (data: {
  platformStoreId: string
  merchantCode: string
  erpStoreCode: string
  startTime: number
  endTime: number
}) => {
  return await request.post<{ success: boolean; message: string; compensatedCount: number }>({
    url: '/ele/order-sync/compensation/trigger',
    data
  })
}

export const getSyncLogPage = async (params: SyncLogPageReqVO) => {
  return await request.get<SyncLogPageRespVO>({
    url: '/ele/sync-log/page',
    params
  })
}

export const getStoreSyncStats = async (platformStoreId: string) => {
  return await request.get<StoreSyncStatsVO>({ url: `/ele/sync-log/stats/${platformStoreId}` })
}

export const getSyncProgress = async (taskId: string) => {
  return await request.get<SyncProgressVO>({ url: '/ele/order/sync/progress', params: { taskId } })
}

export const getBatchSyncProgress = async () => {
  return await request.get<BatchSyncProgressVO>({ url: '/ele/order/sync/batch-progress' })
}

export const getRedisSyncProgress = async (batchId: string, platformStoreId?: string) => {
  return await request.get<StoreSyncProgressVO>({
    url: '/ele/order/sync/redis-progress',
    params: { batchId, platformStoreId }
  })
}

export const getErrorDetail = async (syncLogId: number) => {
  return await request.get<SyncErrorDetailVO>({
    url: '/ele/order-sync/error-detail',
    params: { syncLogId }
  })
}
