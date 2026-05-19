<template>
  <div class="inventory-metrics-card">
    <div class="card-header">
      <span class="card-title">库存指标</span>
      <el-tag size="small" :type="model.source === 'formal' ? 'success' : 'warning'">
        {{ model.source === 'formal' ? '正式库存' : '实时库存' }}
      </el-tag>
    </div>

    <template v-if="model.status === 'ready'">
      <div class="metrics-grid">
        <div v-for="item in metricsItems" :key="item.label" class="metric-item">
          <span class="metric-label">{{ item.label }}</span>
          <span class="metric-value">{{ item.value }}</span>
        </div>
      </div>
      <div class="query-time">最近查询时间：{{ model.lastQueryTime || '--' }}</div>
    </template>

    <div v-else class="empty-state">暂无库存明细</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { InventoryMetricsViewModel } from '@/views/ele/storeInventoryPageLogic'

defineOptions({ name: 'InventoryMetricsCard' })

const props = defineProps<{
  model: InventoryMetricsViewModel
}>()

const metricsItems = computed(() => [
  { label: '总库存', value: formatMetric(props.model.totalStock) },
  { label: '可用库存', value: formatMetric(props.model.availableStock) },
  { label: '在途库存', value: formatMetric(props.model.inTransitStock) },
  { label: '冻结/预留', value: formatMetric(props.model.reservedStock) }
])

const formatMetric = (value: number | null) => {
  return value === null ? '--' : String(value)
}
</script>

<style scoped>
.inventory-metrics-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  border-radius: 6px;
  background: #f8fafc;
}

.metric-label,
.query-time,
.empty-state {
  color: #64748b;
  font-size: 12px;
}

.metric-value {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.query-time {
  margin-top: 10px;
}

.empty-state {
  padding: 12px 0;
}
</style>
