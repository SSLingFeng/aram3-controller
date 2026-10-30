<template>
  <div class="page">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="openCreate">新建用户</el-button>
    </div>

    <el-table :data="users" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="displayName" label="显示名" min-width="110" />
      <el-table-column prop="email" label="邮箱" min-width="140" />
      <el-table-column label="角色" min-width="140">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" style="margin-right:4px">{{ r }}</el-tag>
          <span v-if="row.superAdmin" class="muted">/ 超管</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="150">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          <el-tag v-if="row.locked" type="danger" style="margin-left:4px">锁定</el-tag>
          <span v-if="row.loginFailCount > 0 && !row.locked" class="muted" style="margin-left:4px">
            失败 {{ row.loginFailCount }} 次
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最近登录" min-width="150">
        <template #default="{ row }">
          <span class="muted">{{ row.lastLoginAt || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="400" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="openReset(row)">改密</el-button>
          <el-button v-if="row.locked" size="small" type="danger" @click="unlock(row)">解锁重置</el-button>
          <el-button size="small" :type="row.status === 1 ? 'info' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建 -->
    <el-dialog v-model="createVisible" title="新建用户" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="createForm.displayName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleIds" multiple placeholder="可多选" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="超级管理员">
          <el-switch v-model="createForm.superAdmin" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="editForm.displayName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.roleIds" multiple style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="超级管理员">
          <el-switch v-model="editForm.superAdmin" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 改密 -->
    <el-dialog v-model="resetVisible" :title="`重置密码：${resetForm.username}`" width="420px">
      <el-form label-width="100px">
        <el-form-item label="新密码" required>
          <el-input v-model="resetForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="resetPassword">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 解锁并重置密码结果 -->
    <el-dialog v-model="unlockVisible" title="已解锁并重置密码" width="480px">
      <el-alert type="warning" :closable="false" style="margin-bottom:12px"
                title="新密码只显示这一次，请立即复制并线下交给用户。" />
      <el-form label-width="100px">
        <el-form-item label="用户名">
          <el-input :model-value="unlockResult.username" readonly />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input :model-value="unlockResult.newPassword" readonly class="mono" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyPassword">复制密码</el-button>
        <el-button type="primary" @click="unlockVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/http'

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
    ElMessage.success('用户已创建')
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
    ElMessage.success('已保存')
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
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function unlock(row) {
  await ElMessageBox.confirm(
    `解锁用户 ${row.username} 并重置为 16 位随机密码？新密码只显示一次。`,
    '解锁并重置密码',
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
  navigator.clipboard?.writeText(unlockResult.newPassword).then(() => ElMessage.success('已复制'))
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${next === 0 ? '禁用' : '启用'}用户 ${row.username}？`, '确认', { type: 'warning' })
  try {
    await client.post(`/users/${row.id}/status?status=${next}`)
    ElMessage.success('已更新状态')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`删除用户 ${row.username}？此操作不可恢复。`, '警告', { type: 'warning' })
  try {
    await client.delete(`/users/${row.id}`)
    ElMessage.success('已删除')
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
