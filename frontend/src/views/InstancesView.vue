<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('instances.title') }}</h2>
      <el-button type="primary" @click="openCreate">{{ $t('instances.createBtn') }}</el-button>
    </div>

    <el-table :data="instances" border stripe>
      <el-table-column prop="code" :label="$t('instances.columns.code')" width="110" />
      <el-table-column prop="name" :label="$t('common.name')" min-width="120" />
      <el-table-column prop="status" :label="$t('common.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('instances.columns.port')" width="170">
        <template #default="{ row }">
          <span class="muted">{{ row.gamePort || '-' }} / q{{ row.steamQueryPort || '-' }} / r{{ row.rconPort || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="bindIp" :label="$t('instances.columns.ip')" width="130">
        <template #default="{ row }">
          <span class="muted">{{ row.bindIp || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.action')" width="380">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openControl(row)">{{ $t('instances.actions.console') }}</el-button>
          <el-button size="small" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
          <el-button size="small" type="success" @click="start(row)">{{ $t('instances.actions.start') }}</el-button>
          <el-button size="small" type="warning" @click="stop(row)">{{ $t('instances.actions.stop') }}</el-button>
          <el-button size="small" type="danger" @click="remove(row)">{{ $t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? $t('instances.editTitle') : $t('instances.createTitle')" width="620px">
      <el-form :model="form" label-width="130px">
        <el-form-item :label="$t('instances.form.code')" required>
          <el-input v-model="form.code" :disabled="isEdit" :placeholder="$t('instances.form.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('common.name')" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('instances.form.bindIp')">
          <el-input v-model="form.bindIp" :placeholder="$t('instances.form.bindIpPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('instances.form.gamePort')">
          <el-input-number v-model="form.gamePort" :min="1" :max="65535" />
          <span class="muted" style="margin-left:8px">{{ $t('instances.form.portFlag') }}</span>
        </el-form-item>
        <el-form-item :label="$t('instances.form.queryPort')">
          <el-input-number v-model="form.steamQueryPort" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item :label="$t('instances.form.rconPort')">
          <el-input-number v-model="form.rconPort" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item :label="$t('instances.form.rconPassword')">
          <el-input v-model="form.rconPassword" type="password" show-password />
        </el-form-item>
        <el-form-item :label="$t('instances.form.profileName')">
          <el-input v-model="form.profileName" :placeholder="$t('instances.form.profileNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('instances.form.extraArgs')">
          <el-input
            v-model="form.extraArgs"
            type="textarea"
            :rows="3"
            :placeholder="$t('instances.form.extraArgsPlaceholder')"
          />
          <div class="muted">{{ $t('instances.form.extraArgsHint') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ isEdit ? $t('common.save') : $t('common.create') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/http'
import { useRouter } from 'vue-router'

const { t } = useI18n({ useScope: 'global' })
const router = useRouter()

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

function openControl(row) {
  router.push({ path: '/instance-control', query: { id: row.id } })
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
      ElMessage.success(t('instances.msg.saved'))
    } else {
      await client.post('/instances', payload)
      ElMessage.success(t('instances.msg.created'))
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
    ElMessage.success(t('instances.msg.startSubmitted', { id: res.jobId }))
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function stop(row) {
  try {
    const res = await client.post(`/instances/${row.id}/stop`)
    ElMessage.success(t('instances.msg.stopSubmitted', { id: res.jobId }))
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  await ElMessageBox.confirm(t('common.confirmDelete', { name: row.name }), t('common.warning'), { type: 'warning' })
  try {
    await client.delete(`/instances/${row.id}`)
    ElMessage.success(t('instances.msg.deleted'))
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
