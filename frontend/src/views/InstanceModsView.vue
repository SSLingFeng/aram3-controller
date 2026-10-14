<template>
  <div class="page">
    <div class="page-header">
      <h2>实例模组加载配置</h2>
      <el-button type="primary" :loading="saving" :disabled="!instanceId" @click="save">保存当前实例配置</el-button>
    </div>

    <el-form inline>
      <el-form-item label="实例">
        <el-select v-model="instanceId" placeholder="选择实例" style="width:220px" @change="loadSelection">
          <el-option v-for="i in instances" :key="i.id" :label="`${i.name} (${i.code})`" :value="i.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="搜索">
        <el-input v-model="keyword" placeholder="名称 / Workshop ID" clearable style="width:200px" />
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" style="margin-bottom:12px"
      title="同一模组只能选择「双端加载」或「仅服务端」之一。点击表头复选框可全选/全不选该列。双端 → -mod；仅服务端 → -serverMod。" />

    <el-table :data="filtered" border stripe>
      <el-table-column prop="name" label="模组名称" min-width="220" />
      <el-table-column prop="workshopId" label="Workshop ID" width="130" />
      <el-table-column width="120" align="center">
        <template #header>
          <el-checkbox
            :model-value="allBoth"
            :indeterminate="someBoth && !allBoth"
            @change="toggleAllBoth"
          >
            双端加载
          </el-checkbox>
        </template>
        <template #default="{ row }">
          <el-checkbox v-model="row.both" @change="onBothChange(row)" />
        </template>
      </el-table-column>
      <el-table-column width="120" align="center">
        <template #header>
          <el-checkbox
            :model-value="allServer"
            :indeterminate="someServer && !allServer"
            @change="toggleAllServer"
          >
            仅服务端
          </el-checkbox>
        </template>
        <template #default="{ row }">
          <el-checkbox v-model="row.serverOnly" @change="onServerChange(row)" />
        </template>
      </el-table-column>
    </el-table>

    <div class="summary muted" style="margin-top:12px">
      已选双端 {{ countBoth }} 个 · 仅服务端 {{ countServer }} 个 · 未选 {{ countNone }} 个
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import client from '../api/http'

const instances = ref([])
const mods = ref([])
const rows = ref([])
const instanceId = ref(null)
const keyword = ref('')
const saving = ref(false)

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return rows.value
  return rows.value.filter(r =>
    (r.name || '').toLowerCase().includes(k) || (r.workshopId || '').includes(k)
  )
})

const countBoth = computed(() => rows.value.filter(r => r.both).length)
const countServer = computed(() => rows.value.filter(r => r.serverOnly).length)
const countNone = computed(() => rows.value.filter(r => !r.both && !r.serverOnly).length)

const allBoth = computed(() => rows.value.length > 0 && rows.value.every(r => r.both))
const someBoth = computed(() => rows.value.some(r => r.both))
const allServer = computed(() => rows.value.length > 0 && rows.value.every(r => r.serverOnly))
const someServer = computed(() => rows.value.some(r => r.serverOnly))

function onBothChange(row) {
  if (row.both) row.serverOnly = false
}

function onServerChange(row) {
  if (row.serverOnly) row.both = false
}

function toggleAllBoth(val) {
  const target = filtered.value
  target.forEach(r => {
    r.both = !!val
    if (val) r.serverOnly = false
  })
}

function toggleAllServer(val) {
  const target = filtered.value
  target.forEach(r => {
    r.serverOnly = !!val
    if (val) r.both = false
  })
}

async function loadMeta() {
  instances.value = await client.get('/instances')
  mods.value = await client.get('/mods')
  rows.value = mods.value.map(m => ({
    id: m.id,
    name: m.name,
    workshopId: m.workshopId,
    both: false,
    serverOnly: false
  }))
  if (instances.value.length && !instanceId.value) {
    instanceId.value = instances.value[0].id
    await loadSelection()
  }
}

async function loadSelection() {
  if (!instanceId.value) return
  try {
    const bound = await client.get(`/instances/${instanceId.value}/mods`)
    const map = new Map()
    bound.forEach(b => map.set(b.modId, b.loadType))
    rows.value.forEach(r => {
      const t = map.get(r.id)
      r.both = t === 'BOTH'
      r.serverOnly = t === 'SERVER_ONLY'
    })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function save() {
  if (!instanceId.value) return
  saving.value = true
  try {
    const items = rows.value
      .filter(r => r.both || r.serverOnly)
      .map((r, idx) => ({
        modId: r.id,
        loadType: r.both ? 'BOTH' : 'SERVER_ONLY',
        loadOrder: idx
      }))
    await client.put(`/instances/${instanceId.value}/mods/load-list`, { items })
    ElMessage.success('已保存实例模组加载列表')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

onMounted(loadMeta)
</script>
