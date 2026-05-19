<template>
  <ContentWrap>
    <div class="search-section">
      <el-form class="query-form-grid" :model="queryParams" ref="queryFormRef" label-width="90px">
        <el-row :gutter="16" class="search-row">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="日期" prop="dateRange">
              <el-date-picker
                v-model="dateRange"
                value-format="YYYY-MM-DD"
                type="daterange"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
                class="!w-full"
                @change="handleDateRangeChange"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="门店" prop="storeId">
              <el-select
                v-model="queryParams.storeId"
                placeholder="请选择门店"
                clearable
                filterable
                :loading="storeLoading"
                class="!w-full"
              >
                <el-option
                  v-for="store in storeList"
                  :key="store.storeId"
                  :label="store.storeName"
                  :value="store.storeId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="订单小号" prop="orderNo">
              <el-input
                v-model="queryParams.orderNo"
                placeholder="请输入"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="排序" prop="orderSort">
              <el-select
                v-model="queryParams.orderSort"
                placeholder="请选择"
                clearable
                class="!w-full"
              >
                <el-option label="创建时间正序" value="createTime_asc" />
                <el-option label="创建时间倒序" value="createTime_desc" />
                <el-option label="支付时间正序" value="payTime_asc" />
                <el-option label="支付时间倒序" value="payTime_desc" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="search-row">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="收货人" prop="receiverName">
              <el-input
                v-model="queryParams.receiverName"
                placeholder="请输入"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="手机号" prop="phoneSuffix">
              <el-input
                v-model="queryParams.phoneSuffix"
                placeholder="后4位"
                maxlength="4"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="商品" prop="goodsName">
              <el-autocomplete
                v-model="queryParams.goodsName"
                :fetch-suggestions="queryGoods"
                placeholder="请输入"
                clearable
                :trigger-on-focus="false"
                @input="handleGoodsInput"
                @select="handleSelectGoods"
                class="!w-full"
              >
                <template #default="{ item }"
                  ><div>{{ item.name }}</div></template
                >
              </el-autocomplete>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="订单类型" prop="orderType">
              <el-select
                v-model="queryParams.orderType"
                placeholder="请选择"
                clearable
                class="!w-full"
              >
                <el-option label="即时单" :value="2" />
                <el-option label="预约单" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="search-row">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="渠道" prop="channelType">
              <el-select
                v-model="queryParams.channelType"
                placeholder="请选择"
                clearable
                class="!w-full"
              >
                <el-option label="淘宝闪购" value="ELE" />
                <el-option label="美团" value="MT" />
                <el-option label="京东" value="JD" />
                <el-option label="POS" value="POS" />
                <el-option label="销售开单" value="OO" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="渠道订单号" prop="channelOrderNo">
              <el-input
                v-model="queryParams.channelOrderNo"
                placeholder="请输入"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="订单异常" prop="exceptionType">
              <el-select
                v-model="queryParams.exceptionType"
                placeholder="请选择"
                clearable
                class="!w-full"
              >
                <el-option label="无异常" value="normal" />
                <el-option label="异常订单" value="exception" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="地址" prop="address">
              <el-input
                v-model="queryParams.address"
                placeholder="请输入"
                clearable
                @keyup.enter="handleQuery"
                @blur="handleQuery"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="search-row">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <el-form-item label="配送方式" prop="deliveryMode">
              <el-select
                v-model="queryParams.deliveryMode"
                placeholder="请选择"
                clearable
                class="!w-full"
                @change="handleQuery"
              >
                <el-option label="平台配送" :value="1" />
                <el-option label="自配送" :value="2" />
                <el-option label="自提" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="actions-bar">
        <div class="actions-left">
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-5px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
        </div>
        <div class="actions-right">
          <div class="action-group">
            <span class="selector-label">每行</span>
            <el-radio-group v-model="gridColumns" size="small" @change="handleGridChange">
              <el-radio-button :label="1">1</el-radio-button>
              <el-radio-button :label="2">2</el-radio-button>
            </el-radio-group>
            <span class="selector-suffix">列</span>
          </div>
          <div class="action-group">
            <el-dropdown v-hasPermi="['data:test:export']">
              <el-button type="default" link>
                <Icon icon="ep:download" class="mr-5px" /> 导出<Icon
                  icon="ep:arrow-down"
                  class="ml-5px"
                />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>导出全部</el-dropdown-item>
                  <el-dropdown-item>导出选中</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button type="default" link v-hasPermi="['data:test:import']">
              <Icon icon="ep:upload" class="mr-5px" /> 批量导入配送费
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="activeTab" @tab-click="handleTabClick" class="status-tabs">
      <el-tab-pane :label="'全部 (' + statusCounts.all + ')'" name="all" />
      <el-tab-pane :label="'已支付 (' + statusCounts.paid + ')'" name="paid" />
      <el-tab-pane :label="'已接单 (' + statusCounts.accepted + ')'" name="accepted" />
      <el-tab-pane :label="'已拣货 (' + statusCounts.picked + ')'" name="picked" />
      <el-tab-pane :label="'已打包 (' + statusCounts.packed + ')'" name="packed" />
      <el-tab-pane :label="'已发货 (' + statusCounts.shipped + ')'" name="shipped" />
      <el-tab-pane :label="'交易成功 (' + statusCounts.done + ')'" name="done" />
      <el-tab-pane :label="'交易关闭 (' + statusCounts.closed + ')'" name="closed" />
    </el-tabs>

    <div :class="['order-list', 'order-grid-' + gridColumns]" v-loading="loading">
      <div v-for="order in orderList" :key="order.id" class="order-card">
        <div class="order-header">
          <div class="order-header-left">
            <div class="order-main-meta">
              <el-tag :type="getChannelTagType(order.channelType)" size="small">{{
                order.channelLabel
              }}</el-tag>
              <span class="store-name">{{ order.storeName }}</span>
              <el-tag :type="getArriveTypeTagType(order.arriveType)" size="small">{{
                order.arriveTypeText
              }}</el-tag>
            </div>
            <div class="order-time-meta">
              <span class="expect-time">下单：{{ order.createTime }}</span>
              <span class="expect-time">支付：{{ order.payTime }}</span>
            </div>
          </div>
          <div class="order-header-right">
            <span class="channel-order-no">{{ order.channelOrderId || '--' }}</span>
            <span v-if="order.orderIndex" class="order-index">流水号：{{ order.orderIndex }}</span>
          </div>
        </div>

        <div class="order-content">
          <div class="order-content-top">
            <el-tag
              :type="getStatusType(order.status)"
              effect="dark"
              size="small"
              class="status-tag-left"
              >{{ order.statusTabText }}</el-tag
            >
            <div class="user-info compact">
              <p><strong>收货人：</strong>{{ order.userName }}</p>
              <p><strong>联系电话：</strong>{{ order.userPhone }}</p>
              <p><strong>收货地址：</strong>{{ order.userAddress }}</p>
              <p v-if="order.remark"><strong>订单备注：</strong>{{ order.remark }}</p>
            </div>
          </div>
          <div class="delivery-info">
            <div class="delivery-item">
              <span class="delivery-label">骑手 / 配送员</span>
              <span class="delivery-value">{{ order.deliveryNameText }}</span>
            </div>
            <div class="delivery-item">
              <span class="delivery-label">配送费</span>
              <span class="delivery-value emphasis">{{
                formatAmount(order.deliveryFeeAmount)
              }}</span>
            </div>
            <div class="delivery-item">
              <span class="delivery-label">渠道</span>
              <span class="delivery-value">{{ order.channelLabel }}</span>
            </div>
            <div class="delivery-item">
              <span class="delivery-label">配送方式</span>
              <span class="delivery-value">{{ order.deliveryPlatformText }}</span>
            </div>
            <div class="delivery-item">
              <span class="delivery-label">配送状态</span>
              <el-tag
                size="small"
                :type="getDeliveryStatusType(order.deliveryStatus)"
                effect="plain"
                >{{ order.deliveryStatusText }}</el-tag
              >
            </div>
          </div>
        </div>

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
            <span
              >预计收入：<strong class="income">{{
                formatAmount(order.estimatedIncome)
              }}</strong></span
            >
            <span
              >平台佣金：<strong class="commission">{{
                formatAmount(order.platformCommissionFee)
              }}</strong></span
            >
            <span v-if="order.settlementStatus !== undefined && order.settlementStatus !== null"
              >结算金额：<strong :class="order.settlementStatus === 1 ? 'settled' : 'unsettled'">{{
                formatYuan(order.settlementAmount)
              }}</strong></span
            >
            <span v-if="order.settlementStatus !== undefined && order.settlementStatus !== null"
              >结算状态：
              <el-tag size="small" :type="order.settlementStatus === 1 ? 'success' : 'info'">
                {{ order.settlementStatus === 1 ? '已结算' : '未结算' }}
              </el-tag>
            </span>
            <span v-if="order.settlementStatus === 0">
              <el-button type="danger" size="small" link @click="handleRetryBill(order)">重试账单同步</el-button>
            </span>
          </div>
          <div class="price-right">
            <el-button type="default" link @click="viewOrderLogs(order)">查看日志</el-button>
            <el-button type="default" link @click="openGoodsDetail(order)">查看详情</el-button>
            <el-button type="default" link @click="openBillDetail(order)">账单详情</el-button>
          </div>
        </div>

        <div class="order-footer">
          <span>创建时间：{{ order.createTime }}</span>
          <span>支付时间：{{ order.payTime }}</span>
          <span
            >订单编号：<el-link type="primary">{{ order.orderSn }}</el-link></span
          >
          <span>渠道订单号：{{ order.channelOrderId || '--' }}</span>
        </div>
      </div>

      <div v-if="!loading && orderList.length === 0" class="empty-order">
        <el-empty description="暂无订单" />
      </div>
    </div>

    <Pagination
      v-show="total > 0"
      v-model:total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @pagination="handlePageChange"
    />
  </ContentWrap>

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

  <el-dialog
    title="商品详情"
    v-model="goodsDetailVisible"
    width="900px"
    destroy-on-close
    :z-index="2001"
  >
    <div v-if="currentOrder" class="goods-detail-content">
      <el-descriptions :column="2" border class="info-row" label-width="80px">
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ currentOrder.storeName }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions :column="3" border class="info-row" label-width="80px">
        <el-descriptions-item label="商品种类"
          >{{ currentOrder.goodsCount }}种</el-descriptions-item
        >
        <el-descriptions-item label="商品件数"
          >{{ currentOrder.goodsQuantity }}件</el-descriptions-item
        >
        <el-descriptions-item label="重量">{{ currentOrder.totalWeight }}g</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-title">商品明细</div>
      <el-table :data="currentOrder.goodsList || []" border size="small">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="goodsName" label="商品名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" min-width="100" />
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column prop="weight" label="重量" width="80" align="center" />
        <el-table-column label="单价" width="100" align="right">
          <template #default="scope">{{ formatAmount(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="100" align="right">
          <template #default="scope">{{ formatAmount(scope.row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column label="赠品" width="80" align="center">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.giftFlag === 1 ? 'success' : 'info'">
              {{ scope.row.giftFlag === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="换货" width="80" align="center">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.exchangeFlag === 1 ? 'warning' : 'info'">
              {{ scope.row.exchangeFlag === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="hasExchangeAmount(currentOrder.goodsList)"
          label="换货金额"
          width="100"
          align="right"
        >
          <template #default="scope">{{ formatAmount(scope.row.exchangeAmount) }}</template>
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
        <div class="fee-item"
          ><span>预计收入</span><span>{{ formatAmount(currentOrder.estimatedIncome) }}</span></div
        >
        <div class="fee-item"
          ><span>平台佣金</span
          ><span>{{ formatAmount(currentOrder.platformCommissionFee) }}</span></div
        >
        <div class="fee-item total"
          ><span>实付金额</span><span>{{ formatAmount(currentOrder.payFee) }}</span></div
        >
      </div>

      <div class="detail-section-title">平台信息</div>
      <el-descriptions :column="2" border label-width="100px">
        <el-descriptions-item label="平台订单状态">{{
          currentOrder.platformOrderStatusName || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="配送平台">{{
          currentOrder.deliveryPlatformName || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="平台门店ID">{{
          currentOrder.platformStoreId || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="ERP门店编码">{{
          currentOrder.erpStoreCode || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="商户编码">{{
          currentOrder.merchantCode || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="渠道来源ID">{{
          currentOrder.channelSourceId || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="测试订单">{{
          currentOrder.isTestOrder === 1 ? '是' : '否'
        }}</el-descriptions-item>
        <el-descriptions-item label="历史订单">{{
          currentOrder.isHistoryOrder === 1 ? '是' : '否'
        }}</el-descriptions-item>
      </el-descriptions>

      <div
        class="detail-section-title"
        v-if="currentOrder.discounts && currentOrder.discounts.length > 0"
        >优惠分摊明细</div
      >
      <el-table
        v-if="currentOrder.discounts && currentOrder.discounts.length > 0"
        :data="currentOrder.discounts"
        border
        size="small"
      >
        <el-table-column prop="activityName" label="优惠名称" min-width="150" />
        <el-table-column prop="typeText" label="类型" width="100" />
        <el-table-column label="平台承担" width="120" align="right">
          <template #default="scope">{{ formatAmount(scope.row.platformFee) }}</template>
        </el-table-column>
        <el-table-column label="商家承担" width="120" align="right">
          <template #default="scope">{{ formatAmount(scope.row.merchantFee) }}</template>
        </el-table-column>
        <el-table-column label="合计" width="120" align="right">
          <template #default="scope">{{ formatAmount(scope.row.discountFee) }}</template>
        </el-table-column>
      </el-table>

      <div class="detail-section-title">元数据</div>
      <el-descriptions :column="2" border label-width="100px">
        <el-descriptions-item label="订单来源">{{
          currentOrder.orderFrom || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="当日流水号">{{
          currentOrder.orderIndex || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{
          currentOrder.longitude || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="纬度">{{
          currentOrder.latitude || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{
          currentOrder.updateTimeText || '--'
        }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-title" v-if="currentOrder.billInfo">账单信息</div>
      <el-descriptions v-if="currentOrder.billInfo" :column="2" border label-width="120px">
        <el-descriptions-item label="账单日期">{{ currentOrder.billInfo.billDate || '--' }}</el-descriptions-item>
        <el-descriptions-item label="结算状态">
          <el-tag size="small" :type="currentOrder.billInfo.status === 1 ? 'success' : 'info'">
            {{ currentOrder.billInfo.statusText || '--' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="结算类型">{{ currentOrder.billInfo.billTypeDesc || '--' }}</el-descriptions-item>
        <el-descriptions-item label="结算金额">{{ formatYuan(currentOrder.billInfo.billAmount) || '--' }}</el-descriptions-item>
        <el-descriptions-item label="配送方式">{{ currentOrder.billInfo.shippingType || '--' }}</el-descriptions-item>
        <el-descriptions-item label="结算ID">{{ currentOrder.billInfo.settleOrderId || '--' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-title" v-if="currentOrder.billInfo">账单费用明细</div>
      <el-descriptions v-if="currentOrder.billInfo" :column="2" border label-width="150px">
        <el-descriptions-item label="商品原价">{{ formatYuan(currentOrder.billInfo.itemPrice) }}</el-descriptions-item>
        <el-descriptions-item label="包装费原价">{{ formatYuan(currentOrder.billInfo.packageFee) }}</el-descriptions-item>
        <el-descriptions-item label="配送费原价">{{ formatYuan(currentOrder.billInfo.deliveryFee) }}</el-descriptions-item>
        <el-descriptions-item label="用户实付金额">{{ formatYuan(currentOrder.billInfo.userOnlinePayAmount) }}</el-descriptions-item>
        <el-descriptions-item label="商家活动费用">{{ formatYuan(currentOrder.billInfo.shopMarketingFee) }}</el-descriptions-item>
        <el-descriptions-item label="平台费用">{{ formatYuan(currentOrder.billInfo.platformFee) }}</el-descriptions-item>
        <el-descriptions-item label="商家商品补贴">{{ formatYuan(currentOrder.billInfo.productPreferences) }}</el-descriptions-item>
        <el-descriptions-item label="商家配送费补贴">{{ formatYuan(currentOrder.billInfo.notProductPreferences) }}</el-descriptions-item>
        <el-descriptions-item label="商家配送服务费">{{ formatYuan(currentOrder.billInfo.performanceServiceFee) }}</el-descriptions-item>
        <el-descriptions-item label="商家平台服务费">{{ formatYuan(currentOrder.billInfo.platformChargeFee) }}</el-descriptions-item>
        <el-descriptions-item label="平台活动补贴">{{ formatYuan(currentOrder.billInfo.activityAmount) }}</el-descriptions-item>
        <el-descriptions-item label="公益捐赠">{{ formatYuan(currentOrder.billInfo.donationFee) }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <template #footer>
      <el-button @click="goodsDetailVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog
    title="账单详情"
    v-model="billDetailVisible"
    width="1200px"
    destroy-on-close
    :z-index="2001"
  >
    <div v-if="currentOrder && currentOrder.billInfo" class="bill-detail-content">
      <div class="bill-summary-info">
        <el-descriptions :column="3" border class="info-row" label-width="100px">
          <el-descriptions-item label="订单号">{{ currentOrder.billInfo.orderId || currentOrder.orderId || '--' }}</el-descriptions-item>
          <el-descriptions-item label="汇总结算金额">{{ formatAmount(currentOrder.billInfo.totalBillAmount) }}</el-descriptions-item>
          <el-descriptions-item label="汇总结算状态">
            <el-tag :type="currentOrder.billInfo.totalStatus === 1 ? 'success' : 'warning'">
              {{ currentOrder.billInfo.totalStatus === 1 ? '已结算' : '未结算' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="detail-section-title">账单明细列表</div>
      <el-table :data="currentOrder.billInfo.billDetails || []" border size="small" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="billTypeDesc" label="账单类型" width="120" />
        <el-table-column prop="billDate" label="账单日期" width="110" />
        <el-table-column prop="settleOrderId" label="结算单号" width="150" show-overflow-tooltip />
        <el-table-column prop="refundId" label="退款单号" width="130" show-overflow-tooltip />
        <el-table-column label="结算状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '已结算' : '未结算' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账单金额" width="110" align="right">
          <template #default="scope">{{ formatAmount(scope.row.billAmount) }}</template>
        </el-table-column>
        <el-table-column label="商品金额" width="110" align="right">
          <template #default="scope">{{ formatAmount(scope.row.itemPrice) }}</template>
        </el-table-column>
        <el-table-column label="包装费" width="90" align="right">
          <template #default="scope">{{ formatAmount(scope.row.packageFee) }}</template>
        </el-table-column>
        <el-table-column label="配送费" width="90" align="right">
          <template #default="scope">{{ formatAmount(scope.row.deliveryFee) }}</template>
        </el-table-column>
        <el-table-column label="商家营销费" width="110" align="right">
          <template #default="scope">{{ formatAmount(scope.row.shopMarketingFee) }}</template>
        </el-table-column>
        <el-table-column label="平台费" width="90" align="right">
          <template #default="scope">{{ formatAmount(scope.row.platformFee) }}</template>
        </el-table-column>
        <el-table-column label="用户实付" width="110" align="right">
          <template #default="scope">{{ formatAmount(scope.row.userOnlinePayAmount) }}</template>
        </el-table-column>
        <el-table-column label="履约服务费" width="110" align="right">
          <template #default="scope">{{ formatAmount(scope.row.performanceServiceFee) }}</template>
        </el-table-column>
        <el-table-column label="平台服务费" width="110" align="right">
          <template #default="scope">{{ formatAmount(scope.row.platformChargeFee) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="scope">{{ scope.row.createTime || '--' }}</template>
        </el-table-column>
      </el-table>
    </div>
    <div v-else class="empty-bill-info">
      <el-empty description="暂无账单信息" />
    </div>
    <template #footer>
      <el-button @click="billDetailVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ElMessage } from 'element-plus'
import { getOrderPage, getOrderDetail, getOrderStatusCounts, getOrderBillInfo } from '@/api/ele/order'
import { retryByOrderId } from '@/api/ele/billSync'
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
      total: 0,
      orderList: [],
      allOrderList: [],
      title: '',
      open: false,
      dateRange: [],
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
        channelType: null,
        orderType: null,
        exceptionType: null,
        address: null,
        status: null,
        beginTime: null,
        endTime: null,
        deliveryMode: null
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
      billDetailVisible: false,
      currentOrder: null,
      currentOrderLogs: []
    }
  },
  computed: {},
  created() {
    this.loadStoreList()
    this.loadGoodsList()
    this.initDateRange()
  },
  mounted() {},
  methods: {
    initDateRange() {
      const now = new Date()
      const today = this.formatLocalDate(now)
      this.dateRange = [today, today]
      this.queryParams.beginTime = today
      this.queryParams.endTime = today
    },
    handleDateRangeChange(val) {
      if (val && val.length === 2) {
        this.queryParams.beginTime = val[0]
        this.queryParams.endTime = val[1]
      } else {
        this.queryParams.beginTime = null
        this.queryParams.endTime = null
      }
    },
    handleGridChange(val) {},
    openGoodsDetail(order) {
      this.currentOrder = order
      this.goodsDetailVisible = true
      if (!order.billInfo && order.settlementStatus !== undefined && order.settlementStatus !== null) {
        this.loadBillInfo(order)
      }
    },
    async loadBillInfo(order) {
      try {
        const res = await getOrderBillInfo(order.channelOrderId)
        if (res && res.billDetails && res.billDetails.length > 0) {
          order.billInfo = res
        } else {
          order.billInfo = null
        }
      } catch (e) {
        order.billInfo = null
      }
    },
    async openBillDetail(order) {
      this.currentOrder = order
      this.billDetailVisible = true
      if (!order.billInfo) {
        await this.loadBillInfo(order)
      }
    },
    async handleRetryBill(order) {
      try {
        const res = await retryByOrderId(order.channelOrderId)
        ElMessage.success(res || '重试成功')
        this.getList()
      } catch (e) {
        ElMessage.error('请求失败')
      }
    },
    loadStoreList() {
      this.storeLoading = true
      TableApi.getTableAllSimpleList()
        .then((res) => {
          const list = Array.isArray(res) ? res : []
          this.storeList = list.sort((a, b) => {
            const aStatus = a.storeStatus ?? 0
            const bStatus = b.storeStatus ?? 0
            return bStatus - aStatus
          })
          this.queryParams.storeId = null
          this.getList()
          this.loadStatusCounts()
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
    matchesChannelType(order) {
      if (!this.queryParams.channelType) {
        return true
      }
      return order.channelType === this.queryParams.channelType
    },
    matchesOrderType(order) {
      if (!this.queryParams.orderType) {
        return true
      }
      return Number(order.arriveType) === Number(this.queryParams.orderType)
    },
    matchesExceptionType(order) {
      if (!this.queryParams.exceptionType) {
        return true
      }
      const isException = this.isExceptionStatus(order.status)
      return this.queryParams.exceptionType === 'exception' ? isException : !isException
    },
    matchesDeliveryMode(order) {
      if (!this.queryParams.deliveryMode) {
        return true
      }
      const mode = Number(this.queryParams.deliveryMode)
      if (mode === 1) {
        return Number(order.deliveryPlatform) > 0
      }
      if (mode === 2) {
        return Number(order.deliveryPlatform) === 0
      }
      if (mode === 3) {
        return order.channelType === 'POS'
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
      const storeId = this.queryParams.storeId
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
      return orderList.filter((order) => {
        return (
          (this.activeTab === 'all' || order.status === targetStatus) &&
          this.includesKeyword(order.orderNo, orderNoKeyword) &&
          this.includesKeyword(order.userName, receiverKeyword) &&
          this.matchesPhoneSuffix(order.userPhone, phoneSuffix) &&
          this.includesKeyword(order.channelOrderId, channelOrderKeyword) &&
          this.includesKeyword(order.userAddress, addressKeyword) &&
          this.matchesChannelType(order) &&
          this.matchesOrderType(order) &&
          this.matchesExceptionType(order) &&
          this.matchesDeliveryMode(order) &&
          this.matchesGoods(order) &&
          (!storeId || order.storeCode === storeId)
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
      const timeRange = this.getTimestampRange()
      if (!timeRange) {
        this.statusCounts = {
          all: 0,
          paid: 0,
          accepted: 0,
          picked: 0,
          packed: 0,
          shipped: 0,
          done: 0,
          closed: 0
        }
        return
      }
      const params = {
        startTime: timeRange.startTime,
        endTime: timeRange.endTime
      }
      if (this.queryParams.storeId) {
        params.platformStoreId = this.queryParams.storeId.trim()
      }
      if (this.queryParams.orderNo) {
        params.orderId = this.queryParams.orderNo.trim()
      }
      if (this.queryParams.channelOrderNo) {
        params.channelOrderId = this.queryParams.channelOrderNo.trim()
      }
      if (this.queryParams.receiverName) {
        params.buyerName = this.queryParams.receiverName.trim()
      }
      if (this.queryParams.phoneSuffix) {
        params.buyerPhoneSuffix = this.queryParams.phoneSuffix.trim()
      }
      if (this.queryParams.goodsName) {
        params.skuName = this.queryParams.goodsName.trim()
      }
      if (this.queryParams.channelType) {
        params.channelType = this.queryParams.channelType
      }
      if (this.queryParams.orderType) {
        params.arriveType = this.queryParams.orderType
      }
      if (this.queryParams.exceptionType) {
        params.exceptionType = this.queryParams.exceptionType
      }
      if (this.queryParams.deliveryMode) {
        params.deliveryMode = this.queryParams.deliveryMode
      }
      if (this.queryParams.address) {
        params.address = this.queryParams.address.trim()
      }
      getOrderStatusCounts(params)
        .then((res) => {
          const counts = res.data || res
          const totalCount = Object.values(counts).reduce((sum, c) => sum + Number(c || 0), 0)
          this.statusCounts = {
            all: totalCount,
            paid: Number(counts['1'] || counts[1] || 0),
            accepted: Number(counts['2'] || counts[2] || 0),
            picked: Number(counts['3'] || counts[3] || 0),
            packed: Number(counts['4'] || counts[4] || 0),
            shipped: Number(counts['5'] || counts[5] || 0),
            done: Number(counts['6'] || counts[6] || 0),
            closed: Number(counts['-1'] || counts[-1] || 0)
          }
        })
        .catch(() => {})
    },
    formatLocalDate(date) {
      const year = date.getFullYear()
      const month = `${date.getMonth() + 1}`.padStart(2, '0')
      const day = `${date.getDate()}`.padStart(2, '0')
      return `${year}-${month}-${day}`
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
    clearListState() {
      this.allOrderList = []
      this.orderList = []
      this.total = 0
      this.goodsList = []
    },
    getList() {
      this.loading = true
      this.queryParams.status = this.activeTab
      if (this.useMockData) {
        setTimeout(() => {
          this.loadMockData()
          this.loading = false
        }, 300)
        return
      }
      const timeRange = this.getTimestampRange()
      if (!timeRange) {
        // console.warn('[ELE前端] 时间范围未设置，将使用今日日期进行查询')
        const today = this.formatLocalDate(new Date())
        this.queryParams.beginTime = today
        this.queryParams.endTime = today
      }
      const finalTimeRange = timeRange || this.getTimestampRange()
      if (!finalTimeRange) {
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
        requestParams.platformStoreId = this.queryParams.storeId.trim()
      }
      if (this.queryParams.orderNo) {
        requestParams.orderId = this.queryParams.orderNo.trim()
      }
      if (this.queryParams.channelOrderNo) {
        requestParams.channelOrderId = this.queryParams.channelOrderNo.trim()
      }
      if (this.queryParams.receiverName) {
        requestParams.buyerName = this.queryParams.receiverName.trim()
      }
      if (this.queryParams.phoneSuffix) {
        requestParams.buyerPhoneSuffix = this.queryParams.phoneSuffix.trim()
      }
      if (this.queryParams.goodsName) {
        requestParams.skuName = this.queryParams.goodsName.trim()
      }
      if (this.queryParams.channelType) {
        requestParams.channelType = this.queryParams.channelType
      }
      if (this.queryParams.deliveryType) {
        requestParams.arriveType = this.queryParams.deliveryType
      }
      if (this.queryParams.orderType) {
        requestParams.arriveType = this.queryParams.orderType
      }
      if (this.queryParams.exceptionType) {
        requestParams.exceptionType = this.queryParams.exceptionType
      }
      if (this.queryParams.deliveryMode) {
        requestParams.deliveryMode = this.queryParams.deliveryMode
      }
      if (this.queryParams.address) {
        requestParams.address = this.queryParams.address.trim()
      }
      if (this.queryParams.orderSort) {
        requestParams.orderSort = this.queryParams.orderSort
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
      getOrderPage(requestParams)
        .then((response) => {
          const orderList = Array.isArray(response?.list) ? response.list : []
          const normalizedList = orderList.map((item) => this.normalizeOrder(item))
          this.total = response.total || 0
          this.allOrderList = normalizedList
          this.syncGoodsListFromOrders(normalizedList)
          this.dataSource = 'api'
          this.applyLocalView()
          this.loading = false
        })
        .catch((error) => {
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
          deliveryType: 1,
          arriveType: 2,
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
          deliveryType: 1,
          arriveType: 2,
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
          deliveryType: 1,
          arriveType: 1,
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
      this.loadStatusCounts()
    },
    handlePageChange() {
      if (this.dataSource === 'mock') {
        this.applyLocalView()
      } else {
        this.getList()
      }
    },
    resetQuery() {
      this.dateRange = []
      this.initDateRange()
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
        channelType: null,
        orderType: null,
        exceptionType: null,
        address: null,
        status: null,
        beginTime: null,
        endTime: null,
        deliveryMode: null
      }
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleTabClick(tab) {
      this.activeTab = tab.name
      this.queryParams.pageNum = 1
      this.getList()
      this.loadStatusCounts()
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
      const statusMap = { 6: '交易成功' }
      return statusMap[status] || this.getStatusText(status)
    },
    getChannelLabel(channelType) {
      const channelMap = {
        ELE: '淘宝闪购',
        MT: '美团',
        JD: '京东',
        POS: 'POS',
        OO: '销售开单'
      }
      return channelMap[channelType] || channelType || '--'
    },
    getChannelTagType(channelType) {
      const typeMap = {
        ELE: 'primary',
        MT: 'warning',
        JD: 'success',
        POS: 'info',
        OO: 'success'
      }
      return typeMap[channelType] || 'info'
    },
    getDeliveryTypeTagType(deliveryType) {
      const typeMap = { 1: 'success', 2: 'warning', 3: 'info' }
      return typeMap[deliveryType] || 'info'
    },
    getDeliveryTypeText(deliveryType) {
      const typeMap = { 1: '平台配', 2: '自配送', 3: '自提' }
      return typeMap[deliveryType] || '--'
    },
    getArriveTypeTagType(arriveType) {
      const typeMap = { 1: 'warning', 2: 'primary' }
      return typeMap[arriveType] || 'info'
    },
    getArriveTypeText(arriveType) {
      const typeMap = { 1: '预约单', 2: '即时单' }
      return typeMap[arriveType] || '--'
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
    hasExchangeAmount(goodsList) {
      if (!Array.isArray(goodsList)) return false
      return goodsList.some((g) => Number(g.exchangeAmount || 0) > 0)
    },
    getDiscountTypeText(type) {
      const typeMap = { sku: '商品优惠', post: '配送优惠' }
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
    formatYuan(value) {
      if (value === null || value === undefined || value === '') {
        return '--'
      }
      return `¥${Number(value).toFixed(2)}`
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
              weight: sub.weight ? `${sub.weight}g` : '--',
              price: sub.price,
              quantity: sub.buyAmount ?? 0,
              spec: sub.specification || '--',
              location: sub.cabinetCode || '--',
              totalAmount,
              discountAmount: Math.max(totalAmount - actualAmount, 0),
              actualAmount,
              giftFlag: sub.giftFlag ?? 0,
              exchangeFlag: sub.exchangeFlag ?? 0,
              exchangeAmount: sub.exchangeAmount ?? 0
            }
          })
        : []
      const discounts = Array.isArray(item.discounts)
        ? item.discounts.map((discount) => ({
            ...discount,
            activityName: discount.activityName || discount.name || '--',
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
            (store) => store.platformStoreId === item.storeCode || store.storeId === item.storeCode
          )?.storeName ||
          item.channelSourceName ||
          '--',
        channelLabel: this.getChannelLabel(item.channelType),
        deliveryTypeText: this.getDeliveryTypeText(item.deliveryType),
        deliveryPlatformText: this.getDeliveryPlatformText(item),
        arriveType: item.arriveType,
        arriveTypeText: this.getArriveTypeText(item.arriveType),
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
        estimatedIncome: item.estimatedIncome ?? 0,
        platformCommissionFee: item.platformCommissionFee ?? 0,
        platformOrderStatus: item.platformOrderStatus,
        platformOrderStatusName: item.platformOrderStatusName,
        deliveryPlatformName: item.deliveryPlatformName,
        platformStoreId: item.platformStoreId,
        erpStoreCode: item.erpStoreCode,
        merchantCode: item.merchantCode,
        channelSourceId: item.channelSourceId,
        isTestOrder: item.isTestOrder,
        isHistoryOrder: item.isHistoryOrder,
        orderFrom: item.orderFrom,
        orderIndex: item.orderIndex,
        longitude: item.longitude,
        latitude: item.latitude,
        createTimeRaw: Number(item.createTime || 0),
        payTimeRaw: Number(item.payTime || 0),
        updateTimeText: item.updateTime ? this.formatTimestamp(item.updateTime) : '--',
        createTime: this.formatTimestamp(item.createTime),
        payTime: this.formatTimestamp(item.payTime),
        goodsList,
        discounts,
        logs: this.buildOrderLogs(item),
        showDetail: false,
        settlementAmount: item.settlementAmount != null ? Number(item.settlementAmount) : null,
        settlementStatus: item.settlementStatus != null ? Number(item.settlementStatus) : null,
        billInfo: null
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
      const statusFilteredOrders = this.filterOrdersByTab(this.allOrderList)
      const searchFilteredOrders = this.applySearchFilters(statusFilteredOrders)
      const sortedOrders = this.sortOrders(searchFilteredOrders)
      if (this.dataSource === 'mock') {
        const pageNum = this.queryParams.pageNum
        const pageSize = this.queryParams.pageSize
        const start = (pageNum - 1) * pageSize
        const end = start + pageSize
        this.orderList = sortedOrders.slice(start, end)
      } else {
        this.orderList = sortedOrders
      }
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
.search-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
}

.query-form-grid {
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.search-row {
  margin-bottom: 16px;
}

.search-row:last-child {
  margin-bottom: 0;
}

.actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.actions-left,
.actions-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 16px;
  border-left: 1px solid #e5e7eb;
  margin-left: 4px;
}

.selector-label,
.selector-suffix {
  font-size: 14px;
  color: #666;
}

.selector-label {
  margin-right: 8px;
}

.selector-suffix {
  margin-left: 8px;
}

@media (max-width: 1200px) {
  .query-form-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .actions-bar {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .actions-left,
  .actions-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .action-group {
    border-left: none;
    padding-left: 0;
    margin-left: 0;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .query-form-grid {
    grid-template-columns: 1fr;
  }
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

.status-tabs {
  margin-bottom: 16px;

  :deep(.el-tabs__header) {
    margin: 0;
    background: #fff;
    padding: 0 16px;
    border-radius: 8px 8px 0 0;
    border: 1px solid #e5e7eb;
    border-bottom: none;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background: #e5e7eb;
  }

  :deep(.el-tabs__item) {
    color: #666;
    font-weight: 500;
    padding: 0 16px;
    height: 48px;
    line-height: 48px;
    font-size: 14px;

    &:hover {
      color: #409eff;
    }

    &.is-active {
      color: #409eff;
      font-weight: 600;
    }
  }

  :deep(.el-tabs__active-bar) {
    height: 2px;
  }
}

.order-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.order-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  background: #fafafa;

  .order-header-left {
    display: flex;
    flex-direction: column;
    gap: 10px;
    min-width: 0;

    .order-main-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .order-time-meta {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }

    .order-no {
      font-weight: 700;
      color: #333;
      font-size: 15px;
    }

    .store-name {
      color: #666;
      font-size: 14px;
    }

    .expect-time {
      color: #999;
      font-size: 12px;
    }
  }

  .order-header-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 8px;
    flex-shrink: 0;

    .channel-order-no {
      color: #999;
      font-size: 12px;
    }

    .order-index {
      color: #909399;
      font-size: 12px;
      font-weight: 500;
    }
  }
}

.order-content {
  padding: 16px;

  .order-content-top {
    display: flex;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .status-tag-left {
    flex-shrink: 0;
    margin-right: 12px;
  }

  .user-info.compact {
    flex: 1;

    p {
      margin: 8px 0;
      font-size: 13px;
      line-height: 1.5;
      color: #666;

      strong {
        color: #333;
      }
    }
  }

  .delivery-info {
    background: #fafafa;
    border: 1px solid #f0f0f0;
    border-radius: 6px;
    padding: 12px;
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-left: 72px;

    .delivery-item {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 150px;

      .delivery-label {
        font-size: 12px;
        color: #999;
      }

      .delivery-value {
        font-size: 13px;
        color: #333;
      }
    }
  }

  .order-status {
    flex: 1;
    display: grid;
    grid-template-columns: minmax(280px, 1.5fr) minmax(200px, 1fr);
    gap: 16px;

    .info-block {
      background: #fafafa;
      border: 1px solid #f0f0f0;
      border-radius: 6px;
      padding: 14px;

      .block-title {
        font-size: 13px;
        font-weight: 600;
        color: #333;
        margin-bottom: 10px;
        border-bottom: 1px solid #e5e7eb;
        padding-bottom: 8px;
      }
    }

    .user-info.compact {
      p {
        margin: 8px 0;
        font-size: 13px;
        line-height: 1.5;
        color: #666;

        strong {
          color: #333;
        }
      }
    }

    .simple-info {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      padding: 12px;
      background: #fafafa;
      border-radius: 6px;
      font-size: 12px;
      color: #666;
      border: 1px solid #f0f0f0;

      span {
        white-space: nowrap;
      }
    }
  }
}

.delivery-info {
  margin-left: 72px;

  .delivery-item {
    .delivery-value {
      &.emphasis {
        color: #409eff;
        font-size: 15px;
      }
    }
  }
}

.price-info {
  margin-left: 72px;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  gap: 12px;

  .price-left {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    font-size: 13px;

    .price {
      color: #409eff;
      font-size: 16px;
      font-weight: 700;
    }

    .income {
      color: #e6a23c;
      font-size: 14px;
      font-weight: 600;
    }

    .commission {
      color: #f56c6c;
      font-size: 14px;
      font-weight: 600;
    }

    .settled {
      color: #67c23a;
      font-size: 14px;
      font-weight: 600;
    }

    .unsettled {
      color: #909399;
      font-size: 14px;
      font-weight: 600;
    }

    .profit {
      color: #67c23a;
      font-size: 14px;
      font-weight: 600;
    }

    span {
      color: #666;
    }
  }

  .price-right {
    display: flex;
    gap: 10px;
    font-size: 13px;
    flex-shrink: 0;
  }
}

.order-footer {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #999;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  background: #fafafa;
}

@media (max-width: 1280px) {
  .delivery-info {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 960px) {
  .order-header {
    flex-direction: column;

    .order-header-right {
      align-items: flex-start;
    }
  }

  .order-content .order-status {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .delivery-info {
    grid-template-columns: 1fr;
  }

  .price-info {
    flex-direction: column;
    align-items: flex-start;
  }
}

.goods-detail-content {
  .info-row {
    margin-bottom: 12px;
  }

  .detail-section-title {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin: 20px 0 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #e5e7eb;
  }

  .fee-summary {
    background: #fafafa;
    border-radius: 6px;
    padding: 16px;
    border: 1px solid #e5e7eb;

    .fee-item {
      display: flex;
      justify-content: space-between;
      padding: 6px 0;
      color: #666;
      font-size: 13px;

      &.total {
        border-top: 1px solid #e5e7eb;
        margin-top: 8px;
        padding-top: 12px;
        font-weight: 700;
        color: #409eff;
        font-size: 16px;
      }
    }
  }
}

.bill-detail-content {
  .info-row {
    margin-bottom: 12px;
  }

  .detail-section-title {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin: 20px 0 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #e5e7eb;
  }
}

.empty-bill-info {
  padding: 40px 0;
  text-align: center;
}

:deep(.el-table) {
  font-size: 13px;
}

:deep(.el-descriptions) {
  .el-descriptions__cell {
    background: #fff !important;
  }
}

:deep(.el-link) {
  color: #409eff;
}
</style>
