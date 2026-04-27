import request from '@/config/axios'

export interface TagValue {
  id?: number
  dimensionId: number
  name: string
  code: string
  tagMethod: string
  dataSource?: string
  updateFrequency?: string
  logicDescription?: string
  sort?: number
  status?: number
  createTime?: string
}

export interface TagValuePageReqVO {
  pageNo?: number
  pageSize?: number
  domainType?: string
  dimensionId?: number
  name?: string
  code?: string
  tagMethod?: string
  status?: number
}

export interface TagValueImportReqVO {
  domainType: string
  l1Name: string
  l1Code: string
  l2Name: string
  l2Code: string
  l3Name: string
  l3Code: string
  tagValueName: string
  tagValueCode: string
  tagMethod: string
  dataSource?: string
  updateFrequency?: string
  logicDescription?: string
  sort?: number
  status?: number
}

export interface TagValueImportRespVO {
  createNames?: string[]
  updateNames?: string[]
  failureNames?: Record<string, string>
}

export interface TagSelectableValue {
  tagValueId: number
  tagValueCode: string
  tagValueName: string
  dimensionId: number
  dimensionName: string
  dimensionPath: string
  status: number
}

export const TagValueApi = {
  getTagValuePage: async (params: TagValuePageReqVO) => {
    return await request.get({ url: '/business/tag-value/page', params })
  },

  getTagValue: async (id: number) => {
    return await request.get({ url: '/business/tag-value/get?id=' + id })
  },

  getTagValueListByDimensionId: async (dimensionId: number) => {
    return await request.get({ url: '/business/tag-value/list-by-dimension', params: { dimensionId } })
  },

  getTagValueListForObject: async (objectType: string) => {
    return await request.get<TagSelectableValue[]>({
      url: '/business/tag-value/list-for-object',
      params: { objectType }
    })
  },

  createTagValue: async (data: TagValue) => {
    return await request.post({ url: '/business/tag-value/create', data })
  },

  updateTagValue: async (data: TagValue) => {
    return await request.put({ url: '/business/tag-value/update', data })
  },

  deleteTagValue: async (id: number) => {
    return await request.delete({ url: '/business/tag-value/delete?id=' + id })
  },

  getImportTemplate: async () => {
    return await request.download({ url: '/business/tag-value/get-import-template' })
  },

  importTagValue: async (file: File, updateSupport: boolean = false) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('updateSupport', String(updateSupport))
    return await request.post<TagValueImportRespVO>({
      url: '/business/tag-value/import',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
