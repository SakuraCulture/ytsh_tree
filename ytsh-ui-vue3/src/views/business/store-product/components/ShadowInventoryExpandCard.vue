<template>
  <div v-loading="loading" class="shadow-card">
    <div class="shadow-header">
      <span class="shadow-title">影子商品库存</span>
      <el-button link type="primary" :loading="refreshing" @click="handleRefreshInventory">
        刷新库存
      </el-button>
    </div>

    <InventoryMetricsCard :model="displayMetrics" />

    <div v-if="reasonText" class="shadow-reason">原因：{{ reasonText }}</div>
    <div class="shadow-tip">影子商品请前往治理池处理</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import type { StoreProductTable } from '@/api/business/store-product'
import { queryStoreInventory } from '@/api/ele/storeInventory'
import { getShadow, type StoreGoodsShadowRespVO } from '@/api/ele/storeGoods'
import InventoryMetricsCard from '@/views/ele/components/InventoryMetricsCard.vue'
import {
  adaptEleInventoryMetrics,
  adaptShadowSnapshotMetrics,
  buildInventoryQueryRequest,
  findMatchingInventoryRow
} from '@/views/ele/storeInventoryPageLogic'

defineOptions({ name: 'ShadowInventoryExpandCard' })

const props = defineProps<{
  row: StoreProductTable
}>()

const loading = ref(false)
const refreshing = ref(false)
const shadowDetail = ref<StoreGoodsShadowRespVO | null>(null)
const realtimeMetrics = ref<ReturnType<typeof adaptEleInventoryMetrics> | null>(null)

const displayMetrics = computed(() => {
  return realtimeMetrics.value || adaptShadowSnapshotMetrics(shadowDetail.value)
})

const reasonText = computed(() => {
  return shadowDetail.value?.reasonMsg || shadowDetail.value?.conflictReason || ''
})

const loadShadowDetail = async () => {
  if (!props.row.shadowId) {
    shadowDetail.value = null
    return
  }

  loading.value = true
  realtimeMetrics.value = null
  try {
    shadowDetail.value = await getShadow(Number(props.row.shadowId))
  } catch {
    shadowDetail.value = null
  } finally {
    loading.value = false
  }
}

const handleRefreshInventory = async () => {
  try {
    const payload = buildInventoryQueryRequest({
      platformStoreId: shadowDetail.value?.platformStoreId || props.row.platformStoreId || '',
      merchantCode: shadowDetail.value?.merchantCode || '',
      erpStoreCode: shadowDetail.value?.erpStoreCode || '',
      skuCodesText: shadowDetail.value?.skuCode || props.row.skuCode || ''
    })

    refreshing.value = true
    const data = await queryStoreInventory(payload)
    const refreshedRow = findMatchingInventoryRow(data.inventoryRows, {
      skuCode: shadowDetail.value?.skuCode || props.row.skuCode || '',
      subSkuCode: shadowDetail.value?.subSkuCode || ''
    })

    if (!refreshedRow) {
      ElMessage.warning('本次未查询到库存结果')
      return
    }

    realtimeMetrics.value = adaptEleInventoryMetrics(refreshedRow)
    ElMessage.success('影子商品库存已刷新')
  } catch (error: any) {
    ElMessage.error(error?.message || '刷新库存失败')
  } finally {
    refreshing.value = false
  }
}

watch(
  () => props.row.shadowId,
  () => {
    loadShadowDetail()
  },
  { immediate: true }
)
</script>

<style scoped>
.shadow-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.shadow-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.shadow-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.shadow-reason,
.shadow-tip {
  font-size: 12px;
  color: #64748b;
}
</style>
