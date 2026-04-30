<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760" :appendToBody="true">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="formLoading"
    >
      <el-form-item label="仓库选择" prop="warehouseId">
        <el-select v-model="formData.warehouseId" placeholder="请选择仓库" filterable class="!w-100%">
          <el-option
            v-for="item in warehouseList"
            :key="item.warehouseId"
            :label="`${item.warehouseId} - ${item.warehouseName}`"
            :value="item.warehouseId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="线路编码" prop="lineCode">
        <el-input v-model="formData.lineCode" placeholder="请输入线路编码" />
      </el-form-item>
      <el-form-item label="线路名称" prop="lineName">
        <el-input v-model="formData.lineName" placeholder="请输入线路名称" />
      </el-form-item>
      <el-form-item label="可下单星期" prop="orderWeekdays">
        <el-checkbox-group v-model="formData.orderWeekdays">
          <el-checkbox v-for="item in weekdayOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="参与门店" prop="storeIds">
        <div class="store-selector-row">
          <el-input :model-value="selectedStoreText" readonly placeholder="请选择参与门店" />
          <el-button type="primary" plain @click="openStoreDialog" :disabled="!formData.warehouseId">
            选择门店
          </el-button>
          <el-button v-if="formData.storeIds?.length" @click="clearStoreSelection">清空</el-button>
        </div>
        <el-alert
          type="info"
          :closable="false"
          title="先建供货关系，再绑定线路门店。未建立该仓供货关系的门店不会出现在可选列表中。"
          class="mt-10px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="lineStatus">
        <el-select v-model="formData.lineStatus" placeholder="请选择状态" class="!w-100%">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <LineStoreBindDialog ref="storeBindDialogRef" @success="handleStoreBindSuccess" />
</template>

<script setup lang="ts">
import {
  WarehouseLineApi,
  type WarehouseLineRespVO,
  type WarehouseLineSaveReqVO
} from '@/api/business/warehouse/line'
import { WarehouseApi, type WarehouseSimpleRespVO } from '@/api/business/warehouse/warehouse'
import LineStoreBindDialog from './LineStoreBindDialog.vue'

defineOptions({ name: 'BusinessWarehouseLineForm' })

interface WarehouseLineFormData extends WarehouseLineSaveReqVO {
  warehouseId?: string
  lineCode?: string
  lineName?: string
  orderWeekdays?: number[]
  lineStatus?: number
  remark?: string
  storeIds?: string[]
}

const { t } = useI18n()
const message = useMessage()

const weekdayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const storeBindDialogRef = ref()
const warehouseList = ref<WarehouseSimpleRespVO[]>([])

const createDefaultFormData = (): WarehouseLineFormData => ({
  lineId: undefined,
  warehouseId: undefined,
  lineCode: undefined,
  lineName: undefined,
  orderWeekdays: [],
  lineStatus: 1,
  remark: undefined,
  storeIds: []
})

const formData = ref<WarehouseLineFormData>(createDefaultFormData())
const formRules = reactive({
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  lineCode: [{ required: true, message: '请输入线路编码', trigger: 'blur' }],
  lineName: [{ required: true, message: '请输入线路名称', trigger: 'blur' }],
  orderWeekdays: [{ required: true, message: '请选择可下单星期', trigger: 'change' }],
  storeIds: [{ required: true, message: '请选择参与门店', trigger: 'change' }],
  lineStatus: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

const selectedStoreText = computed(() => {
  const count = formData.value.storeIds?.length || 0
  return count > 0 ? `已选择 ${count} 个门店` : ''
})

const loadWarehouseList = async () => {
  warehouseList.value = (await WarehouseApi.getWarehouseSimpleList()) || []
}

const openStoreDialog = () => {
  if (!formData.value.warehouseId) {
    message.warning('请先选择仓库')
    return
  }
  storeBindDialogRef.value.open(formData.value.warehouseId, formData.value.storeIds || [])
}

const handleStoreBindSuccess = (storeIds: string[]) => {
  formData.value.storeIds = storeIds
}

const clearStoreSelection = () => {
  formData.value.storeIds = []
}

watch(
  () => formData.value.warehouseId,
  (value, oldValue) => {
    if (!dialogVisible.value || !oldValue || value === oldValue) {
      return
    }
    clearStoreSelection()
    message.warning('仓库已变更，请重新选择参与门店')
  }
)

const open = async (type: 'create' | 'update', lineId?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    await loadWarehouseList()
    if (!lineId) {
      return
    }
    const data = (await WarehouseLineApi.get(lineId)) as WarehouseLineRespVO
    formData.value = {
      ...createDefaultFormData(),
      ...data,
      orderWeekdays: data.orderWeekdays || [],
      storeIds: data.storeIds || []
    }
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const payload: WarehouseLineSaveReqVO = {
      lineId: formType.value === 'update' ? formData.value.lineId : undefined,
      warehouseId: formData.value.warehouseId,
      lineCode: formData.value.lineCode,
      lineName: formData.value.lineName,
      orderWeekdays: formData.value.orderWeekdays,
      lineStatus: formData.value.lineStatus,
      remark: formData.value.remark,
      storeIds: formData.value.storeIds
    }
    if (formType.value === 'create') {
      await WarehouseLineApi.create(payload)
      message.success(t('common.createSuccess'))
    } else {
      await WarehouseLineApi.update(payload)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  formRef.value?.resetFields()
}
</script>

<style scoped>
.store-selector-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  width: 100%;
}
</style>
