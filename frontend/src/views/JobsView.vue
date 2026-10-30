<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('jobs.title') }}</h2>
      <el-button @click="load">{{ $t('common.refresh') }}</el-button>
    </div>
    <el-table :data="jobs" border stripe>
      <el-table-column prop="id" :label="$t('common.id')" width="70" />
      <el-table-column prop="jobType" :label="$t('jobs.columns.type')" width="160" />
      <el-table-column prop="targetType" :label="$t('jobs.columns.target')" width="100" />
      <el-table-column prop="targetId" :label="$t('jobs.columns.targetId')" width="90" />
      <el-table-column prop="status" :label="$t('common.status')" width="110">
        <template #default="{ row }">
          <el-tag :type="jobType(row.status)">{{ $t('jobs.status.' + row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="progress" :label="$t('jobs.columns.progress')" width="90">
        <template #default="{ row }">{{ row.progress }}%</template>
      </el-table-column>
      <el-table-column prop="errorMessage" :label="$t('jobs.columns.error')" min-width="160" />
      <el-table-column prop="createdAt" :label="$t('jobs.columns.createdAt')" min-width="160" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import client from '../api/http'

const jobs = ref([])

function jobType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

async function load() {
  jobs.value = await client.get('/jobs')
}

onMounted(load)
</script>
