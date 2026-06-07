<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="7" fill="currentColor" fill-opacity="0.2"/>
            <path d="M8 14L12 18L20 10" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <span class="logo-text">Collab Flow</span>
      </div>

      <el-menu
        router
        :default-active="activeMenu"
        class="sidebar-menu"
        :popper-offset="12"
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

      <div class="sidebar-footer">
        <div class="sidebar-user">
          <el-avatar :size="32" :src="avatarUrl">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="sidebar-user-info">
            <div class="sidebar-user-name">{{ displayName }}</div>
            <div class="sidebar-user-role">{{ userRole }}</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-area">
      <Header />

      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { User, Folder, Tickets, ChatDotRound, Document } from '@element-plus/icons-vue'
import Header from '../components/Layout/Header.vue'
import { useUserStore } from '../store/user'

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const displayName = computed(() => {
  const info = userStore.info
  return info?.nickname || info?.username || '用户'
})

const userRole = computed(() => {
  return userStore.info?.roleName || '成员'
})

const avatarUrl = computed(() => {
  const avatar = userStore.info?.avatar
  if (avatar && avatar.startsWith('/uploads')) return `/api${avatar}?t=${Date.now()}`
  return ''
})

</script>

<style scoped>
.layout {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: var(--cf-bg);
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 240px;
  min-width: 240px;
  background: var(--cf-sidebar-bg);
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 100;
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.logo-icon {
  color: var(--cf-primary-light);
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.3px;
}

/* 菜单 */
.sidebar-menu {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px;
}
.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  border-radius: 8px;
  margin: 2px 0;
  color: var(--cf-sidebar-text);
  font-size: 14px;
  transition: var(--cf-transition);
}
.sidebar-menu :deep(.el-menu-item:hover) {
  background: var(--cf-sidebar-hover);
  color: var(--cf-sidebar-text-active);
}
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--cf-sidebar-active);
  color: var(--cf-sidebar-text-active);
}
.sidebar-menu :deep(.el-menu-item .el-icon) {
  margin-right: 10px;
  font-size: 18px;
}

/* 底部用户信息 */
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sidebar-user :deep(.el-avatar) {
  background: var(--cf-primary);
  flex-shrink: 0;
}

.sidebar-user-info {
  min-width: 0;
  flex: 1;
}

.sidebar-user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--cf-sidebar-text-active);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-user-role {
  font-size: 11px;
  color: var(--cf-sidebar-text);
  margin-top: 1px;
}

/* ===== 主区域 ===== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
</style>
