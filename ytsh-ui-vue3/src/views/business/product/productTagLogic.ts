type TagLike = {
  tagValueId: number
  sources: string[]
}

export const collectSelectedTagIds = (ids: number[]) => [...new Set(ids)].sort((a, b) => a - b)

export const extractManualTagIds = <T extends TagLike>(tags: T[]) => {
  return tags.filter((item) => item.sources.includes('MANUAL')).map((item) => item.tagValueId)
}

export const formatBatchTagResult = (resp: { successCount: number; failureCount: number }) => {
  return `成功 ${resp.successCount} 条，失败 ${resp.failureCount} 条`
}
