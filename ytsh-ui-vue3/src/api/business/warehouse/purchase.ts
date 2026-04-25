import request from '@/config/axios'

export interface WarehousePurchasePageReqVO {
  pageNo?: number
  pageSize?: number
  purchaseOrderId?: number
  purchaseOrderNo?: string
  supplierId?: string
  warehouseId?: string
  orderStatus?: string
  receiveStatus?: string
  productSkuId?: number
  skuCode?: string
  skuName?: string
  purchaser?: string
  purchaseDate?: string[]
  auditDate?: string[]
  createTime?: string[]
}

export interface WarehousePurchaseSaveItemReqVO {
  detailId?: number
  productSkuId?: number
  purchaseQty?: number
  boxQty?: number
  standardBoxQty?: number
  purchasePrice?: number
}

export interface WarehousePurchaseSaveReqVO {
  purchaseOrderId?: number
  supplierId?: string
  warehouseId?: string
  purchaseDate?: string
  purchaser?: string
  receiveAddress?: string
  remark?: string
  items: WarehousePurchaseSaveItemReqVO[]
}

export interface WarehousePurchaseRespItemVO {
  detailId?: number
  purchaseOrderId?: number
  purchaseOrderNo?: string
  productSkuId?: number
  productSkuCode?: string
  productSkuName?: string
  purchaseQty?: number
  boxQty?: number
  standardBoxQty?: number
  purchasePrice?: number
  purchaseAmount?: number
  inboundQty?: number
  returnQty?: number
  diffQty?: number
}

export interface WarehousePurchaseRespVO {
  purchaseOrderId: number
  purchaseOrderNo?: string
  supplierId?: string
  supplierName?: string
  warehouseId?: string
  warehouseName?: string
  purchaseDate?: string
  orderStatus?: string
  receiveStatus?: string
  totalQty?: number
  totalAmount?: number
  totalInboundQty?: number
  diffQty?: number
  returnQty?: number
  purchaser?: string
  receiveAddress?: string
  auditDate?: string
  remark?: string
  createTime?: string
  productNames?: string
  items?: WarehousePurchaseRespItemVO[]
}

export const WarehousePurchaseApi = {
  getWarehousePurchasePage: async (params: WarehousePurchasePageReqVO) => {
    return await request.get({ url: `/warehouse-purchase/page`, params })
  },

  getWarehousePurchase: async (purchaseOrderId: number) => {
    return await request.get({ url: `/warehouse-purchase/get`, params: { purchaseOrderId } })
  },

  createWarehousePurchase: async (data: WarehousePurchaseSaveReqVO) => {
    return await request.post({ url: `/warehouse-purchase/create`, data })
  },

  updateWarehousePurchase: async (data: WarehousePurchaseSaveReqVO) => {
    return await request.put({ url: `/warehouse-purchase/update`, data })
  },

  deleteWarehousePurchase: async (purchaseOrderId: number) => {
    return await request.delete({ url: `/warehouse-purchase/delete`, params: { purchaseOrderId } })
  },

  submitWarehousePurchase: async (purchaseOrderId: number) => {
    return await request.put({ url: `/warehouse-purchase/submit`, params: { purchaseOrderId } })
  },

  auditWarehousePurchase: async (purchaseOrderId: number) => {
    return await request.put({ url: `/warehouse-purchase/audit`, params: { purchaseOrderId } })
  },

  confirmInbound: async (purchaseOrderId: number) => {
    return await request.put({ url: `/warehouse-purchase/confirm-inbound`, params: { purchaseOrderId } })
  },

  cancelWarehousePurchase: async (purchaseOrderId: number) => {
    return await request.put({ url: `/warehouse-purchase/cancel`, params: { purchaseOrderId } })
  },

  exportWarehousePurchase: async (params: WarehousePurchasePageReqVO) => {
    return await request.download({ url: `/warehouse-purchase/export`, params })
  }
}
