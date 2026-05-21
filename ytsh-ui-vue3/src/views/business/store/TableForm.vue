<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="门店编码" prop="storeId">
          <el-input v-model="formData.storeId" placeholder="请输入门店编码" />
        </el-form-item>
      <el-form-item label="门店名称" prop="storeName">
        <el-input v-model="formData.storeName" placeholder="请输入门店名称" />
      </el-form-item>
      <el-form-item label="行政区划" prop="regionCode">
        <AreaSelect
          v-model="formData.regionCode"
          :level="AreaLevelEnum.DISTRICT"
          placeholder="请选择行政区划"
          clearable
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item label="详细地址" prop="address">
        <el-input v-model="formData.address" type="textarea" placeholder="请输入详细地址" />
      </el-form-item>
      <el-form-item label="门店区域" prop="area">
        <el-select v-model="formData.area" placeholder="请选择区域" class="!w-100%">
          <el-option label="华东" value="EAST" />
          <el-option label="华北" value="NORTH" />
          <el-option label="华南" value="SOUTH" />
          <el-option label="华西" value="WEST" />
          <el-option label="华中" value="CENTRAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="storeStatus">
        <el-radio-group v-model="formData.storeStatus">
          <el-radio :value="0">停用</el-radio>
          <el-radio :value="1">正常</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <!-- 子表的表单 -->
    <el-tabs v-model="subTabsName">
      <el-tab-pane label="门店空间" name="spaceTable">
        <SpaceTableForm ref="spaceTableFormRef" :store-id="formData.storeId" />
      </el-tab-pane>
      <el-tab-pane label="门店架构归属" name="affiliationTable">
        <AffiliationTableForm ref="affiliationTableFormRef" :store-id="formData.storeId" />
      </el-tab-pane>
      <el-tab-pane label="门店经营状态" name="statusTable">
        <StatusTableForm ref="statusTableFormRef" :store-id="formData.storeId" />
      </el-tab-pane>
      <el-tab-pane label="门店加盟商信息" name="franchiseeTable">
        <FranchiseeTableForm ref="franchiseeTableFormRef" :store-id="formData.storeId" />
      </el-tab-pane>
      <el-tab-pane label="门店联系人通讯录" name="contactTable">
        <ContactTableForm ref="contactTableFormRef" :store-id="formData.storeId" />
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { AreaLevelEnum } from '@/utils/constants'
import { TableApi, Table } from '@/api/business/store'
import SpaceTableForm from './components/SpaceTableForm.vue'
import AffiliationTableForm from './components/AffiliationTableForm.vue'
import StatusTableForm from './components/StatusTableForm.vue'
import FranchiseeTableForm from './components/FranchiseeTableForm.vue'
import ContactTableForm from './components/ContactTableForm.vue'
import AreaSelect from '@/components/FormCreate/src/components/AreaSelect.vue'

/** 门店 表单 */
defineOptions({ name: 'TableForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  storeId: undefined,
  storeName: undefined,
  regionCode: undefined,
  address: undefined,
  area: undefined,
  storeStatus: 1
})
const formRules = reactive({
})
const formRef = ref() // 表单 Ref

/** 子表的表单 */
const subTabsName = ref('spaceTable')
const spaceTableFormRef = ref()
const affiliationTableFormRef = ref()
const statusTableFormRef = ref()
const franchiseeTableFormRef = ref()
const contactTableFormRef = ref()

/** 打开弹窗 */
const open = async (type: string, id?: number | string) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  
  // 修改时，设置数据 - 先设置，防止子表组件收到undefined
  if (id) {
    formLoading.value = true
    try {
      const data = await TableApi.getTable(id)
      
      // 处理回显：regionCode 可能是数字，转为数组格式
      if (data.regionCode) {
        data.regionCode = [data.regionCode]
      }
      
      formData.value = data
    } finally {
      formLoading.value = false
    }
  } else {
    // 新增时，才重置表单
    resetForm()
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  await formRef.value.validate()
  // 校验子表单
  try {
    await spaceTableFormRef.value.validate()
  } catch (e) {
    subTabsName.value = 'spaceTable'
    return
  }
  try {
    await affiliationTableFormRef.value.validate()
  } catch (e) {
    subTabsName.value = 'affiliationTable'
    return
  }
  try {
    await statusTableFormRef.value.validate()
  } catch (e) {
    subTabsName.value = 'statusTable'
    return
  }
  try {
    await franchiseeTableFormRef.value.validate()
  } catch (e) {
    subTabsName.value = 'franchiseeTable'
    return
  }
  try {
    await contactTableFormRef.value.validate()
  } catch (e) {
    subTabsName.value = 'contactTable'
    return
  }
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as Table
    // 处理行政区划：只提交最小一级ID
    if (data.regionCode && Array.isArray(data.regionCode) && data.regionCode.length > 0) {
      data.regionCode = data.regionCode[data.regionCode.length - 1] as any
    }
    // 拼接子表的数据
    data.spaceTable = spaceTableFormRef.value.getData()
    data.affiliationTable = affiliationTableFormRef.value.getData()
    data.statusTable = statusTableFormRef.value.getData()
    data.franchiseeTable = franchiseeTableFormRef.value.getData()
    data.contactTables = contactTableFormRef.value.getData()
    if (formType.value === 'create') {
      await TableApi.createTable(data)
      message.success(t('common.createSuccess'))
    } else {
      await TableApi.updateTable(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    storeId: undefined,
    storeName: undefined,
    regionCode: undefined,
    address: undefined,
    area: undefined,
    storeStatus: undefined
  }
  formRef.value?.resetFields()
}
</script>