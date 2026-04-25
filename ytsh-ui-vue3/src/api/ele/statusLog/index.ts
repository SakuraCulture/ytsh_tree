import request from '@/config/axios'

export interface EleStatusLogReqVO {
  orderId?: string
  channelOrderId?: string
  storeId?: number
  changeSource?: string
  startTime?: number
  endTime?: number
  pageNo?: number
  pageSize?: number
}

export interface EleStatusLogRespVO {
  id?: number
  platformType?: string
  orderId?: string
  channelOrderId?: string
  storeId?: number
  beforeOrderStatus?: string
  afterOrderStatus?: string
  beforeDeliveryStatus?: string
  afterDeliveryStatus?: string
  beforePlatformStatus?: string
  afterPlatformStatus?: string
  changeSource?: string
  changeReason?: string
  snapshotContent?: string
  createTime?: string
}

export interface EleStatusLogPageRespVO {
  list?: EleStatusLogRespVO[]
  total?: number
}

export const getStatusLogPage = async (params: EleStatusLogReqVO) => {
  return await request.get<EleStatusLogPageRespVO>({ url: '/ele/status-log/page', params })
}

export const getStatusLog = async (params: { id: number }) => {
  return await request.get<EleStatusLogRespVO>({ url: `/ele/status-log/${params.id}` })
}