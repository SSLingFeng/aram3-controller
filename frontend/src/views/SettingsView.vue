<template>
  <div class="page">
    <div class="page-header">
      <h2>系统设置</h2>
      <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom:14px"
      title="保存后立即生效。路径请填绝对路径（例如 D:\Aram3\...）。下面按「游戏本体 / 模组 / 每个服务器实例 / 程序数据」四类填写，避免混用。" />

    <el-card>
      <template #header>一、游戏服务器（SteamCMD 下载的本体，多个实例共用）</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>游戏服务器安装目录</template>
          <el-input v-model="form.gameServerDir" placeholder="例如 D:\Aram3\game-server" />
          <div class="muted">SteamCMD 安装 Arma3 专用服的目录，里面有 arma3server_x64.exe；地图自动放在该目录下的 mpmissions\</div>
        </el-form-item>
        <el-form-item>
          <template #label>SteamCMD 程序路径</template>
          <el-input v-model="form.steamcmdPath" placeholder="例如 D:\steamcmd\steamcmd.exe" />
          <div class="muted">指向 steamcmd.exe 本体，用于下载游戏、更新模组（需要真实 Steam 账号）</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>二、模组（Workshop 下载后存放的地方）</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>模组存储目录</template>
          <el-input v-model="form.modsDir" placeholder="例如 D:\Aram3\mods" />
          <div class="muted">Workshop 模组下载后落在这里，模组库扫描本地是否已下载也看这个目录；与 Workshop 下载是同一目录</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>三、每个实例自己的配置（按实例名分子文件夹）</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>实例配置根目录</template>
          <el-input v-model="form.instancesDir" placeholder="例如 D:\Aram3\instances" />
          <div class="muted">每个实例一个子目录（如 instances\main-ops\），放 server.cfg、basic.cfg、profiles 等；新建实例会在这里建文件夹</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>四、本程序自身（日志与数据库，不是游戏文件）</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>日志目录</template>
          <el-input v-model="form.logsDir" placeholder="例如 D:\Aram3\logs" />
          <div class="muted">控制器产生的启动日志、SteamCMD 任务日志，不是游戏服务器自己的日志</div>
        </el-form-item>
        <el-form-item>
          <template #label>数据目录</template>
          <el-input v-model="form.dataDir" placeholder="例如 D:\Aram3\data" />
          <div class="muted">存 SQLite 数据库文件（用户、实例、模组记录等）；备份主要备这里</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>五、登录安全</template>
      <el-form :model="form" label-width="200px">
        <el-form-item>
          <template #label>登录失败锁定次数</template>
          <el-input-number v-model="form.maxLoginFailures" :min="1" :max="20" />
          <div class="muted">非 admin 账户连续登录失败达到该次数即锁定，需超管在「用户管理」解锁并重置密码；admin 无失败次数限制（仅限本机登录）</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>只读信息</template>
      <el-form label-width="200px">
        <el-form-item>
          <template #label>地图实际目录</template>
          <el-input :model-value="form.mpmissionsDir" disabled />
          <div class="muted">固定 = 游戏服务器安装目录 + \mpmissions，上传的 .pbo 会放这里</div>
        </el-form-item>
        <el-form-item>
          <template #label>游戏本体 AppID</template>
          <el-input :model-value="String(form.steamAppId)" disabled />
        </el-form-item>
        <el-form-item>
          <template #label>Workshop AppID</template>
          <el-input :model-value="String(form.workshopAppId)" disabled />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import client from '../api/http'

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
    ElMessage.success('设置已保存')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
