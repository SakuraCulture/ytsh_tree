import request from '@/config/axios'

export interface OrderPushSettingVO {
  orderPushEnabled: boolean
  orderPushTypes: string
  orderPushSound: boolean
  orderPushDesktop: boolean
}

export function getOrderPushSetting(): Promise<OrderPushSettingVO> {
  return request.get({ url: '/ele/order/push-setting' })
}

export function updateOrderPushSetting(data: OrderPushSettingVO): Promise<boolean> {
  return request.post({ url: '/ele/order/update-push-setting', data })
}
