<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="query-form"
      :model="queryParams"
      ref="queryFormRef"
      label-width="85px"
    >
      <div class="query-form-grid">
        <el-form-item label="SPU编码" prop="productSpuCode">
          <el-input
            v-model="queryParams.productSpuCode"
            placeholder="请输入SPU编码"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="SPU名称" prop="productSpuName">
          <el-input
            v-model="queryParams.productSpuName"
            placeholder="请输入SPU名称"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="品牌" prop="productBrand">
          <el-input
            v-model="queryParams.productBrand"
            placeholder="请输入品牌"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-tree-select
            v-model="queryParams.categoryId"
            :data="categoryTreeData"
            :props="{ label: 'categoryName', value: 'categoryId', children: 'children' }"
            check-strictly
            default-expand-all
            placeholder="选择类目"
            clearable
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="产地" prop="productOrigin">
          <el-input
            v-model="queryParams.productOrigin"
            placeholder="请输入产地"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="生产商" prop="productManufacturer">
          <el-input
            v-model="queryParams.productManufacturer"
            placeholder="请输入生产商"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="规格模板" prop="productSpecTemplate">
          <el-input
            v-model="queryParams.productSpecTemplate"
            placeholder="请输入规格模板"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="SKU编码" prop="productSkuCode">
          <el-input
            v-model="queryParams.productSkuCode"
            placeholder="请输入SKU编码"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="SKU名称" prop="productSkuName">
          <el-input
            v-model="queryParams.productSkuName"
            placeholder="请输入SKU名称"
            clearable
            @keyup.enter="handleQuery"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="商品标签" prop="tagValueId">
          <el-select
            v-model="queryParams.tagValueId"
            placeholder="请选择标签"
            clearable
            filterable
            class="!w-100%"
          >
            <el-option
              v-for="item in selectableTagList"
              :key="item.tagValueId"
              :label="`${item.tagValueName}（${item.tagValueCode}）`"
              :value="item.tagValueId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="productSpuStatus">
          <el-select
            v-model="queryParams.productSpuStatus"
            placeholder="请选择状态"
            clearable
            class="!w-100%"
          >
            <el-option label="下架" :value="0" />
            <el-option label="上架" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime">
          <el-date-picker
            v-model="queryParams.createTime"
            value-format="YYYY-MM-DD HH:mm:ss"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
            class="!w-100%"
          />
        </el-form-item>
      </div>
      <!-- 操作按钮单独一行 -->
      <div class="query-actions">
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['business:spu-table:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-dropdown trigger="click" @command="handleDownloadTemplate">
          <el-button type="info" plain :loading="templateLoading">
            <Icon icon="ep:document" class="mr-5px" /> 模板下载
            <Icon icon="ep:arrow-down" class="ml-5px" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="excel">
                <Icon icon="ep:document" class="mr-5px" /> Excel 模板
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-upload
          accept=".xlsx, .xls"
          :show-file-list="false"
          :before-upload="handleImport"
        >
          <el-button type="primary" plain :loading="importLoading">
            <Icon icon="ep:upload" class="mr-5px" /> 导入
          </el-button>
        </el-upload>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['business:spu-table:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="isEmpty(checkedIds)"
          @click="handleDeleteBatch"
          v-hasPermi="['business:spu-table:delete']"
        >
          <Icon icon="ep:delete" class="mr-5px" /> 批量删除
        </el-button>
      </div>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table
        ref="tableRef"
        row-key="productSpuId"
        v-loading="loading"
        :data="tableData"
        :stripe="true"
        :show-overflow-tooltip="true"
        @selection-change="handleRowCheckboxChange"
    >
    <el-table-column type="selection" width="55" />
      <el-table-column type="expand" width="50">
        <template #default="{ row }">
          <div v-if="row.skuTables && row.skuTables.length" class="sku-expand-wrapper">
            <div class="sku-expand-container">
              <el-table
                :data="row.skuTables"
                :stripe="false"
                size="small"
                class="sku-table"
                :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
              >
                <el-table-column width="50" />
                <el-table-column type="expand" width="50">
                  <template #default="{ row: skuRow }">
                    <div class="upc-nested-container">
                      <div v-if="skuRow.upcTables && skuRow.upcTables.length" class="upc-content">
                        <el-table
                          :data="skuRow.upcTables"
                          :stripe="false"
                          size="small"
                          class="upc-table"
                          :header-cell-style="{ background: '#fafafa', color: '#909399' }"
                        >
                          <el-table-column label="UPC码类型" align="center" prop="productUpcType" width="200" />
                          <el-table-column label="UPC码值" align="center" prop="productUpcValue" min-width="414" />
                          <el-table-column label="是否主码" align="center" width="200">
                            <template #default="scope">
                              <el-tag :type="scope.row.productUpcIsPrimary === 1 ? 'warning' : 'info'" size="small">
                                {{ scope.row.productUpcIsPrimary === 1 ? '主码' : '副码' }}
                              </el-tag>
                            </template>
                          </el-table-column>
                          <el-table-column label="状态" align="center" width="200">
                            <template #default="scope">
                              <el-tag :type="scope.row.productUpcStatus === 1 ? 'success' : 'danger'" size="small">
                                {{ scope.row.productUpcStatus === 1 ? '启用' : '禁用' }}
                              </el-tag>
                            </template>
                          </el-table-column>
                          <el-table-column label="操作" align="center" width="140">
                            <template #default="scope">
                              <el-button type="primary" link size="small" @click="handleEditUpc(skuRow, scope.row)">编辑</el-button>
                              <el-button type="danger" link size="small" @click="handleDeleteUpc(scope.row)">删除</el-button>
                            </template>
                          </el-table-column>
                          <el-table-column label="新增" align="center" width="100">
                            <template #default>
                              <el-button type="primary" link size="small" @click="handleAddUpc(skuRow)">
                                <Icon icon="ep:plus" />
                              </el-button>
                            </template>
                          </el-table-column>
                        </el-table>
                      </div>
                      <div v-else class="no-upc-tip">
                        <span>暂无UPC码信息</span>
                        <el-button type="primary" link size="small" @click="handleAddUpc(skuRow)" class="ml-10px">
                          <Icon icon="ep:plus" class="mr-2px" /> 新增UPC码
                        </el-button>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="SKU编码" align="center" prop="productSkuCode" width="120" />
                <el-table-column label="SKU名称" align="center" prop="productSkuName" min-width="362" />
                <el-table-column label="EAN码" align="center" prop="productSkuEan" width="120" />
                <el-table-column label="重量" align="center" width="120">
                  <template #default="scope">
                    {{ scope.row.productWeight }} {{ scope.row.productWeightUnit }}
                  </template>
                </el-table-column>
                <el-table-column label="尺寸(cm)" align="center" width="120">
                  <template #default="scope">
                    {{ scope.row.productLength }}×{{ scope.row.productWidth }}×{{ scope.row.productHeight }}
                  </template>
                </el-table-column>
                <el-table-column label="成本价" align="center" prop="productCostPrice" width="120" />
                <el-table-column label="零售价" align="center" prop="productRetailPrice" width="120" />
                <el-table-column label="主图" align="center" width="120">
                  <template #default="scope">
                    <el-image
                      v-if="scope.row.productImageUrl"
                      :src="scope.row.productImageUrl"
                      :preview-src-list="[scope.row.productImageUrl]"
                      fit="cover"
                      preview-teleported
                      class="sku-image"
                    />
                    <span v-else class="no-image">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" align="center" prop="productSkuStatus" width="120">
                  <template #default="scope">
                    <el-tag :type="scope.row.productSkuStatus === 1 ? 'success' : 'info'" size="small">
                      {{ scope.row.productSkuStatus === 1 ? '上架' : '下架' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" align="center" width="120">
                  <template #default="scope">
                    <el-button type="primary" link size="small" @click="handleEditSku(scope.row)">编辑</el-button>
                    <el-button type="danger" link size="small" @click="handleDeleteSku(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
          <div v-else class="no-sku-tip">暂无SKU信息</div>
        </template>
      </el-table-column>
      <!-- <el-table-column label="SPU ID" align="center" prop="productSpuId" width="100" /> -->
      <el-table-column label="SPU编码" align="center" prop="productSpuCode" width="120" />
      <el-table-column label="SPU名称" align="center" prop="productSpuName" min-width="150" />
      <el-table-column label="品牌" align="center" prop="productBrand" width="100" />
      <el-table-column label="类目名称" align="center" prop="categoryName" width="120">
        <template #default="{ row }">
          {{ getCategoryName(row.categoryId) }}
        </template>
      </el-table-column>
      <el-table-column label="产地" align="center" prop="productOrigin" width="120" />
      <el-table-column label="生产商" align="center" prop="productManufacturer" width="150" />
      <el-table-column label="规格模板" align="center" prop="productSpecTemplate" width="120" />
      <el-table-column label="状态" align="center" prop="productSpuStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.productSpuStatus === 1 ? 'success' : 'info'">
            {{ scope.row.productSpuStatus === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="220">
        <template #default="{ row }">
          <el-space wrap>
            <el-tag
              v-for="tag in row.tags || []"
              :key="`${row.productSpuId}-${tag.tagValueId}`"
              type="success"
            >
              {{ tag.tagValueName }}
            </el-tag>
            <span v-if="!(row.tags || []).length" class="text-gray-400">-</span>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" fixed="right" width="220">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.productSpuId)"
            v-hasPermi="['business:spu-table:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="primary"
            @click="openTagForm(scope.row.productSpuId)"
            v-hasPermi="['business:spu-table:update']"
          >
            管理标签
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.productSpuId)"
            v-hasPermi="['business:spu-table:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <SpuTableForm ref="formRef" @success="getList" />
  <ProductSpuTagForm ref="tagFormRef" @success="handleTagSaved" />

  <!-- UPC码表单弹窗 -->
  <el-dialog v-model="upcDialogVisible" :title="upcDialogTitle" width="500px" destroy-on-close>
    <el-form ref="upcFormRef" :model="upcFormData" :rules="upcFormRules" label-width="100px">
      <el-form-item label="UPC码类型" prop="productUpcType">
        <el-select v-model="upcFormData.productUpcType" placeholder="请选择UPC码类型" class="!w-100%">
          <el-option label="UPC-A" value="UPC-A" />
          <el-option label="EAN-13" value="EAN-13" />
          <el-option label="CODE128" value="CODE128" />
          <el-option label="EAN-8" value="EAN-8" />
        </el-select>
      </el-form-item>
      <el-form-item label="UPC码值" prop="productUpcValue">
        <el-input v-model="upcFormData.productUpcValue" placeholder="请输入UPC码值" />
      </el-form-item>
      <el-form-item label="是否主码" prop="productUpcIsPrimary">
        <el-radio-group v-model="upcFormData.productUpcIsPrimary">
          <el-radio :value="1">是</el-radio>
          <el-radio :value="0">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态" prop="productUpcStatus">
        <el-radio-group v-model="upcFormData.productUpcStatus">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="upcDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitUpcForm">确定</el-button>
    </template>
  </el-dialog>

  <!-- SKU编辑弹窗 -->
  <el-dialog v-model="skuDialogVisible" :title="skuDialogTitle" width="600px" destroy-on-close>
    <el-form ref="skuFormRef" :model="skuFormData" :rules="skuFormRules" label-width="120px">
      <el-form-item label="SKU编码" prop="productSkuCode">
        <el-input v-model="skuFormData.productSkuCode" placeholder="请输入SKU编码" />
      </el-form-item>
      <el-form-item label="SKU名称" prop="productSkuName">
        <el-input v-model="skuFormData.productSkuName" placeholder="请输入SKU名称" />
      </el-form-item>
      <el-form-item label="主EAN码" prop="productSkuEan">
        <el-input v-model="skuFormData.productSkuEan" placeholder="请输入主EAN码" />
      </el-form-item>
      <el-form-item label="重量" prop="productWeight">
        <el-input-number v-model="skuFormData.productWeight" :min="0" />
        <el-select v-model="skuFormData.productWeightUnit" placeholder="单位" class="!w-100px">
          <el-option label="kg" value="kg" />
          <el-option label="g" value="g" />
        </el-select>
      </el-form-item>
      <el-form-item label="尺寸(cm)" prop="productLength">
        <div class="dimension-inputs">
          <div class="dimension-row">
            <span class="dimension-label">长:</span>
            <el-input-number v-model="skuFormData.productLength" :min="0" />
          </div>
          <div class="dimension-row">
            <span class="dimension-label">宽:</span>
            <el-input-number v-model="skuFormData.productWidth" :min="0" />
          </div>
          <div class="dimension-row">
            <span class="dimension-label">高:</span>
            <el-input-number v-model="skuFormData.productHeight" :min="0" />
          </div>
        </div>
      </el-form-item>
      <el-form-item label="基准成本价" prop="productCostPrice">
        <el-input-number v-model="skuFormData.productCostPrice" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="基准零售价" prop="productRetailPrice">
        <el-input-number v-model="skuFormData.productRetailPrice" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="SKU主图URL" prop="productImageUrl">
        <el-input v-model="skuFormData.productImageUrl" placeholder="请输入主图URL" />
      </el-form-item>
      <el-form-item label="状态" prop="productSkuStatus">
        <el-radio-group v-model="skuFormData.productSkuStatus">
          <el-radio :value="1">上架</el-radio>
          <el-radio :value="0">下架</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="skuDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitSkuForm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { isEmpty } from '@/utils/is'
import { dateFormatter } from '@/utils/formatTime'
import { handleTree } from '@/utils/tree'
import download from '@/utils/download'
import {
  SpuTableApi,
  type SpuTable,
  UpcTableApi,
  type UpcTable
} from '@/api/business/product'
import { CategoryTableApi } from '@/api/business/category'
import { TagValueApi, type TagSelectableValue } from '@/api/business/tag/value'
import ProductSpuTagForm from './ProductSpuTagForm.vue'
import SpuTableForm from './SpuTableForm.vue'

/** SPU基础分类 列表 */
defineOptions({ name: 'SpuTable' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<SpuTable[]>([]) // 列表的数据
const tableData = ref<SpuTable[]>([]) // 树形表格数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  productSpuCode: undefined,
  productSpuName: undefined,
  productBrand: undefined,
  categoryId: undefined,
  productOrigin: undefined,
  productManufacturer: undefined,
  productSpecTemplate: undefined,
  productImageUrl: undefined,
  productDetailImages: undefined,
  productDescription: undefined,
  productSpuStatus: undefined,
  productSkuCode: undefined,
  productSkuName: undefined,
  tagValueId: undefined as number | undefined,
  createTime: [] as string[]
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const importLoading = ref(false) // 导入的加载中
const templateLoading = ref(false) // 模板下载的加载中
const tableRef = ref()
const categoryMap = ref<Map<number, string>>(new Map()) // categoryId -> categoryName
const categoryTreeData = ref<any[]>([]) // 类目树形数据
const selectableTagList = ref<TagSelectableValue[]>([])
const tagFormRef = ref()

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await SpuTableApi.getSpuTableAggregatePage(queryParams)
    list.value = data.list
    total.value = data.total
    tableData.value = data.list
  } finally {
    loading.value = false
  }
}

const loadSelectableTags = async () => {
  selectableTagList.value = await TagValueApi.getTagValueListForObject('SPU')
}

/** 搜索按钮操作 */
const handleQuery = () => {
  tableData.value.forEach(row => {
    tableRef.value?.toggleRowExpansion(row, false)
  })
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  tableData.value.forEach(row => {
    tableRef.value?.toggleRowExpansion(row, false)
  })
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const openTagForm = (productSpuId: number) => {
  tagFormRef.value.open(productSpuId)
}

const handleTagSaved = async () => {
  await getList()
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await SpuTableApi.deleteSpuTable(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 批量删除SPU基础分类 */
const handleDeleteBatch = async () => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    await SpuTableApi.deleteSpuTableList(checkedIds.value);
    checkedIds.value = [];
    message.success(t('common.delSuccess'))
    await getList();
  } catch {}
}

/** UPC码相关 */
const currentSkuId = ref<number | null>(null)
const upcFormRef = ref()
const upcFormData = ref({
  productUpcId: undefined as number | undefined,
  productSkuId: undefined as number | undefined,
  productUpcType: '',
  productUpcValue: '',
  productUpcIsPrimary: 0,
  productUpcStatus: 1
})
const upcFormRules = {
  productUpcType: [{ required: true, message: '请选择UPC码类型', trigger: 'change' }],
  productUpcValue: [
    { required: true, message: '请输入UPC码值', trigger: 'blur' },
    { pattern: /^\d+$/, message: 'UPC码只能包含数字', trigger: 'blur' }
  ],
  productUpcIsPrimary: [{ required: true, message: '请选择是否主码', trigger: 'change' }],
  productUpcStatus: [{ required: true, message: '请选择状态', trigger: 'change' }]
}
const upcDialogVisible = ref(false)
const upcDialogTitle = ref('')

/** SKU编辑相关 */
const skuDialogVisible = ref(false)
const skuDialogTitle = ref('')
const skuFormRef = ref()
const skuFormData = ref({
  productSkuId: undefined as number | undefined,
  productSpuId: undefined as number | undefined,
  productSkuCode: '',
  productSkuName: '',
  productSkuEan: '',
  productWeight: 0,
  productWeightUnit: 'kg',
  productLength: 0,
  productWidth: 0,
  productHeight: 0,
  productCostPrice: 0,
  productRetailPrice: 0,
  productImageUrl: '',
  productSkuStatus: 1
})
const skuFormRules = {
  productSkuCode: [{ required: true, message: '请输入SKU编码', trigger: 'blur' }],
  productSkuName: [{ required: true, message: '请输入SKU名称', trigger: 'blur' }],
  productWeight: [{ required: true, message: '请输入重量', trigger: 'blur' }],
  productCostPrice: [{ required: true, message: '请输入基准成本价', trigger: 'blur' }],
  productRetailPrice: [{ required: true, message: '请输入基准零售价', trigger: 'blur' }]
}

const handleAddUpc = (row: any) => {
  // 兼容 SPU 行和 SKU 行的参数
  let skuRow = row
  if (row.skuTables) {
    // 传入的是 SPU 行，取第一个 SKU
    const firstSku = row.skuTables?.[0]
    if (!firstSku) {
      message.warning('请先添加SKU')
      return
    }
    skuRow = firstSku
  }
  currentSkuId.value = skuRow.productSkuId
  upcFormData.value = {
    productUpcId: undefined,
    productSkuId: skuRow.productSkuId,
    productUpcType: '',
    productUpcValue: '',
    productUpcIsPrimary: 0,
    productUpcStatus: 1
  }
  upcDialogTitle.value = '新增UPC码'
  upcDialogVisible.value = true
}

const handleEditUpc = (sku: any, upc: UpcTable) => {
  currentSkuId.value = sku.productSkuId
  upcFormData.value = {
    productUpcId: upc.productUpcId,
    productSkuId: upc.productSkuId,
    productUpcType: upc.productUpcType,
    productUpcValue: upc.productUpcValue,
    productUpcIsPrimary: upc.productUpcIsPrimary,
    productUpcStatus: upc.productUpcStatus
  }
  upcDialogTitle.value = '编辑UPC码'
  upcDialogVisible.value = true
}

const handleDeleteUpc = async (upc: UpcTable) => {
  try {
    await message.delConfirm()
    await UpcTableApi.deleteUpcTable(upc.productUpcId)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleDeleteSku = async (sku: any) => {
  try {
    await message.delConfirm()
    const fullSkuList = await SpuTableApi.getSkuTableListByProductSpuId(sku.productSpuId)
    const index = fullSkuList.findIndex(s => s.productSkuId === sku.productSkuId)
    if (index !== -1) {
      fullSkuList.splice(index, 1)
    }
    await SpuTableApi.updateSpuTable({
      productSpuId: sku.productSpuId,
      skuTables: fullSkuList
    })
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleEditSku = (sku: any) => {
  skuFormData.value = {
    productSkuId: sku.productSkuId,
    productSpuId: sku.productSpuId,
    productSkuCode: sku.productSkuCode,
    productSkuName: sku.productSkuName,
    productSkuEan: sku.productSkuEan,
    productWeight: sku.productWeight || 0,
    productWeightUnit: sku.productWeightUnit || 'kg',
    productLength: sku.productLength || 0,
    productWidth: sku.productWidth || 0,
    productHeight: sku.productHeight || 0,
    productCostPrice: sku.productCostPrice || 0,
    productRetailPrice: sku.productRetailPrice || 0,
    productImageUrl: sku.productImageUrl || '',
    productSkuStatus: sku.productSkuStatus ?? 1
  }
  skuDialogTitle.value = '编辑SKU'
  skuDialogVisible.value = true
}

const submitUpcForm = async () => {
  try {
    const formEl = upcFormRef.value
    await formEl.validate()
    if (upcFormData.value.productUpcId) {
      await UpcTableApi.updateUpcTable(upcFormData.value)
      message.success('更新成功')
    } else {
      await UpcTableApi.createUpcTable(upcFormData.value)
      message.success('新增成功')
    }
    upcDialogVisible.value = false
    await getList()
  } catch {}
}

const submitSkuForm = async () => {
  try {
    const formEl = skuFormRef.value
    await formEl.validate()
    const fullSkuList = await SpuTableApi.getSkuTableListByProductSpuId(skuFormData.value.productSpuId)
    const index = fullSkuList.findIndex(s => s.productSkuId === skuFormData.value.productSkuId)
    if (index !== -1) {
      fullSkuList[index] = skuFormData.value
    }
    const spuData = {
      productSpuId: skuFormData.value.productSpuId,
      skuTables: fullSkuList
    }
    await SpuTableApi.updateSpuTable(spuData)
    message.success('更新成功')
    skuDialogVisible.value = false
    await getList()
  } catch (error) {
    console.error('更新失败:', error)
  }
}

const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (records: SpuTable[]) => {
  checkedIds.value = records.map((item) => item.productSpuId!);
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await SpuTableApi.exportSpuTable(queryParams)
    download.excel(data, 'SPU基础分类.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 模板下载 */
const handleDownloadTemplate = async (command: string) => {
  try {
    templateLoading.value = true
    const data = await SpuTableApi.getImportTemplate()
    download.excel(data, 'SPU_SKU_UPC导入模板.xls')
    message.success('模板下载成功')
  } catch {
  } finally {
    templateLoading.value = false
  }
}

/** 导入按钮操作 */
const handleImport = async (file: File) => {
  try {
    await message.importConfirm()
    importLoading.value = true
    const res = await SpuTableApi.importSpuTable(file, true)
    // 处理导入结果
    if (res.spuSuccessCount > 0) {
      message.success(`成功导入 ${res.spuSuccessCount} 条SPU数据`)
    }
    if (res.skuSuccessCount > 0) {
      message.success(`成功导入 ${res.skuSuccessCount} 条SKU数据`)
    }
    if (res.upcSuccessCount > 0) {
      message.success(`成功导入 ${res.upcSuccessCount} 条UPC码数据`)
    }
    // 处理失败记录
    if (res.failureList && res.failureList.length > 0) {
      const failureText = res.failureList
        .map((item: any) => `${item.row}: ${item.message}`)
        .join('\n')
      message.warning(`导入失败 ${res.failureList.length} 条：\n${failureText}`)
    }
    await getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'cls') {
      message.error('导入失败，请重试')
    }
  } finally {
    importLoading.value = false
  }
  return false
}

/** 初始化 **/
onMounted(async () => {
  await Promise.all([
    getList(),
    fetchCategoryData().catch(() => undefined),
    loadSelectableTags().catch(() => undefined)
  ])
})

/** 获取类目数据（映射和树形） */
const fetchCategoryData = async () => {
  const data = await CategoryTableApi.getCategoryTableList()
  const list = data.list || data || []
  categoryMap.value.clear()
  list.forEach(item => {
    categoryMap.value.set(item.categoryId, item.categoryName)
  })
  const root = { categoryId: 0, categoryName: '顶级（无父类目）', children: [] }
  root.children = handleTree(list, 'categoryId', 'parentId')
  categoryTreeData.value = [root]
}

/** 通过categoryId获取类目名称 */
const getCategoryName = (categoryId: number | undefined): string => {
  if (!categoryId) return '-'
  return categoryMap.value.get(categoryId) || `类目${categoryId}`
}
</script>

<style scoped>
.query-form {
  padding-bottom: 10px;
}
.query-form-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  align-items: flex-start;
}
.query-form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}
.query-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: flex-start;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;
  margin-top: 8px;
}
.sku-expand-wrapper {
  padding: 8px 0;
}
.sku-expand-container {
  margin-left: 16px;
  padding: 8px 12px;
  border-left: 1px solid #e5e7eb;
  border-radius: 4px;
}
.sku-table {
  width: 100%;
  background-color: transparent;
}
.sku-table :deep(.el-table__header th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
  font-size: 13px;
}
.sku-table :deep(.el-table__row) {
  background-color: #fff;
  font-size: 13px;
}
.sku-table :deep(.el-table__row:hover > td) {
  background-color: #f5f7fa;
}
.upc-nested-container {
  margin-left: 32px;
  padding: 8px 12px;
  border-left: 1px solid #e5e7eb;
  border-radius: 4px;
}
.upc-table {
  width: 100%;
  background-color: transparent;
}
.upc-table :deep(.el-table__header th) {
  background-color: #fafafa;
  color: #606266;
  font-weight: 600;
  font-size: 12px;
}
.upc-table :deep(.el-table__row) {
  background-color: #fff;
  font-size: 12px;
}
.sku-image {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  object-fit: cover;
}
.no-image {
  color: #c0c4cc;
}
.no-upc-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  color: #909399;
  font-size: 13px;
}
.no-sku-tip {
  padding: 16px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.dimension-inputs {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.dimension-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.dimension-label {
  width: 24px;
  text-align: justify;
}
:deep(.el-table__expand-icon) {
  color: #409eff;
  font-size: 16px;
  transition: transform 200ms ease-out;
}
:deep(.el-table__expand-icon--expanded) {
  transform: rotate(90deg);
}
@media (max-width: 1200px) {
  .sku-expand-container {
    overflow-x: auto;
  }
}
</style>
