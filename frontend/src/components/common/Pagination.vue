<template>
  <div class="pagination-wrap">
    <el-pagination
      :current-page="page"
      :page-size="limit"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  total: number
  page: number
  limit: number
}>()

const emit = defineEmits<{
  (e: 'update:page', value: number): void
  (e: 'update:limit', value: number): void
  (e: 'pagination'): void
}>()

const page = computed({
  get: () => props.page,
  set: (v) => emit('update:page', v)
})
const limit = computed({
  get: () => props.limit,
  set: (v) => emit('update:limit', v)
})

function handleSizeChange(val: number) {
  emit('update:limit', val)
  emit('pagination')
}

function handleCurrentChange(val: number) {
  emit('update:page', val)
  emit('pagination')
}
</script>
