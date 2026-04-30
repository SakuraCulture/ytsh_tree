import request from '@/config/axios'

export interface WarehouseLinePageReqVO {
  pageNo?: number
  pageSize?: number
  warehouseId?: string
  lineCode?: string
  lineName?: string
  lineStatus?: number
  createTime?: string[]
}

export interface WarehouseLineSaveReqVO {
  lineId?: number
  warehouseId?: string
  lineCode?: string
  lineName?: string
  orderWeekdays?: number[]
  lineStatus?: number
  remark?: string
  storeIds?: string[]
}

export interface WarehouseLineRespVO {
  lineId: number
  warehouseId?: string
  warehouseName?: string
  lineCode?: string
  lineName?: string
  orderWeekdays?: number[]
  lineStatus?: number
  remark?: string
  storeCount?: number
  storeIds?: string[]
  createTime?: string
}

export interface WarehouseStoreSupplySimpleRespVO {
  id: number
  warehouseId: string
  warehouseName?: string
  storeId: string
  storeName?: string
  isPrimary?: number
  supplyStatus?: number
  remark?: string
}

export const WarehouseLineApi = {
  getPage: async (params: WarehouseLinePageReqVO) => {
    return await request.get({ url: `/warehouse-line/page`, params })
  },
  get: async (lineId: number) => {
    return await request.get({ url: `/warehouse-line/get`, params: { lineId } })
  },
  create: async (data: WarehouseLineSaveReqVO) => {
    return await request.post({ url: `/warehouse-line/create`, data })
  },
  update: async (data: WarehouseLineSaveReqVO) => {
    return await request.put({ url: `/warehouse-line/update`, data })
  },
  delete: async (lineId: number) => {
    return await request.delete({ url: `/warehouse-line/delete`, params: { lineId } })
  },
  export: async (params: WarehouseLinePageReqVO) => {
    return await request.download({ url: `/warehouse-line/export`, params })
  },
  getImportTemplate: async () => {
    return await request.download({ url: `/warehouse-line/get-import-template` })
  },
  importExcel: async (data: FormData) => {
    return await request.post({
      url: `/warehouse-line/import`,
      data,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getEligibleStoreList: async (warehouseId: string) => {
    return await request.get({ url: `/warehouse-store-supply/simple-list`, params: { warehouseId } })
  }
}
