<template>
  <div v-loading="loading" class="stock-card">
    <InventoryMetricsCard :model="adaptStoreProductMetrics(stockData)" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

import { StoreProductApi, type StoreProductStockRespVO } from '@/api/business/store-product'
import InventoryMetricsCard from '@/views/ele/components/InventoryMetricsCard.vue'
import { adaptStoreProductMetrics } from '@/views/ele/storeInventoryPageLogic'

defineOptions({ name: 'StockExpandCard' })

const props = defineProps<{
  storeProductId?: number | string
}>()

const stockData = ref<StoreProductStockRespVO | null>(null)
const loading = ref(false)

const loadStockData = async () => {
  if (!props.storeProductId) return
  loading.value = true
  try {
    const data = await StoreProductApi.getStockById(props.storeProductId)
    stockData.value = data
  } catch {
    stockData.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => props.storeProductId,
  () => {
    loadStockData()
  },
  { immediate: true }
)

defineExpose({ loadStockData })
</script>

<style scoped>
.stock-card {
  padding: 16px;
}
</style>
