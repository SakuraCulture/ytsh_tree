import request from '@/config/axios'

export interface WarehousePageReqVO {
  pageNo?: number
  pageSize?: number
  warehouseId?: string
  warehouseCode?: string
  warehouseName?: string
  warehouseType?: string
  regionCode?: string
  address?: string
  warehouseStatus?: number
  isDefault?: number
  createTime?: string[]
}

export interface WarehouseSaveReqVO {
  warehouseId?: string
  warehouseCode?: string
  warehouseName?: string
  warehouseType?: string
  regionCode?: string
  address?: string
  warehouseStatus?: number
  isDefault?: number
}

export interface WarehouseRespVO {
  warehouseId: string
  warehouseCode?: string
  warehouseName?: string
  warehouseType?: string
  regionCode?: string
  address?: string
  warehouseStatus?: number
  isDefault?: number
  createTime?: string
}

export interface WarehouseSimpleRespVO {
  warehouseId: string
  warehouseName: string
  isDefault?: number
}

export const WarehouseApi = {
  getWarehousePage: async (params: WarehousePageReqVO) => {
    return await request.get({ url: `/warehouse/page`, params })
  },

  getWarehouseSimpleList: async () => {
    return await request.get({ url: `/warehouse/simple-list` })
  },

  getWarehouse: async (warehouseId: string) => {
    return await request.get({ url: `/warehouse/get`, params: { warehouseId } })
  },

  createWarehouse: async (data: WarehouseSaveReqVO) => {
    return await request.post({ url: `/warehouse/create`, data })
  },

  updateWarehouse: async (data: WarehouseSaveReqVO) => {
    return await request.put({ url: `/warehouse/update`, data })
  },

  updateWarehouseDefaultStatus: async (warehouseId: string, isDefault: number) => {
    return await request.put({
      url: `/warehouse/update-default-status`,
      params: {
        warehouseId,
        isDefault
      }
    })
  },

  deleteWarehouse: async (warehouseId: string) => {
    return await request.delete({ url: `/warehouse/delete`, params: { warehouseId } })
  },

  exportWarehouse: async (params: WarehousePageReqVO) => {
    return await request.download({ url: `/warehouse/export`, params })
  }
}
