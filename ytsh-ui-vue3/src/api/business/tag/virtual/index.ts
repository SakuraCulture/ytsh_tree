import request from '@/config/axios'

export interface TagVirtual {
  id?: number
  domainType: string
  name: string
  code: string
  expressionJson: string
  expressionSummary?: string
  usageScenario?: string
  status?: number
  createTime?: string
}

export interface TagVirtualPageReqVO {
  pageNo?: number
  pageSize?: number
  domainType?: string
  name?: string
  code?: string
  status?: number
}

export const TagVirtualApi = {
  getTagVirtualPage: async (params: TagVirtualPageReqVO) => {
    return await request.get({ url: '/business/tag-virtual/page', params })
  },

  getTagVirtual: async (id: number) => {
    return await request.get({ url: '/business/tag-virtual/get?id=' + id })
  },

  createTagVirtual: async (data: TagVirtual) => {
    return await request.post({ url: '/business/tag-virtual/create', data })
  },

  updateTagVirtual: async (data: TagVirtual) => {
    return await request.put({ url: '/business/tag-virtual/update', data })
  },

  deleteTagVirtual: async (id: number) => {
    return await request.delete({ url: '/business/tag-virtual/delete?id=' + id })
  }
}
