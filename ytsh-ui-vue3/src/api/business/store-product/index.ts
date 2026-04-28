import request from '@/config/axios'

/** 门店商品简单信息 */
export interface StoreProductSimpleRespVO {
  storeProductId?: number | string
  storeId?: string
  productSkuId?: string
  storeProductOwnership?: string
  storeProductIsActive?: number
}

/** 门店商品库存信息 */
export interface StoreProductStockRespVO {
  storeProductId: number | string
  availableQuantity: number
  inventoryQuantity: number
  inTransitQuantity: number
  frozenQuantity: number
  outOfStockDuration?: number
}

/** 门店商品列表信息 */
export interface StoreProductTable {
  storeProductId?: number | string
  shadowId?: number
  rowSource?: string
  completenessStatus?: string
  matchStatus?: string
  platformStoreId?: string
  spuCode?: string
  specification?: string
  storeId: string
  storeName?: string
  productSkuId?: string
  skuCode?: string
  skuName?: string
  productAttribution?: string
  posStatus?: number
  storeRetailPrice?: number
  enterShopStatus?: number
  firstEnterShopDate?: string
  createTime?: string
}

/** 门店商品详情 */
export interface StoreProductDetailRespVO {
  storeProductId?: string | number
  storeId?: string
  storeName?: string
  productSkuId?: string | number
  skuCode?: string
  skuName?: string
  productAttribution?: string
  posStatus?: string | number
  storeRetailPrice?: number
  enterShopStatus?: number
  firstEnterShopDate?: string
  createTime?: string
}

/** 门店商品新增/修改请求 */
export interface StoreProductSaveReqVO {
  storeProductId?: string | number
  storeId?: string
  productSkuId?: string
  storeProductOwnership?: string
  storeProductPosStatus?: string
  storeProductPrice?: number
  storeProductIsActive?: number
  storeProductFirstDate?: string
}

/** 门店商品导入结果 */
export interface StoreProductImportRespVO {
  createStoreProductIds?: string[]
  updateStoreProductIds?: string[]
  failureStoreProductIds?: Record<string, string>
}

export const StoreProductApi = {
  getTablePage: async (params: any) => {
    return await request.get({ url: `/store-product/page`, params })
  },

  getTableSimpleList: async (storeId?: string) => {
    return await request.get({ url: `/store-product/simple-list`, params: { storeId } })
  },

  getTable: async (id: number | string) => {
    return await request.get({ url: `/store-product/get/` + id })
  },

  createTable: async (data: StoreProductSaveReqVO) => {
    return await request.post({ url: `/store-product/create`, data })
  },

  updateTable: async (data: StoreProductSaveReqVO) => {
    return await request.put({ url: `/store-product/update`, data })
  },

  deleteTable: async (id: number | string) => {
    return await request.delete({ url: `/store-product/delete/` + id })
  },

  deleteTableList: async (ids: (number | string)[]) => {
    return await request.delete({ url: `/store-product/delete-list?ids=${ids.join(',')}` })
  },

  exportTable: async (params: any) => {
    return await request.download({ url: `/store-product/export`, params })
  },

  getImportTemplate: async (format?: string) => {
    const params = format ? { format } : {}
    return await request.download({ url: `/store-product/get-import-template`, params })
  },

  importTable: async (file: File, updateSupport?: boolean) => {
    const formData = new FormData()
    formData.append('file', file)
    if (updateSupport !== undefined) {
      formData.append('updateSupport', String(updateSupport))
    }
    return await request.post<StoreProductImportRespVO>({
      url: `/store-product/import`,
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  getStockById: async (id: number | string) => {
    return await request.get({ url: `/store-product/get-by-store-product/` + id })
  },

  getSkuSimpleList: async () => {
    return await request.get({ url: `/store-product/store/product/simple-list` })
  }
}
