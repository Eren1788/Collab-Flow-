<template>

  <div class="login-container">

    <el-card class="login-card">

      <h2 class="title">Collab Flow 登录</h2>

      <el-form :model="loginForm">

        <el-form-item>

          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
          />

        </el-form-item>

        <el-form-item>

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
          >
            登录
          </el-button>

        </el-form-item>

      </el-form>

    </el-card>

  </div>

</template>

<script setup lang="ts">

import { reactive } from 'vue'

import { useRouter } from 'vue-router'

import { ElMessage } from 'element-plus'

import request from '../utils/request'

import { useUserStore } from '../store/user'

import websocket from '../utils/websocket'

const router = useRouter()

const userStore = useUserStore()

/**
 * 登录表单
 */
const loginForm = reactive({

  username:'',

  password:''
})

/**
 * 登录
 */
const handleLogin = async () => {

  try{

    /**
     * 登录接口
     */
    const loginRes:any = await request({

      url:'/user/login',

      method:'post',

      data:loginForm
    })

    /**
     * 保存token
     */
    userStore.setToken(loginRes.data.token)

    /**
     * 获取用户信息
     */
    const userInfoRes:any = await request({

      url:'/user/info',

      method:'get'
    })

    /**
     * 保存用户信息
     */
    userStore.setUserInfo(userInfoRes.data)

    /**
     * 建立WebSocket连接
     */
    websocket.connect(userInfoRes.data.id)

    ElMessage.success('登录成功')

    router.push('/')

  }catch(error){

    console.error(error)
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

.login-card{

  width:400px;

  padding:20px;
}

.title{

  text-align:center;

  margin-bottom:30px;
}

</style>