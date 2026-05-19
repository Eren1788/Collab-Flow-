<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>Collab Flow 登录</h2>
      <el-form :model="form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名"/>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码"/>
        </el-form-item>
        <el-button type="primary" style="width:100%" @click="login">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { requestWithType } from '../utils/request'
import router from '../router'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

// 定义接口返回类型
interface LoginData {
  code: number
  message: string
  data: string
}

// 表单数据
const form = reactive({
  username: '',
  password: ''
})

// Pinia 状态
const userStore = useUserStore()

// 登录函数
const login = async () => {
  try {
    const res = await requestWithType<LoginData>({
      url: '/user/login',
      method: 'post',
      data: form
    })

    if (res.code === 200) {
      userStore.setToken(res.data)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message)
    }
  } catch (err) {
    ElMessage.error('登录失败')
    console.error(err)
  }
}
</script>

<style scoped>
.login-container{
  width:100%;
  height:100vh;
  display:flex;
  justify-content:center;
  align-items:center;
  background:#f5f5f5;
}
.login-card{width:400px;}
</style>