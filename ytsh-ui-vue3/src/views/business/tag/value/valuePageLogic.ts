export interface DimensionLike {
  id?: number
  domainType: string
  parentId: number
  level: number
  name: string
  code: string
  children?: DimensionLike[]
}

export interface DimensionOption {
  id: number
  name: string
  level: number
  children?: DimensionOption[]
}

const getDimensionMap = (dimensions: DimensionLike[]) => {
  const map = new Map<number, DimensionLike>()
  dimensions.forEach((item) => {
    if (item.id !== undefined) {
      map.set(item.id, item)
    }
  })
  return map
}

const getDimensionPathName = (dimensionMap: Map<number, DimensionLike>, item: DimensionLike) => {
  const names = [item.name]
  let parentId = item.parentId
  while (parentId && dimensionMap.has(parentId)) {
    const parent = dimensionMap.get(parentId)!
    names.unshift(parent.name)
    parentId = parent.parentId
  }
  return names.join(' / ')
}

export const filterDimensionsByDomain = (dimensions: DimensionLike[], domainType?: string) => {
  if (!domainType) {
    return dimensions
  }
  return dimensions.filter((item) => item.domainType === domainType)
}

export const buildDimensionTreeOptions = (dimensions: DimensionLike[], domainType?: string) => {
  const dimensionMap = getDimensionMap(dimensions)
  return filterDimensionsByDomain(dimensions, domainType)
    .filter((item) => item.level === 3 && item.id !== undefined)
    .map((item) => ({
      id: item.id!,
      name: getDimensionPathName(dimensionMap, item),
      level: item.level
    }))
}

export const getDimensionNameMap = (dimensions: DimensionLike[]) => {
  const dimensionMap = getDimensionMap(dimensions)
  const map = new Map<number, string>()
  dimensions.forEach((item) => {
    if (item.id !== undefined) {
      map.set(item.id, getDimensionPathName(dimensionMap, item))
    }
  })
  return map
}
