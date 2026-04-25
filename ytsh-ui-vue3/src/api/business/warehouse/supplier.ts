import request from '@/config/axios'

export interface WarehouseSupplierPageReqVO {
  pageNo?: number
  pageSize?: number
  supplierId?: string
  supplierName?: string
  categoryName?: string
  managerName?: string
  phone?: string
  supplierStatus?: number
  createTime?: string[]
}

export interface WarehouseSupplierSaveReqVO {
  supplierId?: string
  supplierName?: string
  categoryName?: string
  managerName?: string
  phone?: string
  address?: string
  paymentMethod?: string
  paymentDays?: number
  supplierStatus?: number
}

export interface WarehouseSupplierRespVO {
  supplierId: string
  supplierName?: string
  categoryName?: string
  managerName?: string
  phone?: string
  address?: string
  paymentMethod?: string
  paymentDays?: number
  supplierStatus?: number
  createTime?: string
}

export interface WarehouseSupplierSimpleRespVO {
  supplierId: string
  supplierName: string
}

export const WarehouseSupplierApi = {
  getWarehouseSupplierPage: async (params: WarehouseSupplierPageReqVO) => {
    return await request.get({ url: `/warehouse-supplier/page`, params })
  },

  getWarehouseSupplierSimpleList: async () => {
    return await request.get({ url: `/warehouse-supplier/simple-list` })
  },

  getWarehouseSupplier: async (supplierId: string) => {
    return await request.get({ url: `/warehouse-supplier/get`, params: { supplierId } })
  },

  createWarehouseSupplier: async (data: WarehouseSupplierSaveReqVO) => {
    return await request.post({ url: `/warehouse-supplier/create`, data })
  },

  updateWarehouseSupplier: async (data: WarehouseSupplierSaveReqVO) => {
    return await request.put({ url: `/warehouse-supplier/update`, data })
  },

  deleteWarehouseSupplier: async (supplierId: string) => {
    return await request.delete({ url: `/warehouse-supplier/delete`, params: { supplierId } })
  },

  exportWarehouseSupplier: async (params: WarehouseSupplierPageReqVO) => {
    return await request.download({ url: `/warehouse-supplier/export`, params })
  }
}
