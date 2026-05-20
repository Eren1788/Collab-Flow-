<template>

  <div class="login-container">

    <div class="login-box">

      <h2 class="title">
        Collab Flow
      </h2>

      <el-form
        :model="loginForm"
        label-width="80px"
        @keyup.enter="handleLogin"
      >

        <el-form-item label="用户名">

          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
          />

        </el-form-item>

        <el-form-item label="密码">

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
            style="width:100%;"
            @click="handleLogin"
          >
            登录
          </el-button>

        </el-form-item>

      </el-form>

    </div>

  </div>

</template>

<script setup lang="ts">

import { reactive } from 'vue'

import { ElMessage } from 'element-plus'

import { useRouter } from 'vue-router'

import { useUserStore } from '../store/user'

import { loginApi } from '../api/user'

const router = useRouter()

const userStore = useUserStore()

const loginForm = reactive({

  username:'',

  password:''
})

const handleLogin = async ()=>{

  if(!loginForm.username){

    ElMessage.warning('请输入用户名')

    return
  }

  if(!loginForm.password){

    ElMessage.warning('请输入密码')

    return
  }

  try{

    const res:any = await loginApi(loginForm)

    const token = res.data.token

    userStore.setToken(token)

    ElMessage.success('登录成功')

    router.push('/users')

  }catch(error){

    console.log(error)
  }
}

</script>

<style scoped>

.login-container{

  width:100vw;

  height:100vh;

  display:flex;

  justify-content:center;

  align-items:center;

  background:#f5f7fa;
}

.login-box{

  width:420px;

  padding:40px;

  background:#fff;

  border-radius:12px;

  box-shadow:0 2px 12px rgba(0,0,0,.1);
}

.title{

  text-align:center;

  margin-bottom:30px;

  color:#409EFF;
}

</style>