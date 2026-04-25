<template>
  <div class="stock-card">
    <el-row :gutter="16">
      <el-col :span="4">
        <div class="stock-item">
          <div class="stock-label">可用量</div>
          <div class="stock-value">{{ stockData?.availableQuantity ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stock-item">
          <div class="stock-label">库存数量</div>
          <div class="stock-value">{{ stockData?.inventoryQuantity ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stock-item">
          <div class="stock-label">在途</div>
          <div class="stock-value">{{ stockData?.inTransitQuantity ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stock-item">
          <div class="stock-label">冻结</div>
          <div class="stock-value">{{ stockData?.frozenQuantity ?? '-' }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stock-item">
          <div class="stock-label">缺货时长</div>
          <div class="stock-value">{{ stockData?.outOfStockDuration ? stockData.outOfStockDuration + 'h' : '-' }}</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { StoreProductApi } from '@/api/business/store-product'
import { StoreProductStockRespVO } from '@/api/business/store-product'

defineOptions({ name: 'StockExpandCard' })

const props = defineProps<{
  storeProductId: number | string
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

watch(() => props.storeProductId, () => {
  loadStockData()
}, { immediate: true })

defineExpose({ loadStockData })
</script>

<style scoped>
.stock-card {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 4px;
}

.stock-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stock-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.stock-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
</style>
