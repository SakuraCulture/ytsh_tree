import request from '@/config/axios'

export interface TagDimension {
  id?: number
  domainType: string
  parentId: number
  level: number
  name: string
  code: string
  sort?: number
  status?: number
  description?: string
  createTime?: string
  children?: TagDimension[]
}

export interface TagDimensionListReqVO {
  domainType?: string
  parentId?: number
  level?: number
}

export const TagDimensionApi = {
  getTagDimensionList: async (params?: TagDimensionListReqVO) => {
    return await request.get({ url: '/business/tag-dimension/list', params })
  },

  getTagDimension: async (id: number) => {
    return await request.get({ url: '/business/tag-dimension/get?id=' + id })
  },

  createTagDimension: async (data: TagDimension) => {
    return await request.post({ url: '/business/tag-dimension/create', data })
  },

  updateTagDimension: async (data: TagDimension) => {
    return await request.put({ url: '/business/tag-dimension/update', data })
  },

  deleteTagDimension: async (id: number) => {
    return await request.delete({ url: '/business/tag-dimension/delete?id=' + id })
  }
}
