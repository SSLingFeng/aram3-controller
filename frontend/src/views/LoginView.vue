<template>
  <div class="login-wrap">
    <div class="lang-corner">
      <LanguageSwitcher />
    </div>
    <el-card class="login-card">
      <h2>{{ $t('login.title') }}</h2>
      <p class="muted">{{ $t('login.techStack') }}</p>
      <el-form @submit.prevent="onSubmit">
        <el-form-item :label="$t('common.username')">
          <el-input v-model="username" :placeholder="$t('login.usernamePlaceholder')" autocomplete="username" />
        </el-form-item>
        <el-form-item :label="$t('common.password')">
          <el-input
            v-model="password"
            type="password"
            show-password
            :placeholder="$t('login.passwordPlaceholder')"
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width:100%" @click="onSubmit">{{ $t('login.loginBtn') }}</el-button>
      </el-form>
      <p class="muted" style="margin-top:12px">
        {{ $t('login.firstStartTip') }}
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/auth'
import LanguageSwitcher from '../components/LanguageSwitcher.vue'

const { t } = useI18n({ useScope: 'global' })
const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function onSubmit() {
  if (!username.value || !password.value) {
    ElMessage.warning(t('login.msg.enterUsernamePassword'))
    return
  }
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    ElMessage.success(t('login.msg.loginSuccess'))
    router.push({ name: 'instances' })
  } catch (e) {
    ElMessage.error(e.message || t('login.msg.loginFailed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #16324f, #2d4030);
  position: relative;
}
.lang-corner {
  position: absolute;
  top: 16px;
  right: 20px;
}
.lang-corner :deep(.lang-btn) {
  color: #ffffff;
}
.lang-corner :deep(.lang-btn:hover) {
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
}
.login-card {
  width: 360px;
}
.login-card h2 {
  margin: 0 0 4px;
}
</style>
