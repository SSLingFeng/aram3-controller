<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('instanceMods.title') }}</h2>
      <el-button type="primary" :loading="saving" :disabled="!instanceId" @click="save">{{ $t('instanceMods.saveInstanceConfig') }}</el-button>
    </div>

    <el-form inline>
      <el-form-item :label="$t('instanceMods.instance')">
        <el-select v-model="instanceId" :placeholder="$t('instanceMods.selectInstance')" style="width:220px" @change="loadSelection">
          <el-option v-for="i in instances" :key="i.id" :label="`${i.name} (${i.code})`" :value="i.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.search')">
        <el-input v-model="keyword" :placeholder="$t('instanceMods.searchPlaceholder')" clearable style="width:200px" />
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" style="margin-bottom:12px"
      :title="$t('instanceMods.loadTip')" />

    <el-table :data="filtered" border stripe>
      <el-table-column prop="name" :label="$t('instanceMods.modName')" min-width="220" />
      <el-table-column prop="workshopId" :label="$t('instanceMods.workshopId')" width="130" />
      <el-table-column width="120" align="center">
        <template #header>
          <el-checkbox
            :model-value="allBoth"
            :indeterminate="someBoth && !allBoth"
            @change="toggleAllBoth"
          >
            {{ $t('instanceMods.bothLoad') }}
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
            {{ $t('instanceMods.serverOnlyLoad') }}
          </el-checkbox>
        </template>
        <template #default="{ row }">
          <el-checkbox v-model="row.serverOnly" @change="onServerChange(row)" />
        </template>
      </el-table-column>
    </el-table>

    <div class="summary muted" style="margin-top:12px">
      {{ $t('instanceMods.summary', { both: countBoth, server: countServer, none: countNone }) }}
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/http'

const { t } = useI18n({ useScope: 'global' })

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
      const loadType = map.get(r.id)
      r.both = loadType === 'BOTH'
      r.serverOnly = loadType === 'SERVER_ONLY'
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
    ElMessage.success(t('instanceMods.saveSuccess'))
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

onMounted(loadMeta)
</script>
