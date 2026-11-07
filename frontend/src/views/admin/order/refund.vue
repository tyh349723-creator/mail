<template>
  <div class="app-container">
    <!-- 平台退款率统计条（5.3.4：大额退款审核 + 退款率监控） -->
    <div class="stat-bar">
      <div class="stat"><div class="v">{{ summary.refundRate ?? 0 }}%</div><div class="k">平台退款率</div></div>
      <div class="stat"><div class="v money">¥{{ fmt(refundMoney) }}</div><div class="k">累计退款金额</div></div>
      <div class="stat"><div class="v">{{ refundCount }}</div><div class="k">退款笔数</div></div>
      <div class="stat"><div class="v dim">¥{{ fmt(totalMoney) }}</div><div class="k">累计交易额</div></div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="query.orderNo"
        placeholder="原订单号"
        style="width: 200px"
        clearable
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="退款状态" style="width: 140px" clearable>
        <el-option label="待处理" :value="0" />
        <el-option label="已处理" :value="1" />
        <el-option label="退款失败" :value="2" />
        <el-option label="已撤销" :value="3" />
      </el-select>
      <el-checkbox v-model="largeOnly" @change="handleSearch">仅看大额（≥5000 元）</el-checkbox>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column prop="refundOrderId" label="退款单号" width="170" />
        <el-table-column prop="orderNo" label="原订单号" width="190" />
        <el-table-column prop="merchantNo" label="商户编号" width="110" />
        <el-table-column label="退款金额" width="130">
          <template #default="{ row }">
            <span class="amount">¥{{ fmt(row.money) }}</span>
            <el-tag v-if="Number(row.money) >= 5000" type="danger" size="small" effect="dark" class="big-tag">大额</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="140" show-overflow-tooltip />
        <el-table-column prop="auditRemark" label="处理意见" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="refundStatus[row.status]?.type">{{ row.statusText || refundStatus[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column prop="auditTime" label="处理时间" width="160" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="primary" size="small" link @click="handleAudit(row, true)">同意</el-button>
              <el-button type="danger" size="small" link @click="handleAudit(row, false)">驳回</el-button>
            </template>
            <el-button v-else size="small" link @click="openDetail(row)">查看</el-button>
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

    <!-- 退款详情弹窗 -->
    <el-dialog v-model="detailVisible" title="退款详情" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="退款单号">{{ currentRow?.refundOrderId }}</el-descriptions-item>
        <el-descriptions-item label="原订单号">{{ currentRow?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="商户编号">{{ currentRow?.merchantNo }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">¥{{ fmt(currentRow?.money) }}</el-descriptions-item>
        <el-descriptions-item label="退款原因">{{ currentRow?.reason || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="refundStatus[currentRow?.status]?.type">{{ currentRow?.statusText || refundStatus[currentRow?.status]?.text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentRow?.createTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ currentRow?.auditTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="处理意见">{{ currentRow?.auditRemark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回退款申请" width="480px">
      <el-form label-width="90px">
        <el-form-item label="退款单号">{{ currentRow?.refundOrderId }}</el-form-item>
        <el-form-item label="驳回原因">
          <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getRefundList, auditRefund } from '@/api/admin/order'
import type { RefundOrder } from '@/types'
import Pagination from '@/components/common/Pagination.vue'

const LARGE_AMOUNT = 5000 // 大额退款阈值，与系统参数 refund.large-threshold 一致

const loading = ref(false)
const list = ref<RefundOrder[]>([])
const total = ref(0)
const largeOnly = ref(false)
const summary = ref({ refundCount: 0, refundMoney: 0, totalMoney: 0, refundRate: 0 })
const refundMoney = computed(() => summary.value.refundMoney)
const refundCount = computed(() => summary.value.refundCount)
const totalMoney = computed(() => summary.value.totalMoney)

const query = reactive({ pageNum: 1, pageSize: 10, orderNo: '', status: undefined as number | undefined, amountMin: undefined as number | undefined })

const rejectVisible = ref(false)
const detailVisible = ref(false)
const currentRow = ref<any>(null)
const rejectReason = ref('')

const refundStatus: Record<number, { text: string; type: string }> = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '已处理', type: 'success' },
  2: { text: '退款失败', type: 'danger' },
  3: { text: '已撤销', type: 'info' }
}

function fmt(v: any) {
  return Number(v || 0).toLocaleString()
}

async function loadList() {
  loading.value = true
  try {
    const res = await getRefundList(query)
    list.value = res.list || []
    total.value = res.total || 0
    summary.value = {
      refundCount: res.refundCount,
      refundMoney: res.refundMoney,
      totalMoney: res.totalMoney,
      refundRate: res.refundRate
    }
  } finally {
    loading.value = false
  }
}

async function handleAudit(row: RefundOrder, approve: boolean) {
  currentRow.value = row
  if (approve) {
    try {
      await ElMessageBox.confirm(
        `确认同意退款申请「${row.refundOrderId}」，金额 ¥${fmt(row.money)}？`,
        '审批确认',
        { type: 'warning' }
      )
    } catch (e) {
      return
    }
    await auditRefund({ id: row.id, approve: true })
    ElMessage.success('已同意，退款办理中')
    loadList()
  } else {
    rejectReason.value = ''
    rejectVisible.value = true
  }
}

async function confirmReject() {
  if (!rejectReason.value) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  if (!currentRow.value) return
  await auditRefund({ id: currentRow.value.id, approve: false, reason: rejectReason.value })
  ElMessage.success('已驳回')
  rejectVisible.value = false
  loadList()
}

function openDetail(row: RefundOrder) {
  currentRow.value = row
  detailVisible.value = true
}

function handleSearch() {
  query.amountMin = largeOnly.value ? LARGE_AMOUNT : undefined
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  query.orderNo = ''
  query.status = undefined
  query.amountMin = undefined
  largeOnly.value = false
  query.pageNum = 1
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.stat-bar {
  display: flex;
  gap: 12px;
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}
.stat { flex: 1; text-align: center; }
.stat .v { font-size: 22px; font-weight: 700; color: #303133; }
.stat .v.money { color: #f56c6c; }
.stat .v.dim { color: #909399; font-weight: 600; }
.stat .k { font-size: 12px; color: #909399; margin-top: 4px; }
.amount { color: #f56c6c; font-weight: 600; }
.big-tag { margin-left: 8px; }
</style>