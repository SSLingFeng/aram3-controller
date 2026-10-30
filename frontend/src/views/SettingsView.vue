<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('settings.title') }}</h2>
      <el-button type="primary" :loading="saving" @click="save">{{ $t('settings.saveSettings') }}</el-button>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom:14px"
      :title="$t('settings.alertTop')" />

    <el-card>
      <template #header>{{ $t('settings.cards.gameServer') }}</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>{{ $t('settings.gameServerDir') }}</template>
          <el-input v-model="form.gameServerDir" :placeholder="$t('settings.placeholders.gameServerDir')" />
          <div class="muted">{{ $t('settings.hints.gameServerDir') }}</div>
        </el-form-item>
        <el-form-item>
          <template #label>{{ $t('settings.steamcmdPath') }}</template>
          <el-input v-model="form.steamcmdPath" :placeholder="$t('settings.placeholders.steamcmdPath')" />
          <div class="muted">{{ $t('settings.hints.steamcmdPath') }}</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>{{ $t('settings.cards.mods') }}</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>{{ $t('settings.modsDir') }}</template>
          <el-input v-model="form.modsDir" :placeholder="$t('settings.placeholders.modsDir')" />
          <div class="muted">{{ $t('settings.hints.modsDir') }}</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>{{ $t('settings.cards.instances') }}</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>{{ $t('settings.instancesDir') }}</template>
          <el-input v-model="form.instancesDir" :placeholder="$t('settings.placeholders.instancesDir')" />
          <div class="muted">{{ $t('settings.hints.instancesDir') }}</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>{{ $t('settings.cards.app') }}</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>{{ $t('settings.logsDir') }}</template>
          <el-input v-model="form.logsDir" :placeholder="$t('settings.placeholders.logsDir')" />
          <div class="muted">{{ $t('settings.hints.logsDir') }}</div>
        </el-form-item>
        <el-form-item>
          <template #label>{{ $t('settings.dataDir') }}</template>
          <el-input v-model="form.dataDir" :placeholder="$t('settings.placeholders.dataDir')" />
          <div class="muted">{{ $t('settings.hints.dataDir') }}</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>{{ $t('settings.cards.loginSecurity') }}</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>{{ $t('settings.maxLoginFailures') }}</template>
          <el-input-number v-model="form.maxLoginFailures" :min="1" :max="20" />
          <div class="muted">{{ $t('settings.hints.maxLoginFailures') }}</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>{{ $t('settings.cards.readonly') }}</template>
      <el-form label-width="200px">
        <el-form-item>
          <template #label>{{ $t('settings.mpmissionsDir') }}</template>
          <el-input :model-value="form.mpmissionsDir" disabled />
          <div class="muted">{{ $t('settings.hints.mpmissionsDir') }}</div>
        </el-form-item>
        <el-form-item>
          <template #label>{{ $t('settings.steamAppId') }}</template>
          <el-input :model-value="String(form.steamAppId)" disabled />
        </el-form-item>
        <el-form-item>
          <template #label>{{ $t('settings.workshopAppId') }}</template>
          <el-input :model-value="String(form.workshopAppId)" disabled />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import client from '../api/http'

const { t } = useI18n({ useScope: 'global' })
const saving = ref(false)
const form = reactive({
  gameServerDir: '',
  steamcmdPath: '',
  modsDir: '',
  instancesDir: '',
  logsDir: '',
  dataDir: '',
  maxLoginFailures: 3,
  mpmissionsDir: '',
  steamAppId: 233780,
  workshopAppId: 107410
})

async function load() {
  try {
    const data = await client.get('/system/settings')
    Object.assign(form, data)
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function save() {
  saving.value = true
  try {
    const data = await client.put('/system/settings', {
      gameServerDir: form.gameServerDir,
      steamcmdPath: form.steamcmdPath,
      modsDir: form.modsDir,
      instancesDir: form.instancesDir,
      logsDir: form.logsDir,
      dataDir: form.dataDir,
      maxLoginFailures: String(form.maxLoginFailures)
    })
    Object.assign(form, data)
    ElMessage.success(t('settings.msg.saved'))
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
