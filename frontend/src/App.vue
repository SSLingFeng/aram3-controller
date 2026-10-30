<template>
  <el-config-provider :locale="elLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import ru from 'element-plus/es/locale/lang/ru'

const { locale } = useI18n({ useScope: 'global' })

const epLocales = { 'zh-CN': zhCn, 'en-US': en, 'ru-RU': ru }
const elLocale = computed(() => epLocales[locale.value] || zhCn)
</script>
