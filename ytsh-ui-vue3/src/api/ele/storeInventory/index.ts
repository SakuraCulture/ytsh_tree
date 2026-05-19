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

export const queryStoreInventory = async (data: EleStoreInventoryQueryReqVO) => {
  return await request.post<EleStoreInventoryQueryRespVO>({ url: '/ele/store-inventory/query', data })
}
