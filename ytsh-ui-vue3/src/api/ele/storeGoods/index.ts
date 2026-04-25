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
  return await request.post<number>({
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

export const createCurrentStoreFullSync = async (data: StoreGoodsFullSyncCurrentReqVO) => {
  return await request.post<number>({ url: '/ele/store-goods/full-sync/current', data })
}

export const createAllOpenStoresFullSync = async (data?: StoreGoodsFullSyncAllOpenReqVO) => {
  return await request.post<number>({ url: '/ele/store-goods/full-sync/all-open', data })
}

export const getFullSyncTaskPage = async (params: StoreGoodsFullSyncTaskReqVO) => {
  return await request.get<StoreGoodsFullSyncTaskPageRespVO>({ url: '/ele/store-goods/full-sync/page', params })
}

export const getFullSyncTask = async (id: number) => {
  return await request.get<StoreGoodsFullSyncTaskRespVO>({ url: `/ele/store-goods/full-sync/${id}` })
}

export const getFullSyncTaskStores = async (id: number, params: StoreGoodsFullSyncTaskStoreReqVO) => {
  return await request.get<StoreGoodsFullSyncTaskStorePageRespVO>({
    url: `/ele/store-goods/full-sync/${id}/stores`,
    params
  })
}

export const cancelFullSyncTask = async (id: number) => {
  return await request.post<boolean>({ url: `/ele/store-goods/full-sync/${id}/cancel` })
}

export const retryFailedFullSyncTask = async (id: number) => {
  return await request.post<boolean>({ url: `/ele/store-goods/full-sync/${id}/retry-failed` })
}
