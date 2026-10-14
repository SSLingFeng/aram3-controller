<template>
  <div class="page">
    <div class="page-header">
      <h2>模组库</h2>
    </div>

    <el-tabs v-model="tab">
      <!-- 模组列表 -->
      <el-tab-pane label="模组列表" name="mods">
        <div style="margin-bottom:12px;display:flex;gap:8px;flex-wrap:wrap">
          <el-upload :show-file-list="false" :http-request="importHtml" accept=".html,.htm">
            <el-button>导入 HTML 预设</el-button>
          </el-upload>
          <el-button @click="openAdd">手动添加</el-button>
          <el-button @click="localSearch">本地搜索</el-button>
          <el-button type="success" :loading="movingKeys" @click="moveKeys">移动 Key</el-button>
          <el-button type="warning" @click="askUpdateAll">更新全部模组</el-button>
          <el-button type="primary" :disabled="!selected.length" @click="askUpdateSelected">
            更新模组 ({{ selected.length }})
          </el-button>
        </div>

        <el-alert type="info" :closable="false" style="margin-bottom:12px"
          title="「移动 Key」会把各模组里的 .bikey 复制到游戏服务器 keys 目录；重名自动改名。可在「Key 管理」页查看来源并删除/上传。" />

        <el-form inline>
          <el-form-item label="搜索">
            <el-input v-model="keyword" placeholder="名称 / Workshop ID" clearable style="width:220px" />
          </el-form-item>
          <el-form-item>
            <el-button size="small" @click="selectAllFiltered">全选当前筛选</el-button>
            <el-button size="small" @click="selected = []">清空选择</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="filtered" border stripe @selection-change="onSelectionChange" ref="tableRef">
          <el-table-column type="selection" width="48" />
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" min-width="200" />
          <el-table-column prop="workshopId" label="Workshop ID" width="130" />
          <el-table-column prop="downloadStatus" label="任务状态" width="110">
            <template #default="{ row }"><span class="muted">{{ row.downloadStatus }}</span></template>
          </el-table-column>
          <el-table-column prop="localPath" label="本地路径" min-width="150">
            <template #default="{ row }"><span class="muted">{{ row.localPath || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" @click="askUpdateOne(row)">更新</el-button>
              <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" fixed="right">
            <template #default="{ row }">
              <el-tag v-if="row.scanStatus === 'OK'" type="success" size="small">正常</el-tag>
              <el-tag v-else-if="row.scanStatus === 'MISSING'" type="danger" size="small">不存在</el-tag>
              <span v-else class="muted">未扫描</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Key 管理 -->
      <el-tab-pane label="Key 管理" name="keys">
        <div style="margin-bottom:12px;display:flex;gap:8px">
          <el-button @click="loadKeys">刷新</el-button>
          <el-upload :show-file-list="false" :http-request="uploadKey" accept=".bikey">
            <el-button type="primary">手动上传 Key</el-button>
          </el-upload>
          <el-button type="success" :loading="movingKeys" @click="moveKeys">从模组移动 Key</el-button>
        </div>
        <el-alert type="info" :closable="false" style="margin-bottom:12px"
          :title="`Key 目录：${keysDir || '（游戏服务器目录/keys）'}`" />
        <el-table :data="keys" border stripe>
          <el-table-column prop="fileName" label="Key 文件" min-width="220" />
          <el-table-column prop="sourceModName" label="来源模组" min-width="180">
            <template #default="{ row }">
              <span v-if="row.sourceModId">#{{ row.sourceModId }} {{ row.sourceModName }}</span>
              <span v-else class="muted">{{ row.sourceModName || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sourceType" label="来源类型" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.sourceType === 'MOD' ? 'success' : 'info'">
                {{ row.sourceType === 'MOD' ? '模组' : row.sourceType === 'MANUAL' ? '手动' : '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="110">
            <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="filePath" label="路径" min-width="200">
            <template #default="{ row }"><span class="muted">{{ row.filePath }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="deleteKey(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 更新方式 -->
    <el-dialog v-model="updateVisible" :title="updateTitle" width="480px">
      <p style="margin:0 0 12px">请选择更新方式：</p>
      <el-radio-group v-model="updateMode" style="display:flex;flex-direction:column;gap:10px">
        <el-radio value="DIRECT">直接更新（保留本地文件，SteamCMD 增量更新）</el-radio>
        <el-radio value="REDOWNLOAD">删除源文件后重新下载（彻底重装）</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="updateVisible = false">取消更新</el-button>
        <el-button type="primary" @click="doUpdate">下一步</el-button>
      </template>
    </el-dialog>

    <!-- Steam 账号 -->
    <el-dialog v-model="steamVisible" title="Steam 账号验证" width="480px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        title="需真实 Steam 账号。令牌可选（手机 Steam Guard 6 位）。" />
      <el-form label-width="100px">
        <el-form-item label="Steam 账号" required>
          <el-input v-model="steamForm.username" />
        </el-form-item>
        <el-form-item label="Steam 密码" required>
          <el-input v-model="steamForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="Steam 令牌">
          <el-input v-model="steamForm.guardCode" placeholder="可选，4-12 位数字或字母" maxlength="12" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="steamVisible = false">取消</el-button>
        <el-button type="primary" :loading="starting" @click="startWithSteam">开始更新并打开控制台</el-button>
      </template>
    </el-dialog>

    <!-- 手动添加模组 -->
    <el-dialog v-model="addVisible" title="手动添加模组" width="460px">
      <el-form :model="addForm" label-width="110px">
        <el-form-item label="名称">
          <el-input v-model="addForm.name" />
        </el-form-item>
        <el-form-item label="Workshop ID" required>
          <el-input v-model="addForm.workshopId" />
        </el-form-item>
        <el-form-item label="默认加载">
          <el-radio-group v-model="addForm.loadType">
            <el-radio value="BOTH">双端</el-radio>
            <el-radio value="SERVER_ONLY">仅服务端</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="add">添加（不自动下载）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/http'

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
  if (updateScope.value.type === 'all') return '更新全部模组'
  return `更新所选模组（${updateScope.value.ids.length} 个）`
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
    ElMessage.success(`移动完成：复制 ${res.copiedCount}，重命名 ${res.renamedCount}，跳过 ${res.skippedCount}`)
    keys.value = res.items || await client.get('/keys')
    tab.value = 'keys'
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    movingKeys.value = false
  }
}

async function deleteKey(row) {
  await ElMessageBox.confirm(`删除 Key 文件「${row.fileName}」？`, '警告', { type: 'warning' })
  try {
    await client.delete('/keys', { params: { fileName: row.fileName } })
    ElMessage.success('已删除')
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
    ElMessage.success(`已上传 ${res.fileName}`)
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
    ElMessage.success(`导入完成：新增 ${res.createdCount}，跳过 ${res.skippedCount}`)
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
    ElMessage.warning('请先勾选模组')
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
    ElMessage.warning('Steam 账号和密码必填')
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
  await ElMessageBox.confirm(`删除模组「${row.name}」？`, '警告', { type: 'warning' })
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
