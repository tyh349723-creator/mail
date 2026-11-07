<template>
  <div class="app-container">
    <!-- 状态快捷筛选（异常单 = 支付失败/待支付，可直接人工关单） -->
    <div class="chips">
      <el-radio-group v-model="quickStatus" @change="handleQuick">
        <el-radio-button :value="-99">全部</el-radio-button>
        <el-radio-button :value="1">⚠ 支付失败</el-radio-button>
        <el-radio-button :value="-1">待支付</el-radio-button>
        <el-radio-button :value="4">待收货</el-radio-button>
        <el-radio-button :value="5">已完成</el-radio-button>
      </el-radio-group>
    </div>

    <div class="search-bar">
      <el-input
        v-model="query.orderNo"
        placeholder="订单号"
        style="width: 200px"
        clearable
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="query.shopName"
        placeholder="店铺名称"
        style="width: 180px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="订单状态" style="width: 150px" clearable>
        <el-option v-for="(v, k) in orderStatus" :key="k" :label="v" :value="Number(k)" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="175" />
        <el-table-column prop="shopName" label="店铺" min-width="120" />
        <el-table-column prop="merchantNo" label="商户编号" width="105" />
        <el-table-column prop="buyerName" label="买家" width="90" />
        <el-table-column label="实付金额" width="115">
          <template #default="{ row }">
            <span class="amount">¥{{ fmt(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付渠道" width="100">
          <template #default="{ row }">{{ channelText(row.channel) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]">{{ row.statusText || orderStatus[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openDetail(row)">详情</el-button>
            <!-- 异常单处理（5.3.3）：支付失败/待支付超期 → 人工关单回补库存 -->
            <el-button
              v-if="row.status === -1 || row.status === 1"
              type="danger"
              size="small"
              link
              @click="handleClose(row)"
            >
              处理关单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadList"
      />
    </div>

    <!-- 订单详情 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType[detail?.status ?? -99]">{{ detail?.statusText || orderStatus[detail?.status ?? -99] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="店铺">{{ detail?.shopName }}</el-descriptions-item>
        <el-descriptions-item label="买家">{{ detail?.buyerName }}</el-descriptions-item>
        <el-descriptions-item label="商品总额">¥{{ fmt(detail?.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">¥{{ fmt(detail?.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="支付渠道">{{ channelText(detail?.channel) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail?.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间" :span="2">{{ detail?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getOrderList, getOrderDetail, orderHandle } from '@/api/admin/order'
import type { Order } from '@/types'
import Pagination from '@/components/common/Pagination.vue'

const loading = ref(false)
const list = ref<Order[]>([])
const total = ref(0)
const quickStatus = ref(-99)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  shopName: '',
  status: undefined as number | undefined
})

const detailVisible = ref(false)
const detail = ref<any>(null)

/** 订单状态（StatusConst：-1待支付 0待发货 1支付失败 2已退款 3已关闭 4待收货 5已完成） */
const orderStatus: Record<number, string> = {
  [-1]: '待支付',
  0: '待发货',
  1: '支付失败',
  2: '已退款',
  3: '已关闭',
  4: '待收货',
  5: '已完成'
}

const statusType: Record<number, string> = {
  [-1]: 'warning',
  0: 'primary',
  1: 'danger',
  2: 'info',
  3: 'info',
  4: 'primary',
  5: 'success'
}

function fmt(v: any) {
  return Number(v || 0).toLocaleString()
}

function channelText(code?: string) {
  return ({ WECHAT: '微信支付', ALIPAY: '支付宝', UNIONPAY: '银联支付' } as Record<string, string>)[code || ''] || code || '-'
}

async function loadList() {
  loading.value = true
  try {
    const res = await getOrderList(query)
    list.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

/** 快捷筛选与状态下拉联动 */
function handleQuick() {
  query.status = quickStatus.value === -99 ? undefined : quickStatus.value
  query.pageNum = 1
  loadList()
}

async function handleClose(row: Order) {
  try {
    await ElMessageBox.confirm(
      `确认关闭异常订单「${row.orderNo}」（${row.statusText || orderStatus[row.status]}）？关闭后将回补商品库存。`,
      '异常单处理',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await orderHandle({ orderNo: row.orderNo })
  ElMessage.success('已关闭，库存已回补')
  loadList()
}

async function openDetail(row: Order) {
  detailVisible.value = true
  detail.value = row
  try {
    detail.value = await getOrderDetail(row.orderNo)
  } catch (e) {
    // 详情拉取失败时兜底展示列表行数据
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  quickStatus.value = -99
  Object.assign(query, { pageNum: 1, orderNo: '', shopName: '', status: undefined })
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.chips {
  margin-bottom: 12px;
}
.chips .el-radio-button__inner {
  font-size: 13px;
}
.amount {
  color: #f56c6c;
  font-weight: 600;
}
</style>