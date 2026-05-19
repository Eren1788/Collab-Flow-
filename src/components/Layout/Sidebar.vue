<template>
  <el-menu router class="sidebar" :default-active="activeMenu">
    <el-menu-item index="/users">用户管理</el-menu-item>
    <el-menu-item index="/projects">项目管理</el-menu-item>
    <el-menu-item index="/tasks">任务管理</el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = ref('')

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