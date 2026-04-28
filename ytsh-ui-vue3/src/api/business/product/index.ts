import request from '@/config/axios'

/** SKU商品主数据信息 */
export interface SkuTable {
          productSkuId: number; // SKU ID
          productSkuCode: string; // SKU编码
          productSkuName: string; // SKU名称
          productSpuId: number; // 所属SPU
          productSkuEan: string; // 主EAN码(13位)
          productWeight: number; // 重量
          productWeightUnit: string; // 重量单位
          productLength: number; // 长度(cm)
          productWidth: number; // 宽度(cm)
          productHeight: number; // 高度(cm)
          productCostPrice: number; // 基准成本价
          productRetailPrice: number; // 基准零售价
          productImageUrl: string; // SKU主图URL
          productSkuStatus: number; // 状态(0下架1上架)
          upcTables?: UpcTable[] // UPC码列表
}

/** UPC码信息 */
export interface UpcTable {
          productUpcId: number; // UPC码ID
          productSkuId: number; // 关联的SKU ID
          productUpcType: string; // UPC码类型 (UPC-A/EAN-13/CODE128)
          productUpcValue: string; // UPC码值
          productUpcIsPrimary: number; // 是否主码 (0=否 1=是)
          productUpcStatus: number; // 状态 (0=禁用 1=启用)
}

/** SPU基础分类信息 */
export interface SpuTable {
          productSpuId: number; // SPU ID
          productSpuCode: string; // SPU编码
          productSpuName: string; // SPU名称
          productBrand: string; // 品牌
          categoryId: number; // 分类ID
          productOrigin: string; // 产地
          productManufacturer: string; // 生产商
          productSpecTemplate: string; // 规格模板
          productImageUrl: string; // 商品主图URL
          productDetailImages: string; // 商品详情图片
          productDescription: string; // 商品描述
          productSpuStatus: number; // 状态(0下架1上架)
          tags?: ProductSpuTagRespVO[]
          skuTables?: SkuTable[]
}

export interface ProductSpuTagSourceRespVO {
  sourceType: string
  sourceRef: string
  status: number
  effectiveTime?: string
  expireTime?: string
}

export interface ProductSpuTagRespVO {
  tagValueId: number
  tagValueCode: string
  tagValueName: string
  dimensionPath: string
  sources: string[]
  sourceDetails: ProductSpuTagSourceRespVO[]
}

// SPU基础分类 API
export const SpuTableApi = {
  // 查询SPU基础分类分页
  getSpuTablePage: async (params: any) => {
    return await request.get({ url: `/business/spu-table/page`, params })
  },

  getSpuTableAggregatePage: async (params: any) => {
    return await request.get<PageResult<SpuTable[]>>({ url: `/business/spu-table/page-aggregate`, params })
  },

  // 查询SPU基础分类详情
  getSpuTable: async (productSpuId: number) => {
    return await request.get({ url: `/business/spu-table/get?productSpuId=` + productSpuId })
  },

  getProductSpuTagList: async (productSpuId: number) => {
    return await request.get<ProductSpuTagRespVO[]>({
      url: '/business/product-spu-tag/list',
      params: { productSpuId }
    })
  },

  saveProductSpuManualTags: async (data: { productSpuId: number; tagValueIds: number[] }) => {
    return await request.post({ url: '/business/product-spu-tag/save-manual', data })
  },

  // 新增SPU基础分类
  createSpuTable: async (data: SpuTable) => {
    return await request.post({ url: `/business/spu-table/create`, data })
  },

  // 修改SPU基础分类
  updateSpuTable: async (data: SpuTable) => {
    return await request.put({ url: `/business/spu-table/update`, data })
  },

  // 删除SPU基础分类
  deleteSpuTable: async (productSpuId: number) => {
    return await request.delete({ url: `/business/spu-table/delete?productSpuId=` + productSpuId })
  },

  /** 批量删除SPU基础分类 */
  deleteSpuTableList: async (ids: number[]) => {
    return await request.delete({ url: `/business/spu-table/delete-list?ids=${ids.join(',')}` })
  },

  // 导出SPU基础分类 Excel
  exportSpuTable: async (params) => {
    return await request.download({ url: `/business/spu-table/export-excel`, params })
  },

  // 获取导入模板
  getImportTemplate: async () => {
    return await request.download({ url: `/business/spu-table/get-import-template` })
  },

  // 导入SPU/SKU/UPC数据
  importSpuTable: async (file: File, updateSupport?: boolean) => {
    const formData = new FormData()
    formData.append('file', file)
    if (updateSupport !== undefined) {
      formData.append('updateSupport', String(updateSupport))
    }
    return await request.post({ url: `/business/spu-table/import`, data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
  },

// ==================== 子表（SKU商品主数据） ====================

  // 获得SKU商品主数据列表
  getSkuTableListByProductSpuId: async (productSpuId) => {
    return await request.get({ url: `/business/spu-table/sku-table/list-by-product-spu-id?productSpuId=` + productSpuId })
  }
}

// UPC码管理 API
export const UpcTableApi = {
  // 新增UPC码
  createUpcTable: async (data: UpcTable) => {
    return await request.post({ url: `/business/upc-table/create`, data })
  },

  // 更新UPC码
  updateUpcTable: async (data: UpcTable) => {
    return await request.put({ url: `/business/upc-table/update`, data })
  },

  // 删除UPC码
  deleteUpcTable: async (productUpcId: number) => {
    return await request.delete({ url: `/business/upc-table/delete`, params: { productUpcId } })
  }
}