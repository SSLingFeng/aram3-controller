<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('mods.title') }}</h2>
    </div>

    <el-tabs v-model="tab">
      <!-- 模组列表 -->
      <el-tab-pane :label="$t('mods.tabMods')" name="mods">
        <div style="margin-bottom:12px;display:flex;gap:8px;flex-wrap:wrap">
          <el-upload :show-file-list="false" :http-request="importHtml" accept=".html,.htm">
            <el-button>{{ $t('mods.importPreset') }}</el-button>
          </el-upload>
          <el-button @click="openAdd">{{ $t('mods.manualAdd') }}</el-button>
          <el-button @click="localSearch">{{ $t('mods.localSearch') }}</el-button>
          <el-button type="success" :loading="movingKeys" @click="moveKeys">{{ $t('mods.moveKeys') }}</el-button>
          <el-button type="warning" @click="askUpdateAll">{{ $t('mods.updateAll') }}</el-button>
          <el-button type="primary" :disabled="!selected.length" @click="askUpdateSelected">
            {{ $t('mods.updateSelected', { n: selected.length }) }}
          </el-button>
        </div>

        <el-alert type="info" :closable="false" style="margin-bottom:12px"
          :title="$t('mods.moveKeysTip')" />

        <el-form inline>
          <el-form-item :label="$t('common.search')">
            <el-input v-model="keyword" :placeholder="$t('mods.searchPlaceholder')" clearable style="width:220px" />
          </el-form-item>
          <el-form-item>
            <el-button size="small" @click="selectAllFiltered">{{ $t('mods.selectAllFiltered') }}</el-button>
            <el-button size="small" @click="selected = []">{{ $t('mods.clearSelection') }}</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="filtered" border stripe @selection-change="onSelectionChange" ref="tableRef">
          <el-table-column type="selection" width="48" />
          <el-table-column prop="id" :label="$t('common.id')" width="70" />
          <el-table-column prop="name" :label="$t('common.name')" min-width="200" />
          <el-table-column prop="workshopId" :label="$t('mods.workshopId')" width="130" />
          <el-table-column prop="downloadStatus" :label="$t('mods.taskStatus')" width="110">
            <template #default="{ row }"><span class="muted">{{ row.downloadStatus }}</span></template>
          </el-table-column>
          <el-table-column prop="localPath" :label="$t('mods.localPath')" min-width="150">
            <template #default="{ row }"><span class="muted">{{ row.localPath || '—' }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('common.action')" width="160">
            <template #default="{ row }">
              <el-button size="small" @click="askUpdateOne(row)">{{ $t('mods.update') }}</el-button>
              <el-button size="small" type="danger" @click="remove(row)">{{ $t('common.delete') }}</el-button>
            </template>
          </el-table-column>
          <el-table-column :label="$t('common.status')" width="100" fixed="right">
            <template #default="{ row }">
              <el-tag v-if="row.scanStatus === 'OK'" type="success" size="small">{{ $t('mods.statusOk') }}</el-tag>
              <el-tag v-else-if="row.scanStatus === 'MISSING'" type="danger" size="small">{{ $t('mods.statusMissing') }}</el-tag>
              <span v-else class="muted">{{ $t('mods.statusUnscanned') }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Key 管理 -->
      <el-tab-pane :label="$t('mods.tabKeys')" name="keys">
        <div style="margin-bottom:12px;display:flex;gap:8px">
          <el-button @click="loadKeys">{{ $t('common.refresh') }}</el-button>
          <el-upload :show-file-list="false" :http-request="uploadKey" accept=".bikey">
            <el-button type="primary">{{ $t('mods.uploadKey') }}</el-button>
          </el-upload>
          <el-button type="success" :loading="movingKeys" @click="moveKeys">{{ $t('mods.moveKeysFromMods') }}</el-button>
        </div>
        <el-alert type="info" :closable="false" style="margin-bottom:12px"
          :title="$t('mods.keysDirTip', { dir: keysDir || $t('mods.keysDirDefault') })" />
        <el-table :data="keys" border stripe>
          <el-table-column prop="fileName" :label="$t('mods.keyFile')" min-width="220" />
          <el-table-column prop="sourceModName" :label="$t('mods.sourceMod')" min-width="180">
            <template #default="{ row }">
              <span v-if="row.sourceModId">#{{ row.sourceModId }} {{ row.sourceModName }}</span>
              <span v-else class="muted">{{ row.sourceModName || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sourceType" :label="$t('mods.sourceType')" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.sourceType === 'MOD' ? 'success' : 'info'">
                {{ row.sourceType === 'MOD' ? $t('mods.sourceTypeMod') : row.sourceType === 'MANUAL' ? $t('mods.sourceTypeManual') : $t('common.unknown') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" :label="$t('mods.size')" width="110">
            <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="filePath" :label="$t('mods.filePath')" min-width="200">
            <template #default="{ row }"><span class="muted">{{ row.filePath }}</span></template>
          </el-table-column>
          <el-table-column :label="$t('common.action')" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="deleteKey(row)">{{ $t('common.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 更新方式 -->
    <el-dialog v-model="updateVisible" :title="updateTitle" width="480px">
      <p style="margin:0 0 12px">{{ $t('mods.chooseUpdateMode') }}</p>
      <el-radio-group v-model="updateMode" style="display:flex;flex-direction:column;gap:10px">
        <el-radio value="DIRECT">{{ $t('mods.modeDirect') }}</el-radio>
        <el-radio value="REDOWNLOAD">{{ $t('mods.modeRedownload') }}</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="updateVisible = false">{{ $t('mods.cancelUpdate') }}</el-button>
        <el-button type="primary" @click="doUpdate">{{ $t('mods.nextStep') }}</el-button>
      </template>
    </el-dialog>

    <!-- Steam 账号 -->
    <el-dialog v-model="steamVisible" :title="$t('mods.steamVerifyTitle')" width="480px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        :title="$t('mods.steamTip')" />
      <el-form label-width="100px">
        <el-form-item :label="$t('mods.steamAccount')" required>
          <el-input v-model="steamForm.username" />
        </el-form-item>
        <el-form-item :label="$t('mods.steamPassword')" required>
          <el-input v-model="steamForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item :label="$t('mods.steamGuard')">
          <el-input v-model="steamForm.guardCode" :placeholder="$t('mods.guardCodePlaceholder')" maxlength="12" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="steamVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="starting" @click="startWithSteam">{{ $t('mods.startUpdate') }}</el-button>
      </template>
    </el-dialog>

    <!-- 手动添加模组 -->
    <el-dialog v-model="addVisible" :title="$t('mods.addDialogTitle')" width="460px">
      <el-form :model="addForm" label-width="110px">
        <el-form-item :label="$t('common.name')">
          <el-input v-model="addForm.name" />
        </el-form-item>
        <el-form-item :label="$t('mods.workshopId')" required>
          <el-input v-model="addForm.workshopId" />
        </el-form-item>
        <el-form-item :label="$t('mods.defaultLoadType')">
          <el-radio-group v-model="addForm.loadType">
            <el-radio value="BOTH">{{ $t('mods.loadTypeBoth') }}</el-radio>
            <el-radio value="SERVER_ONLY">{{ $t('mods.loadTypeServerOnly') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="add">{{ $t('mods.addNoDownload') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/http'

const { t } = useI18n({ useScope: 'global' })

const router = useRouter()
const tab = ref('mods')
const mods = ref([])
const keys = ref([])
const keysDir = ref('')
const keyword = ref('')
const selected = ref([])
const tableRef = ref()
const saving = ref(false)
const starting = ref(false)
const movingKeys = ref(false)
const addVisible = ref(false)
const updateVisible = ref(false)
const steamVisible = ref(false)
const updateMode = ref('DIRECT')
const updateScope = ref({ type: 'selected', ids: [] })
const addForm = reactive({ name: '', workshopId: '', loadType: 'BOTH' })
const steamForm = reactive({ username: '', password: '', guardCode: '' })

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return mods.value
  return mods.value.filter(m =>
    (m.name || '').toLowerCase().includes(k) || (m.workshopId || '').includes(k)
  )
})

const updateTitle = computed(() => {
  if (updateScope.value.type === 'all') return t('mods.updateAll')
  return t('mods.updateSelectedTitle', { n: updateScope.value.ids.length })
})

function formatSize(n) {
  if (!n) return '-'
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}

function onSelectionChange(rows) {
  selected.value = rows
}

function selectAllFiltered() {
  filtered.value.forEach(row => tableRef.value?.toggleRowSelection(row, true))
}

async function load() {
  mods.value = await client.get('/mods')
}

async function loadKeys() {
  try {
    keys.value = await client.get('/keys')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function moveKeys() {
  movingKeys.value = true
  try {
    // use selection if any, else all
    const useSelected = selected.value.length > 0
    const res = await client.post('/mods/actions/move-keys', {
      all: !useSelected,
      modIds: useSelected ? selected.value.map(r => r.id) : []
    })
    keysDir.value = res.keysDir || ''
    ElMessage.success(t('mods.moveKeysResult', {
      copied: res.copiedCount,
      renamed: res.renamedCount,
      skipped: res.skippedCount
    }))
    keys.value = res.items || await client.get('/keys')
    tab.value = 'keys'
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    movingKeys.value = false
  }
}

async function deleteKey(row) {
  await ElMessageBox.confirm(t('common.confirmDelete', { name: row.fileName }), t('common.warning'), { type: 'warning' })
  try {
    await client.delete('/keys', { params: { fileName: row.fileName } })
    ElMessage.success(t('mods.deleted'))
    await loadKeys()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function uploadKey({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await client.post('/keys/upload', fd)
    ElMessage.success(t('mods.keyUploaded', { name: res.fileName }))
    keys.value = res.items || await client.get('/keys')
    tab.value = 'keys'
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function localSearch() {
  try {
    const res = await client.post('/mods/actions/local-search')
    const map = new Map()
    ;(res.items || []).forEach(item => map.set(item.id, item))
    mods.value = mods.value.map(m => {
      const hit = map.get(m.id)
      return {
        ...m,
        localStatus: hit ? hit.localStatus : m.localStatus,
        localPath: hit ? hit.localPath : m.localPath,
        scanStatus: hit ? (hit.localStatus === 'DOWNLOADED' ? 'OK' : 'MISSING') : 'MISSING'
      }
    })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function importHtml({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await client.post('/mods/preset-import', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success(t('mods.importResult', { created: res.createdCount, skipped: res.skippedCount }))
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openAdd() {
  Object.assign(addForm, { name: '', workshopId: '', loadType: 'BOTH' })
  addVisible.value = true
}

async function add() {
  saving.value = true
  try {
    await client.post('/mods', { ...addForm })
    addVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

function askUpdateAll() {
  updateScope.value = { type: 'all', ids: [] }
  updateMode.value = 'DIRECT'
  updateVisible.value = true
}

function askUpdateSelected() {
  if (!selected.value.length) {
    ElMessage.warning(t('mods.selectModsFirst'))
    return
  }
  updateScope.value = { type: 'selected', ids: selected.value.map(r => r.id) }
  updateMode.value = 'DIRECT'
  updateVisible.value = true
}

function askUpdateOne(row) {
  updateScope.value = { type: 'selected', ids: [row.id] }
  updateMode.value = 'DIRECT'
  updateVisible.value = true
}

function doUpdate() {
  updateVisible.value = false
  steamForm.password = ''
  steamForm.guardCode = ''
  steamVisible.value = true
}

async function startWithSteam() {
  if (!steamForm.username || !steamForm.password) {
    ElMessage.warning(t('mods.steamRequired'))
    return
  }
  starting.value = true
  try {
    const session = await client.post('/steamcmd/sessions', {
      username: steamForm.username,
      password: steamForm.password,
      guardCode: steamForm.guardCode || undefined,
      mode: updateMode.value,
      all: updateScope.value.type === 'all',
      modIds: updateScope.value.ids
    })
    steamVisible.value = false
    router.push({ name: 'steamcmd', params: { sessionId: session.id } })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    starting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(t('common.confirmDelete', { name: row.name }), t('common.warning'), { type: 'warning' })
  try {
    await client.delete(`/mods/${row.id}`)
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(() => {
  load()
  loadKeys()
})
</script>
