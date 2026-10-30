<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('users.title') }}</h2>
      <el-button type="primary" @click="openCreate">{{ $t('users.createUser') }}</el-button>
    </div>

    <el-table :data="users" border stripe>
      <el-table-column prop="id" :label="$t('common.id')" width="70" />
      <el-table-column prop="username" :label="$t('common.username')" width="130" />
      <el-table-column prop="displayName" :label="$t('users.displayName')" min-width="110" />
      <el-table-column prop="email" :label="$t('users.email')" min-width="140" />
      <el-table-column :label="$t('users.role')" min-width="140">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" style="margin-right:4px">{{ r }}</el-tag>
          <span v-if="row.superAdmin" class="muted">/ {{ $t('users.superAdminShort') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.status')" width="150">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? $t('common.enabled') : $t('common.disabled') }}</el-tag>
          <el-tag v-if="row.locked" type="danger" style="margin-left:4px">{{ $t('users.locked') }}</el-tag>
          <span v-if="row.loginFailCount > 0 && !row.locked" class="muted" style="margin-left:4px">
            {{ $t('users.loginFailCount', { n: row.loginFailCount }) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" :label="$t('users.lastLogin')" min-width="150">
        <template #default="{ row }">
          <span class="muted">{{ row.lastLoginAt || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.action')" width="400" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
          <el-button size="small" type="warning" @click="openReset(row)">{{ $t('users.resetPassword') }}</el-button>
          <el-button v-if="row.locked" size="small" type="danger" @click="unlock(row)">{{ $t('users.unlockReset') }}</el-button>
          <el-button size="small" :type="row.status === 1 ? 'info' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? $t('common.disabled') : $t('common.enabled') }}
          </el-button>
          <el-button size="small" type="danger" @click="remove(row)">{{ $t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建 -->
    <el-dialog v-model="createVisible" :title="$t('users.createUser')" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item :label="$t('common.username')" required>
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item :label="$t('common.password')" required>
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item :label="$t('users.displayName')">
          <el-input v-model="createForm.displayName" />
        </el-form-item>
        <el-form-item :label="$t('users.email')">
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item :label="$t('users.role')">
          <el-select v-model="createForm.roleIds" multiple :placeholder="$t('users.multiSelect')" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('users.superAdmin')">
          <el-switch v-model="createForm.superAdmin" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="create">{{ $t('common.create') }}</el-button>
      </template>
    </el-dialog>

    <!-- 编辑 -->
    <el-dialog v-model="editVisible" :title="$t('users.editUser')" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item :label="$t('common.username')">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item :label="$t('users.displayName')">
          <el-input v-model="editForm.displayName" />
        </el-form-item>
        <el-form-item :label="$t('users.email')">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item :label="$t('users.role')">
          <el-select v-model="editForm.roleIds" multiple style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('users.superAdmin')">
          <el-switch v-model="editForm.superAdmin" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 改密 -->
    <el-dialog v-model="resetVisible" :title="$t('users.resetPasswordTitle', { username: resetForm.username })" width="420px">
      <el-form label-width="100px">
        <el-form-item :label="$t('users.newPassword')" required>
          <el-input v-model="resetForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="resetPassword">{{ $t('users.confirmReset') }}</el-button>
      </template>
    </el-dialog>

    <!-- 解锁并重置密码结果 -->
    <el-dialog v-model="unlockVisible" :title="$t('users.unlockResultTitle')" width="480px">
      <el-alert type="warning" :closable="false" style="margin-bottom:12px"
                :title="$t('users.newPasswordNotice')" />
      <el-form label-width="100px">
        <el-form-item :label="$t('common.username')">
          <el-input :model-value="unlockResult.username" readonly />
        </el-form-item>
        <el-form-item :label="$t('users.newPassword')">
          <el-input :model-value="unlockResult.newPassword" readonly class="mono" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyPassword">{{ $t('users.copyPassword') }}</el-button>
        <el-button type="primary" @click="unlockVisible = false">{{ $t('users.iHaveSaved') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/http'

const { t } = useI18n({ useScope: 'global' })
const users = ref([])
const roles = ref([])
const saving = ref(false)
const createVisible = ref(false)
const editVisible = ref(false)
const resetVisible = ref(false)

const createForm = reactive({
  username: '', password: '', displayName: '', email: '', superAdmin: false, roleIds: []
})
const editForm = reactive({
  id: 0, username: '', displayName: '', email: '', superAdmin: false, roleIds: []
})
const resetForm = reactive({ id: 0, username: '', newPassword: '' })
const unlockVisible = ref(false)
const unlockResult = reactive({ username: '', newPassword: '' })

async function load() {
  users.value = await client.get('/users')
  roles.value = await client.get('/users/roles')
}

function openCreate() {
  Object.assign(createForm, { username: '', password: '', displayName: '', email: '', superAdmin: false, roleIds: [] })
  createVisible.value = true
}

async function create() {
  saving.value = true
  try {
    await client.post('/users', { ...createForm })
    ElMessage.success(t('users.msg.created'))
    createVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

function openEdit(row) {
  Object.assign(editForm, {
    id: row.id,
    username: row.username,
    displayName: row.displayName || '',
    email: row.email || '',
    superAdmin: !!row.superAdmin,
    roleIds: [...(row.roleIds || [])]
  })
  editVisible.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await client.put(`/users/${editForm.id}`, {
      displayName: editForm.displayName,
      email: editForm.email,
      superAdmin: editForm.superAdmin,
      roleIds: editForm.roleIds
    })
    ElMessage.success(t('users.msg.saved'))
    editVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

function openReset(row) {
  resetForm.id = row.id
  resetForm.username = row.username
  resetForm.newPassword = ''
  resetVisible.value = true
}

async function resetPassword() {
  saving.value = true
  try {
    await client.post(`/users/${resetForm.id}/password`, { newPassword: resetForm.newPassword })
    ElMessage.success(t('users.msg.passwordReset'))
    resetVisible.value = false
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function unlock(row) {
  await ElMessageBox.confirm(
    t('users.confirmUnlock', { username: row.username }),
    t('users.confirmUnlockTitle'),
    { type: 'warning' }
  )
  saving.value = true
  try {
    const data = await client.post(`/users/${row.id}/unlock`)
    unlockResult.username = data.username
    unlockResult.newPassword = data.newPassword
    unlockVisible.value = true
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

function copyPassword() {
  navigator.clipboard?.writeText(unlockResult.newPassword).then(() => ElMessage.success(t('common.copied')))
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(t(next === 0 ? 'users.confirmDisable' : 'users.confirmEnable', { username: row.username }), t('common.confirm'), { type: 'warning' })
  try {
    await client.post(`/users/${row.id}/status?status=${next}`)
    ElMessage.success(t('users.msg.statusUpdated'))
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  await ElMessageBox.confirm(t('users.confirmRemove', { username: row.username }), t('common.warning'), { type: 'warning' })
  try {
    await client.delete(`/users/${row.id}`)
    ElMessage.success(t('users.msg.deleted'))
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.mono :deep(input) {
  font-family: Consolas, Monaco, monospace;
  letter-spacing: 0.04em;
}
</style>
