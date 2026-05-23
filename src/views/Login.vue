<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">Collab Flow 登录</h2>

      <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            style="width:100%"
            @click="handleLogin"
            :loading="loginLoading"
          >
            登录
          </el-button>
        </el-form-item>

        <!-- [MODIFIED] 添加注册提示链接 -->
        <div class="register-tip">
          <span>当前没有账号？</span>
          <el-link type="primary" @click="openRegisterDialog">立即注册</el-link>
        </div>
      </el-form>
    </el-card>

    <!-- [MODIFIED] 注册对话框 - 完整复用“新增用户”弹窗的字段和逻辑 -->
    <el-dialog v-model="registerDialogVisible" title="注册账号" width="500px">
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="请输入昵称（可选）" />
        </el-form-item>
        <!-- [MODIFIED] 新增邮箱和手机号字段（与UserList.vue中的新增用户弹窗一致） -->
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <!-- [MODIFIED] 新增职位选择（与UserList.vue中的新增用户弹窗一致） -->
        <el-form-item label="职位" prop="roleId">
          <el-select
            v-model="registerForm.roleId"
            placeholder="请选择职位"
            style="width: 100%"
          >
            <el-option
              v-for="item in roleList"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            普通用户注册请选择“普通成员”
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRegister" :loading="registerLoading">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '../utils/request'
import { useUserStore } from '../store/user'
import websocket from '../utils/websocket'
// [MODIFIED] 导入注册接口和获取角色列表接口
import { registerApi, getRoleListApi } from '../api/user'

const router = useRouter()
const userStore = useUserStore()

// 登录表单及验证规则
const loginForm = reactive({
  username: '',
  password: ''
})

const loginFormRef = ref<FormInstance>()
const loginLoading = ref(false)

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/**
 * 登录
 */
const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate()

  loginLoading.value = true
  try {
    const loginRes: any = await request({
      url: '/user/login',
      method: 'post',
      data: loginForm
    })

    userStore.setToken(loginRes.data.token)

    const userInfoRes: any = await request({
      url: '/user/info',
      method: 'get'
    })

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

// [MODIFIED] 注册相关逻辑 - 完全复用UserList.vue中的“新增用户”逻辑
const registerDialogVisible = ref(false)
const registerLoading = ref(false)
const registerFormRef = ref<FormInstance>()
const roleList = ref<any[]>([])  // 职位列表

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',      // [MODIFIED] 新增邮箱
  phone: '',      // [MODIFIED] 新增手机号
  roleId: null    // [MODIFIED] 新增职位ID
})

// 确认密码自定义验证函数
const validateConfirmPassword = (rule: any, value: string, callback: (error?: Error) => void) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 邮箱验证（简单示例，可按需增强）
const validateEmail = (rule: any, value: string, callback: (error?: Error) => void) => {
  if (value && !/^[^\s@]+@([^\s@]+\.)+[^\s@]+$/.test(value)) {
    callback(new Error('请输入正确的邮箱地址'))
  } else {
    callback()
  }
}

// 手机号验证（简单示例）
const validatePhone = (rule: any, value: string, callback: (error?: Error) => void) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  nickname: [
    { max: 50, message: '昵称不能超过50个字符', trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  roleId: [
    { required: true, message: '请选择职位', trigger: 'change' }
  ]
}

// [MODIFIED] 获取职位列表（复用UserList.vue中的方法）
const loadRoleList = async () => {
  try {
    const res: any = await getRoleListApi()
    roleList.value = res.data || []
  } catch (error) {
    ElMessage.error('获取职位列表失败')
  }
}

// [MODIFIED] 打开注册对话框，并加载职位列表
const openRegisterDialog = () => {
  // 重置表单
  registerForm.username = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  registerForm.nickname = ''
  registerForm.email = ''
  registerForm.phone = ''
  registerForm.roleId = null
  registerDialogVisible.value = true
  // 加载职位列表（如果还没加载过）
  if (roleList.value.length === 0) {
    loadRoleList()
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate()

  registerLoading.value = true
  try {
    // [MODIFIED] 调用注册接口，传递所有字段（与UserList.vue中新增用户保持一致）
    const res: any = await registerApi({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname || registerForm.username,
      email: registerForm.email,
      phone: registerForm.phone,
      roleId: registerForm.roleId
    })

    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      registerDialogVisible.value = false
      // 自动填充用户名到登录框，方便用户直接登录
      loginForm.username = registerForm.username
      // 清空密码框（安全）
      loginForm.password = ''
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch (error: any) {
    console.error('注册失败:', error)
  } finally {
    registerLoading.value = false
  }
}

// [MODIFIED] 组件挂载时预加载职位列表（可选）
onMounted(() => {
  loadRoleList()
})
</script>

<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;
}

.login-card {
  width: 400px;
  padding: 20px;
}

.title {
  text-align: center;
  margin-bottom: 30px;
}

.register-tip {
  text-align: center;
  margin-top: 10px;
  font-size: 14px;
  color: #909399;
}

.register-tip span {
  margin-right: 5px;
}
</style>