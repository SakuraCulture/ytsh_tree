import request from '@/config/axios'

/** 商品类目表（三级树形结构）信息 */
export interface CategoryTable {
  categoryId: number; // 类目ID
  categoryName?: string; // 类目名称
  parentId: number; // 父类目ID（0表示一级类目）
  categoryLevel?: number; // 层级（1一级/2二级/3三级）
  categoryPath: string; // 类目路径（如：1/2/3）
  categoryIcon: string; // 类目图标URL
  categoryImage: string; // 类目配图URL
  sortOrder: number; // 同级排序
  isLeaf: number; // 是否叶子类目（0否 1是）
  status: number; // 状态（0禁用 1启用）
  children?: CategoryTable[];
  // 前端显示用扩展属性
  parentCategoryName?: string; // 父类目名称（前端计算）
  categoryPathNames?: string; // 类目路径名称（前端计算）
}

// 商品类目表（三级树形结构） API
export const CategoryTableApi = {
  // 查询商品类目表（三级树形结构）列表
  getCategoryTableList: async (params) => {
    return await request.get({ url: `/business/category-table/list`, params })
  },

  // 查询商品类目表（三级树形结构）详情
  getCategoryTable: async (id: number) => {
    return await request.get({ url: `/business/category-table/get?id=` + id })
  },

  // 新增商品类目表（三级树形结构）
  createCategoryTable: async (data: CategoryTable) => {
    return await request.post({ url: `/business/category-table/create`, data })
  },

  // 修改商品类目表（三级树形结构）
  updateCategoryTable: async (data: CategoryTable) => {
    return await request.put({ url: `/business/category-table/update`, data })
  },

  // 删除商品类目表（三级树形结构）
  deleteCategoryTable: async (id: number) => {
    return await request.delete({ url: `/business/category-table/delete?id=` + id })
  },

  // 批量删除商品类目表（三级树形结构）
  deleteCategoryTableByIds: async (ids: number[]) => {
    return await request.delete({ url: `/business/category-table/delete-by-ids?ids=` + ids.join(',') })
  },


  // 导出商品类目表（三级树形结构） Excel
  exportCategoryTable: async (params) => {
    return await request.download({ url: `/business/category-table/export-excel`, params })
  },

  // 获取导入类目模板
  importCategoryTableTemplate: async () => {
    return await request.download({ url: `/business/category-table/get-import-template` })
  },

  // 导入类目
  importCategoryTable: async (file: File, updateSupport: boolean = false) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('updateSupport', String(updateSupport))
    return await request.post({ url: `/business/category-table/import`, data: formData })
  }
}