<template>
  <div class="page">
    <div class="page-header">
      <h2>地图管理（mpmissions / PBO）</h2>
      <el-button type="primary" @click="openUpload">上传 PBO</el-button>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom:12px"
      title="地图文件固定存放于「游戏服务器本体目录\mpmissions」。Map Key 自动取自文件名（去掉 .pbo 后缀）。" />

    <el-table :data="maps" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="mapKey" label="Map Key" width="140" />
      <el-table-column prop="fileName" label="文件" min-width="180" />
      <el-table-column prop="fileSize" label="大小" width="110">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="checksum" label="SHA-256" width="150">
        <template #default="{ row }">
          <span class="muted">{{ (row.checksum || '').slice(0, 12) }}…</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 上传弹窗 -->
    <el-dialog v-model="dialogVisible" title="上传地图 PBO" width="520px" @closed="resetForm">
      <el-form label-width="110px">
        <el-form-item label="地图名称">
          <el-input v-model="name" placeholder="非必填；不填则使用文件名" />
        </el-form-item>
        <el-form-item label="Map Key">
          <el-input :model-value="mapKeyPreview" disabled placeholder="选择文件后自动填入（去后缀）" />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            :auto-upload="false"
            :limit="1"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            accept=".pbo"
            drag
          >
            <el-icon class="el-icon--upload"><i class="el-icon-upload" /></el-icon>
            <div class="el-upload__text">将 .pbo 文件拖到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .pbo；Map Key = 文件名去掉扩展名</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!file" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/http'

const maps = ref([])
const dialogVisible = ref(false)
const uploading = ref(false)
const name = ref('')
const file = ref(null)

const mapKeyPreview = computed(() => {
  if (!file.value) return ''
  return mapKeyFromName(file.value.name)
})

function mapKeyFromName(fileName) {
  if (!fileName) return ''
  return fileName.replace(/\.[^.]+$/, '')
}

function formatSize(n) {
  if (!n) return '-'
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}

async function load() {
  maps.value = await client.get('/maps')
}

function openUpload() {
  resetForm()
  dialogVisible.value = true
}

function resetForm() {
  name.value = ''
  file.value = null
}

function onFileChange(uploadFile) {
  const raw = uploadFile?.raw
  if (!raw) return
  if (!raw.name.toLowerCase().endsWith('.pbo')) {
    ElMessage.warning('仅支持 .pbo 文件')
    file.value = null
    return
  }
  file.value = raw
}

function onFileRemove() {
  file.value = null
}

async function submitUpload() {
  if (!file.value) {
    ElMessage.warning('请选择 .pbo 文件')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('name', name.value || mapKeyFromName(file.value.name))
    fd.append('mapKey', mapKeyFromName(file.value.name))
    fd.append('file', file.value)
    await client.post('/maps', fd)
    ElMessage.success('上传成功')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    uploading.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`删除地图 ${row.name}？`, '警告', { type: 'warning' })
  try {
    await client.delete(`/maps/${row.id}`)
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
