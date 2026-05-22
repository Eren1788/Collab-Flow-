<template>
  <el-menu router class="sidebar" :default-active="activeMenu">
    <!-- <el-menu-item index="/users">用户管理</el-menu-item> -->
    <el-menu-item v-if="userStore.hasPermission('user:list')" index="/users">
      用户管理
    </el-menu-item>
    <el-menu-item index="/projects">项目管理</el-menu-item>
    <el-menu-item index="/tasks">任务管理</el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../../store/user'

const route = useRoute()
const activeMenu = ref('')
const userStore = useUserStore()

onMounted(() => {
  activeMenu.value = route.path
})
</script>

<style scoped>
.sidebar {
  height: 100%;
  background-color: #ffffff;
}
</style>