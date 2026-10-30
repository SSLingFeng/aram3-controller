import { createI18n } from 'vue-i18n'
import messages from '../locales'

export const SUPPORTED = [
  { code: 'zh-CN', label: '中文' },
  { code: 'en-US', label: 'English' },
  { code: 'ru-RU', label: 'Русский' }
]

function initialLocale() {
  try {
    const saved = localStorage.getItem('aram3_lang')
    if (saved && messages[saved]) {
      return saved
    }
  } catch (e) {
    // ignore
  }
  return 'zh-CN'
}

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: initialLocale(),
  fallbackLocale: 'zh-CN',
  messages
})

document.documentElement.setAttribute('lang', i18n.global.locale.value)

export function setLanguage(code) {
  if (!messages[code]) {
    return
  }
  i18n.global.locale.value = code
  try {
    localStorage.setItem('aram3_lang', code)
  } catch (e) {
    // ignore
  }
  document.documentElement.setAttribute('lang', code)
}

export default i18n
