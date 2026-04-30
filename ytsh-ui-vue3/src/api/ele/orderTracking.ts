import request from '@/config/axios'

export interface OrderTrackingAlertVO {
  id: number
  orderId: string
  platformStoreId: string
  erpStoreCode: string
  orderStatus: number
  alertLevel: string
  createTime: number
  createTimeStr: string
  daysElapsed: number
  remark: string
}

export const getUnshownAlerts = () => {
  return request.get({ url: '/ele/order-tracking/alert/list' })
}

export const markAllAlertsAsShown = () => {
  return request.post({ url: '/ele/order-tracking/alert/mark-all-shown' })
}
