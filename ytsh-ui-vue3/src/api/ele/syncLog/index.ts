import request from '@/config/axios'

export interface EleSyncLogReqVO {
  platformStoreId?: string
  merchantCode?: string
  erpStoreCode?: string
  storeName?: string
  status?: number
  startTime?: number
  endTime?: number
  pageNo?: number
  pageSize?: number
}

export interface EleSyncLogRespVO {
  id?: number
  platformStoreId?: string
  merchantCode?: string
  erpStoreCode?: string
  storeName?: string
  lastSyncTime?: number
  syncStartTime?: string | number
  syncEndTime?: string | number
  syncTime?: number
  syncCount?: number
  successCount?: number
  failCount?: number
  status?: number
  errorMsg?: string
  createTime?: string
}

export interface EleSyncLogPageRespVO {
  list?: EleSyncLogRespVO[]
  total?: number
}

export interface EleSyncStatsRespVO {
  platformStoreId?: string
  storeName?: string
  totalSyncCount?: number
  successCount?: number
  failCount?: number
  successRate?: number
  avgDuration?: number
  lastSyncTime?: number
}

export const getSyncLogPage = async (params: EleSyncLogReqVO) => {
  return await request.get<EleSyncLogPageRespVO>({ url: '/ele/sync-log/page', params })
}

export const getSyncLog = async (params: { id: number }) => {
  return await request.get<EleSyncLogRespVO>({ url: `/ele/sync-log/${params.id}` })
}

export const getStoreSyncStats = async (platformStoreId: string) => {
  return await request.get<EleSyncStatsRespVO>({ url: `/ele/sync-log/stats/${platformStoreId}` })
}