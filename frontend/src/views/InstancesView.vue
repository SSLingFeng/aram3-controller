<template>
  <div class="page">
    <div class="page-header">
      <h2>实例管理</h2>
      <el-button type="primary" @click="openCreate">新建实例</el-button>
    </div>

    <el-table :data="instances" border stripe>
      <el-table-column prop="code" label="Code" width="110" />
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="端口" width="170">
        <template #default="{ row }">
          <span class="muted">{{ row.gamePort || '-' }} / q{{ row.steamQueryPort || '-' }} / r{{ row.rconPort || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="bindIp" label="IP" width="130">
        <template #default="{ row }">
          <span class="muted">{{ row.bindIp || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="success" @click="start(row)">启动</el-button>
          <el-button size="small" type="warning" @click="stop(row)">停止</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑实例' : '新建实例'" width="620px">
      <el-form :model="form" label-width="130px">
        <el-form-item label="Code" required>
          <el-input v-model="form.code" :disabled="isEdit" placeholder="如 main-ops" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="绑定 IP">
          <el-input v-model="form.bindIp" placeholder="非必填，如 192.0.2.1；不填则不加 -ip" />
        </el-form-item>
        <el-form-item label="游戏端口">
          <el-input-number v-model="form.gamePort" :min="1" :max="65535" />
          <span class="muted" style="margin-left:8px">-port=</span>
        </el-form-item>
        <el-form-item label="查询端口">
          <el-input-number v-model="form.steamQueryPort" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="RCON 端口">
          <el-input-number v-model="form.rconPort" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="RCON 密码">
          <el-input v-model="form.rconPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="Profile 名">
          <el-input v-model="form.profileName" placeholder="-name= ；可选" />
        </el-form-item>
        <el-form-item label="自定义启动参数">
          <el-input
            v-model="form.extraArgs"
            type="textarea"
            :rows="3"
            placeholder="追加到命令行，例如：-filepatching -bandwidthalg=2 -loadmissiontomemory -cpucount=6 -limitfps=75"
          />
          <div class="muted">按空格拆分追加；-mod/-serverMod 由系统按实例模组自动生成</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/http'

const instances = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const form = reactive({
  id: 0,
  code: '',
  name: '',
  bindIp: '',
  gamePort: 2302,
  steamQueryPort: 2303,
  rconPort: 2306,
  rconPassword: '',
  profileName: '',
  extraArgs: ''
})

function statusType(s) {
  if (s === 'RUNNING') return 'success'
  if (s === 'ERROR') return 'danger'
  if (s === 'STARTING' || s === 'STOPPING') return 'warning'
  return 'info'
}

async function load() {
  instances.value = await client.get('/instances')
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: 0, code: '', name: '', bindIp: '',
    gamePort: 2302, steamQueryPort: 2303, rconPort: 2306,
    rconPassword: '', profileName: '', extraArgs: ''
  })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    code: row.code,
    name: row.name,
    bindIp: row.bindIp || '',
    gamePort: row.gamePort ?? 2302,
    steamQueryPort: row.steamQueryPort ?? 2303,
    rconPort: row.rconPort ?? 2306,
    rconPassword: row.rconPassword || '',
    profileName: row.profileName || '',
    extraArgs: row.extraArgs || ''
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = {
      code: form.code,
      name: form.name,
      bindIp: form.bindIp,
      gamePort: form.gamePort,
      steamQueryPort: form.steamQueryPort,
      rconPort: form.rconPort,
      rconPassword: form.rconPassword,
      profileName: form.profileName,
      extraArgs: form.extraArgs
    }
    if (isEdit.value) {
      await client.put(`/instances/${form.id}`, payload)
      ElMessage.success('已保存')
    } else {
      await client.post('/instances', payload)
      ElMessage.success('实例已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function start(row) {
  try {
    const res = await client.post(`/instances/${row.id}/start`)
    ElMessage.success(`已提交启动任务 #${res.jobId}`)
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function stop(row) {
  try {
    const res = await client.post(`/instances/${row.id}/stop`)
    ElMessage.success(`已提交停止任务 #${res.jobId}`)
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除实例 ${row.name}？`, '警告', { type: 'warning' })
  try {
    await client.delete(`/instances/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
