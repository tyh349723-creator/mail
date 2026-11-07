<template>
  <div class="app-container">
    <div class="search-bar">
      <el-input
        v-model="query.keyword"
        placeholder="商户名称"
        style="width: 220px"
        clearable
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="资质状态" style="width: 150px" clearable>
        <el-option label="待审查" :value="0" />
        <el-option label="审查通过" :value="1" />
        <el-option label="审查不通过" :value="2" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column prop="merchantNo" label="商户编号" width="140" />
        <el-table-column prop="shopName" label="店铺名称" min-width="160" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column label="资质文件数" width="110" align="center">
          <template #default="{ row }">{{ row.qualifications?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="审查状态" width="110">
          <template #default="{ row }">
            <el-tag :type="qualStatus[row.status]?.type">{{ qualStatus[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openReview(row)">审查</el-button>
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

    <!-- 资质审查弹窗 -->
    <el-dialog v-model="reviewVisible" title="资质审查" width="760px">
      <div class="qual-grid">
        <div v-for="q in currentQuals" :key="q.id" class="qual-card">
          <el-image
            :src="q.url"
            fit="cover"
            class="qual-img"
            :preview-src-list="currentQuals.map((x) => x.url)"
            preview-teleported
          />
          <div class="qual-info">
            <span class="qual-title">{{ qualTypeMap[q.type] || q.name }}</span>
            <el-radio-group v-model="q.auditStatus" size="small">
              <el-radio-button :label="1">通过</el-radio-button>
              <el-radio-button :label="2">不通过</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </div>

      <template #footer>
        <el-input v-model="reviewReason" type="textarea" :rows="2" placeholder="审查意见（可选）" />
        <div class="dialog-footer">
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReview">提交审查</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getMerchantList, getQualifications, auditQualification } from '@/api/admin/merchant'
import type { Merchant, Qualification } from '@/types'
import Pagination from '@/components/common/Pagination.vue'

const loading = ref(false)
const list = ref<Merchant[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })

const reviewVisible = ref(false)
const currentMerchantId = ref(0)
const currentQuals = ref<Qualification[]>([])
const reviewReason = ref('')

const qualStatus: Record<number, { text: string; type: string }> = {
  0: { text: '待审查', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '未通过', type: 'danger' }
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
    list.value = Array.from({ length: 8 }, (_, i) => ({
      id: i + 1,
      merchantNo: `M2024${String(i + 1).padStart(4, '0')}`,
      shopName: `测试店铺${i + 1}`,
      contactName: `联系人${i + 1}`,
      contactPhone: `138${String(i).padStart(8, '0')}`,
      categoryId: (i % 4) + 1,
      categoryName: ['数码家电', '服饰鞋包', '食品生鲜', '美妆个护'][i % 4],
      status: [0, 1, 0, 2, 0, 1, 2, 0][i % 8],
      createTime: '2024-09-02 09:30:00',
      qualifications: [{ id: 1, type: 'business_license', name: '营业执照', url: '' }]
    }))
    total.value = 20
  } finally {
    loading.value = false
  }
}

async function openReview(row: Merchant) {
  currentMerchantId.value = row.id
  reviewReason.value = ''
  try {
    currentQuals.value = await getQualifications(row.id)
  } catch (e) {
    currentQuals.value = [
      { id: 1, type: 'business_license', name: '营业执照', url: 'https://picsum.photos/300/200', auditStatus: 0 },
      { id: 2, type: 'id_card', name: '法人身份证', url: 'https://picsum.photos/300/200', auditStatus: 0 },
      { id: 3, type: 'bank', name: '银行开户许可', url: 'https://picsum.photos/300/200', auditStatus: 0 }
    ]
  }
  reviewVisible.value = true
}

async function submitReview() {
  const rejected = currentQuals.value.filter((q) => q.auditStatus === 2)
  const allPassed = currentQuals.value.every((q) => q.auditStatus === 1)
  if (!allPassed && rejected.length === 0) {
    ElMessage.warning('请完成所有资质文件的审查')
    return
  }
  try {
    await auditQualification({
      id: currentMerchantId.value,
      approve: allPassed,
      reason: reviewReason.value
    })
  } catch (e) {
    // 模拟
  }
  ElMessage.success('审查结果已提交')
  reviewVisible.value = false
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
.qual-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  .qual-card {
    .qual-img {
      width: 100%;
      height: 140px;
      border-radius: 4px;
      border: 1px solid #e4e7ed;
    }
    .qual-info {
      display: flex;
      flex-direction: column;
      gap: 8px;
      align-items: center;
      margin-top: 8px;

      .qual-title {
        font-size: 13px;
        color: #606266;
      }
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
