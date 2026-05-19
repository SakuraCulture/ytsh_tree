export function buildOrderDetailParams(order) {
  return {
    orderId: order?.orderId || order?.orderNo || order?.id || '',
    platformStoreId: order?.platformStoreId || undefined,
    merchantCode: order?.merchantCode || undefined,
    erpStoreCode: order?.erpStoreCode || undefined
  }
}

export function normalizeBillInfoResponse(billInfo) {
  if (!billInfo || typeof billInfo !== 'object') {
    return null
  }
  const hasSummary =
    billInfo.orderId !== undefined ||
    billInfo.totalBillAmount !== undefined ||
    billInfo.totalStatus !== undefined
  const billDetails = Array.isArray(billInfo.billDetails) ? billInfo.billDetails : []
  if (!hasSummary && billDetails.length === 0) {
    return null
  }
  return {
    ...billInfo,
    billDetails
  }
}

export async function loadOrderDetail(detailFetcher, order, normalizeOrder) {
  const params = buildOrderDetailParams(order)
  if (!params.orderId) {
    return order
  }
  const detail = await detailFetcher(params)
  const normalized = normalizeOrder({
    ...order,
    ...(detail && typeof detail === 'object' ? detail : {}),
    billInfo: order?.billInfo ?? null
  })
  return {
    ...order,
    ...normalized,
    billInfo: order?.billInfo ?? normalized?.billInfo ?? null
  }
}
