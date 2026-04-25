<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <div class="search-container">
      <div class="search-bar">
        <div class="search-item">
          <span class="search-label">日期</span>
          <el-radio-group v-model="dateType" class="date-radio">
            <el-radio-button label="today">今日</el-radio-button>
            <el-radio-button label="yesterday">昨日</el-radio-button>
            <el-radio-button label="week">本周</el-radio-button>
            <el-radio-button label="month">本月</el-radio-button>
            <el-radio-button label="custom">自定义</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-if="dateType === 'custom'"
            v-model="queryParams.beginTime"
            type="date"
            placeholder="开始"
            value-format="YYYY-MM-DD"
            :picker-options="beginDateOptions"
            class="date-picker"
          />
          <span v-if="dateType === 'custom'" class="date-sep">至</span>
          <el-date-picker
            v-if="dateType === 'custom'"
            v-model="queryParams.endTime"
            type="date"
            placeholder="结束"
            value-format="YYYY-MM-DD"
            :picker-options="endDateOptions"
            class="date-picker"
          />
        </div>

        <div class="search-item">
          <span class="search-label">门店</span>
          <el-select
            v-model="queryParams.storeId"
            placeholder="请选择门店"
            clearable
            filterable
            :loading="storeLoading"
            class="search-select"
          >
            <el-option
              v-for="store in storeList"
              :key="store.platformStoreId || store.storeName"
              :label="store.storeName"
              :value="store.platformStoreId || store.storeName"
              :disabled="store.storeStatus !== 1"
              class="store-option"
            />
          </el-select>
        </div>

        <div class="search-item">
          <span class="search-label">订单小号</span>
          <el-input
            v-model="queryParams.orderNo"
            placeholder="请输入"
            clearable
            class="search-input"
            @keyup.enter="handleQuery"
          />
        </div>

        <div class="search-item toggle-item">
          <el-button type="text" @click="toggleSearch" v-hasPermi="['data:test:list']">
            <span>{{ showSearch ? '收起' : '展开' }}</span>
            <i :class="showSearch ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
          </el-button>
        </div>

        <div class="search-item btn-item">
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div class="search-expand" v-show="showSearch">
        <div class="expand-bar">
          <div class="search-item">
            <span class="search-label">收货人</span>
            <el-input
              v-model="queryParams.receiverName"
              placeholder="请输入"
              clearable
              class="search-input-mini"
              @keyup.enter="handleQuery"
            />
          </div>
          <div class="search-item">
            <span class="search-label">手机号</span>
            <el-input
              v-model="queryParams.phoneSuffix"
              placeholder="后4位"
              maxlength="4"
              clearable
              class="search-input-mini"
              @keyup.enter="handleQuery"
            />
          </div>
          <div class="search-item">
            <span class="search-label">排序</span>
            <el-select
              v-model="queryParams.orderSort"
              placeholder="请选择"
              clearable
              class="search-select-mini"
            >
              <el-option label="创建时间正序" value="createTime_asc" />
              <el-option label="创建时间倒序" value="createTime_desc" />
              <el-option label="支付时间正序" value="payTime_asc" />
              <el-option label="支付时间倒序" value="payTime_desc" />
            </el-select>
          </div>
          <div class="search-item">
            <span class="search-label">商品</span>
            <el-autocomplete
              v-model="queryParams.goodsName"
              :fetch-suggestions="queryGoods"
              placeholder="请输入"
              clearable
              :trigger-on-focus="false"
              @input="handleGoodsInput"
              @select="handleSelectGoods"
              class="search-input-mini"
            >
              <template #default="{ item }"
                ><div>{{ item.name }}</div></template
              >
            </el-autocomplete>
          </div>
          <div class="search-item">
            <span class="search-label">配送</span>
            <el-select
              v-model="queryParams.deliveryType"
              placeholder="请选择"
              clearable
              class="search-select-mini"
            >
              <el-option label="即时单" value="instant" />
              <el-option label="预约单" value="appointment" />
            </el-select>
          </div>
          <div class="search-item">
            <span class="search-label">渠道</span>
            <el-select
              v-model="queryParams.channelType"
              placeholder="请选择"
              clearable
              class="search-select-mini"
            >
              <el-option label="美团" value="meituan" />
              <el-option label="饿了么" value="eleme" />
              <el-option label="自有" value="self" />
            </el-select>
          </div>
          <div class="search-item">
            <span class="search-label">渠道订单号</span>
            <el-input
              v-model="queryParams.channelOrderNo"
              placeholder="请输入"
              clearable
              class="search-input"
              @keyup.enter="handleQuery"
            />
          </div>
          <div class="search-item">
            <span class="search-label">类型</span>
            <el-select
              v-model="queryParams.orderType"
              placeholder="请选择"
              clearable
              class="search-select-mini"
            >
              <el-option label="普通订单" value="normal" />
              <el-option label="退款订单" value="refund" />
            </el-select>
          </div>
          <div class="search-item">
            <span class="search-label">地址</span>
            <el-input
              v-model="queryParams.address"
              placeholder="请输入"
              clearable
              class="search-input"
              @keyup.enter="handleQuery"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 状态标签页 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick" class="status-tabs">
      <el-tab-pane :label="'全部 (' + statusCounts.all + ')'" name="all" />
      <el-tab-pane :label="'交易成功 (' + statusCounts.done + ')'" name="done" />
      <el-tab-pane :label="'交易关闭 (' + statusCounts.closed + ')'" name="closed" />
      <template #append>
        <el-button type="text" size="small" v-hasPermi="['data:test:list']">历史记录</el-button>
        <el-dropdown v-hasPermi="['data:test:export']">
          <el-button type="text" size="small">
            导出<i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>导出全部</el-dropdown-item>
              <el-dropdown-item>导出选中</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="text" size="small" v-hasPermi="['data:test:import']"
          >批量导入配送费</el-button
        >
      </template>
    </el-tabs>

    <!-- 布局选择器 -->
    <div class="layout-selector">
      <span class="selector-label">每行</span>
      <el-radio-group v-model="gridColumns" size="small" @change="handleGridChange">
        <el-radio-button :label="1">1</el-radio-button>
        <el-radio-button :label="2">2</el-radio-button>
        <el-radio-button :label="3">3</el-radio-button>
        <el-radio-button :label="4">4</el-radio-button>
      </el-radio-group>
      <span class="selector-suffix">列</span>
    </div>

    <!-- 订单列表 -->
    <div :class="['order-list', 'order-grid-' + gridColumns]" v-loading="loading">
      <div v-for="order in orderList" :key="order.id" class="order-card">
        <!-- 订单头部 -->
        <div class="order-header">
          <div class="order-header-left">
            <div class="order-main-meta">
              <el-tag
                :type="getChannelTagType(order.channelType)"
                size="small"
                class="channel-tag"
                >{{ order.channelLabel }}</el-tag
              >
              <span class="order-no">#{{ order.orderNo }}</span>
              <span class="store-name">{{ order.storeName }}</span>
            </div>
            <div class="order-time-meta">
              <el-tag type="info" size="small" class="delivery-tag">{{
                order.deliveryTypeText
              }}</el-tag>
              <span class="expect-time">下单：{{ order.createTime }}</span>
              <span class="expect-time">支付：{{ order.payTime }}</span>
            </div>
          </div>
          <div class="order-header-right">
            <el-tag
              :type="getStatusType(order.status)"
              effect="dark"
              size="small"
              class="status-pill"
              >{{ order.statusTabText }}</el-tag
            >
            <span class="channel-order-no">渠道单号：{{ order.channelOrderId || '--' }}</span>
          </div>
        </div>

        <!-- 订单内容 -->
        <div class="order-content">
          <div class="order-status">
            <div class="info-block">
              <div class="block-title">收货信息</div>
              <div class="user-info compact">
                <p><strong>收货人：</strong>{{ order.userName }}</p>
                <p><strong>联系电话：</strong>{{ order.userPhone }}</p>
                <p><strong>收货地址：</strong>{{ order.userAddress }}</p>
                <p v-if="order.remark"><strong>订单备注：</strong>{{ order.remark }}</p>
              </div>
            </div>
            <div class="simple-info">
              <span>商品：{{ order.goodsCount }}种/{{ order.goodsQuantity }}件</span>
              <span>重量：{{ order.totalWeight }}g</span>
              <span>渠道：{{ order.channelLabel }}</span>
            </div>
          </div>
        </div>

        <!-- 配送信息 -->
        <div class="delivery-info">
          <div class="delivery-item">
            <span class="delivery-label">骑手 / 配送员</span>
            <span class="delivery-value">{{ order.deliveryNameText }}</span>
          </div>
          <div class="delivery-item">
            <span class="delivery-label">配送费</span>
            <span class="delivery-value emphasis">{{ formatAmount(order.deliveryFeeAmount) }}</span>
          </div>
          <div class="delivery-item">
            <span class="delivery-label">配送渠道</span>
            <span class="delivery-value">{{ order.deliveryPlatformText }}</span>
          </div>
          <div class="delivery-item">
            <span class="delivery-label">配送状态</span>
            <el-tag
              size="small"
              :type="getDeliveryStatusType(order.deliveryStatus)"
              effect="plain"
              class="delivery-status-tag"
              >{{ order.deliveryStatusText }}</el-tag
            >
          </div>
        </div>

        <!-- 价格信息 -->
        <div class="price-info">
          <div class="price-left">
            <span
              >用户实付：<strong class="price">{{ formatAmount(order.payFee) }}</strong></span
            >
            <span
              >订单原价：<strong class="income">{{ formatAmount(order.totalFee) }}</strong></span
            >
            <span
              >优惠合计：<strong class="profit">{{ formatAmount(order.discountFee) }}</strong></span
            >
            <span
              >包装费：<strong>{{ formatAmount(order.packageFee) }}</strong></span
            >
          </div>
          <div class="price-right">
            <el-button type="text" @click="viewOrderLogs(order)">查看日志</el-button>
            <el-button type="text" @click="openGoodsDetail(order)">查看详情</el-button>
          </div>
        </div>

        <!-- 订单底部 -->
        <div class="order-footer">
          <span>创建时间：{{ order.createTime }}</span>
          <span>支付时间：{{ order.payTime }}</span>
          <span
            >订单编号：<el-link type="primary">{{ order.orderSn }}</el-link></span
          >
          <span>渠道订单号：{{ order.channelOrderId || '--' }}</span>
        </div>
      </div>

      <!-- 无订单提示 -->
      <div v-if="!loading && orderList.length === 0" class="empty-order">
        <el-empty description="暂无订单" />
      </div>
    </div>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      v-model:total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @pagination="handlePageChange"
    />

    <!-- 查看日志弹窗 -->
    <el-dialog
      title="查看日志"
      v-model="logDialogVisible"
      width="800px"
      destroy-on-close
      :z-index="2000"
    >
      <el-table :data="currentOrderLogs" style="width: 100%" stripe border>
        <el-table-column prop="operation" label="操作" min-width="120" />
        <el-table-column prop="operator" label="操作人" min-width="100" />
        <el-table-column prop="time" label="时间" min-width="150" />
        <el-table-column prop="status" label="操作状态" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 商品详情弹窗 -->
    <el-dialog
      title="商品详情"
      v-model="goodsDetailVisible"
      width="900px"
      destroy-on-close
      :z-index="2001"
    >
      <div v-if="currentOrder" class="goods-detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="门店">{{ currentOrder.storeName }}</el-descriptions-item>
          <el-descriptions-item label="商品种类"
            >{{ currentOrder.goodsCount }}种</el-descriptions-item
          >
          <el-descriptions-item label="商品件数"
            >{{ currentOrder.goodsQuantity }}件</el-descriptions-item
          >
        </el-descriptions>

        <div class="detail-section-title">商品明细</div>
        <el-table :data="currentOrder.goodsList || []" border size="small">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column
            prop="goodsName"
            label="商品名称"
            min-width="200"
            show-overflow-tooltip
          />
          <el-table-column prop="spec" label="规格" min-width="100" />
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="单价" width="100" align="right">
            <template #default="scope">{{ formatAmount(scope.row.price) }}</template>
          </el-table-column>
          <el-table-column label="小计" width="100" align="right">
            <template #default="scope">{{ formatAmount(scope.row.actualAmount) }}</template>
          </el-table-column>
        </el-table>

        <div class="detail-section-title">费用信息</div>
        <div class="fee-summary">
          <div class="fee-item"
            ><span>商品总额</span><span>{{ formatAmount(currentOrder.goodsAmount) }}</span></div
          >
          <div class="fee-item"
            ><span>包装费</span><span>{{ formatAmount(currentOrder.packageFee) }}</span></div
          >
          <div class="fee-item"
            ><span>配送费</span><span>{{ formatAmount(currentOrder.postFee) }}</span></div
          >
          <div class="fee-item"
            ><span>优惠</span><span>{{ formatAmount(currentOrder.discountFee) }}</span></div
          >
          <div class="fee-item total"
            ><span>实付金额</span><span>{{ formatAmount(currentOrder.payFee) }}</span></div
          >
        </div>
      </div>
      <template #footer>
        <el-button @click="goodsDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { getOrderPage } from '@/api/ele/order'
import { TableApi } from '@/api/business/store'
import Pagination from '@/components/Pagination/index.vue'

export default {
  name: 'Order',
  components: { Pagination },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: false,
      total: 0,
      orderList: [],
      allOrderList: [],
      title: '',
      open: false,
      dateType: 'today',
      activeTab: 'all',
      gridColumns: 2,
      storeList: [],
      storeLoading: false,
      goodsList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        orderNo: null,
        storeId: null,
        receiverName: null,
        phoneSuffix: null,
        channelOrderNo: null,
        orderSort: 'createTime_desc',
        goodsId: null,
        goodsName: null,
        deliveryType: null,
        channelType: null,
        orderType: null,
        address: null,
        status: null,
        beginTime: null,
        endTime: null
      },
      form: {},
      rules: {},
      useMockData: false,
      dataSource: 'mock',
      statusCounts: {
        all: 0,
        paid: 0,
        accepted: 0,
        picked: 0,
        packed: 0,
        shipped: 0,
        done: 0,
        closed: 0
      },
      logDialogVisible: false,
      goodsDetailVisible: false,
      currentOrder: null,
      currentOrderLogs: []
    }
  },
  computed: {
    beginDateOptions() {
      return {
        disabledDate: (time) => {
          const endTime = this.queryParams.endTime
          if (endTime) {
            const endDate = new Date(endTime)
            const maxDate = new Date(endDate.getTime() + 32 * 24 * 60 * 60 * 1000)
            return time.getTime() > maxDate.getTime() || time.getTime() > Date.now()
          }
          return time.getTime() > Date.now()
        }
      }
    },
    endDateOptions() {
      return {
        disabledDate: (time) => {
          const beginTime = this.queryParams.beginTime
          if (beginTime) {
            const beginDate = new Date(beginTime)
            const minDate = new Date(beginDate.getTime() - 1 * 24 * 60 * 60 * 1000)
            const maxDate = new Date(beginDate.getTime() + 32 * 24 * 60 * 60 * 1000)
            return (
              time.getTime() < minDate.getTime() ||
              time.getTime() > maxDate.getTime() ||
              time.getTime() > Date.now()
            )
          }
          return time.getTime() > Date.now()
        }
      }
    }
  },
  created() {
    this.loadStoreList()
    this.loadGoodsList()
    this.loadStatusCounts()
  },
  mounted() {},
  methods: {
    handleGridChange(val) {
      // console.log('切换布局:', val, '列')
    },
    openGoodsDetail(order) {
      this.currentOrder = order
      this.goodsDetailVisible = true
    },
    loadStoreList() {
      this.storeLoading = true
      TableApi.getTableAllSimpleList(1)
        .then((res) => {
          const list = Array.isArray(res) ? res : []
          // 排序：开店(1)在前面，关店/停用(0)在后面
          this.storeList = list.sort((a, b) => {
            const aStatus = a.storeStatus ?? 0
            const bStatus = b.storeStatus ?? 0
            return bStatus - aStatus
          })
          // 不默认选择门店，查询所有订单
          this.queryParams.storeId = null
          this.getList()
        })
        .catch(() => {
          this.storeList = []
          this.loading = false
        })
        .finally(() => {
          this.storeLoading = false
        })
    },
    loadGoodsList() {
      if (!this.useMockData) {
        this.goodsList = []
        return
      }
      this.goodsList = [
        { id: '1', name: '宝娜斯无痕T裆丝袜' },
        { id: '2', name: '上野拉美拉刮毛刀' },
        { id: '3', name: '电动剃须刀' },
        { id: '4', name: '农夫山泉矿泉水550ml' },
        { id: '5', name: '可口可乐330ml' },
        { id: '6', name: '百事可乐330ml' },
        { id: '7', name: '康师傅方便面' },
        { id: '8', name: '统一冰红茶' }
      ]
    },
    syncGoodsListFromOrders(orderList) {
      const goodsMap = new Map()
      orderList.forEach((order) => {
        ;(order.goodsList || []).forEach((goods) => {
          const id = goods.goodsCode || goods.barcode || goods.goodsName
          const name = goods.goodsName || '--'
          if (!id || !name || goodsMap.has(id)) {
            return
          }
          goodsMap.set(id, { id, name })
        })
      })
      this.goodsList = Array.from(goodsMap.values())
    },
    queryGoods(queryString, cb) {
      const keyword = (queryString || '').trim().toLowerCase()
      const results = keyword ? this.goodsList.filter(this.createFilter(keyword)) : this.goodsList
      cb(results)
    },
    createFilter(queryString) {
      return (goods) => {
        return goods.name.toLowerCase().includes(queryString)
      }
    },
    handleSelectGoods(item) {
      this.queryParams.goodsId = item.id || null
      this.queryParams.goodsName = item.name
    },
    handleGoodsInput() {
      this.queryParams.goodsId = null
    },
    normalizeKeyword(value) {
      return `${value ?? ''}`.trim().toLowerCase()
    },
    includesKeyword(value, keyword) {
      if (!keyword) {
        return true
      }
      return this.normalizeKeyword(value).includes(keyword)
    },
    matchesPhoneSuffix(value, suffix) {
      if (!suffix) {
        return true
      }
      return `${value ?? ''}`.endsWith(suffix)
    },
    matchesDeliveryType(order) {
      if (!this.queryParams.deliveryType) {
        return true
      }
      const deliveryTypeMap = {
        instant: [1],
        appointment: [2]
      }
      return (
        deliveryTypeMap[this.queryParams.deliveryType]?.includes(Number(order.deliveryType)) ?? true
      )
    },
    matchesChannelType(order) {
      if (!this.queryParams.channelType) {
        return true
      }
      const channelTypeMap = {
        meituan: 'MT',
        eleme: 'ELE',
        self: 'POS'
      }
      return order.channelType === channelTypeMap[this.queryParams.channelType]
    },
    matchesOrderType(order) {
      if (!this.queryParams.orderType) {
        return true
      }
      if (this.queryParams.orderType === 'refund') {
        return false
      }
      if (this.queryParams.orderType === 'normal') {
        return true
      }
      return true
    },
    matchesGoods(order) {
      const goodsKeyword = this.normalizeKeyword(this.queryParams.goodsName)
      const goodsId = this.queryParams.goodsId
      if (!goodsKeyword && !goodsId) {
        return true
      }
      return (order.goodsList || []).some((goods) => {
        const matchesId = !goodsId || `${goods.goodsCode || goods.barcode || ''}` === `${goodsId}`
        const matchesName = !goodsKeyword || this.includesKeyword(goods.goodsName, goodsKeyword)
        return matchesId && matchesName
      })
    },
    sortOrders(orderList) {
      const sortMap = {
        createTime_asc: { key: 'createTimeRaw', order: 'asc' },
        createTime_desc: { key: 'createTimeRaw', order: 'desc' },
        payTime_asc: { key: 'payTimeRaw', order: 'asc' },
        payTime_desc: { key: 'payTimeRaw', order: 'desc' }
      }
      const sortConfig = sortMap[this.queryParams.orderSort]
      if (!sortConfig) {
        return orderList
      }
      return [...orderList].sort((a, b) => {
        const aValue = Number(a[sortConfig.key] || 0)
        const bValue = Number(b[sortConfig.key] || 0)
        return sortConfig.order === 'asc' ? aValue - bValue : bValue - aValue
      })
    },
    applySearchFilters(orderList) {
      const orderNoKeyword = this.normalizeKeyword(this.queryParams.orderNo)
      const receiverKeyword = this.normalizeKeyword(this.queryParams.receiverName)
      const channelOrderKeyword = this.normalizeKeyword(this.queryParams.channelOrderNo)
      const addressKeyword = this.normalizeKeyword(this.queryParams.address)
      const phoneSuffix = `${this.queryParams.phoneSuffix ?? ''}`.trim()
      return orderList.filter((order) => {
        return (
          this.includesKeyword(order.orderNo, orderNoKeyword) &&
          this.includesKeyword(order.userName, receiverKeyword) &&
          this.matchesPhoneSuffix(order.userPhone, phoneSuffix) &&
          this.includesKeyword(order.channelOrderId, channelOrderKeyword) &&
          this.includesKeyword(order.userAddress, addressKeyword) &&
          this.matchesDeliveryType(order) &&
          this.matchesChannelType(order) &&
          this.matchesOrderType(order) &&
          this.matchesGoods(order)
        )
      })
    },
    viewOrderLogs(order) {
      try {
        const mockLogs = order.logs || []
        if (mockLogs.length > 0) {
          this.currentOrderLogs = mockLogs
        } else {
          this.currentOrderLogs = [
            {
              operation: '订单创建',
              operator: '系统',
              time: order.createTime || new Date().toLocaleString(),
              status: '已创建'
            },
            {
              operation: '订单付款',
              operator: '用户',
              time: order.payTime || '--',
              status: '已支付'
            },
            { operation: '订单接单', operator: '门店', time: '--', status: '已接单' },
            { operation: '订单完成', operator: '系统', time: '--', status: '已完成' }
          ]
        }
      } catch (error) {
        this.currentOrderLogs = order.logs || [
          {
            operation: '订单创建',
            operator: '系统',
            time: order.createTime || '--',
            status: '已创建'
          },
          {
            operation: '订单付款',
            operator: '用户',
            time: order.payTime || '--',
            status: '已支付'
          },
          { operation: '订单接单', operator: '门店', time: '--', status: '已接单' },
          { operation: '订单完成', operator: '系统', time: '--', status: '已完成' }
        ]
      }
      this.logDialogVisible = true
    },
    loadStatusCounts() {
      if (this.useMockData) {
        this.statusCounts = {
          done: 3890
        }
      }
    },
    formatLocalDate(date) {
      const year = date.getFullYear()
      const month = `${date.getMonth() + 1}`.padStart(2, '0')
      const day = `${date.getDate()}`.padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    updateStatusCount(statusKey, count) {
      this.statusCounts[statusKey] = count
    },
    getTimestampRange() {
      if (!this.queryParams.beginTime || !this.queryParams.endTime) {
        return null
      }
      const [year, month, day] = this.queryParams.beginTime.split('-').map(Number)
      const [endYear, endMonth, endDay] = this.queryParams.endTime.split('-').map(Number)
      const startDate = new Date(year, month - 1, day, 0, 0, 0)
      const endDate = new Date(endYear, endMonth - 1, endDay, 23, 59, 59)
      return {
        startTime: Math.floor(startDate.getTime() / 1000),
        endTime: Math.floor(endDate.getTime() / 1000)
      }
    },
    syncDateRange() {
      const now = new Date()
      let beginDate = ''
      let endDate = ''
      switch (this.dateType) {
        case 'today':
          beginDate = endDate = this.formatLocalDate(now)
          break
        case 'yesterday': {
          const yesterday = new Date(now)
          yesterday.setDate(yesterday.getDate() - 1)
          beginDate = endDate = this.formatLocalDate(yesterday)
          break
        }
        case 'week': {
          const weekStart = new Date(now)
          const day = weekStart.getDay()
          const diff = day === 0 ? 6 : day - 1
          weekStart.setDate(weekStart.getDate() - diff)
          beginDate = this.formatLocalDate(weekStart)
          endDate = this.formatLocalDate(now)
          break
        }
        case 'month': {
          const monthStart = new Date(now.getFullYear(), now.getMonth(), 1)
          beginDate = this.formatLocalDate(monthStart)
          endDate = this.formatLocalDate(now)
          break
        }
        case 'custom':
          if (!this.queryParams.beginTime || !this.queryParams.endTime) {
            return true
          }
          beginDate = this.queryParams.beginTime
          endDate = this.queryParams.endTime
          break
        default:
          beginDate = endDate = this.formatLocalDate(now)
          break
      }
      this.queryParams.beginTime = beginDate
      this.queryParams.endTime = endDate
      return true
    },
    clearListState() {
      this.allOrderList = []
      this.orderList = []
      this.total = 0
      this.goodsList = []
    },
    getList() {
      this.loading = true
      this.queryParams.status = this.activeTab
      const dateSynced = this.syncDateRange()
      if (!dateSynced) {
        this.clearListState()
        this.loading = false
        return
      }
      if (
        this.dateType === 'custom' &&
        (!this.queryParams.beginTime || !this.queryParams.endTime)
      ) {
        ElMessage.warning('请选择完整的开始和结束日期')
        this.clearListState()
        this.loading = false
        return
      }
      if (this.useMockData) {
        setTimeout(() => {
          this.loadMockData()
          this.loading = false
        }, 300)
        return
      }
      const timeRange = this.getTimestampRange()
      if (!timeRange) {
        console.warn('[ELE前端] 时间范围未设置，将使用今日日期进行查询')
        const today = this.formatLocalDate(new Date())
        this.queryParams.beginTime = today
        this.queryParams.endTime = today
      }
      const finalTimeRange = timeRange || this.getTimestampRange()
      if (!finalTimeRange) {
        console.error('[ELE前端] 无法生成有效的时间范围')
        this.clearListState()
        this.loading = false
        return
      }
      const { startTime, endTime } = finalTimeRange
      const requestParams = {
        pageNo: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize,
        startTime,
        endTime
      }
      if (this.queryParams.storeId) {
        requestParams.platformStoreId = this.queryParams.storeId
      }
      if (this.activeTab !== 'all') {
        const statusMap = {
          paid: 1,
          accepted: 2,
          picked: 3,
          packed: 4,
          shipped: 5,
          done: 6,
          closed: -1
        }
        requestParams.status = statusMap[this.activeTab]
      }
      console.log('[ELE前端] 发送本地订单查询请求:', {
        requestParams,
        dateType: this.dateType,
        activeTab: this.activeTab,
        beginTime: this.queryParams.beginTime,
        endTime: this.queryParams.endTime,
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize
      })
      getOrderPage(requestParams)
        .then((response) => {
          console.log('[ELE前端] 收到本地订单查询响应:', response)
          const orderList = Array.isArray(response?.list) ? response.list : []
          const normalizedList = orderList.map((item) => this.normalizeOrder(item))
          console.log('[ELE前端] 订单数量统计:', {
            rawCount: orderList.length,
            normalizedCount: normalizedList.length,
            totalFromBackend: response.total,
            activeTab: this.activeTab
          })
          this.total = response.total || 0
          this.allOrderList = normalizedList
          this.syncGoodsListFromOrders(normalizedList)
          this.updateStatusCount(this.activeTab, response.total || 0)
          this.dataSource = 'api'
          this.applyLocalView()
          this.loading = false
        })
        .catch((error) => {
          console.error('[ELE前端] 本地订单查询失败:', error)
          ElMessage.error(error?.message || '订单查询失败')
          this.clearListState()
          this.loading = false
        })
    },
    formatTimestamp(timestamp) {
      if (!timestamp) {
        return '--'
      }
      const date = new Date(Number(timestamp))
      const year = date.getFullYear()
      const month = `${date.getMonth() + 1}`.padStart(2, '0')
      const day = `${date.getDate()}`.padStart(2, '0')
      const hours = `${date.getHours()}`.padStart(2, '0')
      const minutes = `${date.getMinutes()}`.padStart(2, '0')
      const seconds = `${date.getSeconds()}`.padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },
    loadMockData() {
      const mockOrders = [
        {
          id: 1,
          orderNo: '30',
          orderSn: '2002008244207066718',
          storeName: '柴小园24H便利超市（遥墙店）',
          deliveryType: '即时单',
          expectTime: '今日 15:19',
          status: 'pendingPick',
          statusText: '待拣货',
          userName: '邱***',
          userPhone: '19053115262',
          userAddress: '***如家旗下-济南遥墙机场华驿精选酒店 (325)',
          distance: '177m',
          isNewUser: true,
          remark: '【如遇缺货】：缺货时电话与我沟通',
          goodsCount: 2,
          payAmount: '32.20',
          income: '30.59',
          profit: '20.64',
          totalWeight: '110',
          goodsAmount: '30.85',
          packAmount: '1.00',
          discountAmount: '1.55',
          deliveryOriginalAmount: '1.90',
          deliveryDiscountAmount: '0.00',
          createTime: '2026-02-23 14:49:45',
          acceptTime: '2026-02-23 14:49:44',
          channel: '美团',
          goodsList: [
            {
              goodsCode: '1894306525422641166',
              goodsName:
                '【丝袜】宝娜斯无痕T裆丝袜女夏天超薄款连裤袜防勾丝隐形形袜长筒袜子 1双（颜色可选）',
              barcode: '6972228776729',
              goodsImage: 'https://via.placeholder.com/60',
              weight: '10g',
              price: '24.90',
              quantity: '1件',
              spec: '黑丝',
              location: '4D-1-4-8',
              totalAmount: '24.90',
              discountAmount: '1.00',
              actualAmount: '23.90'
            },
            {
              goodsCode: '176237573238094101',
              goodsName: '【耙型剃刀】上野拉美拉刮毛刀女生专用腋毛腿毛手动修剪器脱毛刀 /把',
              barcode: '6943182836675',
              goodsImage: 'https://via.placeholder.com/60',
              weight: '100g',
              price: '5.95',
              quantity: '1个',
              spec: '1把',
              location: '3D-3-3-9',
              totalAmount: '5.95',
              discountAmount: '0.55',
              actualAmount: '5.40'
            }
          ],
          logs: [
            {
              operation: '订单付款',
              operator: '用户',
              time: '2026-02-23 16:40:47',
              status: '已支付'
            },
            {
              operation: '订单接单',
              operator: '平台',
              time: '2026-02-23 16:40:50',
              status: '已接单'
            },
            {
              operation: '呼单成功',
              operator: '运力平台',
              time: '2026-02-23 16:40:59',
              status: '已呼单'
            },
            {
              operation: '订单拣货',
              operator: '赵偉萱',
              time: '2026-02-23 16:44:02',
              status: '已拣货'
            },
            {
              operation: '订单打包',
              operator: '平台',
              time: '2026-02-23 16:44:02',
              status: '已打包'
            },
            {
              operation: '骑手接单',
              operator: '运力平台',
              time: '2026-02-23 16:48:00',
              status: '骑手已接单'
            }
          ]
        },
        {
          id: 2,
          orderNo: '31',
          orderSn: '2002008244207066719',
          storeName: '柴小园24H便利超市（高新店）',
          deliveryType: '即时单',
          expectTime: '今日 15:45',
          status: 'pendingCall',
          statusText: '待呼单',
          userName: '张**',
          userPhone: '13853115262',
          userAddress: '***高新区创新谷 (201)',
          distance: '256m',
          isNewUser: false,
          remark: '',
          goodsCount: 1,
          payAmount: '18.50',
          income: '16.20',
          profit: '10.50',
          totalWeight: '50',
          goodsAmount: '17.00',
          packAmount: '1.00',
          discountAmount: '0.50',
          deliveryOriginalAmount: '2.00',
          deliveryDiscountAmount: '0.50',
          createTime: '2026-02-23 14:50:12',
          acceptTime: '2026-02-23 14:50:10',
          channel: '饿了么',
          goodsList: [
            {
              goodsCode: '1894306525422641167',
              goodsName: '农夫山泉矿泉水550ml',
              barcode: '6901028003831',
              goodsImage: 'https://via.placeholder.com/60',
              weight: '50g',
              price: '2.00',
              quantity: '1瓶',
              spec: '550ml',
              location: '1A-2-3-4',
              totalAmount: '2.00',
              discountAmount: '0.00',
              actualAmount: '2.00'
            }
          ],
          logs: [
            {
              operation: '订单付款',
              operator: '用户',
              time: '2026-02-23 15:30:20',
              status: '已支付'
            },
            {
              operation: '订单接单',
              operator: '平台',
              time: '2026-02-23 15:30:25',
              status: '已接单'
            },
            {
              operation: '呼单成功',
              operator: '运力平台',
              time: '2026-02-23 15:31:00',
              status: '已呼单'
            }
          ]
        },
        {
          id: 3,
          orderNo: '32',
          orderSn: '2002008244207066720',
          storeName: '柴小园24H便利超市（遥墙店）',
          deliveryType: '预约单',
          expectTime: '今日 18:00',
          status: 'delivering',
          statusText: '配送中',
          userName: '李**',
          userPhone: '15653115262',
          userAddress: '***济南大学中心校区 (401)',
          distance: '520m',
          isNewUser: true,
          remark: '请提前电话联系',
          goodsCount: 3,
          payAmount: '89.90',
          income: '75.00',
          profit: '45.00',
          totalWeight: '350',
          goodsAmount: '85.00',
          packAmount: '3.00',
          discountAmount: '2.00',
          deliveryOriginalAmount: '5.00',
          deliveryDiscountAmount: '1.10',
          createTime: '2026-02-23 14:45:30',
          acceptTime: '2026-02-23 14:45:28',
          channel: '美团',
          goodsList: [
            {
              goodsCode: '1894306525422641168',
              goodsName: '可口可乐330ml',
              barcode: '6920202888888',
              goodsImage: 'https://via.placeholder.com/60',
              weight: '100g',
              price: '3.00',
              quantity: '1罐',
              spec: '330ml',
              location: '1B-1-1-1',
              totalAmount: '3.00',
              discountAmount: '0.00',
              actualAmount: '3.00'
            }
          ],
          logs: [
            {
              operation: '订单付款',
              operator: '用户',
              time: '2026-02-23 16:00:00',
              status: '已支付'
            },
            {
              operation: '订单接单',
              operator: '平台',
              time: '2026-02-23 16:00:05',
              status: '已接单'
            },
            {
              operation: '呼单成功',
              operator: '运力平台',
              time: '2026-02-23 16:01:00',
              status: '已呼单'
            },
            {
              operation: '订单拣货',
              operator: '王师傅',
              time: '2026-02-23 16:05:00',
              status: '已拣货'
            },
            {
              operation: '订单打包',
              operator: '李师傅',
              time: '2026-02-23 16:06:00',
              status: '已打包'
            },
            {
              operation: '骑手接单',
              operator: '运力平台',
              time: '2026-02-23 16:08:00',
              status: '骑手已接单'
            },
            {
              operation: '骑手取货',
              operator: '运力平台',
              time: '2026-02-23 16:10:00',
              status: '骑手已取货'
            },
            {
              operation: '配送中',
              operator: '运力平台',
              time: '2026-02-23 16:15:00',
              status: '配送中'
            }
          ]
        }
      ]
      const visibleMockOrders = this.excludeExceptionOrders(mockOrders)
      this.allOrderList = visibleMockOrders.map((item) => ({
        ...item,
        showDetail: false,
        createTimeRaw: item.createTime ? new Date(item.createTime).getTime() : 0,
        payTimeRaw: item.payTime ? new Date(item.payTime).getTime() : 0
      }))
      this.syncGoodsListFromOrders(this.allOrderList)
      this.statusCounts = this.buildStatusCounts(visibleMockOrders)
      this.dataSource = 'mock'
      this.applyLocalView()
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: null,
        orderNo: null,
        status: null,
        storeId: null,
        userId: null,
        userName: null,
        userPhone: null,
        userAddress: null,
        payAmount: null,
        income: null,
        profit: null,
        remark: null,
        createTime: null
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    handlePageChange() {
      this.applyLocalView()
    },
    resetQuery() {
      this.dateType = 'today'
      this.queryParams = {
        pageNum: 1,
        pageSize: 20,
        orderNo: null,
        storeId: null,
        receiverName: null,
        phoneSuffix: null,
        channelOrderNo: null,
        orderSort: 'createTime_desc',
        goodsId: null,
        goodsName: null,
        deliveryType: null,
        channelType: null,
        orderType: null,
        address: null,
        status: null,
        beginTime: null,
        endTime: null
      }
      this.resetForm('queryForm')
      this.handleQuery()
    },
    toggleSearch() {
      this.showSearch = !this.showSearch
    },
    handleTabClick(tab) {
      this.activeTab = tab.name
      this.queryParams.pageNum = 1
      this.getList()
    },
    getStatusType(status) {
      const statusMap = {
        1: 'info',
        2: 'warning',
        3: 'warning',
        4: 'warning',
        5: 'primary',
        6: 'success',
        '-1': 'danger'
      }
      return statusMap[status] || 'info'
    },
    getStatusText(status) {
      const statusMap = {
        1: '已支付',
        2: '已接单',
        3: '已拣货',
        4: '已打包',
        5: '已发货',
        6: '交易成功',
        '-1': '交易关闭'
      }
      return statusMap[status] || `状态${status}`
    },
    getStatusTabText(status) {
      const statusMap = {
        6: '交易成功'
      }
      return statusMap[status] || this.getStatusText(status)
    },
    getChannelLabel(channelType) {
      const channelMap = {
        MT: '美团',
        ELE: '饿了么',
        JD: '京东',
        POS: 'POS'
      }
      return channelMap[channelType] || channelType || '--'
    },
    getChannelTagType(channelType) {
      const typeMap = {
        MT: 'warning',
        ELE: 'primary',
        JD: 'success',
        POS: 'info'
      }
      return typeMap[channelType] || 'info'
    },
    getDeliveryTypeText(deliveryType) {
      const typeMap = {
        1: '即时单',
        2: '预约单'
      }
      return typeMap[deliveryType] || '--'
    },
    getDeliveryPlatformText(order) {
      if (order.deliveryPlatform === '4' || order.deliveryPlatform === 4) {
        return '平台配送'
      }
      if (order.channelType === 'POS') {
        return '到店自取/门店处理'
      }
      return order.deliveryPlatform || '--'
    },
    getDeliveryStatusText(deliveryStatus) {
      const statusMap = {
        29: '待请求配送',
        30: '待骑手接单',
        31: '配送取消',
        32: '骑手接单',
        33: '骑手到店',
        34: '配送异常',
        35: '骑手揽收',
        36: '骑手送达'
      }
      return statusMap[deliveryStatus] || (deliveryStatus ?? '--')
    },
    getDeliveryStatusType(deliveryStatus) {
      const statusMap = {
        29: 'info',
        30: 'warning',
        31: 'danger',
        32: 'primary',
        33: 'warning',
        34: 'danger',
        35: 'success',
        36: 'success'
      }
      return statusMap[deliveryStatus] || 'info'
    },
    getDeliveryNameText(order) {
      if (order.deliveryName) {
        return `${order.deliveryName}${order.deliveryPhone ? `(${order.deliveryPhone})` : ''}`
      }
      return '暂无骑手接单'
    },
    getDiscountTypeText(type) {
      const typeMap = {
        sku: '商品优惠',
        post: '配送优惠'
      }
      return typeMap[type] || type || '--'
    },
    isExceptionStatus(status) {
      return Number(status) === -2
    },
    isClosedStatus(status) {
      return Number(status) === -1
    },
    isPaidStatus(status) {
      return Number(status) === 1
    },
    isAcceptedStatus(status) {
      return Number(status) === 2
    },
    isPickedStatus(status) {
      return Number(status) === 3
    },
    isPackedStatus(status) {
      return Number(status) === 4
    },
    isShippedStatus(status) {
      return Number(status) === 5
    },
    isDoneStatus(status) {
      return Number(status) === 6
    },
    isProcessingStatus(status) {
      return Number(status) === 5
    },
    formatAmount(value) {
      if (value === null || value === undefined || value === '') {
        return '--'
      }
      return `¥${(Number(value) / 100).toFixed(2)}`
    },
    normalizeOrder(item) {
      const goodsList = Array.isArray(item.subOrders)
        ? item.subOrders.map((sub) => {
            const totalAmount = Number(sub.totalFee || 0)
            const actualAmount = Number(sub.payFee || 0)
            return {
              goodsCode: sub.skuCode || '--',
              goodsName: sub.skuName || '--',
              barcode: sub.barcode || '--',
              weight: sub.weight ?? '--',
              price: sub.price,
              quantity: sub.buyAmount ?? 0,
              spec: sub.specification || '--',
              location: sub.cabinetCode || '--',
              totalAmount,
              discountAmount: Math.max(totalAmount - actualAmount, 0),
              actualAmount
            }
          })
        : []
      const discounts = Array.isArray(item.discounts)
        ? item.discounts.map((discount) => ({
            ...discount,
            typeText: this.getDiscountTypeText(discount.type)
          }))
        : []
      const goodsCount = goodsList.length
      const goodsQuantity = goodsList.reduce((sum, goods) => sum + Number(goods.quantity || 0), 0)
      const totalWeight = goodsList.reduce((sum, goods) => sum + Number(goods.weight || 0), 0)
      const goodsAmount = goodsList.reduce((sum, goods) => sum + Number(goods.totalAmount || 0), 0)
      const deliveryOriginalAmount = Number(item.postFee || 0)
      const deliveryFeeAmount = item.deliveryFee ?? item.postFee ?? null
      return {
        ...item,
        id: item.orderId,
        orderNo: item.orderId,
        orderSn: item.orderId,
        storeName:
          this.storeList.find(
            (store) => store.platformStoreId === (item.storeCode || this.queryParams.storeId)
          )?.storeName ||
          item.channelSourceName ||
          '--',
        channelLabel: this.getChannelLabel(item.channelType),
        deliveryTypeText: this.getDeliveryTypeText(item.deliveryType),
        deliveryPlatformText: this.getDeliveryPlatformText(item),
        deliveryStatus: item.deliveryStatus,
        deliveryStatusText: this.getDeliveryStatusText(item.deliveryStatus),
        deliveryNameText: this.getDeliveryNameText(item),
        statusText: this.getStatusText(item.status),
        statusTabText: this.getStatusTabText(item.status),
        userName: item.buyerName || '--',
        userPhone: item.buyerPhone || '--',
        userAddress: item.buyerAddress || '--',
        goodsCount,
        goodsQuantity,
        totalWeight,
        goodsAmount,
        packageFee: item.packageFee,
        discountFee: item.discountFee,
        postFee: deliveryOriginalAmount,
        deliveryFeeAmount,
        deliveryDiscountAmount: Math.max(deliveryOriginalAmount - Number(item.deliveryFee || 0), 0),
        payFee: item.payFee,
        totalFee: item.totalFee,
        createTimeRaw: Number(item.createTime || 0),
        payTimeRaw: Number(item.payTime || 0),
        createTime: this.formatTimestamp(item.createTime),
        payTime: this.formatTimestamp(item.payTime),
        goodsList,
        discounts,
        logs: this.buildOrderLogs(item),
        showDetail: false
      }
    },
    buildOrderLogs(order) {
      const logs = [
        {
          operation: '订单创建',
          operator: '系统',
          time: this.formatTimestamp(order.createTime),
          status: '已创建'
        }
      ]
      if (order.payTime) {
        logs.push({
          operation: '订单付款',
          operator: order.buyerName || '用户',
          time: this.formatTimestamp(order.payTime),
          status: '已支付'
        })
      }
      logs.push({
        operation: '订单状态',
        operator: order.deliveryName || '系统',
        time: this.formatTimestamp(order.payTime || order.createTime),
        status: this.getStatusText(order.status)
      })
      return logs
    },
    filterOrdersByTab(orderList) {
      if (this.activeTab === 'all') {
        return orderList
      }
      const tabStatusMap = {
        paid: 1,
        accepted: 2,
        picked: 3,
        packed: 4,
        shipped: 5,
        done: 6,
        closed: -1
      }
      const targetStatus = tabStatusMap[this.activeTab]
      if (targetStatus === undefined) {
        return orderList
      }
      return orderList.filter((order) => Number(order.status) === targetStatus)
    },
    applyLocalView() {
      const searchFilteredOrders = this.applySearchFilters(this.allOrderList)
      const sortedOrders = this.sortOrders(searchFilteredOrders)
      this.orderList = sortedOrders
    },
    excludeExceptionOrders(orderList) {
      return orderList.filter((order) => !this.isExceptionStatus(order.status))
    },
    buildStatusCounts(orderList) {
      return {
        all: orderList.length,
        paid: orderList.filter((order) => this.isPaidStatus(order.status)).length,
        accepted: orderList.filter((order) => this.isAcceptedStatus(order.status)).length,
        picked: orderList.filter((order) => this.isPickedStatus(order.status)).length,
        packed: orderList.filter((order) => this.isPackedStatus(order.status)).length,
        shipped: orderList.filter((order) => this.isShippedStatus(order.status)).length,
        done: orderList.filter((order) => this.isDoneStatus(order.status)).length,
        closed: orderList.filter((order) => this.isClosedStatus(order.status)).length
      }
    },
    openOrderDrawer(order) {
      this.currentOrder = order
      this.drawerVisible = true
    },
    toggleOrderDetail(order) {
      order.showDetail = !order.showDetail
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
  padding: 24px;
}

.search-container {
  margin-bottom: 20px;
}

.order-grid-1 {
  grid-template-columns: repeat(1, 1fr);
}
.order-grid-2 {
  grid-template-columns: repeat(2, 1fr);
}
.order-grid-3 {
  grid-template-columns: repeat(3, 1fr);
}
.order-grid-4 {
  grid-template-columns: repeat(4, 1fr);
}

.order-list {
  display: grid;
  gap: 20px;
  align-items: flex-start;
}

@media (max-width: 1400px) {
  .order-grid-4,
  .order-grid-3 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1200px) {
  .order-grid-4,
  .order-grid-3,
  .order-grid-2 {
    grid-template-columns: 1fr;
  }
}

.layout-selector {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 20px;
  padding: 14px 20px;
  background: white;
  border-radius: 12px;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 1px 2px rgba(0, 0, 0, 0.02);

  .selector-label,
  .selector-suffix {
    font-size: 14px;
    color: #64748b;
  }

  .selector-label {
    margin-right: 10px;
  }

  .selector-suffix {
    margin-left: 10px;
  }

  :deep(.el-radio-group) {
    display: flex;
  }

  :deep(.el-radio-button__inner) {
    padding: 5px 12px;
    min-width: 36px;
    background: #f8fafc;
    border-color: #e2e8f0;
    color: #64748b;
    font-size: 13px;
    transition: all 0.2s ease;
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
    border-color: #6366f1;
    color: #fff;
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
  }

  :deep(.el-radio-button:first-child .el-radio-button__inner) {
    border-radius: 8px 0 0 8px;
  }
  :deep(.el-radio-button:last-child .el-radio-button__inner) {
    border-radius: 0 8px 8px 0;
  }
}

.search-bar {
  background: white;
  border-radius: 16px;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.05),
    0 1px 3px rgba(0, 0, 0, 0.03);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.search-label {
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  font-weight: 500;
}

.date-radio {
  .el-radio-button__inner {
    padding: 6px 14px;
    font-size: 13px;
    background: #f8fafc;
    border-color: #e2e8f0;
    color: #64748b;
    transition: all 0.2s ease;
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
    border-color: #0ea5e9;
    color: #fff;
    box-shadow: 0 4px 12px rgba(14, 165, 233, 0.35);
  }
}

.date-picker {
  width: 130px !important;

  :deep(.el-input__wrapper) {
    background: #f8fafc;
    border-color: #e2e8f0;
    box-shadow: none;
    border-radius: 8px;

    &:hover,
    &:focus-within {
      border-color: #0ea5e9;
    }
  }

  :deep(.el-input__inner) {
    color: #334155;
  }
}

.date-sep {
  color: #94a3b8;
  font-size: 13px;
}

.search-select {
  width: 160px;

  :deep(.el-input__wrapper) {
    background: #f8fafc;
    border-color: #e2e8f0;
    box-shadow: none;
    border-radius: 8px;

    &:hover,
    &:focus-within {
      border-color: #0ea5e9;
    }
  }

  :deep(.el-input__inner) {
    color: #334155;
  }
  :deep(.el-select-dropdown__item) {
    color: #475569;

    &.is-disabled {
      color: #c0c4cc !important;
      cursor: not-allowed;
    }
  }

  .store-option {
    &.is-disabled {
      color: #c0c4cc !important;
    }
  }
}

.search-select-mini {
  width: 110px;
}

.search-input {
  width: 150px;

  :deep(.el-input__wrapper) {
    background: #f8fafc;
    border-color: #e2e8f0;
    box-shadow: none;
    border-radius: 8px;

    &:hover,
    &:focus-within {
      border-color: #0ea5e9;
    }
  }

  :deep(.el-input__inner) {
    color: #334155;
  }
}

.search-input-mini {
  width: 110px;
}

.toggle-item {
  margin-left: auto;

  .el-button {
    color: #64748b;
    font-size: 13px;
    transition: color 0.2s ease;

    &:hover {
      color: #6366f1;
    }
  }
}

.btn-item {
  margin-left: 12px;
  display: flex;
  gap: 10px;

  .el-button--primary {
    background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
    border: none;
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
    transition: all 0.2s ease;
    border-radius: 8px;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
    }
  }

  .el-button:not(.el-button--primary) {
    background: white;
    border-color: #e2e8f0;
    color: #64748b;
    border-radius: 8px;

    &:hover {
      background: #f8fafc;
      border-color: #6366f1;
      color: #4f46e5;
    }
  }
}

.search-expand {
  background: white;
  border-radius: 0 0 16px 16px;
  padding: 16px 24px 20px;
  margin-top: -4px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-top: none;
  animation: expandFadeIn 0.25s ease;
}

.expand-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

@keyframes expandFadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.status-tabs {
  margin-bottom: 20px;

  :deep(.el-tabs__header) {
    margin: 0;
    background: white;
    padding: 0 20px;
    border-radius: 16px 16px 0 0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    border: 1px solid rgba(226, 232, 240, 0.6);
    border-bottom: none;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background: linear-gradient(90deg, transparent, #e2e8f0, transparent);
  }

  :deep(.el-tabs__item) {
    color: #64748b;
    font-weight: 500;
    padding: 0 20px;
    height: 52px;
    line-height: 52px;
    font-size: 14px;
    transition: all 0.2s ease;

    &:hover {
      color: #6366f1;
    }

    &.is-active {
      color: #6366f1;
      font-weight: 600;
    }
  }

  :deep(.el-tabs__active-bar) {
    background: linear-gradient(90deg, #6366f1, #4f46e5);
    height: 3px;
    border-radius: 2px;
  }
}

.order-card {
  background: white;
  border-radius: 16px;
  margin-bottom: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 1px 2px rgba(0, 0, 0, 0.02);
  transition: all 0.25s ease;
  overflow: hidden;

  &:hover {
    transform: translateY(-2px);
    box-shadow:
      0 8px 24px rgba(0, 0, 0, 0.08),
      0 2px 4px rgba(0, 0, 0, 0.04);
    border-color: rgba(99, 102, 241, 0.3);
  }
}

.order-header {
  padding: 18px 22px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: linear-gradient(180deg, #fafbfc 0%, #f8fafc 100%);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;

  .order-header-left {
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-width: 0;

    .order-main-meta {
      display: flex;
      align-items: center;
      gap: 14px;
      flex-wrap: wrap;
    }

    .order-time-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .channel-tag {
      background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
      color: #1f2937;
      border: none;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(251, 191, 36, 0.25);
    }

    .order-no {
      font-weight: 700;
      color: #1e293b;
      font-size: 15px;
    }

    .store-name {
      color: #64748b;
      font-size: 14px;
    }

    .delivery-tag {
      background: rgba(14, 165, 233, 0.1);
      color: #0284c7;
      border: 1px solid rgba(14, 165, 233, 0.2);
    }

    .expect-time {
      color: #94a3b8;
      font-size: 13px;
    }
  }

  .order-header-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 10px;
    flex-shrink: 0;

    .status-pill {
      font-weight: 600;
      padding: 6px 14px;
      border-radius: 20px;
    }

    .channel-order-no {
      color: #94a3b8;
      font-size: 13px;
    }
  }
}

.order-content {
  padding: 20px;

  .order-status {
    display: grid;
    grid-template-columns: minmax(340px, 1.6fr) minmax(280px, 1fr);
    gap: 20px;

    .info-block {
      background: #fafbfc;
      border: 1px solid rgba(226, 232, 240, 0.6);
      border-radius: 12px;
      padding: 16px 18px;

      .block-title {
        font-size: 13px;
        font-weight: 700;
        color: #334155;
        margin-bottom: 12px;
        display: flex;
        align-items: center;
        gap: 8px;

        &::before {
          content: '';
          width: 4px;
          height: 16px;
          background: linear-gradient(180deg, #6366f1, #4f46e5);
          border-radius: 2px;
        }
      }
    }

    .user-info.compact {
      p {
        margin: 10px 0;
        font-size: 14px;
        line-height: 1.6;
        color: #475569;

        strong {
          color: #334155;
        }
      }
    }

    .simple-info {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      padding: 14px 18px;
      background: #f8fafc;
      border-radius: 10px;
      font-size: 13px;
      color: #64748b;
      border: 1px solid rgba(226, 232, 240, 0.5);

      span {
        white-space: nowrap;
      }
    }

    .order-kpis {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;

      .kpi-item {
        border-radius: 12px;
        background: linear-gradient(135deg, #fafbfc 0%, #f8fafc 100%);
        padding: 14px;
        display: flex;
        flex-direction: column;
        gap: 8px;
        border: 1px solid rgba(226, 232, 240, 0.5);

        .kpi-label {
          color: #94a3b8;
          font-size: 12px;
        }

        .kpi-value {
          color: #1e293b;
          font-size: 20px;
          font-weight: 700;

          &.small {
            font-size: 18px;
          }
        }
      }
    }
  }
}

.delivery-info {
  padding: 16px 22px;
  background: linear-gradient(180deg, #fafbfc 0%, #f8fafc 100%);
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  font-size: 14px;
  color: #64748b;
  border-top: 1px solid rgba(226, 232, 240, 0.5);

  .delivery-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 12px 14px;
    background: white;
    border-radius: 12px;
    border: 1px solid rgba(226, 232, 240, 0.5);

    .delivery-label {
      font-size: 12px;
      color: #94a3b8;
      font-weight: 500;
    }

    .delivery-value {
      font-size: 14px;
      color: #334155;
      font-weight: 600;

      &.emphasis {
        color: #6366f1;
        font-size: 16px;
      }
    }

    .delivery-status-tag {
      align-self: flex-start;
      font-weight: 600;
    }
  }
}

.price-info {
  padding: 16px 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  gap: 16px;
  background: white;

  .price-left {
    display: flex;
    flex-wrap: wrap;
    gap: 24px;
    font-size: 14px;

    .price {
      color: #6366f1;
      font-size: 18px;
      font-weight: 700;
    }

    .income {
      color: #f59e0b;
      font-size: 16px;
      font-weight: 600;
    }

    .profit {
      color: #10b981;
      font-size: 16px;
      font-weight: 600;
    }

    span {
      color: #64748b;
    }
  }

  .price-right {
    display: flex;
    gap: 12px;
    font-size: 14px;
    flex-shrink: 0;

    .el-button {
      color: #64748b;
      transition: all 0.2s ease;
      font-size: 13px;

      &:hover {
        color: #6366f1;
        background: rgba(99, 102, 241, 0.08);
      }
    }
  }
}

.goods-summary {
  padding: 18px 22px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  font-size: 14px;
  color: #64748b;
  background: #fafbfc;

  .summary-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    background: white;
    border: 1px solid rgba(226, 232, 240, 0.5);
    border-radius: 12px;
    padding: 14px 16px;

    &.accent {
      background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
      border-color: rgba(99, 102, 241, 0.2);
    }

    span:first-child {
      color: #1e293b;
      font-weight: 700;
    }
  }
}

.goods-detail {
  padding: 0 22px 20px;

  .detail-section-title {
    font-size: 15px;
    font-weight: 700;
    color: #334155;
    margin: 16px 0 12px;
    display: flex;
    align-items: center;
    gap: 10px;

    &::before {
      content: '';
      width: 3px;
      height: 18px;
      background: linear-gradient(180deg, #0ea5e9, #0284c7);
      border-radius: 2px;
    }
  }

  .goods-detail-table {
    margin-top: 8px;
  }
  .discount-section {
    margin-top: 20px;
  }
}

.order-footer {
  padding: 14px 22px 18px;
  border-top: 1px solid rgba(226, 232, 240, 0.6);
  font-size: 13px;
  color: #94a3b8;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  background: #fafbfc;
}

@media (max-width: 1280px) {
  .goods-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .delivery-info {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .order-header {
    flex-direction: column;
    .order-header-right {
      align-items: flex-start;
    }
  }

  .order-content {
    .order-status {
      grid-template-columns: 1fr;
    }
  }

  .goods-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .delivery-info,
  .goods-summary {
    grid-template-columns: 1fr;
  }
  .price-info {
    flex-direction: column;
    align-items: flex-start;
  }
}

.el-dialog {
  :deep(.el-dialog__header) {
    padding: 18px 24px;
    border-bottom: 1px solid #e2e8f0;
    background: white;
  }

  :deep(.el-dialog__title) {
    font-size: 17px;
    font-weight: 700;
    color: #1e293b;
  }

  :deep(.el-dialog__body) {
    padding: 24px;
    max-height: 60vh;
    overflow-y: auto;
    background: #f8fafc;
  }

  :deep(.el-dialog__footer) {
    padding: 14px 24px;
    border-top: 1px solid #e2e8f0;
    background: white;
  }
}

.goods-detail-content {
  .detail-section-title {
    font-size: 15px;
    font-weight: 600;
    color: #334155;
    margin: 22px 0 14px;
    padding-bottom: 10px;
    border-bottom: 1px solid #e2e8f0;
  }

  .fee-summary {
    background: white;
    border-radius: 12px;
    padding: 18px;
    border: 1px solid #e2e8f0;

    .fee-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      color: #64748b;

      &.total {
        border-top: 1px solid #e2e8f0;
        margin-top: 10px;
        padding-top: 14px;
        font-weight: 700;
        color: #6366f1;
        font-size: 17px;
      }
    }
  }
}

.el-table {
  font-size: 14px;

  :deep(.el-table__header-wrapper) {
    .el-table__header {
      th {
        background-color: #f8fafc !important;
        color: #334155;
        font-weight: 700;
        text-align: center;
        border-bottom: 1px solid #e2e8f0 !important;
      }
    }
  }

  :deep(.el-table__body-wrapper) {
    .el-table__row {
      td {
        background: white !important;
        border-bottom: 1px solid #f1f5f9 !important;
        color: #475569;
      }

      &:hover {
        background: #f8fafc !important;
      }
    }
  }
}

:deep(.el-descriptions) {
  background: transparent;

  .el-descriptions__header {
    margin-bottom: 16px;
  }

  .el-descriptions__title {
    color: #1e293b;
    font-weight: 700;
  }

  .el-descriptions__body {
    background: transparent;
  }

  .el-descriptions__cell {
    background: white !important;
    border: 1px solid #e2e8f0 !important;
    color: #475569;
  }
}

:deep(.el-tag) {
  border-radius: 6px;

  &.el-tag--info {
    background: #f1f5f9;
    border-color: #e2e8f0;
    color: #64748b;
  }

  &.el-tag--success {
    background: #ecfdf5;
    border-color: #a7f3d0;
    color: #10b981;
  }

  &.el-tag--warning {
    background: #fef3c7;
    border-color: #fde68a;
    color: #d97706;
  }

  &.el-tag--danger {
    background: #fef2f2;
    border-color: #fecaca;
    color: #ef4444;
  }

  &.el-tag--primary {
    background: #e0e7ff;
    border-color: #c7d2fe;
    color: #6366f1;
  }
}

:deep(.el-dropdown-menu) {
  background: white;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);

  .el-dropdown-menu__item {
    color: #475569;
    font-size: 13px;

    &:hover {
      background: #f1f5f9;
      color: #6366f1;
    }
  }
}

:deep(.el-link) {
  color: #0ea5e9;

  &:hover {
    color: #0284c7;
  }
}

:deep(.el-autocomplete) {
  .el-input__wrapper {
    background: #f8fafc;
    border-color: #e2e8f0;
    box-shadow: none;
    border-radius: 8px;
  }
}

:deep(.el-pagination) {
  margin-top: 24px;
  justify-content: center;

  .el-pager li {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
    border-radius: 8px;

    &:hover {
      color: #6366f1;
      border-color: #6366f1;
    }

    &.is-active {
      background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
      border-color: #6366f1;
      color: #fff;
    }
  }

  button {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
  }
}
</style>
