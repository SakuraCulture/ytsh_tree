import request from '@/config/axios'

export interface WarehouseProductPageReqVO {
  pageNo?: number
  pageSize?: number
  warehouseProductId?: number
  warehouseId?: string
  productSkuId?: number
  skuCode?: string
  skuName?: string
  warehouseProductLocation?: string
  createTime?: string[]
}

export interface WarehouseProductSaveReqVO {
  warehouseProductId?: number
  warehouseId?: string
  productSkuId?: number
  warehouseProductCostPrice?: number
  warehouseProductLocation?: string
  warehouseProductFirstDate?: string
  warehouseProductLastDate?: string
}

export interface WarehouseProductRespVO {
  warehouseProductId: number
  warehouseId?: string
  warehouseName?: string
  productSkuId?: number
  skuCode?: string
  skuName?: string
  retailPrice?: number
  warehouseProductCostPrice?: number
  warehouseProductLocation?: string
  warehouseProductFirstDate?: string
  warehouseProductLastDate?: string
  createTime?: string
}

export interface WarehouseProductSimpleRespVO {
  warehouseProductId: number
  warehouseId?: string
  productSkuId?: number
  skuName?: string
}

export interface SkuSimpleRespVO {
  productSkuId: number
  productSkuCode?: string
  productSkuName?: string
  productSkuEan?: string
  productRetailPrice?: number
  productSkuStatus?: number
}

export const WarehouseProductApi = {
  getWarehouseProductPage: async (params: WarehouseProductPageReqVO) => {
    return await request.get({ url: `/warehouse-product/page`, params })
  },

  getWarehouseProductSimpleList: async (warehouseId: string) => {
    return await request.get({ url: `/warehouse-product/simple-list`, params: { warehouseId } })
  },

  getSkuSimpleList: async () => {
    return await request.get({ url: `/warehouse-product/sku-simple-list` })
  },

  getWarehouseProduct: async (warehouseProductId: number) => {
    return await request.get({ url: `/warehouse-product/get`, params: { warehouseProductId } })
  },

  createWarehouseProduct: async (data: WarehouseProductSaveReqVO) => {
    return await request.post({ url: `/warehouse-product/create`, data })
  },

  updateWarehouseProduct: async (data: WarehouseProductSaveReqVO) => {
    return await request.put({ url: `/warehouse-product/update`, data })
  },

  deleteWarehouseProduct: async (warehouseProductId: number) => {
    return await request.delete({ url: `/warehouse-product/delete`, params: { warehouseProductId } })
  },

  exportWarehouseProduct: async (params: WarehouseProductPageReqVO) => {
    return await request.download({ url: `/warehouse-product/export`, params })
  },

  getImportTemplate: async (format: string) => {
    return await request.download({ url: `/warehouse-product/get-import-template`, params: { format } })
  },

  importWarehouseProduct: async (data: any) => {
    return await request.post({ url: `/warehouse-product/import`, data, headers: { 'Content-Type': 'multipart/form-data' } })
  }
}
