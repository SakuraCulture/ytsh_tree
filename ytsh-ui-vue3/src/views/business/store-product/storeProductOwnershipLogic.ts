import type {
  StoreProductDetailRespVO,
  StoreProductSaveReqVO,
  StoreProductTable
} from '@/api/business/store-product'

export interface OwnershipOption {
  label: string
  value: string
}

export interface StoreProductOwnershipFormData {
  storeProductId?: string | number
  storeId?: string
  productSkuId?: string
  productAttribution?: string
  storeRetailPrice?: number
  firstEnterShopDate?: string
  posStatus?: number
  enterShopStatus?: number
}

const DEFAULT_OWNERSHIP_OPTIONS: OwnershipOption[] = [{ label: '入店', value: '入店' }]

const normalizePosStatus = (value?: string | number) => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const normalizedValue = Number(value)
  return Number.isNaN(normalizedValue) ? undefined : normalizedValue
}

export const getOwnershipOptions = (currentValue?: string) => {
  if (!currentValue || DEFAULT_OWNERSHIP_OPTIONS.some((item) => item.value === currentValue)) {
    return DEFAULT_OWNERSHIP_OPTIONS
  }
  return [...DEFAULT_OWNERSHIP_OPTIONS, { label: currentValue, value: currentValue }]
}

export const formatOwnershipLabel = (value?: string) => {
  if (!value) {
    return '-'
  }
  return getOwnershipOptions(value).find((item) => item.value === value)?.label || value
}

export const fillStoreProductFormData = (
  data: StoreProductDetailRespVO,
  row?: StoreProductTable
): StoreProductOwnershipFormData => {
  return {
    storeProductId: data.storeProductId,
    storeId: data.storeId ?? row?.storeId,
    productSkuId: data.productSkuId != null ? String(data.productSkuId) : row?.productSkuId,
    productAttribution: data.productAttribution ?? row?.productAttribution,
    storeRetailPrice: data.storeRetailPrice ?? row?.storeRetailPrice,
    firstEnterShopDate: data.firstEnterShopDate ?? row?.firstEnterShopDate,
    posStatus: normalizePosStatus(data.posStatus ?? row?.posStatus),
    enterShopStatus: data.enterShopStatus ?? row?.enterShopStatus
  }
}

export const buildStoreProductSavePayload = (
  formData: StoreProductOwnershipFormData,
  formType: string
): StoreProductSaveReqVO => {
  return {
    storeProductId: formType === 'update' ? formData.storeProductId : undefined,
    storeId: formData.storeId,
    productSkuId: formData.productSkuId,
    storeProductOwnership: formData.productAttribution,
    storeProductPrice: formData.storeRetailPrice,
    storeProductFirstDate: formData.firstEnterShopDate,
    storeProductPosStatus:
      formData.posStatus === undefined ? undefined : String(formData.posStatus),
    storeProductIsActive: formData.enterShopStatus
  }
}
