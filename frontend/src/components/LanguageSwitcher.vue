<template>
  <el-dropdown trigger="click" @command="switchLang">
    <span class="lang-btn" :title="$t('common.language')">
      <svg class="globe" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
           stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="9" />
        <path d="M3 12h18" />
        <path d="M12 3c2.5 2.6 3.8 5.6 3.8 9s-1.3 6.4-3.8 9c-2.5-2.6-3.8-5.6-3.8-9S9.5 5.6 12 3z" />
      </svg>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="l in languages" :key="l.code" :command="l.code">
          <el-icon v-if="l.code === locale" class="lang-check"><Check /></el-icon>
          <span v-else class="lang-check" />
          {{ l.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { Check } from '@element-plus/icons-vue'
import { SUPPORTED, setLanguage } from '../i18n'

const { locale } = useI18n({ useScope: 'global' })
const languages = SUPPORTED

function switchLang(code) {
  setLanguage(code)
}
</script>

<style scoped>
.lang-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #4b5563;
  padding: 5px;
  border-radius: 6px;
  outline: none;
}
.lang-btn:hover {
  background: #f3f4f6;
  color: #16324f;
}
.globe {
  width: 18px;
  height: 18px;
  display: block;
}
.lang-check {
  width: 16px;
  margin-right: 4px;
  font-size: 12px;
}
</style>
