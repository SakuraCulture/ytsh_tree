export interface CategoryFormCategoryLike {
  categoryId: number
  parentId: number
  categoryLevel?: string | number
  categoryName?: string
  categoryPath?: string
}

export interface DerivedCategoryFields {
  categoryLevel: string
  categoryPath: string
  categoryPathNames: string
}

const DEFAULT_CATEGORY_FIELDS: DerivedCategoryFields = {
  categoryLevel: '1',
  categoryPath: '',
  categoryPathNames: '-'
}

export const buildCategoryMap = <T extends CategoryFormCategoryLike>(categories: T[]) => {
  return new Map(categories.map((item) => [item.categoryId, item]))
}

export const buildCategoryPathNames = (
  categoryPath: string | undefined,
  categoryMap: Map<number, CategoryFormCategoryLike>
) => {
  if (!categoryPath) {
    return '-'
  }

  const names = categoryPath
    .split('/')
    .filter((id) => id && id !== '0')
    .map((id) => categoryMap.get(Number(id))?.categoryName || id)

  return names.join(' / ') || '-'
}

const normalizeCategoryParentId = (parentId: string | number | undefined | null) => {
  if (parentId === undefined || parentId === null || parentId === '') {
    return undefined
  }

  const normalized = Number(parentId)
  return Number.isNaN(normalized) || normalized <= 0 ? undefined : normalized
}

export const deriveCategoryFieldsByParent = (
  parentId: string | number | undefined | null,
  categoryMap: Map<number, CategoryFormCategoryLike>
): DerivedCategoryFields => {
  const normalizedParentId = normalizeCategoryParentId(parentId)
  if (!normalizedParentId) {
    return { ...DEFAULT_CATEGORY_FIELDS }
  }

  const parent = categoryMap.get(normalizedParentId)
  if (!parent) {
    return { ...DEFAULT_CATEGORY_FIELDS }
  }

  const parentLevel = Number(parent.categoryLevel || 0)
  const categoryLevel = String(parentLevel > 0 ? Math.min(parentLevel + 1, 3) : 1)
  const categoryPath = parent.categoryPath
    ? `${parent.categoryPath}${normalizedParentId}/`
    : `${normalizedParentId}/`

  return {
    categoryLevel,
    categoryPath,
    categoryPathNames: buildCategoryPathNames(categoryPath, categoryMap)
  }
}
