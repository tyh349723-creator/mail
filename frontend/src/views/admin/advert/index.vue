<template>
  <div class="app-container">
    <!-- 广告位管理 -->
    <el-card shadow="never" class="mb16">
      <template #header>
        <div class="card-header">
          <span>广告位配置</span>
          <el-button type="primary" size="small" :icon="Plus" @click="openSlotDialog()">新增广告位</el-button>
        </div>
      </template>
      <el-table :data="slots" stripe border>
        <el-table-column prop="slotName" label="广告位名称" min-width="160" />
        <el-table-column prop="slotCode" label="编码" width="160" />
        <el-table-column prop="position" label="展示位置" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 广告列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>广告列表</span>
          <el-button type="primary" size="small" :icon="Plus" @click="openAdDialog()">新增广告</el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-select v-model="query.slotId" placeholder="广告位" style="width: 180px" clearable>
          <el-option v-for="s in slots" :key="s.id" :label="s.slotName" :value="s.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" style="width: 140px" clearable>
          <el-option label="上线" :value="1" />
          <el-option label="下线" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      </div>

      <el-table :data="adverts" stripe border v-loading="loading">
        <el-table-column prop="title" label="广告标题" min-width="180" />
        <el-table-column label="广告图" width="120">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" fit="cover" style="width: 80px; height: 40px" :preview-src-list="[row.imageUrl]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="slotName" label="所属广告位" width="140" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="投放时间" width="200">
          <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '上线' : '下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openAdDialog(row)">编辑</el-button>
            <el-button
              size="small"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleAd(row)"
            >
              {{ row.status === 1 ? '下线' : '上线' }}
            </el-button>
            <el-button type="danger" size="small" link @click="deleteAd(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadAdverts"
      />
    </el-card>

    <!-- 广告位弹窗 -->
    <el-dialog v-model="slotDialogVisible" :title="slotForm.id ? '编辑广告位' : '新增广告位'" width="480px">
      <el-form :model="slotForm" label-width="90px">
        <el-form-item label="广告位名称">
          <el-input v-model="slotForm.slotName" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="slotForm.slotCode" />
        </el-form-item>
        <el-form-item label="展示位置">
          <el-select v-model="slotForm.position" style="width: 100%">
            <el-option label="首页轮播" value="首页轮播" />
            <el-option label="分类页" value="分类页" />
            <el-option label="商品详情页" value="商品详情页" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="slotDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSlot">确定</el-button>
      </template>
    </el-dialog>

    <!-- 广告弹窗 -->
    <el-dialog v-model="adDialogVisible" :title="adForm.id ? '编辑广告' : '新增广告'" width="560px">
      <el-form :model="adForm" label-width="90px">
        <el-form-item label="广告标题">
          <el-input v-model="adForm.title" />
        </el-form-item>
        <el-form-item label="所属广告位">
          <el-select v-model="adForm.slotId" style="width: 100%">
            <el-option v-for="s in slots" :key="s.id" :label="s.slotName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="广告图片">
          <el-input v-model="adForm.imageUrl" placeholder="图片URL" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="adForm.linkUrl" placeholder="https://" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="adForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="投放时间">
          <el-date-picker
            v-model="adTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getAdvertSlots, getAdvertList, addAdvert, updateAdvert, deleteAdvert, updateAdvertStatus, addAdvertSlot } from '@/api/admin/advert'
import type { Advert, AdvertSlot } from '@/types'
import Pagination from '@/components/common/Pagination.vue'

const loading = ref(false)
const slots = ref<AdvertSlot[]>([])
const adverts = ref<Advert[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, slotId: undefined as number | undefined, status: undefined as number | undefined })

const slotDialogVisible = ref(false)
const slotForm = reactive({ id: undefined as number | undefined, slotName: '', slotCode: '', position: '首页轮播', status: 1 as 0 | 1 })

const adDialogVisible = ref(false)
const adTimeRange = ref<string[]>([])
const adForm = reactive({
  id: undefined as number | undefined,
  title: '',
  slotId: undefined as number | undefined,
  slotName: '',
  imageUrl: '',
  linkUrl: '',
  sort: 0,
  startTime: '',
  endTime: '',
  status: 1 as 0 | 1
})

async function loadSlots() {
  try {
    slots.value = await getAdvertSlots()
  } catch (e) {
    slots.value = [
      { id: 1, slotName: '首页顶部轮播', slotCode: 'home_top_banner', position: '首页轮播', status: 1 },
      { id: 2, slotName: '首页中部广告', slotCode: 'home_mid_banner', position: '首页轮播', status: 1 },
      { id: 3, slotName: '分类页广告', slotCode: 'category_banner', position: '分类页', status: 1 }
    ]
  }
}

async function loadAdverts() {
  loading.value = true
  try {
    const res = await getAdvertList(query)
    adverts.value = res?.list || []
    total.value = res?.total || 0
  } catch (e) {
    adverts.value = Array.from({ length: 6 }, (_, i) => ({
      id: i + 1,
      title: `广告活动${i + 1}`,
      slotId: (i % 3) + 1,
      slotName: slots.value[i % 3]?.slotName || '首页顶部轮播',
      imageUrl: `https://picsum.photos/300/150?random=${i}`,
      linkUrl: 'https://example.com',
      sort: i,
      startTime: '2024-09-01 00:00:00',
      endTime: '2024-12-31 23:59:59',
      status: i % 2 === 0 ? 1 : 0
    }))
    total.value = 18
  } finally {
    loading.value = false
  }
}

function openSlotDialog() {
  slotForm.id = undefined
  slotForm.slotName = ''
  slotForm.slotCode = ''
  slotForm.position = '首页轮播'
  slotDialogVisible.value = true
}

async function saveSlot() {
  try {
    await addAdvertSlot({ ...slotForm })
  } catch (e) {
    // 模拟
  }
  ElMessage.success('保存成功')
  slotDialogVisible.value = false
  loadSlots()
}

function openAdDialog(row?: Advert) {
  if (row) {
    Object.assign(adForm, row)
    adTimeRange.value = [row.startTime, row.endTime]
  } else {
    Object.assign(adForm, {
      id: undefined, title: '', slotId: undefined, slotName: '',
      imageUrl: '', linkUrl: '', sort: 0, startTime: '', endTime: '', status: 1
    })
    adTimeRange.value = []
  }
  adDialogVisible.value = true
}

async function saveAd() {
  adForm.startTime = adTimeRange.value?.[0] || ''
  adForm.endTime = adTimeRange.value?.[1] || ''
  const slot = slots.value.find((s) => s.id === adForm.slotId)
  adForm.slotName = slot?.slotName || ''
  try {
    if (adForm.id) {
      await updateAdvert({ ...adForm })
    } else {
      await addAdvert({ ...adForm })
    }
  } catch (e) {
    // 模拟
  }
  ElMessage.success('保存成功')
  adDialogVisible.value = false
  loadAdverts()
}

async function toggleAd(row: Advert) {
  const target = row.status === 1 ? 0 : 1
  try {
    await updateAdvertStatus({ id: row.id, status: target as 0 | 1 })
  } catch (e) {
    // 模拟
  }
  ElMessage.success(target === 1 ? '已上线' : '已下线')
  loadAdverts()
}

async function deleteAd(row: Advert) {
  try {
    await ElMessageBox.confirm(`确认删除广告「${row.title}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteAdvert(row.id)
  } catch (e) {
    // 模拟
  }
  ElMessage.success('删除成功')
  loadAdverts()
}

function handleSearch() {
  query.pageNum = 1
  loadAdverts()
}

onMounted(async () => {
  await loadSlots()
  loadAdverts()
})
</script>

<style scoped lang="scss">
.mb16 {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.search-bar {
  padding: 12px 0;
  background: transparent;
}
</style>
