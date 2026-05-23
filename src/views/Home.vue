<template>
  <div class="layout-container">
    <!-- 左侧菜单（保持原样） -->
    <div class="sidebar">
      <div class="logo">
        Collab Flow
      </div>
      <el-menu
        router
        :default-active="activeMenu"
        class="menu"
      >
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/projects">
          <el-icon><Folder /></el-icon>
          <span>项目管理</span>
        </el-menu-item>
        <el-menu-item index="/tasks">
          <el-icon><Tickets /></el-icon>
          <span>任务管理</span>
        </el-menu-item>
        <el-menu-item index="/comments">
          <el-icon><ChatDotRound /></el-icon>
          <span>评论管理</span>
        </el-menu-item>
        <el-menu-item index="/files">
          <el-icon><Document /></el-icon>
          <span>文件管理</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 右侧容器 -->
    <div class="main-container">
      <!-- 顶部：使用独立的 Header 组件（相对路径导入） -->
      <Header />

      <!-- 内容区域 -->
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  User, Folder, Tickets, ChatDotRound, Document
} from '@element-plus/icons-vue'
// 使用相对路径导入 Header 组件（确保路径正确）
import Header from '../components/Layout/Header.vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.layout-container {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* 左侧菜单 */
.sidebar {
  width: 220px;
  background: #001529;
  color: white;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,.1);
}

.menu {
  flex: 1;
  border: none;
  background: #001529;
}

.menu :deep(.el-menu-item) {
  color: #ccc;
}
.menu :deep(.el-menu-item:hover) {
  background: #0c2135;
}
.menu :deep(.el-menu-item.is-active) {
  background: #1677ff;
  color: white;
}

/* 右侧容器 */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 内容区域 */
.content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: #f5f7fa;
}
</style>