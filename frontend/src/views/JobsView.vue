<template>
  <div class="page">
    <div class="page-header">
      <h2>任务中心</h2>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="jobs" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="jobType" label="类型" width="160" />
      <el-table-column prop="targetType" label="目标" width="100" />
      <el-table-column prop="targetId" label="目标ID" width="90" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="jobType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="progress" label="进度" width="90">
        <template #default="{ row }">{{ row.progress }}%</template>
      </el-table-column>
      <el-table-column prop="errorMessage" label="错误" min-width="160" />
      <el-table-column prop="createdAt" label="创建时间" min-width="160" />
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
