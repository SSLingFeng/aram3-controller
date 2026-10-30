<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('instanceConfig.title') }}</h2>
      <div>
        <el-button :disabled="!instanceId" @click="reset">{{ $t('instanceConfig.resetDefault') }}</el-button>
        <el-button type="primary" :loading="saving" :disabled="!instanceId" @click="save">{{ $t('instanceConfig.saveConfig') }}</el-button>
      </div>
    </div>

    <el-form inline>
      <el-form-item :label="$t('instanceConfig.instance')">
        <el-select v-model="instanceId" :placeholder="$t('instanceConfig.selectInstance')" style="width:240px" @change="load">
          <el-option v-for="i in instances" :key="i.id" :label="`${i.name} (${i.code})`" :value="i.id" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" style="margin-bottom:12px"
      :title="$t('instanceConfig.mountTip')" />

    <el-card v-if="paths.serverCfgPath" style="margin-bottom:12px">
      <div class="muted">{{ $t('instanceConfig.serverCfgPathLabel') }}{{ paths.serverCfgPath }}</div>
      <div class="muted">{{ $t('instanceConfig.basicCfgPathLabel') }}{{ paths.basicCfgPath }}</div>
    </el-card>

    <el-tabs>
      <el-tab-pane label="server.cfg">
        <el-input
          v-model="serverCfg"
          type="textarea"
          :rows="18"
          :disabled="!instanceId"
          :placeholder="$t('instanceConfig.serverCfgPlaceholder')"
          style="font-family: Consolas, Monaco, monospace"
        />
      </el-tab-pane>
      <el-tab-pane label="basic.cfg">
        <el-input
          v-model="basicCfg"
          type="textarea"
          :rows="18"
          :disabled="!instanceId"
          :placeholder="$t('instanceConfig.basicCfgPlaceholder')"
          style="font-family: Consolas, Monaco, monospace"
        />
      </el-tab-pane>
    </el-tabs>

    <el-card style="margin-top:12px">
      <template #header>{{ $t('instanceConfig.startCommandPreview') }}</template>
      <el-input type="textarea" :rows="4" :model-value="preview" readonly />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/http'

const { t } = useI18n({ useScope: 'global' })

const instances = ref([])
const instanceId = ref(null)
const serverCfg = ref('')
const basicCfg = ref('')
const preview = ref('')
const paths = ref({})
const saving = ref(false)

async function loadMeta() {
  instances.value = await client.get('/instances')
  if (instances.value.length && !instanceId.value) {
    instanceId.value = instances.value[0].id
    await load()
  }
}

async function load() {
  if (!instanceId.value) return
  try {
    const data = await client.get(`/instances/${instanceId.value}/config-files`)
    serverCfg.value = data.serverCfg || ''
    basicCfg.value = data.basicCfg || ''
    paths.value = {
      serverCfgPath: data.serverCfgPath,
      basicCfgPath: data.basicCfgPath
    }
    const p = await client.get(`/instances/${instanceId.value}/start-command-preview`)
    preview.value = p.command || ''
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function save() {
  saving.value = true
  try {
    await client.put(`/instances/${instanceId.value}/config-files`, {
      serverCfg: serverCfg.value,
      basicCfg: basicCfg.value
    })
    ElMessage.success(t('instanceConfig.saveSuccess'))
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function reset() {
  await ElMessageBox.confirm(t('instanceConfig.resetConfirm'), t('common.warning'), { type: 'warning' })
  try {
    await client.post(`/instances/${instanceId.value}/config-files/reset`)
    ElMessage.success(t('instanceConfig.resetSuccess'))
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(loadMeta)
</script>
