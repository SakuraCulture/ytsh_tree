import request from '@/config/axios'

export const getFailLogList = async () => {
  return await request.get({ url: '/ele/bill-sync/fail-log-list' })
}

export const getBillInfo = async (orderId: string) => {
  return await request.get({ url: '/ele/bill-sync/bill-info', params: { orderId } })
}

export const getBillSummary = async (orderId: string) => {
  return await request.get({ url: '/ele/bill-sync/bill-summary', params: { orderId } })
}

export const syncBillsByDate = async (billDate: string) => {
  return await request.post({ url: '/ele/bill-sync/sync-by-date', params: { billDate } })
}

export const syncBillsByDateRange = async (startDate: string, endDate: string) => {
  return await request.post({ url: '/ele/bill-sync/sync-by-date-range', params: { startDate, endDate } })
}

export const syncBillByStore = async (merchantCode: string, storeCode: string, billDate: string) => {
  return await request.post({ url: '/ele/bill-sync/retry-by-store', params: { merchantCode, storeCode, billDate } })
}

export const retryByOrderId = async (orderId: string) => {
  return await request.post({ url: '/ele/bill-sync/retry-by-order', params: { orderId } })
}

export const retryByStore = async (merchantCode: string, storeCode: string, billDate: string) => {
  return await request.post({ url: '/ele/bill-sync/retry-by-store', params: { merchantCode, storeCode, billDate } })
}

export const retryAllPending = async () => {
  return await request.post({ url: '/ele/bill-sync/retry-all' })
}
