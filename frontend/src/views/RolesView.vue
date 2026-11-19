<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('roles.title') }}</h2>
      <el-button type="primary" @click="openCreate">{{ $t('roles.createBtn') }}</el-button>
    </div>

    <el-table :data="roles" border stripe v-loading="loading">
      <el-table-column prop="id" :label="$t('common.id')" width="70" />
      <el-table-column prop="code" :label="$t('roles.code')" width="160">
        <template #default="{ row }"><span class="mono">{{ row.code }}</span></template>
      </el-table-column>
      <el-table-column prop="name" :label="$t('common.name')" min-width="130" />
      <el-table-column prop="description" :label="$t('common.description')" min-width="180">
        <template #default="{ row }">
          <span class="muted">{{ row.description || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.status')" width="110">
        <template #default="{ row }">
          <el-tag :type="row.isSystem ? 'warning' : 'info'" size="small">
            {{ row.isSystem ? $t('roles.systemTag') : $t('roles.customTag') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('roles.permsHeader')" min-width="220">
        <template #default="{ row }">
          <el-tag v-for="p in row.perms" :key="p" size="small" style="margin:2px 4px 2px 0">
            {{ permLabel(p) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.action')" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
          <el-button v-if="!row.isSystem" size="small" type="danger" @click="remove(row)">{{ $t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建角色 -->
    <el-dialog v-model="createVisible" :title="$t('roles.createTitle')" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item :label="$t('roles.code')" required>
          <el-input v-model="createForm.code" :placeholder="$t('roles.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('common.name')" required>
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item :label="$t('common.description')">
          <el-input v-model="createForm.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="create">{{ $t('common.create') }}</el-button>
      </template>
    </el-dialog>

    <!-- 编辑角色（含权限配置） -->
    <el-dialog v-model="editVisible" :title="$t('roles.editTitle')" width="560px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item :label="$t('roles.code')">
          <el-input :model-value="editForm.code" disabled />
          <div class="muted">{{ $t('roles.codeImmutable') }}</div>
        </el-form-item>
        <el-form-item :label="$t('common.name')" required>
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item :label="$t('common.description')">
          <el-input v-model="editForm.description" />
        </el-form-item>
        <el-form-item :label="$t('roles.permsHeader')">
          <div class="perms-groups">
            <div v-for="g in permGroups" :key="g.key" class="perm-group">
              <div class="perm-group-title">{{ $t('perms.groups.' + g.key) }}</div>
              <el-checkbox
                v-for="c in g.codes"
                :key="c"
                :model-value="editForm.perms.includes(c)"
                :label="permLabel(c)"
                @change="(v) => togglePerm(c, v)"
              />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/http'

const { t } = useI18n({ useScope: 'global' })

const roles = ref([])
const loading = ref(false)
const saving = ref(false)
const createVisible = ref(false)
const editVisible = ref(false)
const catalog = ref([])

const createForm = reactive({ code: '', name: '', description: '' })
const editForm = reactive({ id: 0, code: '', name: '', description: '', perms: [] })

const permGroups = computed(() => {
  const groups = [
    { key: 'SYSTEM', codes: [] },
    { key: 'INSTANCE', codes: [] }
  ]
  catalog.value.forEach((c) => {
    const g = groups.find((x) => x.key === c.group)
    if (g) g.codes.push(c.code)
  })
  return groups
})

function permLabel(code) {
  return t('perms.codes.' + code)
}

function togglePerm(code, on) {
  const idx = editForm.perms.indexOf(code)
  if (on && idx === -1) editForm.perms.push(code)
  if (!on && idx !== -1) editForm.perms.splice(idx, 1)
}

async function load() {
  loading.value = true
  try {
    roles.value = await client.get('/roles')
    catalog.value = await client.get('/roles/perm-catalog')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, { code: '', name: '', description: '' })
  createVisible.value = true
}

async function create() {
  saving.value = true
  try {
    await client.post('/roles', { ...createForm, perms: [] })
    ElMessage.success(t('roles.msg.created'))
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
    code: row.code,
    name: row.name,
    description: row.description || '',
    perms: [...(row.perms || [])]
  })
  editVisible.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await client.put(`/roles/${editForm.id}`, {
      name: editForm.name,
      description: editForm.description,
      perms: editForm.perms
    })
    ElMessage.success(t('roles.msg.saved'))
    editVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(t('roles.confirmRemove', { name: row.name }), t('common.warning'), { type: 'warning' })
  try {
    await client.delete(`/roles/${row.id}`)
    ElMessage.success(t('roles.msg.deleted'))
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.mono {
  font-family: Consolas, Monaco, monospace;
}
.perms-groups {
  width: 100%;
}
.perm-group {
  margin-bottom: 8px;
}
.perm-group-title {
  font-weight: 600;
  margin-bottom: 4px;
}
.perm-group :deep(.el-checkbox) {
  display: inline-flex;
  margin-right: 16px;
}
</style>
