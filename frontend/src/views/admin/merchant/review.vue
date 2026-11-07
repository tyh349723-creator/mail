<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="query.keyword"
        placeholder="商户名称/联系人/电话"
        style="width: 220px"
        clearable
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="审核状态" style="width: 150px" clearable>
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column prop="merchantNo" label="商户编号" width="140" />
        <el-table-column prop="shopName" label="店铺名称" min-width="160" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="shopPhone" label="联系电话" width="130" />
        <el-table-column prop="categoryName" label="经营类目" width="120" />
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="primary" size="small" link @click="openDetail(row)">
              审核
            </el-button>
            <el-button size="small" link @click="openDetail(row)">详情</el-button>
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

    <!-- 审核详情弹窗 -->
    <el-dialog v-model="detailVisible" title="商户入驻审核" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商户编号">{{ detail?.merchantNo }}</el-descriptions-item>
        <el-descriptions-item label="店铺名称">{{ detail?.shopName }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail?.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail?.shopPhone }}</el-descriptions-item>
        <el-descriptions-item label="经营类目">{{ detail?.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail?.createTime }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">资质文件</el-divider>
      <div class="qualification-list">
        <div v-for="q in detail?.qualifications" :key="q.id" class="qual-item">
          <el-image :src="q.url" fit="cover" class="qual-img" :preview-src-list="[q.url]" preview-teleported />
          <span class="qual-name">{{ qualTypeMap[q.type] || q.name }}</span>
        </div>
      </div>

      <template v-if="detail?.status === 0" #footer>
        <el-input v-model="rejectReason" type="textarea" :rows="2" placeholder="驳回时请填写原因" />
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">取消</el-button>
          <el-button type="danger" @click="handleAudit(false)">驳回</el-button>
          <el-button type="primary" @click="handleAudit(true)">通过</el-button>
        </div>
      </template>
      <template v-else #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getMerchantList, getMerchantDetail, auditMerchant } from '@/api/admin/merchant'
import type { Merchant } from '@/types'
import Pagination from '@/components/common/Pagination.vue'

const loading = ref(false)
const list = ref<Merchant[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, status: undefined as number | undefined, keyword: '' })

const detailVisible = ref(false)
const detail = ref<Merchant | null>(null)
const rejectReason = ref('')

const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' },
  3: { text: '已关闭', type: 'info' }
}

const qualTypeMap: Record<string, string> = {
  business_license: '营业执照',
  id_card: '法人身份证',
  bank: '银行开户许可'
}

async function loadList() {
  loading.value = true
  try {
    const res = await getMerchantList(query)
    list.value = res?.list || []
    total.value = res?.total || 0
  } catch (e) {
    loadMock()
  } finally {
    loading.value = false
  }
}

function loadMock() {
  list.value = Array.from({ length: 8 }, (_, i) => ({
    id: i + 1,
    merchantNo: `M2024${String(i + 1).padStart(4, '0')}`,
    shopName: `测试店铺${i + 1}`,
    contactName: `联系人${i + 1}`,
    shopPhone: `138${String(i).padStart(8, '0')}`,
    categoryId: (i % 4) + 1,
    categoryName: ['数码家电', '服饰鞋包', '食品生鲜', '美妆个护'][i % 4],
    status: [0, 0, 1, 2, 0, 1, 0, 2][i % 8],
    createTime: '2024-09-01 10:00:00',
    qualifications: []
  }))
  total.value = 32
}

async function openDetail(row: Merchant) {
  detailVisible.value = true
  rejectReason.value = ''
  try {
    detail.value = await getMerchantDetail(row.id)
  } catch (e) {
    detail.value = {
      ...row,
      qualifications: [
        { id: 1, type: 'business_license', name: '营业执照', url: 'https://picsum.photos/300/200' },
        { id: 2, type: 'id_card', name: '法人身份证', url: 'https://picsum.photos/300/200' },
        { id: 3, type: 'bank', name: '银行开户许可', url: 'https://picsum.photos/300/200' }
      ]
    }
  }
}

async function handleAudit(approve: boolean) {
  if (!approve && !rejectReason.value) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  if (!detail.value) return

  try {
    await ElMessageBox.confirm(
      approve ? '确认通过该商户入驻申请？' : '确认驳回该商户入驻申请？',
      '审核确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }

  try {
    await auditMerchant({ id: detail.value.id, approve, rejectReason: rejectReason.value })
  } catch (e) {
    // 模拟成功
  }
  ElMessage.success(approve ? '审核通过' : '已驳回')
  detailVisible.value = false
  loadList()
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  query.keyword = ''
  query.status = undefined
  query.pageNum = 1
  loadList()
}

onMounted(loadList)
</script>

<style scoped lang="scss">
.qualification-list {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;

  .qual-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;

    .qual-img {
      width: 160px;
      height: 120px;
      border-radius: 4px;
      border: 1px solid #e4e7ed;
    }
    .qual-name {
      font-size: 12px;
      color: #606266;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
