<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    v-loading="formLoading"
    label-width="0px"
    :inline-message="true"
  >
    <el-table :data="formData" class="-mt-10px">
      <el-table-column label="序号" type="index" width="60" />
       <el-table-column label="联系人姓名" min-width="120">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.contactName`" :rules="formRules.contactName" class="mb-0px!">
            <el-input v-model="row.contactName" placeholder="请输入联系人姓名" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="联系人类型" min-width="120">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.contactType`" :rules="formRules.contactType" class="mb-0px!">
            <el-select v-model="row.contactType" placeholder="请选择">
                <el-option label="公司" value="COMPANY" />
                <el-option label="门店" value="STORE" />
                <el-option label="供应商" value="SUPPLIER" />
                <el-option label="物流" value="LOGISTICS" />
                <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="业务角色" min-width="120">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.contactRole`" :rules="formRules.contactRole" class="mb-0px!">
            <el-select v-model="row.contactRole" placeholder="请选择">
                <el-option label="运营" value="OPERATION" />
                <el-option label="店长" value="SUPERVISOR" />
                <el-option label="财务" value="FINANCE" />
                <el-option label="老板" value="OWNER" />
                <el-option label="经理" value="MANAGER" />
                <el-option label="采购" value="PROCUREMENT" />
                <el-option label="仓库" value="WAREHOUSE" />
            </el-select>
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="联系电话" min-width="120">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.phone`" :rules="formRules.phone" class="mb-0px!">
            <el-input v-model="row.phone" placeholder="请输入联系电话" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="主联系人" min-width="80">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.isPrimary`" :rules="formRules.isPrimary" class="mb-0px!">
            <el-switch v-model="row.isPrimary" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="80">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.status`" :rules="formRules.status" class="mb-0px!">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="100">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.remark`" :rules="formRules.remark" class="mb-0px!">
            <el-input v-model="row.remark" placeholder="请输入备注" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" label="操作" width="60">
        <template #default="{ $index }">
          <el-button @click="handleDelete($index)" link>—</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-form>
  <el-row justify="center" class="mt-3">
    <el-button @click="handleAdd" round>+ 添加门店联系人通讯录</el-button>
  </el-row>
</template>
<script setup lang="ts">
import { TableApi } from '@/api/business/store'

const props = defineProps<{
  storeId: number | string // 门店ID（主表的关联字段，支持String和Number类型）
}>()
const formLoading = ref(false) // 表单的加载中
const formData = ref<any[]>([])
const formRules = reactive({
})
const formRef = ref() // 表单 Ref

/** 监听主表的关联字段的变化，加载对应的子表数据 */
watch(
  () => props.storeId,
  async (val) => {
    // 1. 重置表单
    formData.value = []
    // 2. val 非空，则加载数据
    if (!val) {
      return;
    }
    try {
      formLoading.value = true
      formData.value = await TableApi.getContactTableListByStoreId(val)
    } finally {
      formLoading.value = false
    }
  },
  { immediate: true }
)

/** 新增按钮操作 */
const handleAdd = () => {
  const row = {
    contactId: undefined,
    storeId: undefined,
    contactName: undefined,
    contactType: undefined,
    contactRole: undefined,
    phone: undefined,
    isPrimary: undefined,
    status: undefined,
    remark: undefined
  }
  row.storeId = props.storeId as any
  formData.value.push(row)
}

/** 删除按钮操作 */
const handleDelete = (index) => {
  formData.value.splice(index, 1)
}

/** 表单校验 */
const validate = () => {
  return formRef.value.validate()
}

/** 表单值 */
const getData = () => {
  return formData.value
}

defineExpose({ validate, getData })
</script>