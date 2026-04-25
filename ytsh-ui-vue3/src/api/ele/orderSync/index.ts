import request from '@/config/axios'

export interface SyncRangeReqVO {
  startTime: number
  endTime: number
}

export interface PullSingleStoreReqVO {
  platformStoreId: string
  startTime: number
  endTime: number
}

export interface SyncResultVO {
  totalCount: number
  successCount: number
  failCount: number
  elapsedSeconds: number
  completed: boolean
  failedStores: string[]
}

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

export interface SyncScheduleConfigVO {
  exists: boolean
  enabled?: boolean
  intervalMinutes?: number | null
  cronExpression?: string
  jobId?: number
  jobStatus?: number
}

export const getSyncScheduleConfig = async () => {
  return await request.get<SyncScheduleConfigVO>({
    url: '/ele/order/sync/schedule-config'
  })
}

export const updateSyncScheduleConfig = async (data: {
  intervalMinutes: number
  enabled: boolean
}) => {
  return await request.put<boolean>({
    url: '/ele/order/sync/schedule-config',
    params: data
  })
}

export const pullOrdersByRange = async (params: SyncRangeReqVO) => {
  return await request.post<SyncResultVO>({
    url: '/ele/order/sync/all',
    params
  })
}

export const pullSingleStore = async (params: PullSingleStoreReqVO) => {
  return await request.post<SyncResultVO>({
    url: '/ele/order/sync/submit',
    params
  })
}

export const pullAllStores = async (params?: { startTime?: number; endTime?: number }) => {
  return await request.post<boolean>({
    url: '/ele/order/sync/all',
    params
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
