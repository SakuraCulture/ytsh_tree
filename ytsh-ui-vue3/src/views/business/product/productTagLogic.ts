export const collectSelectedTagIds = (ids: number[]) => [...new Set(ids)].sort((a, b) => a - b)
