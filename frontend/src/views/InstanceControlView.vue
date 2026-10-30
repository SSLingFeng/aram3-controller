<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('instanceControl.title') }}</h2>
      <el-button :disabled="!instanceId" @click="refreshAll" :loading="loading">{{ $t('common.refresh') }}</el-button>
    </div>

    <el-form inline>
      <el-form-item :label="$t('instanceControl.instance')">
        <el-select v-model="instanceId" :placeholder="$t('instanceControl.selectInstance')" style="width:260px" @change="onInstanceChange">
          <el-option v-for="i in instances" :key="i.id" :label="`${i.name} (${i.code})`" :value="i.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="instance">
        <el-tag size="small" type="info">{{ $t('instanceControl.rconTag', { host: instance.rconHost, port: instance.rconPort }) }}</el-tag>
        <el-tag size="small">{{ $t('instanceControl.onlineCount', { n: players.length }) }}</el-tag>
      </el-form-item>
      <el-form-item>
        <el-switch v-model="autoRefresh" :active-text="$t('instanceControl.autoRefresh')" />
      </el-form-item>
    </el-form>

    <el-alert v-if="!instanceId" type="info" :closable="false" style="margin-bottom:12px"
              :title="$t('instanceControl.selectInstanceAlert')" />

    <template v-if="instanceId">
      <el-tabs v-model="tab">
        <!-- 玩家 -->
        <el-tab-pane :label="$t('instanceControl.tabs.players')" name="players">
          <div class="toolbar">
            <el-button @click="loadPlayers" :loading="loading">{{ $t('instanceControl.players.refreshBtn') }}</el-button>
            <span class="muted">{{ $t('instanceControl.players.numHint') }}</span>
          </div>
          <el-table :data="players" border stripe v-loading="loading">
            <el-table-column prop="num" :label="$t('instanceControl.players.colNum')" width="60" />
            <el-table-column :label="$t('instanceControl.players.colName')" min-width="180">
              <template #default="{ row }">
                {{ row.name || $t('instanceControl.players.unnamed') }}
                <el-tag v-if="row.lobby" size="small" type="warning">{{ $t('instanceControl.players.lobbyTag') }}</el-tag>
                <el-tag v-if="row.verified" size="small" type="success">{{ $t('instanceControl.players.okTag') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('instanceControl.players.colGuid')" min-width="220">
              <template #default="{ row }">
                <span v-if="row.guid" class="mono">{{ row.guid }}</span>
                <span v-else class="muted">-</span>
                <el-button v-if="row.guid" link type="primary" size="small" @click="copy(row.guid)">{{ $t('common.copy') }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="ip" :label="$t('instanceControl.players.colIp')" width="140" />
            <el-table-column prop="ping" :label="$t('instanceControl.players.colPing')" width="80" />
            <el-table-column :label="$t('common.action')" width="230" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openSayTo(row)">{{ $t('instanceControl.sayTo') }}</el-button>
                <el-button link type="warning" size="small" @click="openKick(row)">{{ $t('instanceControl.kick') }}</el-button>
                <el-button link type="danger" size="small" @click="openBan(row)">{{ $t('instanceControl.ban') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 公告与私聊 -->
        <el-tab-pane :label="$t('instanceControl.tabs.say')" name="say">
          <el-card style="margin-bottom:12px">
            <template #header>{{ $t('instanceControl.say.broadcastHeader') }}</template>
            <el-input v-model="sayForm.message" type="textarea" :rows="3" maxlength="500" show-word-limit
                      :placeholder="$t('instanceControl.say.broadcastPlaceholder')" />
            <div class="toolbar" style="margin-top:8px">
              <el-button type="primary" @click="sendSay" :loading="busy">{{ $t('instanceControl.say.sendBroadcast') }}</el-button>
            </div>
          </el-card>
          <el-card>
            <template #header>{{ $t('instanceControl.say.pmHeader') }}</template>
            <el-form inline>
              <el-form-item :label="$t('instanceControl.say.playerNum')">
                <el-input-number v-model="sayToForm.playerNum" :min="0" :max="999" />
              </el-form-item>
              <el-form-item :label="$t('instanceControl.say.message')">
                <el-input v-model="sayToForm.message" maxlength="500" :placeholder="$t('instanceControl.say.messagePlaceholder')" style="width:320px" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="sendSayTo" :loading="busy">{{ $t('instanceControl.say.sendPm') }}</el-button>
              </el-form-item>
            </el-form>
            <div class="muted">{{ $t('instanceControl.say.pmHint') }}</div>
          </el-card>
        </el-tab-pane>

        <!-- 封禁管理 -->
        <el-tab-pane :label="$t('instanceControl.tabs.bans')" name="bans">
          <div class="toolbar">
            <el-button @click="loadBansFile" :loading="busy">{{ $t('instanceControl.bans.refreshBtn') }}</el-button>
            <el-button @click="loadBans" :loading="busy">{{ $t('instanceControl.bans.reloadBtn') }}</el-button>
            <span class="muted" v-if="bansPath">{{ bansPath }}</span>
            <span class="muted" v-else>{{ $t('instanceControl.bans.notFound') }}</span>
          </div>
          <el-table :data="bans" border stripe v-loading="busy">
            <el-table-column prop="lineNo" :label="$t('instanceControl.bans.colLineNo')" width="80" />
            <el-table-column prop="kind" :label="$t('instanceControl.bans.colKind')" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.kind === 'GUID' ? 'danger' : 'warning'">{{ row.kind }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('instanceControl.bans.colTarget')" min-width="220">
              <template #default="{ row }"><span class="mono">{{ row.target || row.raw }}</span></template>
            </el-table-column>
            <el-table-column prop="reason" :label="$t('instanceControl.bans.reasonLabel')" min-width="180" />
            <el-table-column :label="$t('common.action')" width="100" fixed="right">
              <template #default="{ row }">
                <el-popconfirm :title="$t('instanceControl.bans.unbanConfirm')" @confirm="unban(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">{{ $t('instanceControl.bans.unbanBtn') }}</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-card style="margin-top:12px">
            <template #header>{{ $t('instanceControl.bans.banPlayerHeader') }}</template>
            <el-form inline>
              <el-form-item :label="$t('instanceControl.bans.targetLabel')">
                <el-input v-model="banPlayerForm.target" :placeholder="$t('instanceControl.bans.targetPlaceholder')" style="width:280px" />
              </el-form-item>
              <el-form-item :label="$t('instanceControl.bans.minutesLabel')">
                <el-input-number v-model="banPlayerForm.minutes" :min="0" :max="525600" />
              </el-form-item>
              <el-form-item :label="$t('instanceControl.bans.reasonLabel')">
                <el-input v-model="banPlayerForm.reason" style="width:200px" />
              </el-form-item>
              <el-form-item>
                <el-button type="danger" @click="sendBanPlayer" :loading="busy">{{ $t('instanceControl.ban') }}</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-tab-pane>

        <!-- 服务器控制 -->
        <el-tab-pane :label="$t('instanceControl.tabs.server')" name="server">
          <el-card style="margin-bottom:12px">
            <template #header>{{ $t('instanceControl.server.connHeader') }}</template>
            <div class="toolbar">
              <el-button @click="doCmd(() => client.post(base + '/lock'), $t('instanceControl.msg.locked'))" :loading="busy">{{ $t('instanceControl.server.lockBtn') }}</el-button>
              <el-button @click="doCmd(() => client.post(base + '/unlock'), $t('instanceControl.msg.unlocked'))" :loading="busy">{{ $t('instanceControl.server.unlockBtn') }}</el-button>
              <span class="muted">{{ $t('instanceControl.server.maxPlayersLabel') }}</span>
              <el-input-number v-model="maxPlayersValue" :min="1" :max="200" />
              <el-button @click="setMaxPlayers" :loading="busy">{{ $t('instanceControl.server.setMaxPlayersBtn') }}</el-button>
            </div>
          </el-card>
          <el-card style="margin-bottom:12px">
            <template #header>{{ $t('instanceControl.server.restartHeader') }}</template>
            <div class="toolbar">
              <el-button @click="doCmd(() => client.post(base + '/reassign'), $t('instanceControl.msg.reassigned'))" :loading="busy">{{ $t('instanceControl.server.reassignBtn') }}</el-button>
              <el-button @click="confirmCmd($t('instanceControl.server.confirmRestartServer'), () => client.post(base + '/restart-server'), $t('instanceControl.msg.restartServerSent'))" :loading="busy">{{ $t('instanceControl.server.restartServerBtn') }}</el-button>
              <el-button @click="confirmCmd($t('instanceControl.server.confirmRestart'), () => client.post(base + '/restart'), $t('instanceControl.msg.restartSent'))" :loading="busy">{{ $t('instanceControl.server.restartBtn') }}</el-button>
              <el-button v-if="isSuper" type="danger" plain
                         @click="confirmCmd($t('instanceControl.server.confirmShutdown'), () => client.post(base + '/shutdown'), $t('instanceControl.msg.shutdownSent'))"
                         :loading="busy">{{ $t('instanceControl.server.shutdownBtn') }}</el-button>
            </div>
            <div class="toolbar" style="margin-top:8px">
              <span class="muted">{{ $t('instanceControl.server.restartTimeLabel') }}</span>
              <el-input-number v-model="restartTime.minutes" :min="0" :max="720" /> {{ $t('instanceControl.server.minutes') }}
              <el-input-number v-model="restartTime.seconds" :min="0" :max="59" /> {{ $t('instanceControl.server.seconds') }}
              <el-button @click="setRestartTime" :loading="busy">{{ $t('instanceControl.server.setBtn') }}</el-button>
            </div>
          </el-card>
          <el-card style="margin-bottom:12px">
            <template #header>{{ $t('instanceControl.server.hotReloadHeader') }}</template>
            <div class="toolbar">
              <el-button @click="doCmd(() => client.post(base + '/load-bans'), $t('instanceControl.msg.loadBansDone'))" :loading="busy">{{ $t('instanceControl.server.loadBansBtn') }}</el-button>
              <el-button @click="doCmd(() => client.post(base + '/load-scripts'), $t('instanceControl.msg.loadScriptsDone'))" :loading="busy">{{ $t('instanceControl.server.loadScriptsBtn') }}</el-button>
              <el-button @click="doCmd(() => client.post(base + '/load-events'), $t('instanceControl.msg.loadEventsDone'))" :loading="busy">{{ $t('instanceControl.server.loadEventsBtn') }}</el-button>
              <el-button @click="doCmd(() => client.post(base + '/load-rpt'), $t('instanceControl.msg.loadRptDone'))" :loading="busy">{{ $t('instanceControl.server.loadRptBtn') }}</el-button>
            </div>
          </el-card>
          <el-card>
            <template #header>{{ $t('instanceControl.server.rptHeader') }}</template>
            <div class="toolbar">
              <el-input-number v-model="rptLines" :min="20" :max="1000" :step="20" />
              <el-button @click="loadRpt" :loading="busy">{{ $t('instanceControl.server.readBtn') }}</el-button>
              <span class="muted" v-if="rptPath">{{ $t('instanceControl.server.rptMeta', { path: rptPath, n: rptTotal }) }}</span>
            </div>
            <el-input v-model="rptContent" type="textarea" :rows="12" readonly class="mono" />
          </el-card>
        </el-tab-pane>

        <!-- 任务与事件 -->
        <el-tab-pane :label="$t('instanceControl.tabs.missions')" name="missions">
          <el-card style="margin-bottom:12px">
            <template #header>{{ $t('instanceControl.missions.listHeader') }}</template>
            <div class="toolbar">
              <el-button @click="loadMissions" :loading="busy">{{ $t('instanceControl.missions.fetchBtn') }}</el-button>
              <span class="muted">{{ $t('instanceControl.missions.clickHint') }}</span>
            </div>
            <el-input v-model="missionsText" type="textarea" :rows="8" readonly class="mono" />
            <div class="toolbar" style="margin-top:8px; flex-wrap:wrap">
              <el-tag v-for="m in missionNames" :key="m" class="mission-tag" style="cursor:pointer"
                      @click="missionForm.name = m">{{ m }}</el-tag>
            </div>
          </el-card>
          <el-card style="margin-bottom:12px">
            <template #header>{{ $t('instanceControl.missions.switchHeader') }}</template>
            <div class="toolbar">
              <el-input v-model="missionForm.name" :placeholder="$t('instanceControl.missions.namePlaceholder')" style="width:320px" />
              <el-button type="primary" @click="confirmCmd($t('instanceControl.missions.confirmSwitch'), setMission, $t('instanceControl.msg.missionSent'))" :loading="busy">{{ $t('instanceControl.missions.switchBtn') }}</el-button>
            </div>
          </el-card>
          <el-card>
            <template #header>{{ $t('instanceControl.missions.eventsHeader') }}</template>
            <div class="toolbar">
              <el-button @click="loadEvents" :loading="busy">{{ $t('instanceControl.missions.fetchEventsBtn') }}</el-button>
            </div>
            <el-input v-model="eventsText" type="textarea" :rows="6" readonly class="mono" />
          </el-card>
        </el-tab-pane>

        <!-- RCON 控制台 -->
        <el-tab-pane :label="$t('instanceControl.tabs.console')" name="console">
          <el-card>
            <template #header>{{ $t('instanceControl.console.rawHeader') }}</template>
            <div v-if="!isSuper" class="muted" style="margin-bottom:8px">{{ $t('instanceControl.console.superOnly') }}</div>
            <div class="toolbar">
              <el-input v-model="execForm.command" :placeholder="$t('instanceControl.console.placeholder')"
                        style="width:420px" @keyup.enter="sendExec" :disabled="!isSuper" />
              <el-button type="primary" @click="sendExec" :loading="busy" :disabled="!isSuper">{{ $t('instanceControl.console.execBtn') }}</el-button>
              <el-button @click="consoleLog = ''">{{ $t('instanceControl.console.clearBtn') }}</el-button>
            </div>
            <el-input v-model="consoleLog" type="textarea" :rows="14" readonly class="mono console-out" />
          </el-card>
        </el-tab-pane>
      </el-tabs>

      <!-- 最近命令结果 -->
      <el-card v-if="lastResult" style="margin-top:12px">
        <template #header>{{ $t('instanceControl.lastResultHeader') }}</template>
        <el-input :model-value="lastResult" type="textarea" :rows="4" readonly class="mono" />
      </el-card>
    </template>

    <!-- 踢出 -->
    <el-dialog v-model="kickDialog" :title="$t('instanceControl.kickDialog.title')" width="420px">
      <p>{{ $t('instanceControl.targetPlayer', { num: targetPlayer?.num, name: targetPlayer?.name || '' }) }}</p>
      <el-input v-model="kickForm.reason" :placeholder="$t('instanceControl.kickDialog.reasonPlaceholder')" />
      <template #footer>
        <el-button @click="kickDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="warning" @click="sendKick" :loading="busy">{{ $t('instanceControl.kick') }}</el-button>
      </template>
    </el-dialog>

    <!-- 封禁 -->
    <el-dialog v-model="banDialog" :title="$t('instanceControl.banDialog.title')" width="420px">
      <p>{{ $t('instanceControl.targetPlayer', { num: targetPlayer?.num, name: targetPlayer?.name || '' }) }}</p>
      <el-form label-width="140px">
        <el-form-item :label="$t('instanceControl.bans.minutesLabel')">
          <el-input-number v-model="banForm.minutes" :min="0" :max="525600" />
        </el-form-item>
        <el-form-item :label="$t('instanceControl.bans.reasonLabel')">
          <el-input v-model="banForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="danger" @click="sendBan" :loading="busy">{{ $t('instanceControl.ban') }}</el-button>
      </template>
    </el-dialog>

    <!-- 私聊 -->
    <el-dialog v-model="sayToDialog" :title="$t('instanceControl.pmDialog.title')" width="420px">
      <p>{{ $t('instanceControl.targetPlayer', { num: targetPlayer?.num, name: targetPlayer?.name || '' }) }}</p>
      <el-input v-model="sayToForm.message" type="textarea" :rows="3" maxlength="500" :placeholder="$t('instanceControl.pmDialog.messagePlaceholder')" />
      <template #footer>
        <el-button @click="sayToDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="sendSayTo" :loading="busy">{{ $t('instanceControl.pmDialog.sendBtn') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/http'
import { useAuthStore } from '../stores/auth'

const { t } = useI18n({ useScope: 'global' })
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
  navigator.clipboard?.writeText(text).then(() => ElMessage.success(t('common.copied')))
}

function showResult(text) {
  lastResult.value = text == null || text === '' ? t('instanceControl.msg.noOutput') : String(text)
}

async function doCmd(fn, okMsg) {
  busy.value = true
  try {
    const data = await fn()
    showResult(data)
    if (okMsg) ElMessage.success(okMsg)
  } catch (e) {
    ElMessage.error(e.message || t('instanceControl.msg.commandFailed'))
  } finally {
    busy.value = false
  }
}

async function confirmCmd(title, fn, okMsg) {
  try {
    await ElMessageBox.confirm(title, t('common.confirmTitle'), { type: 'warning' })
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
    ElMessage.error(t('instanceControl.msg.loadInstancesFailed', { msg: e.message || '' }))
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
    ElMessage.error(t('instanceControl.msg.loadInstanceFailed', { msg: e.message || '' }))
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
    ElMessage.error(t('instanceControl.msg.loadPlayersFailed', { msg: e.message || '' }))
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
    t('instanceControl.msg.kicked', { num: row.num, name: row.name || '' }))
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
    t('instanceControl.msg.banned', { num: row.num, name: row.name || '' }))
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
    ElMessage.warning(t('instanceControl.msg.enterBroadcast'))
    return
  }
  await doCmd(() => client.post(base.value + '/say', { message: sayForm.message }), t('instanceControl.msg.broadcastSent'))
  sayForm.message = ''
}

async function sendSayTo() {
  if (!sayToForm.message.trim()) {
    ElMessage.warning(t('instanceControl.msg.enterMessage'))
    return
  }
  sayToDialog.value = false
  await doCmd(() => client.post(base.value + '/say-to', { playerNum: sayToForm.playerNum, message: sayToForm.message }),
    t('instanceControl.msg.pmSent'))
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
    ElMessage.error(t('instanceControl.msg.loadBansFailed', { msg: e.message || '' }))
  } finally {
    busy.value = false
  }
}

async function unban(row) {
  await doCmd(async () => {
    const data = await client.post(base.value + '/bans-file/unban', { lineNo: row.lineNo })
    bans.value = data.entries || []
    return data.reload
  }, t('instanceControl.msg.unbanned'))
}

async function sendBanPlayer() {
  if (!banPlayerForm.target.trim()) {
    ElMessage.warning(t('instanceControl.msg.enterGuidOrIp'))
    return
  }
  await doCmd(() => client.post(base.value + '/ban-player', {
    target: banPlayerForm.target.trim(),
    minutes: banPlayerForm.minutes,
    reason: banPlayerForm.reason
  }), t('instanceControl.msg.banPlayerSent'))
  loadBansFile()
}

async function loadBans() {
  await doCmd(() => client.post(base.value + '/load-bans'), t('instanceControl.msg.loadBansDone'))
}

// ---------------- 服务器控制 ----------------
async function setMaxPlayers() {
  await doCmd(() => client.post(base.value + '/max-players', { value: maxPlayersValue.value }), t('instanceControl.msg.maxPlayersSet'))
}

async function setRestartTime() {
  await doCmd(() => client.post(base.value + '/restart-time', { minutes: restartTime.minutes, seconds: restartTime.seconds }),
    t('instanceControl.msg.restartTimeSet'))
}

async function loadRpt() {
  busy.value = true
  try {
    const data = await client.get(base.value + '/rpt-tail', { params: { lines: rptLines.value } })
    rptContent.value = data.content || ''
    rptPath.value = data.path || ''
    rptTotal.value = data.totalLines || 0
  } catch (e) {
    ElMessage.error(e.message || t('instanceControl.msg.loadRptFailed'))
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
  }, t('instanceControl.msg.missionsUpdated'))
}

async function setMission() {
  if (!missionForm.name.trim()) {
    ElMessage.warning(t('instanceControl.msg.enterMissionName'))
    return
  }
  await doCmd(() => client.post(base.value + '/mission', { name: missionForm.name.trim() }), t('instanceControl.msg.missionSent'))
}

async function loadEvents() {
  await doCmd(async () => {
    const data = await client.get(base.value + '/events')
    eventsText.value = data || ''
    return data
  }, t('instanceControl.msg.eventsUpdated'))
}

// ---------------- 控制台 ----------------
async function sendExec() {
  const cmd = execForm.command.trim()
  if (!cmd) return
  busy.value = true
  try {
    const data = await client.post(base.value + '/exec', { command: cmd })
    consoleLog.value += `> ${cmd}\n${data || t('instanceControl.msg.noOutput')}\n\n`
    execForm.command = ''
  } catch (e) {
    consoleLog.value += `> ${cmd}\n[${t('common.error')}] ${e.message || t('instanceControl.msg.execFailed')}\n\n`
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
