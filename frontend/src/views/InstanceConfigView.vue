<template>
  <div class="page">
    <div class="page-header">
      <h2>实例配置文件（server.cfg / basic.cfg）</h2>
      <div>
        <el-button :disabled="!instanceId" @click="reset">恢复默认</el-button>
        <el-button type="primary" :loading="saving" :disabled="!instanceId" @click="save">保存配置</el-button>
      </div>
    </div>

    <el-form inline>
      <el-form-item label="实例">
        <el-select v-model="instanceId" placeholder="选择实例" style="width:240px" @change="load">
          <el-option v-for="i in instances" :key="i.id" :label="`${i.name} (${i.code})`" :value="i.id" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" style="margin-bottom:12px"
      title="启动时通过 -config=<server.cfg绝对路径> -cfg=<basic.cfg绝对路径> 挂载。文件按实例独立存放，可整文件编辑。" />

    <el-card v-if="paths.serverCfgPath" style="margin-bottom:12px">
      <div class="muted">server.cfg：{{ paths.serverCfgPath }}</div>
      <div class="muted">basic.cfg：{{ paths.basicCfgPath }}</div>
    </el-card>

    <el-tabs>
      <el-tab-pane label="server.cfg">
        <el-input
          v-model="serverCfg"
          type="textarea"
          :rows="18"
          :disabled="!instanceId"
          placeholder="server.cfg 全文"
          style="font-family: Consolas, Monaco, monospace"
        />
      </el-tab-pane>
      <el-tab-pane label="basic.cfg">
        <el-input
          v-model="basicCfg"
          type="textarea"
          :rows="18"
          :disabled="!instanceId"
          placeholder="basic.cfg 全文"
          style="font-family: Consolas, Monaco, monospace"
        />
      </el-tab-pane>
    </el-tabs>

    <el-card style="margin-top:12px">
      <template #header>启动命令预览</template>
      <el-input type="textarea" :rows="4" :model-value="preview" readonly />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/http'

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
    ElMessage.success('配置已保存')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function reset() {
  await ElMessageBox.confirm('确定恢复为默认模板？当前内容会被覆盖。', '警告', { type: 'warning' })
  try {
    await client.post(`/instances/${instanceId.value}/config-files/reset`)
    ElMessage.success('已恢复默认')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(loadMeta)
</script>
