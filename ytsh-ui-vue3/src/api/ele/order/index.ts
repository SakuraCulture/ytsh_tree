import request from '@/config/axios'

export interface EleOrderListReqVO {
  platformStoreId?: string
  merchantCode?: string
  erpStoreCode?: string
  status?: number
  startTime?: number
  endTime?: number
}

export interface EleOrderItemVO {
  orderId?: string
  status?: number
  createTime?: number
  payTime?: number
  channelSourceName?: string
  buyerName?: string
  buyerPhone?: string
  buyerAddress?: string
  deliveryName?: string
  deliveryPhone?: string
  deliveryPlatform?: string
  deliveryType?: number
  deliveryStatus?: number
  totalFee?: number
  payFee?: number
  discountFee?: number
  deliveryFee?: number
  postFee?: number
  packageFee?: number
  platformCommissionFee?: number
  remark?: string
  channelSourceId?: string
  channelOrderId?: string
  channelType?: string
  storeCode?: string
  erpStoreCode?: string
  longitude?: string
  latitude?: string
  subOrders?: any[]
  discounts?: any[]
}

export interface EleOrderListRespVO {
  total?: number
  scrollId?: string | null
  orderList?: EleOrderItemVO[]
}

export interface EleOrderPageReqVO {
  platformStoreId?: string
  storeId?: string
  status?: number
  startTime?: number
  endTime?: number
  pageNo?: number
  pageSize?: number
}

export interface EleOrderPageRespVO {
  list?: EleOrderItemVO[]
  total?: number
}

export const listOrder = async (params: EleOrderListReqVO) => {
  return await request.get<EleOrderListRespVO>({ url: '/ele/order/list/remote', params })
}

export const getOrderPage = async (params: EleOrderPageReqVO) => {
  return await request.get<EleOrderPageRespVO>({ url: '/ele/order/list', params })
}

export const getOrderDetail = async (params: { orderId: string }) => {
  return await request.get<EleOrderItemVO>({ url: '/ele/order/detail', params })
}

export const getOrderStatusCounts = async (params: {
  platformStoreId?: string
  startTime: number
  endTime: number
}): Promise<Record<number, number>> => {
  return await request.get({ url: '/ele/order/status-counts', params })
}
