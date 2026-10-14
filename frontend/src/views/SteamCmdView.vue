<template>
  <div class="page">
    <div class="page-header">
      <h2>SteamCMD 控制台</h2>
      <div>
        <el-button type="danger" :disabled="!canCommand" @click="stopSession">停止更新</el-button>
        <el-button @click="refresh">刷新</el-button>
      </div>
    </div>

    <el-alert
      v-if="status === 'STARTING'"
      type="warning"
      :closable="false"
      style="margin-bottom:12px"
      title="SteamCMD 启动中… 进程创建后会持续输出日志，请稍候（SteamCMD 自身启动可能较慢）。"
    />
    <el-alert
      v-else-if="status === 'RUNNING'"
      type="success"
      :closable="false"
      style="margin-bottom:12px"
      title="SteamCMD 运行中。可发送指令；点击「停止更新」可结束进程。"
    />
    <el-alert
      v-else
      type="info"
      :closable="false"
      style="margin-bottom:12px"
      title="SteamCMD 已结束，指令输入已锁定。下方可查看完整日志与结束后的本地检测结果。"
    />

    <el-descriptions :column="3" border size="small" style="margin-bottom:12px">
      <el-descriptions-item label="会话">{{ sessionId }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTag">{{ status }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="PID">{{ pid || '—' }}</el-descriptions-item>
      <el-descriptions-item label="模式">{{ mode }}</el-descriptions-item>
      <el-descriptions-item label="账号">{{ username || '—' }}</el-descriptions-item>
      <el-descriptions-item label="日志行">{{ nextSeq }}</el-descriptions-item>
    </el-descriptions>

    <div ref="logBox" class="log-box">
      <div v-for="(line, i) in logs" :key="i" class="log-line">{{ line }}</div>
    </div>

    <div class="cmd-row">
      <el-input
        v-model="command"
        :disabled="!canCommand"
        :placeholder="canCommand ? '输入 SteamCMD 指令，例如 status / quit' : 'SteamCMD 未运行，输入已锁定'"
        @keyup.enter="sendCommand"
      />
      <el-button type="primary" :disabled="!canCommand || !command.trim()" @click="sendCommand">发送指令</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/http'

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
    ElMessage.warning('SteamCMD 未运行，输入已锁定')
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
  await ElMessageBox.confirm('确认停止 SteamCMD？', '停止更新', { type: 'warning' })
  try {
    await client.post(`/steamcmd/sessions/${sessionId.value}/stop`)
    ElMessage.success('已请求停止')
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
