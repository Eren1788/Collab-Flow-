<template>
  <el-header height="60px" class="header">
    <div class="logo">Collab Flow</div>
    <div class="user-info">
      <span>{{ userStore.info.nickname || '未登录' }}</span>
      <el-button type="text" @click="logout">退出</el-button>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { useUserStore } from '../../store/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

const logout = () => {
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: #409EFF;
  color: #fff;
}
.logo {
  font-size: 20px;
  font-weight: bold;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>