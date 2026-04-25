import request from '@/config/axios'

export interface WarehouseStockPageReqVO {
  pageNo?: number
  pageSize?: number
  warehouseStockId?: number
  warehouseId?: string
  warehouseProductId?: number
  productSkuId?: number
  skuCode?: string
  skuName?: string
  createTime?: string[]
}

export interface WarehouseStockRespVO {
  warehouseStockId: number
  warehouseProductId?: number
  warehouseId?: string
  warehouseName?: string
  productSkuId?: number
  skuCode?: string
  skuName?: string
  warehouseStockQty?: number
  warehouseStockAvailableQty?: number
  warehouseStockTransitQty?: number
  warehouseStockFrozenQty?: number
  warehouseStockOutstockHours?: number
  createTime?: string
}

export interface WarehouseStockStatisticsRespVO {
  stockCount?: number
  totalQuantity?: number
  totalAvailableQuantity?: number
  totalTransitQuantity?: number
  totalFrozenQuantity?: number
}

export interface WarehouseStockRecordPageReqVO {
  pageNo?: number
  pageSize?: number
  stockRecordId?: number
  warehouseId?: string
  warehouseProductId?: number
  productSkuId?: number
  skuCode?: string
  skuName?: string
  bizType?: string
  bizNo?: string
  createTime?: string[]
}

export interface WarehouseStockRecordRespVO {
  stockRecordId: number
  warehouseId?: string
  warehouseName?: string
  warehouseProductId?: number
  productSkuId?: number
  skuCode?: string
  skuName?: string
  bizType?: string
  bizNo?: string
  bizId?: number
  bizItemId?: number
  changeQty?: number
  afterQty?: number
  creator?: string
  createTime?: string
}

export const WarehouseStockApi = {
  getWarehouseStockPage: async (params: WarehouseStockPageReqVO) => {
    return await request.get({ url: `/warehouse-stock/page`, params })
  },

  getWarehouseStock: async (warehouseStockId: number) => {
    return await request.get({ url: `/warehouse-stock/get`, params: { warehouseStockId } })
  },

  getWarehouseStockStatistics: async (params: WarehouseStockPageReqVO) => {
    return await request.get({ url: `/warehouse-stock/statistics`, params })
  },

  exportWarehouseStock: async (params: WarehouseStockPageReqVO) => {
    return await request.download({ url: `/warehouse-stock/export`, params })
  }
}

export const WarehouseStockRecordApi = {
  getWarehouseStockRecordPage: async (params: WarehouseStockRecordPageReqVO) => {
    return await request.get({ url: `/warehouse-stock-record/page`, params })
  },

  getWarehouseStockRecord: async (stockRecordId: number) => {
    return await request.get({ url: `/warehouse-stock-record/get`, params: { stockRecordId } })
  },

  exportWarehouseStockRecord: async (params: WarehouseStockRecordPageReqVO) => {
    return await request.download({ url: `/warehouse-stock-record/export`, params })
  }
}
