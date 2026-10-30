<template>
  <div class="page">
    <div class="page-header">
      <h2>实例控制台（RCON）</h2>
      <el-button :disabled="!instanceId" @click="refreshAll" :loading="loading">刷新</el-button>
    </div>

    <el-form inline>
      <el-form-item label="实例">
        <el-select v-model="instanceId" placeholder="选择运行中的实例" style="width:260px" @change="onInstanceChange">
          <el-option v-for="i in instances" :key="i.id" :label="`${i.name} (${i.code})`" :value="i.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="instance">
        <el-tag size="small" type="info">RCON {{ instance.rconHost }}:{{ instance.rconPort }}</el-tag>
        <el-tag size="small">在线 {{ players.length }} 人</el-tag>
      </el-form-item>
      <el-form-item>
        <el-switch v-model="autoRefresh" active-text="自动刷新(10s)" />
      </el-form-item>
    </el-form>

    <el-alert v-if="!instanceId" type="info" :closable="false" style="margin-bottom:12px"
              title="请先选择实例。需要实例已配置 RCON 地址/端口/密码，且服务器正在运行。" />

    <template v-if="instanceId">
      <el-tabs v-model="tab">
        <!-- 玩家 -->
        <el-tab-pane label="玩家" name="players">
          <div class="toolbar">
            <el-button @click="loadPlayers" :loading="loading">刷新玩家</el-button>
            <span class="muted">编号来自 RCON players 输出，操作前请确认目标</span>
          </div>
          <el-table :data="players" border stripe v-loading="loading">
            <el-table-column prop="num" label="#" width="60" />
            <el-table-column label="玩家" min-width="180">
              <template #default="{ row }">
                {{ row.name || '(未命名)' }}
                <el-tag v-if="row.lobby" size="small" type="warning">Lobby</el-tag>
                <el-tag v-if="row.verified" size="small" type="success">OK</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="GUID" min-width="220">
              <template #default="{ row }">
                <span v-if="row.guid" class="mono">{{ row.guid }}</span>
                <span v-else class="muted">-</span>
                <el-button v-if="row.guid" link type="primary" size="small" @click="copy(row.guid)">复制</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" width="140" />
            <el-table-column prop="ping" label="Ping" width="80" />
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openSayTo(row)">私聊</el-button>
                <el-button link type="warning" size="small" @click="openKick(row)">踢出</el-button>
                <el-button link type="danger" size="small" @click="openBan(row)">封禁</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 公告与私聊 -->
        <el-tab-pane label="公告与私聊" name="say">
          <el-card style="margin-bottom:12px">
            <template #header>全服公告</template>
            <el-input v-model="sayForm.message" type="textarea" :rows="3" maxlength="500" show-word-limit
                      placeholder="公告内容（say -1 全服广播）" />
            <div class="toolbar" style="margin-top:8px">
              <el-button type="primary" @click="sendSay" :loading="busy">发送公告</el-button>
            </div>
          </el-card>
          <el-card>
            <template #header>私聊玩家</template>
            <el-form inline>
              <el-form-item label="玩家编号">
                <el-input-number v-model="sayToForm.playerNum" :min="0" :max="999" />
              </el-form-item>
              <el-form-item label="消息">
                <el-input v-model="sayToForm.message" maxlength="500" placeholder="仅该玩家可见" style="width:320px" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="sendSayTo" :loading="busy">发送私聊</el-button>
              </el-form-item>
            </el-form>
            <div class="muted">也可在玩家列表中直接点「私聊」</div>
          </el-card>
        </el-tab-pane>

        <!-- 封禁管理 -->
        <el-tab-pane label="封禁管理" name="bans">
          <div class="toolbar">
            <el-button @click="loadBansFile" :loading="busy">刷新封禁列表</el-button>
            <el-button @click="loadBans" :loading="busy">loadBans 重载</el-button>
            <span class="muted" v-if="bansPath">{{ bansPath }}</span>
            <span class="muted" v-else>未找到 bans.txt（实例或服务端目录下的 battleye/）</span>
          </div>
          <el-table :data="bans" border stripe v-loading="busy">
            <el-table-column prop="lineNo" label="行号" width="80" />
            <el-table-column prop="kind" label="类型" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.kind === 'GUID' ? 'danger' : 'warning'">{{ row.kind }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="封禁目标(GUID/IP)" min-width="220">
              <template #default="{ row }"><span class="mono">{{ row.target || row.raw }}</span></template>
            </el-table-column>
            <el-table-column prop="reason" label="理由" min-width="180" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-popconfirm title="确认解封该条目？" @confirm="unban(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">解封</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-card style="margin-top:12px">
            <template #header>按 GUID/IP 直接封禁（banPlayer）</template>
            <el-form inline>
              <el-form-item label="GUID / IP">
                <el-input v-model="banPlayerForm.target" placeholder="32位GUID 或 IP" style="width:280px" />
              </el-form-item>
              <el-form-item label="时长(分钟，0=永久)">
                <el-input-number v-model="banPlayerForm.minutes" :min="0" :max="525600" />
              </el-form-item>
              <el-form-item label="理由">
                <el-input v-model="banPlayerForm.reason" style="width:200px" />
              </el-form-item>
              <el-form-item>
                <el-button type="danger" @click="sendBanPlayer" :loading="busy">封禁</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-tab-pane>

        <!-- 服务器控制 -->
        <el-tab-pane label="服务器控制" name="server">
          <el-card style="margin-bottom:12px">
            <template #header>连接与人数</template>
            <div class="toolbar">
              <el-button @click="doCmd(() => client.post(base + '/lock'), '已锁服')" :loading="busy">锁服 lock</el-button>
              <el-button @click="doCmd(() => client.post(base + '/unlock'), '已解锁')" :loading="busy">解锁 unlock</el-button>
              <span class="muted">最大人数：</span>
              <el-input-number v-model="maxPlayersValue" :min="1" :max="200" />
              <el-button @click="setMaxPlayers" :loading="busy">设置 maxPlayers</el-button>
            </div>
          </el-card>
          <el-card style="margin-bottom:12px">
            <template #header>重启 / 重新分配</template>
            <div class="toolbar">
              <el-button @click="doCmd(() => client.post(base + '/reassign'), '已 reassign')" :loading="busy">reassign</el-button>
              <el-button @click="confirmCmd('立即重启服务器？', () => client.post(base + '/restart-server'), 'restartServer 已发送')" :loading="busy">restartServer</el-button>
              <el-button @click="confirmCmd('重启当前任务？', () => client.post(base + '/restart'), 'restart 已发送')" :loading="busy">restart</el-button>
              <el-button v-if="isSuper" type="danger" plain
                         @click="confirmCmd('确认关停服务器？此操作会断开所有玩家！', () => client.post(base + '/shutdown'), 'shutdown 已发送')"
                         :loading="busy">shutdown（超管）</el-button>
            </div>
            <div class="toolbar" style="margin-top:8px">
              <span class="muted">定时重启 restarttime：</span>
              <el-input-number v-model="restartTime.minutes" :min="0" :max="720" /> 分
              <el-input-number v-model="restartTime.seconds" :min="0" :max="59" /> 秒
              <el-button @click="setRestartTime" :loading="busy">设置</el-button>
            </div>
          </el-card>
          <el-card style="margin-bottom:12px">
            <template #header>热重载</template>
            <div class="toolbar">
              <el-button @click="doCmd(() => client.post(base + '/load-bans'), 'loadBans 完成')" :loading="busy">loadBans</el-button>
              <el-button @click="doCmd(() => client.post(base + '/load-scripts'), 'loadScripts 完成')" :loading="busy">loadScripts</el-button>
              <el-button @click="doCmd(() => client.post(base + '/load-events'), 'loadEvents 完成')" :loading="busy">loadEvents</el-button>
              <el-button @click="doCmd(() => client.post(base + '/load-rpt'), 'loadRPT 完成')" :loading="busy">loadRPT</el-button>
            </div>
          </el-card>
          <el-card>
            <template #header>RPT 日志尾部</template>
            <div class="toolbar">
              <el-input-number v-model="rptLines" :min="20" :max="1000" :step="20" />
              <el-button @click="loadRpt" :loading="busy">读取</el-button>
              <span class="muted" v-if="rptPath">{{ rptPath }}（共 {{ rptTotal }} 行）</span>
            </div>
            <el-input v-model="rptContent" type="textarea" :rows="12" readonly class="mono" />
          </el-card>
        </el-tab-pane>

        <!-- 任务与事件 -->
        <el-tab-pane label="任务与事件" name="missions">
          <el-card style="margin-bottom:12px">
            <template #header>任务列表（missions）</template>
            <div class="toolbar">
              <el-button @click="loadMissions" :loading="busy">获取任务列表</el-button>
              <span class="muted">点击任务名可填入下方切换框</span>
            </div>
            <el-input v-model="missionsText" type="textarea" :rows="8" readonly class="mono" />
            <div class="toolbar" style="margin-top:8px; flex-wrap:wrap">
              <el-tag v-for="m in missionNames" :key="m" class="mission-tag" style="cursor:pointer"
                      @click="missionForm.name = m">{{ m }}</el-tag>
            </div>
          </el-card>
          <el-card style="margin-bottom:12px">
            <template #header>切换任务（mission）</template>
            <div class="toolbar">
              <el-input v-model="missionForm.name" placeholder="任务名，如 MP_Coop.Altis" style="width:320px" />
              <el-button type="primary" @click="confirmCmd('确认切换任务？', setMission, 'mission 已发送')" :loading="busy">切换任务</el-button>
            </div>
          </el-card>
          <el-card>
            <template #header>事件处理器（events）</template>
            <div class="toolbar">
              <el-button @click="loadEvents" :loading="busy">获取 events</el-button>
            </div>
            <el-input v-model="eventsText" type="textarea" :rows="6" readonly class="mono" />
          </el-card>
        </el-tab-pane>

        <!-- RCON 控制台 -->
        <el-tab-pane label="RCON 控制台" name="console">
          <el-card>
            <template #header>原始命令</template>
            <div v-if="!isSuper" class="muted" style="margin-bottom:8px">原始命令执行仅限超级管理员。</div>
            <div class="toolbar">
              <el-input v-model="execForm.command" placeholder="如：players / say -1 hello / kick 3 捣乱"
                        style="width:420px" @keyup.enter="sendExec" :disabled="!isSuper" />
              <el-button type="primary" @click="sendExec" :loading="busy" :disabled="!isSuper">执行</el-button>
              <el-button @click="consoleLog = ''">清空输出</el-button>
            </div>
            <el-input v-model="consoleLog" type="textarea" :rows="14" readonly class="mono console-out" />
          </el-card>
        </el-tab-pane>
      </el-tabs>

      <!-- 最近命令结果 -->
      <el-card v-if="lastResult" style="margin-top:12px">
        <template #header>最近命令结果</template>
        <el-input :model-value="lastResult" type="textarea" :rows="4" readonly class="mono" />
      </el-card>
    </template>

    <!-- 踢出 -->
    <el-dialog v-model="kickDialog" title="踢出玩家" width="420px">
      <p>目标：#{{ targetPlayer?.num }} {{ targetPlayer?.name }}</p>
      <el-input v-model="kickForm.reason" placeholder="理由（可空）" />
      <template #footer>
        <el-button @click="kickDialog = false">取消</el-button>
        <el-button type="warning" @click="sendKick" :loading="busy">踢出</el-button>
      </template>
    </el-dialog>

    <!-- 封禁 -->
    <el-dialog v-model="banDialog" title="封禁玩家" width="420px">
      <p>目标：#{{ targetPlayer?.num }} {{ targetPlayer?.name }}</p>
      <el-form label-width="140px">
        <el-form-item label="时长(分钟，0=永久)">
          <el-input-number v-model="banForm.minutes" :min="0" :max="525600" />
        </el-form-item>
        <el-form-item label="理由">
          <el-input v-model="banForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banDialog = false">取消</el-button>
        <el-button type="danger" @click="sendBan" :loading="busy">封禁</el-button>
      </template>
    </el-dialog>

    <!-- 私聊 -->
    <el-dialog v-model="sayToDialog" title="私聊玩家" width="420px">
      <p>目标：#{{ targetPlayer?.num }} {{ targetPlayer?.name }}</p>
      <el-input v-model="sayToForm.message" type="textarea" :rows="3" maxlength="500" placeholder="消息内容" />
      <template #footer>
        <el-button @click="sayToDialog = false">取消</el-button>
        <el-button type="primary" @click="sendSayTo" :loading="busy">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/http'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()

const instances = ref([])
const instanceId = ref(null)
const instance = ref(null)
const base = computed(() => (instanceId.value ? `/instances/${instanceId.value}/rcon` : ''))

const tab = ref('players')
const loading = ref(false)
const busy = ref(false)
const autoRefresh = ref(true)
const players = ref([])
const lastResult = ref('')

const isSuper = computed(() => !!auth.user?.superAdmin)

// 表单
const sayForm = reactive({ message: '' })
const sayToForm = reactive({ playerNum: 0, message: '' })
const kickForm = reactive({ reason: '' })
const banForm = reactive({ minutes: 0, reason: '' })
const banPlayerForm = reactive({ target: '', minutes: 0, reason: '' })
const maxPlayersValue = ref(64)
const restartTime = reactive({ minutes: 30, seconds: 0 })
const missionForm = reactive({ name: '' })
const execForm = reactive({ command: '' })

// 弹窗
const kickDialog = ref(false)
const banDialog = ref(false)
const sayToDialog = ref(false)
const targetPlayer = ref(null)

// 封禁 / 任务 / RPT / 控制台
const bans = ref([])
const bansPath = ref('')
const missionsText = ref('')
const eventsText = ref('')
const rptLines = ref(200)
const rptContent = ref('')
const rptPath = ref('')
const rptTotal = ref(0)
const consoleLog = ref('')

const missionNames = computed(() =>
  missionsText.value.split(/\r?\n/).map(s => s.trim()).filter(s => s && !s.toLowerCase().startsWith('mission')))

let timer = null

function copy(text) {
  navigator.clipboard?.writeText(text).then(() => ElMessage.success('已复制'))
}

function showResult(text) {
  lastResult.value = text == null || text === '' ? '(无输出)' : String(text)
}

async function doCmd(fn, okMsg) {
  busy.value = true
  try {
    const data = await fn()
    showResult(data)
    if (okMsg) ElMessage.success(okMsg)
  } catch (e) {
    ElMessage.error(e.message || '命令失败')
  } finally {
    busy.value = false
  }
}

async function confirmCmd(title, fn, okMsg) {
  try {
    await ElMessageBox.confirm(title, '请确认', { type: 'warning' })
  } catch (e) {
    return
  }
  await doCmd(fn, okMsg)
}

// ---------------- 实例 ----------------
async function loadInstances() {
  try {
    instances.value = await client.get('/instances')
  } catch (e) {
    ElMessage.error('加载实例列表失败：' + (e.message || ''))
  }
}

async function loadInstance() {
  if (!instanceId.value) {
    instance.value = null
    return
  }
  try {
    instance.value = await client.get(`/instances/${instanceId.value}`)
  } catch (e) {
    instance.value = null
    ElMessage.error('加载实例信息失败：' + (e.message || ''))
  }
}

function onInstanceChange() {
  players.value = []
  bans.value = []
  lastResult.value = ''
  refreshAll()
}

// ---------------- 玩家 ----------------
async function loadPlayers() {
  if (!instanceId.value) return
  loading.value = true
  try {
    players.value = await client.get(base.value + '/players')
  } catch (e) {
    ElMessage.error('获取玩家列表失败：' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

function openKick(row) {
  targetPlayer.value = row
  kickForm.reason = ''
  kickDialog.value = true
}

async function sendKick() {
  const row = targetPlayer.value
  kickDialog.value = false
  await doCmd(() => client.post(base.value + '/kick', { playerNum: row.num, reason: kickForm.reason }),
    `已踢出 #${row.num} ${row.name || ''}`)
  loadPlayers()
}

function openBan(row) {
  targetPlayer.value = row
  banForm.minutes = 0
  banForm.reason = ''
  banDialog.value = true
}

async function sendBan() {
  const row = targetPlayer.value
  banDialog.value = false
  await doCmd(() => client.post(base.value + '/ban', { playerNum: row.num, minutes: banForm.minutes, reason: banForm.reason }),
    `已封禁 #${row.num} ${row.name || ''}`)
  loadPlayers()
}

function openSayTo(row) {
  targetPlayer.value = row
  sayToForm.playerNum = row.num
  sayToForm.message = ''
  sayToDialog.value = true
}

// ---------------- 公告 ----------------
async function sendSay() {
  if (!sayForm.message.trim()) {
    ElMessage.warning('请输入公告内容')
    return
  }
  await doCmd(() => client.post(base.value + '/say', { message: sayForm.message }), '公告已发送')
  sayForm.message = ''
}

async function sendSayTo() {
  if (!sayToForm.message.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }
  sayToDialog.value = false
  await doCmd(() => client.post(base.value + '/say-to', { playerNum: sayToForm.playerNum, message: sayToForm.message }),
    '私聊已发送')
  sayToForm.message = ''
}

// ---------------- 封禁 ----------------
async function loadBansFile() {
  if (!instanceId.value) return
  busy.value = true
  try {
    const data = await client.get(base.value + '/bans-file')
    bans.value = data.entries || []
    bansPath.value = data.path || ''
  } catch (e) {
    ElMessage.error('获取封禁列表失败：' + (e.message || ''))
  } finally {
    busy.value = false
  }
}

async function unban(row) {
  await doCmd(async () => {
    const data = await client.post(base.value + '/bans-file/unban', { lineNo: row.lineNo })
    bans.value = data.entries || []
    return data.reload
  }, '已解封并重载')
}

async function sendBanPlayer() {
  if (!banPlayerForm.target.trim()) {
    ElMessage.warning('请输入 GUID 或 IP')
    return
  }
  await doCmd(() => client.post(base.value + '/ban-player', {
    target: banPlayerForm.target.trim(),
    minutes: banPlayerForm.minutes,
    reason: banPlayerForm.reason
  }), '封禁命令已发送')
  loadBansFile()
}

async function loadBans() {
  await doCmd(() => client.post(base.value + '/load-bans'), 'loadBans 完成')
}

// ---------------- 服务器控制 ----------------
async function setMaxPlayers() {
  await doCmd(() => client.post(base.value + '/max-players', { value: maxPlayersValue.value }), 'maxPlayers 已设置')
}

async function setRestartTime() {
  await doCmd(() => client.post(base.value + '/restart-time', { minutes: restartTime.minutes, seconds: restartTime.seconds }),
    'restarttime 已设置')
}

async function loadRpt() {
  busy.value = true
  try {
    const data = await client.get(base.value + '/rpt-tail', { params: { lines: rptLines.value } })
    rptContent.value = data.content || ''
    rptPath.value = data.path || ''
    rptTotal.value = data.totalLines || 0
  } catch (e) {
    ElMessage.error(e.message || '读取 RPT 失败')
  } finally {
    busy.value = false
  }
}

// ---------------- 任务与事件 ----------------
async function loadMissions() {
  await doCmd(async () => {
    const data = await client.get(base.value + '/missions')
    missionsText.value = data || ''
    return data
  }, '任务列表已更新')
}

async function setMission() {
  if (!missionForm.name.trim()) {
    ElMessage.warning('请输入任务名')
    return
  }
  await doCmd(() => client.post(base.value + '/mission', { name: missionForm.name.trim() }), 'mission 已发送')
}

async function loadEvents() {
  await doCmd(async () => {
    const data = await client.get(base.value + '/events')
    eventsText.value = data || ''
    return data
  }, 'events 已更新')
}

// ---------------- 控制台 ----------------
async function sendExec() {
  const cmd = execForm.command.trim()
  if (!cmd) return
  busy.value = true
  try {
    const data = await client.post(base.value + '/exec', { command: cmd })
    consoleLog.value += `> ${cmd}\n${data || '(无输出)'}\n\n`
    execForm.command = ''
  } catch (e) {
    consoleLog.value += `> ${cmd}\n[错误] ${e.message || '执行失败'}\n\n`
  } finally {
    busy.value = false
  }
}

async function refreshAll() {
  if (!instanceId.value) return
  await Promise.all([loadInstance(), loadPlayers(), loadBansFile()])
}

watch(autoRefresh, (on) => {
  if (on && !timer) {
    timer = setInterval(loadPlayers, 10000)
  } else if (!on && timer) {
    clearInterval(timer)
    timer = null
  }
})

onMounted(async () => {
  await loadInstances()
  const qid = Number(route.query.id)
  if (qid && instances.value.some(i => i.id === qid)) {
    instanceId.value = qid
    await refreshAll()
  }
  if (autoRefresh.value) {
    timer = setInterval(loadPlayers, 10000)
  }
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.mono {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.console-out :deep(textarea) {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.mission-tag {
  margin: 2px;
}
</style>
