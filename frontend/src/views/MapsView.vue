<template>
  <div class="page">
    <div class="page-header">
      <h2>{{ $t('maps.title') }}</h2>
      <el-button type="primary" @click="openUpload">{{ $t('maps.uploadPbo') }}</el-button>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom:12px"
      :title="$t('maps.storageTip')" />

    <el-table :data="maps" border stripe>
      <el-table-column prop="id" :label="$t('common.id')" width="70" />
      <el-table-column prop="name" :label="$t('common.name')" min-width="140" />
      <el-table-column prop="mapKey" :label="$t('maps.mapKey')" width="140" />
      <el-table-column prop="fileName" :label="$t('maps.file')" min-width="180" />
      <el-table-column prop="fileSize" :label="$t('maps.size')" width="110">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="checksum" :label="$t('maps.sha256')" width="150">
        <template #default="{ row }">
          <span class="muted">{{ (row.checksum || '').slice(0, 12) }}…</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.action')" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="remove(row)">{{ $t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 上传弹窗 -->
    <el-dialog v-model="dialogVisible" :title="$t('maps.uploadDialogTitle')" width="520px" @closed="resetForm">
      <el-form label-width="110px">
        <el-form-item :label="$t('maps.mapName')">
          <el-input v-model="name" :placeholder="$t('maps.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('maps.mapKey')">
          <el-input :model-value="mapKeyPreview" disabled :placeholder="$t('maps.mapKeyPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('maps.selectFile')">
          <el-upload
            :auto-upload="false"
            :limit="1"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            accept=".pbo"
            drag
          >
            <el-icon class="el-icon--upload"><i class="el-icon-upload" /></el-icon>
            <div class="el-upload__text">{{ $t('maps.uploadDragPrefix') }}<em>{{ $t('maps.uploadClickToSelect') }}</em></div>
            <template #tip>
              <div class="el-upload__tip">{{ $t('maps.uploadTip') }}</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!file" @click="submitUpload">{{ $t('maps.upload') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import client from '../api/http'

const { t } = useI18n({ useScope: 'global' })

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
    ElMessage.warning(t('maps.onlyPbo'))
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
    ElMessage.warning(t('maps.selectPboFirst'))
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('name', name.value || mapKeyFromName(file.value.name))
    fd.append('mapKey', mapKeyFromName(file.value.name))
    fd.append('file', file.value)
    await client.post('/maps', fd)
    ElMessage.success(t('maps.uploadSuccess'))
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    uploading.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(t('common.confirmDelete', { name: row.name }), t('common.warning'), { type: 'warning' })
  try {
    await client.delete(`/maps/${row.id}`)
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
