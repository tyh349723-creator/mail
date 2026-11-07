<template>
  <div class="app-container">
    <div class="search-bar">
      <el-input
        v-model="query.keyword"
        placeholder="账号/昵称/手机号/用户编号"
        style="width: 240px"
        clearable
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="账号状态" style="width: 140px" clearable>
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column label="头像" width="70" align="center">
          <template #default="{ row }">
            <el-avatar :size="32" :src="row.avatar || undefined">
              {{ (row.nickname || row.username || '?').slice(0, 1) }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="userNum" label="用户编号" width="200" />
        <el-table-column prop="username" label="登录账号" width="130" />
        <el-table-column prop="nickname" label="昵称" min-width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="订单数" width="90" align="center">
          <template #default="{ row }">{{ row.orderCount }}</template>
        </el-table-column>
        <el-table-column label="累计消费" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">¥{{ money(row.totalSpent) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openDetail(row)">详情</el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              link
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="warning" size="small" link @click="handleResetPassword(row)">重置密码</el-button>
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

    <el-dialog v-model="detailVisible" title="用户详情" width="780px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="用户编号">{{ detail?.user.userNum }}</el-descriptions-item>
        <el-descriptions-item label="登录账号">{{ detail?.user.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail?.user.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail?.user.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail?.user.status === 1 ? 'success' : 'danger'">
            {{ detail?.user.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ detail?.user.createTime }}</el-descriptions-item>
        <el-descriptions-item label="订单总数">{{ detail?.user.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="有效订单">{{ detail?.user.paidCount }}</el-descriptions-item>
        <el-descriptions-item label="累计消费">¥{{ money(detail?.user.totalSpent) }}</el-descriptions-item>
        <el-descriptions-item label="最近下单" :span="3">
          {{ detail?.user.lastOrderTime || '暂无下单记录' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="detail-section">
        <div class="section-title">收货地址</div>
        <el-table :data="detail?.addresses || []" size="small" border empty-text="暂无收货地址">
          <el-table-column prop="consignee" label="收货人" width="110" />
          <el-table-column prop="phone" label="联系电话" width="140" />
          <el-table-column prop="full" label="收货地址" min-width="260" />
          <el-table-column label="默认" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.defaultFlag" type="success" size="small">默认</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="detail-section">
        <div class="section-title">最近订单</div>
        <el-table :data="detail?.recentOrders || []" size="small" border empty-text="暂无订单记录">
          <el-table-column prop="orderNo" label="订单号" width="180" />
          <el-table-column prop="shopName" label="店铺" min-width="120" />
          <el-table-column label="实付金额" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.payAmount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="95" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" width="160" />
        </el-table>
      </div>

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
import { getUserList, getUserDetail, updateUserStatus, resetUserPassword } from '@/api/admin/user'
import type { MallUserAdmin, MallUserDetail } from '@/types'
import Pagination from '@/components/common/Pagination.vue'

const loading = ref(false)
const list = ref<MallUserAdmin[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<MallUserDetail>()

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined
})

/** 订单状态：-1待支付 0待发货 1支付失败 2已退款 3已关闭 4待收货 5已完成 */
const orderTagType: Record<number, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
  [-1]: 'warning',
  0: 'primary',
  1: 'danger',
  2: 'info',
  3: 'info',
  4: 'primary',
  5: 'success'
}

function statusTagType(status: number) {
  return orderTagType[status] || 'info'
}

function money(v: any) {
  return Number(v || 0).toFixed(2)
}

async function loadList() {
  loading.value = true
  try {
    const res = await getUserList(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function openDetail(row: MallUserAdmin) {
  detail.value = undefined
  detailVisible.value = true
  try {
    detail.value = await getUserDetail({ id: row.id })
  } catch (e) {
    detailVisible.value = false
  }
}

async function toggleStatus(row: MallUserAdmin) {
  const next: 0 | 1 = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确认${action}用户「${row.username}」吗？${next === 0 ? '禁用后该用户将无法登录。' : ''}`,
      `${action}用户`,
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await updateUserStatus({ id: row.id, status: next })
  ElMessage.success(`已${action}`)
  loadList()
}

async function handleResetPassword(row: MallUserAdmin) {
  try {
    const { value } = await ElMessageBox.prompt(
      `为「${row.username}」设置新密码`,
      '重置密码',
      { inputType: 'password', inputPattern: /^.{6,}$/, inputErrorMessage: '密码至少6位' }
    )
    await resetUserPassword({ id: row.id, password: value })
    ElMessage.success('密码已重置')
  } catch (e) {
    // 取消
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, keyword: '', status: undefined })
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.amount {
  color: #f56c6c;
  font-weight: 600;
}
.detail-section {
  margin-top: 16px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
</style>
