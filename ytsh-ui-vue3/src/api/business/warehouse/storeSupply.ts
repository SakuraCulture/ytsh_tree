import request from '@/config/axios'

export interface WarehouseStoreSupplyPageReqVO {
  pageNo?: number
  pageSize?: number
  warehouseId?: string
  storeId?: string
  isPrimary?: number
  supplyStatus?: number
  createTime?: string[]
}

export interface WarehouseStoreSupplySaveReqVO {
  id?: number
  warehouseId: string
  storeId: string
  isPrimary: number
  supplyStatus: number
  remark?: string
}

export interface WarehouseStoreSupplyRespVO {
  id: number
  warehouseId: string
  warehouseName?: string
  storeId: string
  storeName?: string
  isPrimary?: number
  supplyStatus?: number
  remark?: string
  createTime?: string
}

export const WarehouseStoreSupplyApi = {
  getPage: async (params: WarehouseStoreSupplyPageReqVO) => {
    return await request.get({ url: `/warehouse-store-supply/page`, params })
  },
  get: async (id: number) => {
    return await request.get({ url: `/warehouse-store-supply/get`, params: { id } })
  },
  create: async (data: WarehouseStoreSupplySaveReqVO) => {
    return await request.post({ url: `/warehouse-store-supply/create`, data })
  },
  update: async (data: WarehouseStoreSupplySaveReqVO) => {
    return await request.put({ url: `/warehouse-store-supply/update`, data })
  },
  delete: async (id: number) => {
    return await request.delete({ url: `/warehouse-store-supply/delete`, params: { id } })
  },
  export: async (params: WarehouseStoreSupplyPageReqVO) => {
    return await request.download({ url: `/warehouse-store-supply/export`, params })
  },
  getImportTemplate: async () => {
    return await request.download({ url: `/warehouse-store-supply/get-import-template` })
  },
  importExcel: async (data: FormData) => {
    return await request.post({
      url: `/warehouse-store-supply/import`,
      data,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
