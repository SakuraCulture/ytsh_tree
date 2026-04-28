import request from '@/config/axios'

export interface StoreGoodsQueryReqVO {
  merchantCode: string
  erpStoreCode: string
  skuCodeList?: string[]
  pageNo?: number
  pageSize?: number
}

export interface StoreGoodsTestModeRespVO {
  configKey?: string
  enabled?: boolean
}

export interface StoreGoodsQuerySkuItemVO {
  skuCode?: string
  subSkuCode?: string
  specification?: string
  salePrice?: number
  status?: number
}

export interface StoreGoodsQueryGoodsItemVO {
  merchantCode?: string
  storeCode?: string
  title?: string
  spuCode?: string
  mainPic?: string
  subPics?: string[]
  skuList?: StoreGoodsQuerySkuItemVO[]
}

export interface StoreGoodsQueryRespVO {
  merchantCode?: string
  storeCode?: string
  page?: number
  total?: number
  pageSize?: number
  goodsList?: StoreGoodsQueryGoodsItemVO[]
}

export interface StoreGoodsSyncLogReqVO {
  platformStoreId?: string
  erpStoreCode?: string
  skuCode?: string
  success?: boolean
  pageNo?: number
  pageSize?: number
}

export interface StoreGoodsSyncLogRespVO {
  id?: number
  traceId?: string
  ticket?: string
  apiCode?: string
  apiName?: string
  merchantCode?: string
  erpStoreCode?: string
  platformId?: number
  storeId?: string
  platformStoreId?: string
  skuCode?: string
  subSkuCode?: string
  operationType?: string
  pageNo?: number
  pageSize?: number
  dataCount?: number
  success?: boolean
  resultCode?: string
  resultMsg?: string
  durationMs?: number
  requestBody?: string
  responseBody?: string
  createTime?: string
}

export interface StoreGoodsSyncLogPageRespVO {
  list?: StoreGoodsSyncLogRespVO[]
  total?: number
}

export interface StoreGoodsGovernancePoolReqVO {
  merchantCode?: string
  erpStoreCode?: string
  platformStoreId?: string
  skuCode?: string
  spuCode?: string
  reasonCode?: string
  processStatus?: string
  pageNo?: number
  pageSize?: number
}

export interface StoreGoodsGovernancePoolRespVO {
  id?: number
  merchantCode?: string
  erpStoreCode?: string
  platformId?: number
  storeId?: string
  platformStoreId?: string
  skuCode?: string
  subSkuCode?: string
  spuCode?: string
  goodsLevel?: string
  operationType?: string
  reasonCode?: string
  reasonMsg?: string
  processStatus?: string
  rawPayload?: string
  remark?: string
  createTime?: string
}

export interface StoreGoodsGovernancePoolPageRespVO {
  list?: StoreGoodsGovernancePoolRespVO[]
  total?: number
}

export interface StoreGoodsPageSyncResultVO {
  pageNo?: number
  pageSize?: number
  total?: number
  syncCount?: number
  successCount?: number
  failCount?: number
  governanceCount?: number
  shadowCount?: number
}

export interface StoreGoodsPreviewRowVO {
  rowKey: string
  title?: string
  spuCode?: string
  skuCode?: string
  subSkuCode?: string
  specification?: string
  salePrice?: number
  status?: number
}

export interface StoreGoodsShadowReqVO {
  merchantCode?: string
  erpStoreCode?: string
  platformStoreId?: string
  storeId?: string
  skuCode?: string
  title?: string
  matchStatus?: string
  pageNo?: number
  pageSize?: number
}

export interface StoreGoodsShadowRespVO {
  id?: number
  platformId?: number
  merchantCode?: string
  erpStoreCode?: string
  platformStoreId?: string
  storeId?: string
  spuCode?: string
  skuCode?: string
  subSkuCode?: string
  title?: string
  mainPic?: string
  specification?: string
  salePrice?: number
  posStatus?: string
  isActive?: number
  matchStatus?: string
  matchedProductSkuId?: string
  mergedStoreProductId?: string
  conflictReason?: string
  rawPayload?: string
  lastSyncTime?: string
  matchedTime?: string
  mergedTime?: string
  createTime?: string
}

export interface StoreGoodsShadowPageRespVO {
  list?: StoreGoodsShadowRespVO[]
  total?: number
}

export interface StoreGoodsFullSyncCurrentReqVO {
  merchantCode: string
  erpStoreCode: string
  testMode?: boolean
}

export interface StoreGoodsFullSyncAllOpenReqVO {
  testMode?: boolean
}

export interface StoreGoodsFullSyncTaskReqVO {
  taskNo?: string
  scope?: string
  status?: string
  merchantCode?: string
  erpStoreCode?: string
  pageNo?: number
  pageSize?: number
}

export interface StoreGoodsFullSyncTaskRespVO {
  id?: number
  taskNo?: string
  scope?: string
  merchantCode?: string
  erpStoreCode?: string
  testMode?: boolean
  status?: string
  totalStoreCount?: number
  finishedStoreCount?: number
  totalPageCount?: number
  finishedPageCount?: number
  totalSkuCount?: number
  successCount?: number
  failCount?: number
  governanceCount?: number
  errorMsg?: string
  startedAt?: string
  finishedAt?: string
  createTime?: string
}

export interface StoreGoodsFullSyncTaskPageRespVO {
  list?: StoreGoodsFullSyncTaskRespVO[]
  total?: number
}

export interface StoreGoodsFullSyncTaskStoreReqVO {
  status?: string
  erpStoreCode?: string
  storeId?: string
  pageNo?: number
  pageSize?: number
}

export interface StoreGoodsFullSyncTaskStoreRespVO {
  id?: number
  taskId?: number
  taskNo?: string
  storeId?: string
  storeName?: string
  merchantCode?: string
  erpStoreCode?: string
  platformStoreId?: string
  status?: string
  currentPage?: number
  totalPage?: number
  pageSize?: number
  totalSkuCount?: number
  successCount?: number
  failCount?: number
  governanceCount?: number
  retryCount?: number
  errorMsg?: string
  startedAt?: string
  finishedAt?: string
  createTime?: string
}

export interface StoreGoodsFullSyncTaskStorePageRespVO {
  list?: StoreGoodsFullSyncTaskStoreRespVO[]
  total?: number
}

export const getTestMode = async () => {
  return await request.get<StoreGoodsTestModeRespVO>({ url: '/ele/store-goods/test-mode' })
}

export const queryStoreGoods = async (data: StoreGoodsQueryReqVO) => {
  return await request.post<StoreGoodsQueryRespVO>({ url: '/ele/store-goods/query', data })
}

export const queryAndSyncStoreGoods = async (data: StoreGoodsQueryReqVO, testMode = false) => {
  return await request.post<StoreGoodsPageSyncResultVO>({
    url: '/ele/store-goods/query-sync',
    data,
    params: { testMode }
  })
}

export const getSyncLogPage = async (params: StoreGoodsSyncLogReqVO) => {
  return await request.get<StoreGoodsSyncLogPageRespVO>({
    url: '/ele/store-goods/sync-log/page',
    params
  })
}

export const getSyncLog = async (id: number) => {
  return await request.get<StoreGoodsSyncLogRespVO>({ url: `/ele/store-goods/sync-log/${id}` })
}

export const getGovernancePoolPage = async (params: StoreGoodsGovernancePoolReqVO) => {
  return await request.get<StoreGoodsGovernancePoolPageRespVO>({
    url: '/ele/store-goods/governance-pool/page',
    params
  })
}

export const getGovernancePool = async (id: number) => {
  return await request.get<StoreGoodsGovernancePoolRespVO>({
    url: `/ele/store-goods/governance-pool/${id}`
  })
}

export const markGovernancePoolProcessed = async (id: number) => {
  return await request.put<boolean>({ url: `/ele/store-goods/governance-pool/${id}/processed` })
}

export const markGovernancePoolIgnored = async (id: number) => {
  return await request.put<boolean>({ url: `/ele/store-goods/governance-pool/${id}/ignored` })
}

export const getShadowPage = async (params: StoreGoodsShadowReqVO) => {
  return await request.get<StoreGoodsShadowPageRespVO>({
    url: '/ele/store-goods/shadow/page',
    params
  })
}

export const getShadow = async (id: number) => {
  return await request.get<StoreGoodsShadowRespVO>({ url: `/ele/store-goods/shadow/${id}` })
}

export const ignoreShadow = async (id: number) => {
  return await request.put<boolean>({ url: `/ele/store-goods/shadow/${id}/ignored` })
}

export const mergeShadow = async (id: number, data: { matchedProductSkuId: string }) => {
  return await request.put<boolean>({
    url: `/ele/store-goods/shadow/${id}/merge`,
    data
  })
}

export const createCurrentStoreFullSync = async (data: StoreGoodsFullSyncCurrentReqVO) => {
  return await request.post<number>({ url: '/ele/store-goods/full-sync/current', data })
}

export const createAllOpenStoresFullSync = async (data?: StoreGoodsFullSyncAllOpenReqVO) => {
  return await request.post<number>({ url: '/ele/store-goods/full-sync/all-open', data })
}

export const getFullSyncTaskPage = async (params: StoreGoodsFullSyncTaskReqVO) => {
  return await request.get<StoreGoodsFullSyncTaskPageRespVO>({ url: '/ele/store-goods/full-sync/page', params })
}

const normalizeFullSyncTaskId = (id: number | string | undefined) => {
  const normalized = typeof id === 'number' ? id : Number(id)
  if (!Number.isInteger(normalized) || normalized <= 0) {
    throw new Error('任务ID无效，请刷新任务列表后重试')
  }
  return normalized
}

export const getFullSyncTask = async (id: number | string) => {
  const taskId = normalizeFullSyncTaskId(id)
  return await request.get<StoreGoodsFullSyncTaskRespVO>({ url: `/ele/store-goods/full-sync/${taskId}` })
}

export const getFullSyncTaskStores = async (id: number | string, params: StoreGoodsFullSyncTaskStoreReqVO) => {
  const taskId = normalizeFullSyncTaskId(id)
  return await request.get<StoreGoodsFullSyncTaskStorePageRespVO>({
    url: `/ele/store-goods/full-sync/${taskId}/stores`,
    params
  })
}

export const cancelFullSyncTask = async (id: number | string) => {
  const taskId = normalizeFullSyncTaskId(id)
  return await request.post<boolean>({ url: `/ele/store-goods/full-sync/${taskId}/cancel` })
}

export const retryFailedFullSyncTask = async (id: number | string) => {
  const taskId = normalizeFullSyncTaskId(id)
  return await request.post<boolean>({ url: `/ele/store-goods/full-sync/${taskId}/retry-failed` })
}
