export interface DimensionLike {
  id?: number
  domainType: string
  parentId: number
  level: number
  name: string
  code: string
  children?: DimensionLike[]
}

export interface DimensionTreeOption {
  id: number
  name: string
  children?: DimensionTreeOption[]
}

const handleDimensionTree = (data: DimensionLike[]) => {
  const childrenMap: Record<number, DimensionLike[]> = {}
  const nodeIds: Record<number, DimensionLike> = {}
  const tree: DimensionLike[] = []

  for (const item of data) {
    const parentId = item.parentId
    if (!childrenMap[parentId]) {
      childrenMap[parentId] = []
    }
    if (item.id !== undefined) {
      nodeIds[item.id] = item
    }
    childrenMap[parentId].push(item)
  }

  for (const item of data) {
    if (item.id === undefined || !nodeIds[item.parentId]) {
      tree.push(item)
    }
  }

  const attachChildren = (node: DimensionLike) => {
    if (node.id !== undefined && childrenMap[node.id]) {
      node.children = childrenMap[node.id]
    }
    node.children?.forEach(attachChildren)
  }

  tree.forEach(attachChildren)
  return tree
}

export const filterDimensionsByDomain = (dimensions: DimensionLike[], domainType?: string) => {
  if (!domainType) {
    return []
  }
  return dimensions.filter((item) => item.domainType === domainType)
}

export const collectDescendantIds = (dimensions: DimensionLike[], currentId?: number) => {
  if (!currentId) {
    return new Set<number>()
  }
  const descendantIds = new Set<number>()
  const queue = dimensions.filter((item) => item.parentId === currentId).map((item) => item.id!)

  while (queue.length > 0) {
    const id = queue.shift()!
    if (descendantIds.has(id)) {
      continue
    }
    descendantIds.add(id)
    dimensions
      .filter((item) => item.parentId === id && item.id !== undefined)
      .forEach((item) => queue.push(item.id!))
  }

  return descendantIds
}

export const buildParentOptions = (
  dimensions: DimensionLike[],
  domainType?: string,
  currentId?: number
): DimensionTreeOption[] => {
  const descendantIds = collectDescendantIds(dimensions, currentId)
  const candidates = filterDimensionsByDomain(dimensions, domainType)
    .filter((item) => item.id !== currentId)
    .filter((item) => !descendantIds.has(item.id!))
    .filter((item) => item.level < 3)
    .map((item) => ({ ...item }))

  return [
    {
      id: 0,
      name: '顶级（无父级）',
      children: handleDimensionTree(candidates)
    }
  ]
}

export const getNextLevelByParent = (dimensions: DimensionLike[], parentId?: number) => {
  if (!parentId) {
    return 1
  }
  const parent = dimensions.find((item) => item.id === parentId)
  if (!parent) {
    return 1
  }
  return parent.level + 1
}

export const hasChildren = (dimensions: DimensionLike[], currentId?: number) => {
  if (!currentId) {
    return false
  }
  return dimensions.some((item) => item.parentId === currentId)
}

export const filterTreeOptionsByDomain = (dimensions: DimensionLike[], domainType?: string) => {
  const candidates = filterDimensionsByDomain(dimensions, domainType).map((item) => ({ ...item }))
  return [
    {
      id: 0,
      name: '顶级（无父级）',
      children: handleDimensionTree(candidates)
    }
  ]
}
