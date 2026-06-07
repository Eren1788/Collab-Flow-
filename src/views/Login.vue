<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="40" height="40" rx="10" fill="white" fill-opacity="0.2"/>
            <path d="M12 20L18 26L28 14" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h1 class="brand-title">Collab Flow</h1>
        <p class="brand-subtitle">企业级任务协作管理平台</p>
        <div class="brand-features">
          <div class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
            <span>项目管理</span>
          </div>
          <div class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg>
            <span>任务协作</span>
          </div>
          <div class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
            <span>实时沟通</span>
          </div>
        </div>
      </div>
      <!-- 装饰性背景图形 -->
      <div class="brand-decoration">
        <div class="deco-circle deco-1"></div>
        <div class="deco-circle deco-2"></div>
        <div class="deco-circle deco-3"></div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-form-area">
      <div class="form-container">
        <div class="form-header">
          <!-- 系统 Logo -->
          <div class="login-logo-img">
            <img :src="logoSrc" alt="Logo" @error="(e: any) => e.target.style.display='none'" />
          </div>
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-desc">请登录您的账号以继续</p>
        </div>

        <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" class="login-form" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              show-password
              :prefix-icon="Lock"
              size="large"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" size="large" style="width:100%" @click="handleLogin" :loading="loginLoading">
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>还没有账号？</span>
          <el-link type="primary" :underline="false" @click="openRegisterDialog">立即注册</el-link>
        </div>
      </div>
    </div>

    <!-- 注册对话框 -->
    <el-dialog v-model="registerDialogVisible" title="创建账号" width="520px" top="5vh" :close-on-click-modal="false">
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="80px" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="3-20个字符" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="6-20个字符" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="选填" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input disabled :value="'普通成员'" />
          <input type="hidden" v-model="registerForm.roleId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleRegister" :loading="registerLoading" size="large">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useUserStore } from '../store/user'
import websocket from '../utils/websocket'
import { registerApi } from '../api/user'

const router = useRouter()
const userStore = useUserStore()

// 系统 Logo
const logoSrc = `/api/logo/image?t=${Date.now()}`

const loginForm = reactive({ username: '', password: '' })
const loginFormRef = ref<FormInstance>()
const loginLoading = ref(false)

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate()
  loginLoading.value = true
  try {
    const loginRes: any = await request({ url: '/user/login', method: 'post', data: loginForm })
    userStore.setToken(loginRes.data.token)
    const userInfoRes: any = await request({ url: '/user/info', method: 'get' })
    userStore.setUserInfo(userInfoRes.data)
    websocket.connect(userInfoRes.data.id)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error(error)
  } finally {
    loginLoading.value = false
  }
}

// 注册
const registerDialogVisible = ref(false)
const registerLoading = ref(false)
const registerFormRef = ref<FormInstance>()
const DEFAULT_ROLE_ID = 3

const registerForm = reactive({
  username: '', password: '', confirmPassword: '', nickname: '', email: '', phone: '', roleId: DEFAULT_ROLE_ID
})

const validateConfirmPassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const registerRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '3-20个字符', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '6-20个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: 'blur' }],
  nickname: [{ max: 50, message: '不能超过50个字符', trigger: 'blur' }]
}

const openRegisterDialog = () => {
  registerForm.username = ''; registerForm.password = ''; registerForm.confirmPassword = ''
  registerForm.nickname = ''; registerForm.email = ''; registerForm.phone = ''
  registerForm.roleId = DEFAULT_ROLE_ID
  registerDialogVisible.value = true
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate()
  registerLoading.value = true
  try {
    const res: any = await registerApi({
      username: registerForm.username, password: registerForm.password,
      nickname: registerForm.nickname || registerForm.username,
      email: registerForm.email, phone: registerForm.phone, roleId: registerForm.roleId
    })
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      registerDialogVisible.value = false
      loginForm.username = registerForm.username
      loginForm.password = ''
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

/* ===== 左侧品牌区 ===== */
.login-brand {
  flex: 1;
  background: linear-gradient(160deg, #4437a8 0%, #5b4ad0 50%, #7c6ee0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 48px;
}

.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
  max-width: 380px;
}

.brand-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}
.brand-logo svg {
  width: 64px;
  height: 64px;
}

.brand-title {
  font-size: 38px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
  font-family: var(--cf-font);
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.7;
  margin: 0 0 40px;
  font-weight: 400;
  font-family: var(--cf-font-sans);
}

.brand-features {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  opacity: 0.85;
  background: rgba(255,255,255,0.08);
  padding: 6px 14px;
  border-radius: 4px;
  font-family: var(--cf-font-sans);
  letter-spacing: 0.3px;
}
.feature-item svg {
  flex-shrink: 0;
}

/* 装饰图形 */
.brand-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.12);
}
.deco-1 {
  width: 400px; height: 400px;
  top: -100px; right: -100px;
  animation: float 8s ease-in-out infinite;
}
.deco-2 {
  width: 250px; height: 250px;
  bottom: -50px; left: -60px;
  animation: float 6s ease-in-out infinite reverse;
}
.deco-3 {
  width: 150px; height: 150px;
  top: 50%; left: 20%;
  animation: float 10s ease-in-out infinite;
  opacity: 0.5;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -20px) scale(1.05); }
}

/* ===== 右侧登录区 ===== */
.login-form-area {
  width: 480px;
  min-width: 480px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.form-container {
  width: 100%;
  max-width: 360px;
}

/* 登录页 Logo — 居中填充效果 */
.login-logo-img {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 80%;
  margin: 0 auto 28px;
  min-height: 72px;
  background: var(--cf-bg-soft);
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--cf-border-light);
}
.login-logo-img img {
  width: 100%;
  height: 100%;
  min-height: 72px;
  object-fit: cover;
  display: block;
}

.form-header {
  margin-bottom: 32px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--cf-text-heading);
  margin: 0 0 6px;
  font-family: var(--cf-font);
  letter-spacing: -0.3px;
}

.form-desc {
  font-size: 14px;
  color: var(--cf-text-muted);
  margin: 0;
  font-family: var(--cf-font-sans);
}

.login-form {
  margin-bottom: 24px;
}
.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: var(--cf-text-secondary);
}
.form-footer .el-link {
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 900px) {
  .login-brand {
    display: none;
  }
  .login-form-area {
    width: 100%;
    min-width: 0;
  }
}
</style>
