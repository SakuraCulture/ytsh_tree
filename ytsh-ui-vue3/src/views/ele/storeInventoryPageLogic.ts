export interface InventoryQueryFormModel {
  platformStoreId: string
  merchantCode: string
  erpStoreCode: string
  skuCodesText: string
  subSkuCodesText?: string
}

export interface StoreProductStockLike {
  storeProductId?: number | string
  availableQuantity?: number
  inventoryQuantity?: number
  inTransitQuantity?: number
  frozenQuantity?: number
  outOfStockDuration?: number
}

export interface EleInventoryRowLike {
  skuCode?: string
  subSkuCode?: string
  physicalStockTotalAmount?: number
  availableForSale?: number
  physicalStockAvailableAmount?: number
  physicalStockIntransitAmount?: number
  reservedAmount?: number
  physicalStockOccupiedAmount?: number
  lastQueryTime?: string
}

export interface ShadowInventorySnapshotLike {
  physicalStockTotalAmount?: number
  availableForSale?: number
  physicalStockAvailableAmount?: number
  physicalStockIntransitAmount?: number
  reservedAmount?: number
  physicalStockOccupiedAmount?: number
  lastQueryTime?: string
}

export interface InventoryMetricsViewModel {
  totalStock: number | null
  availableStock: number | null
  inTransitStock: number | null
  reservedStock: number | null
  lastQueryTime: string
  source: 'formal' | 'snapshot' | 'realtime'
  status: 'ready' | 'empty'
}

const toNumber = (value: unknown): number | null => {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

const buildMetrics = (
  source: InventoryMetricsViewModel['source'],
  totalStock: number | null,
  availableStock: number | null,
  inTransitStock: number | null,
  reservedStock: number | null,
  lastQueryTime: string
): InventoryMetricsViewModel => {
  const hasAnyMetric = [totalStock, availableStock, inTransitStock, reservedStock].some(
    (item) => item !== null
  )

  return {
    totalStock,
    availableStock,
    inTransitStock,
    reservedStock,
    lastQueryTime,
    source,
    status: hasAnyMetric ? 'ready' : 'empty'
  }
}

export const parseInventoryCodes = (value: string): string[] => {
  const seen = new Set<string>()
  const codes: string[] = []

  for (const item of value.split(/[\n,\s]+/)) {
    const code = item.trim()
    if (!code || seen.has(code)) {
      continue
    }
    seen.add(code)
    codes.push(code)
  }

  return codes
}

export const buildInventoryQueryRequest = (form: InventoryQueryFormModel) => {
  const skuCodes = parseInventoryCodes(form.skuCodesText)

  if (skuCodes.length === 0) {
    throw new Error('至少提供一个 SKU 查询条件')
  }

  return {
    platformStoreId: form.platformStoreId.trim(),
    merchantCode: form.merchantCode.trim(),
    erpStoreCode: form.erpStoreCode.trim(),
    skuCodes
  }
}

export const adaptStoreProductMetrics = (
  stock?: StoreProductStockLike | null
): InventoryMetricsViewModel => {
  return buildMetrics(
    'formal',
    toNumber(stock?.inventoryQuantity),
    toNumber(stock?.availableQuantity),
    toNumber(stock?.inTransitQuantity),
    toNumber(stock?.frozenQuantity),
    ''
  )
}

export const adaptEleInventoryMetrics = (
  row?: EleInventoryRowLike | null
): InventoryMetricsViewModel => {
  return buildMetrics(
    'realtime',
    toNumber(row?.physicalStockTotalAmount),
    toNumber(row?.availableForSale ?? row?.physicalStockAvailableAmount),
    toNumber(row?.physicalStockIntransitAmount),
    toNumber(row?.reservedAmount ?? row?.physicalStockOccupiedAmount),
    row?.lastQueryTime || ''
  )
}

export const adaptShadowSnapshotMetrics = (
  row?: ShadowInventorySnapshotLike | null
): InventoryMetricsViewModel => {
  return buildMetrics(
    'snapshot',
    toNumber(row?.physicalStockTotalAmount),
    toNumber(row?.availableForSale ?? row?.physicalStockAvailableAmount),
    toNumber(row?.physicalStockIntransitAmount),
    toNumber(row?.reservedAmount ?? row?.physicalStockOccupiedAmount),
    row?.lastQueryTime || ''
  )
}

const normalizeInventoryKey = (value?: string) => value?.trim() || ''

export const findMatchingInventoryRow = <T extends EleInventoryRowLike>(
  rows: T[] | undefined,
  target: Pick<EleInventoryRowLike, 'skuCode' | 'subSkuCode'>
): T | undefined => {
  if (!rows?.length) {
    return undefined
  }

  const targetSkuCode = normalizeInventoryKey(target.skuCode)
  const targetSubSkuCode = normalizeInventoryKey(target.subSkuCode)

  return rows.find((row) => {
    return (
      normalizeInventoryKey(row.skuCode) === targetSkuCode &&
      normalizeInventoryKey(row.subSkuCode) === targetSubSkuCode
    )
  })
}
