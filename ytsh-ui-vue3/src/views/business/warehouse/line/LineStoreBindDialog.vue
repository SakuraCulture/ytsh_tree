<template>
  <Dialog v-model="dialogVisible" title="绑定参与门店" width="980px" :appendToBody="true">
    <ContentWrap>
      <el-alert
        type="warning"
        :closable="false"
        title="这里只能选择已建立当前仓供货关系的门店。若找不到门店，请先到供货关系管理页补建。"
        class="mb-12px"
      />
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="list"
        :stripe="true"
        :show-overflow-tooltip="true"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
      >
        <el-table-column type="selection" width="55" :selectable="isRowSelectable" />
        <el-table-column label="门店ID" align="center" prop="storeId" width="140" />
        <el-table-column label="门店名称" align="center" prop="storeName" min-width="180" />
        <el-table-column label="是否主仓" align="center" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isPrimary === 1 ? 'success' : 'info'">
              {{ scope.row.isPrimary === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.supplyStatus === 1 ? 'success' : 'info'">
              {{ scope.row.supplyStatus === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>
    <template #footer>
      <el-button type="primary" @click="submit" :disabled="loading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { WarehouseLineApi, type WarehouseStoreSupplySimpleRespVO } from '@/api/business/warehouse/line'

defineOptions({ name: 'BusinessWarehouseLineStoreBindDialog' })

const message = useMessage()
const dialogVisible = ref(false)
const loading = ref(false)
const list = ref<WarehouseStoreSupplySimpleRespVO[]>([])
const selectedRows = ref<WarehouseStoreSupplySimpleRespVO[]>([])
const selectedStoreIds = ref<string[]>([])
const tableRef = ref()

const handleSelectionChange = (rows: WarehouseStoreSupplySimpleRespVO[]) => {
  selectedRows.value = rows
  selectedStoreIds.value = rows.map((item) => item.storeId)
}

const isRowSelectable = (row: WarehouseStoreSupplySimpleRespVO) => row.supplyStatus === 1

const handleRowClick = (row: WarehouseStoreSupplySimpleRespVO) => {
  if (!isRowSelectable(row)) {
    return
  }
  tableRef.value?.toggleRowSelection(row)
}

const restoreSelection = async () => {
  await nextTick()
  list.value.forEach((row) => {
    if (selectedStoreIds.value.includes(row.storeId)) {
      tableRef.value?.toggleRowSelection(row, true)
    }
  })
}

const open = async (warehouseId: string, storeIds: string[] = []) => {
  dialogVisible.value = true
  selectedRows.value = []
  selectedStoreIds.value = [...storeIds]
  loading.value = true
  try {
    list.value = (await WarehouseLineApi.getEligibleStoreList(warehouseId)) || []
    await restoreSelection()
  } finally {
    loading.value = false
  }
}

defineExpose({ open })

const emit = defineEmits<{
  (e: 'success', storeIds: string[]): void
}>()

const submit = () => {
  if (selectedStoreIds.value.length === 0) {
    message.warning('请至少选择一个门店')
    return
  }
  emit('success', [...selectedStoreIds.value])
  dialogVisible.value = false
}
</script>
