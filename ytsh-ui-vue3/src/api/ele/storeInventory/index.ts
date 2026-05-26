import request from '@/config/axios'

export interface EleStoreInventoryQueryReqVO {
  platformStoreId?: string
  merchantCode?: string
  erpStoreCode?: string
  skuCodes?: string[]
}

export interface EleStoreInventoryRowVO {
  skuCode?: string
  subSkuCode?: string
  availableForSale?: number
  reservedAmount?: number
  physicalStockTotalAmount?: number
  physicalStockAvailableAmount?: number
  physicalStockOccupiedAmount?: number
  physicalStockIntransitAmount?: number
  ownerCode?: string
  ownerName?: string
  persistStatus?: string
  reasonCode?: string
  lastQueryTime?: string
}

export interface EleStoreInventoryQueryRespVO {
  platformStoreId?: string
  merchantCode?: string
  erpStoreCode?: string
  requestSkuCount?: number
  responseRowCount?: number
  formalSuccessCount?: number
  shadowSuccessCount?: number
  governanceCount?: number
  missingRowCount?: number
  failureCount?: number
  status?: string
  errorDetails?: string[]
  inventoryRows?: EleStoreInventoryRowVO[]
}

export interface EleStoreInventoryImportFailureVO {
  rowNo?: number
  skuCode?: string
  message?: string
}

export interface EleStoreInventoryImportRespVO {
  formalSuccessCount?: number
  shadowSuccessCount?: number
  governanceCount?: number
  failureCount?: number
  failureList?: EleStoreInventoryImportFailureVO[]
}

export interface EleStoreInventoryBatchTaskPageReqVO {
  taskNo?: string
  sourceType?: string
  scope?: string
  status?: string
  pageNo?: number
  pageSize?: number
}

export interface EleStoreInventoryBatchTaskRespVO {
  id?: number
  taskNo?: string
  sourceType?: string
  scope?: string
  status?: string
  totalStoreCount?: number
  finishedStoreCount?: number
  totalBatchCount?: number
  finishedBatchCount?: number
  totalSkuCount?: number
  formalSuccessCount?: number
  shadowSuccessCount?: number
  governanceCount?: number
  failureCount?: number
  errorMsg?: string
  startedAt?: string
  finishedAt?: string
  createTime?: string
}

export interface EleStoreInventoryBatchTaskPageRespVO {
  list?: EleStoreInventoryBatchTaskRespVO[]
  total?: number
}

export interface EleStoreInventoryBatchTaskStorePageReqVO {
  status?: string
  erpStoreCode?: string
  storeId?: string
  pageNo?: number
  pageSize?: number
}

export interface EleStoreInventoryBatchTaskStoreRespVO {
  id?: number
  taskId?: number
  taskNo?: string
  storeId?: string
  storeName?: string
  merchantCode?: string
  erpStoreCode?: string
  platformStoreId?: string
  status?: string
  currentBatchNo?: number
  totalBatchNo?: number
  totalSkuCount?: number
  formalSuccessCount?: number
  shadowSuccessCount?: number
  governanceCount?: number
  failureCount?: number
  retryCount?: number
  errorMsg?: string
  startedAt?: string
  finishedAt?: string
  createTime?: string
}

export interface EleStoreInventoryBatchTaskStorePageRespVO {
  list?: EleStoreInventoryBatchTaskStoreRespVO[]
  total?: number
}

export const queryStoreInventory = async (data: EleStoreInventoryQueryReqVO) => {
  return await request.post<EleStoreInventoryQueryRespVO>({ url: '/ele/store-inventory/query', data })
}

export const downloadInventoryImportTemplate = async () => {
  return await request.download({ url: '/ele/store-inventory/import-template' })
}

export const importStoreInventory = async (data: FormData) => {
  return await request.upload<EleStoreInventoryImportRespVO>({ url: '/ele/store-inventory/import', data })
}

export const createAllOpenInventoryTask = async () => {
  return await request.post<number>({ url: '/ele/store-inventory/batch/all-open', data: {} })
}

export const createStoresInventoryTask = async (platformStoreIds: string[]) => {
  return await request.post<number>({ url: '/ele/store-inventory/batch/stores', data: { platformStoreIds } })
}

export const getInventoryTaskPage = async (params: EleStoreInventoryBatchTaskPageReqVO) => {
  return await request.get<EleStoreInventoryBatchTaskPageRespVO>({ url: '/ele/store-inventory/batch/page', params })
}

const normalizeInventoryTaskId = (id: number | string | undefined) => {
  const normalized = typeof id === 'number' ? id : Number(id)
  if (!Number.isInteger(normalized) || normalized <= 0) {
    throw new Error('任务ID无效，请刷新任务列表后重试')
  }
  return normalized
}

export const getInventoryTask = async (id: number | string) => {
  const taskId = normalizeInventoryTaskId(id)
  return await request.get<EleStoreInventoryBatchTaskRespVO>({ url: `/ele/store-inventory/batch/${taskId}` })
}

export const getInventoryTaskStores = async (
  id: number | string,
  params: EleStoreInventoryBatchTaskStorePageReqVO
) => {
  const taskId = normalizeInventoryTaskId(id)
  return await request.get<EleStoreInventoryBatchTaskStorePageRespVO>({
    url: `/ele/store-inventory/batch/${taskId}/stores`,
    params
  })
}

export const cancelInventoryTask = async (id: number | string) => {
  const taskId = normalizeInventoryTaskId(id)
  return await request.post<boolean>({ url: `/ele/store-inventory/batch/${taskId}/cancel` })
}
