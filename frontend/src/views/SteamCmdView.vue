<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('steamcmd.title') }}</h2>
      <div>
        <el-button type="danger" :disabled="!canCommand" @click="stopSession">{{ $t('steamcmd.stopUpdate') }}</el-button>
        <el-button @click="refresh">{{ $t('common.refresh') }}</el-button>
      </div>
    </div>

    <el-alert
      v-if="status === 'STARTING'"
      type="warning"
      :closable="false"
      style="margin-bottom:12px"
      :title="$t('steamcmd.alertStarting')"
    />
    <el-alert
      v-else-if="status === 'RUNNING'"
      type="success"
      :closable="false"
      style="margin-bottom:12px"
      :title="$t('steamcmd.alertRunning')"
    />
    <el-alert
      v-else
      type="info"
      :closable="false"
      style="margin-bottom:12px"
      :title="$t('steamcmd.alertStopped')"
    />

    <el-descriptions :column="3" border size="small" style="margin-bottom:12px">
      <el-descriptions-item :label="$t('steamcmd.session')">{{ sessionId }}</el-descriptions-item>
      <el-descriptions-item :label="$t('common.status')">
        <el-tag :type="statusTag">{{ $t('steamcmd.status.' + status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item :label="$t('steamcmd.pid')">{{ pid || '—' }}</el-descriptions-item>
      <el-descriptions-item :label="$t('steamcmd.mode')">{{ $t('steamcmd.modes.' + mode) }}</el-descriptions-item>
      <el-descriptions-item :label="$t('steamcmd.account')">{{ username || '—' }}</el-descriptions-item>
      <el-descriptions-item :label="$t('steamcmd.logLines')">{{ nextSeq }}</el-descriptions-item>
    </el-descriptions>

    <div ref="logBox" class="log-box">
      <div v-for="(line, i) in logs" :key="i" class="log-line">{{ line }}</div>
    </div>

    <div class="cmd-row">
      <el-input
        v-model="command"
        :disabled="!canCommand"
        :placeholder="canCommand ? $t('steamcmd.inputPlaceholder') : $t('steamcmd.inputLocked')"
        @keyup.enter="sendCommand"
      />
      <el-button type="primary" :disabled="!canCommand || !command.trim()" @click="sendCommand">{{ $t('steamcmd.sendCommand') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/http'

const { t } = useI18n({ useScope: 'global' })
const route = useRoute()
const sessionId = computed(() => route.params.sessionId || route.query.sessionId || '')

const logs = ref([])
const nextSeq = ref(0)
const status = ref('STARTING')
const active = ref(true)
const canCommand = ref(false)
const pid = ref(null)
const mode = ref('DIRECT')
const username = ref('')
const command = ref('')
const logBox = ref(null)
let timer = null
let missCount = 0

const statusTag = computed(() => {
  if (status.value === 'RUNNING') return 'success'
  if (status.value === 'FAILED') return 'danger'
  if (status.value === 'STARTING') return 'warning'
  return 'info'
})

function startPoll() {
  if (timer) return
  timer = setInterval(poll, 1200)
}

function stopPoll() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function poll() {
  if (!sessionId.value) return
  try {
    const data = await client.get(`/steamcmd/sessions/${sessionId.value}`, {
      params: { sinceSeq: nextSeq.value }
    })
    missCount = 0
    if (data.logs && data.logs.length) {
      logs.value = logs.value.concat(data.logs)
    }
    nextSeq.value = data.nextSeq ?? nextSeq.value
    status.value = data.status || 'RUNNING'
    active.value = data.active !== false
    canCommand.value = !!data.canCommand
    pid.value = data.pid
    mode.value = data.mode || 'DIRECT'
    username.value = data.username || ''
    await nextTick()
    if (logBox.value) {
      logBox.value.scrollTop = logBox.value.scrollHeight
    }
    // only stop when truly finished
    if (data.status === 'STOPPED' || data.status === 'FAILED') {
      canCommand.value = false
      if (data.active === false) {
        stopPoll()
      }
    }
  } catch (e) {
    missCount++
    if (!logs.value.length) {
      logs.value.push(String(e.message || e))
    }
    // session may not exist after backend restart
    if (missCount >= 3) {
      status.value = 'STOPPED'
      canCommand.value = false
      active.value = false
      stopPoll()
    }
  }
}

async function sendCommand() {
  if (!canCommand.value) {
    ElMessage.warning(t('steamcmd.inputLocked'))
    return
  }
  const cmd = command.value.trim()
  if (!cmd) return
  try {
    await client.post(`/steamcmd/sessions/${sessionId.value}/command`, { command: cmd })
    command.value = ''
    await poll()
  } catch (e) {
    ElMessage.error(e.message)
    await poll()
  }
}

async function stopSession() {
  await ElMessageBox.confirm(t('steamcmd.confirmStop'), t('steamcmd.stopUpdate'), { type: 'warning' })
  try {
    await client.post(`/steamcmd/sessions/${sessionId.value}/stop`)
    ElMessage.success(t('steamcmd.stopRequested'))
    await poll()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function refresh() {
  logs.value = []
  nextSeq.value = 0
  await poll()
  startPoll()
}

onMounted(() => {
  poll()
  startPoll()
})

onBeforeUnmount(() => {
  stopPoll()
})
</script>

<style scoped>
.log-box {
  height: 420px;
  overflow: auto;
  background: #0f172a;
  color: #d1e3ff;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.55;
  padding: 10px 12px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-all;
}
.log-line { margin: 0; }
.cmd-row {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
</style>
