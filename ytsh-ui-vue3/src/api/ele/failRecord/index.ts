import request from '@/config/axios'

export interface EleFailRecordReqVO {
  storeId?: number
  orderId?: string
  channelOrderId?: string
  bizType?: string
  failStage?: string
  processStatus?: string
  startTime?: number
  endTime?: number
  pageNo?: number
  pageSize?: number
}

export interface EleFailRecordRespVO {
  id?: number
  platformType?: string
  storeId?: number
  orderId?: string
  channelOrderId?: string
  bizType?: string
  failStage?: string
  failCode?: string
  failMessage?: string
  requestParam?: string
  responseContent?: string
  retryCount?: number
  maxRetryCount?: number
  processStatus?: string
  taskId?: string
  remark?: string
  platformStoreId?: string
  merchantCode?: string
  erpStoreCode?: string
  createTime?: string
  updateTime?: string
}

export interface EleFailRecordPageRespVO {
  list?: EleFailRecordRespVO[]
  total?: number
}

export interface FailCountRespVO {
  totalUnhandleCount?: number
  pendingRetryCount?: number
  failedCount?: number
}

export const getFailRecordPage = async (params: EleFailRecordReqVO) => {
  return await request.get<EleFailRecordPageRespVO>({ url: '/ele/order/fail-record/page', params })
}

export const getUnhandledFailCount = async () => {
  return await request.get<FailCountRespVO>({ url: '/ele/order/fail-record/unhandled-count' })
}

export const retryFailRecord = async (id: number) => {
  return await request.post<boolean>({ url: '/ele/order/fail-record/retry', params: { id } })
}

export const retryFailRecordWithOverwrite = async (id: number) => {
  return await request.post<boolean>({ url: '/ele/order/fail-record/retry-with-overwrite', params: { id } })
}

export const batchRetryFailRecord = async (ids: number[]) => {
  return await request.post<boolean>({ url: '/ele/order/fail-record/batch-retry', params: { ids: ids.join(',') } })
}

export const getAllFailedIds = async () => {
  return await request.get<number[]>({ url: '/ele/order/fail-record/all-failed-ids' })
}

export const retryFailRecordsByTimeRange = async (startTime: number, endTime: number, overwrite: boolean = false) => {
  return await request.post<number>({ 
    url: '/ele/order/fail-record/retry-by-time-range', 
    params: { startTime, endTime, overwrite } 
  })
}