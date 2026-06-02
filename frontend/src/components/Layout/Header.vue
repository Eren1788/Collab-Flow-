<template>
  <el-header height="60px" class="header">
    <div class="logo-area">
      <img :src="logoUrl" class="logo-img" alt="Logo" @error="handleLogoError">
      <el-upload v-if="isSuperAdmin" :show-file-list="false" :before-upload="beforeUpload" :http-request="uploadLogo" accept="image/png, image/jpeg, image/jpg" class="logo-upload">
        <el-icon class="edit-icon"><Edit /></el-icon>
      </el-upload>
    </div>
    <div class="right-box">
      <el-popover placement="bottom" :width="450" trigger="click">
        <template #reference>
          <div class="notification-box">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0">
              <el-icon class="bell-icon"><Bell /></el-icon>
            </el-badge>
          </div>
        </template>
        <div class="notification-header">
          <span class="notification-title">通知中心</span>
          <div class="header-actions">
            <el-button link type="primary" @click="toggleEditMode">{{ editMode ? '取消' : '更多' }}</el-button>
            <template v-if="editMode">
              <el-button link type="primary" @click="toggleSelectAll">{{ selectAll ? '取消全选' : '全选' }}</el-button>
              <el-button link type="danger" @click="deleteSelected" :disabled="selectedIds.length === 0">删除选中</el-button>
            </template>
            <el-button link type="primary" @click="readAll">全部已读</el-button>
          </div>
        </div>
        <div v-if="notifications.length > 0" class="notification-list">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: item.isRead === 0, selected: editMode && selectedIds.includes(item.id) }"
            @click="editMode ? null : readNotification(item)"
          >
            <el-checkbox v-if="editMode" v-model="selectedIds" :label="item.id" @click.stop class="select-checkbox" />
            <div class="avatar-area">
              <el-avatar :size="40" :src="getAvatarUrl(item.avatar)"><el-icon><User /></el-icon></el-avatar>
            </div>
            <div class="content-area">
              <div class="sender-name">{{ item.senderName || '系统' }}</div>
              <div v-if="item.taskTitle" class="task-info">{{ item.projectName }} ｜ {{ item.taskTitle }}</div>
              <div class="message-content">{{ formatMessage(item) }}</div>
              <div class="notification-time">{{ item.createTime }}</div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无通知" :image-size="80" />
      </el-popover>
      <div class="user-info">
        <el-button type="danger" plain size="small" @click="logout">退出</el-button>
      </div>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Bell, Edit, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import websocket from '../../utils/websocket'
import { useUserStore } from '../../store/user'
import { uploadLogoApi } from '../../api/logo'

const router = useRouter()
const userStore = useUserStore()

const isSuperAdmin = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员'
})

const logoUrl = ref('')
const fetchLogoUrl = () => {
  logoUrl.value = '/api/logo/image?t=' + Date.now()
}
const handleLogoError = () => {
  console.error('Logo加载失败:', logoUrl.value)
}
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片不能超过5MB')
    return false
  }
  return true
}
const uploadLogo = async (options: any) => {
  try {
    const res: any = await uploadLogoApi(options.file)
    if (res.code === 200) {
      ElMessage.success('Logo更新成功')
      fetchLogoUrl()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  }
}

// 通知相关
const unreadCount = ref(0)
const notifications = ref<any[]>([])
const selectedIds = ref<number[]>([])
const editMode = ref(false)

const loadNotifications = async () => {
  try {
    const res: any = await request({ url: '/notification/my', method: 'get' })
    notifications.value = res.data || []
    if (!editMode.value) selectedIds.value = []
  } catch (error) {
    console.error(error)
  }
}
const loadUnreadCount = async () => {
  try {
    const res: any = await request({ url: '/notification/unread/count', method: 'get' })
    unreadCount.value = res.data || 0
  } catch (error) {
    console.error(error)
  }
}
const readNotification = async (item: any) => {
  try {
    if (item.isRead === 0) {
      await request({ url: `/notification/read/${item.id}`, method: 'put' })
      item.isRead = 1
      loadUnreadCount()
    }
    if (item.type === 'TASK_ASSIGN' || item.type === 'TASK_COMMENT' || item.type === 'TASK_FILE') {
      router.push(`/task/detail/${item.businessId}`)
    }
  } catch (error) {
    console.error(error)
  }
}
const readAll = async () => {
  try {
    await request({ url: '/notification/read/all', method: 'put' })
    notifications.value.forEach(item => { item.isRead = 1 })
    unreadCount.value = 0
    ElMessage.success('全部已读')
  } catch (error) {
    console.error(error)
  }
}
const toggleEditMode = () => {
  editMode.value = !editMode.value
  if (!editMode.value) selectedIds.value = []
}
const selectAll = computed(() => {
  return notifications.value.length > 0 && selectedIds.value.length === notifications.value.length
})
const toggleSelectAll = () => {
  if (selectAll.value) selectedIds.value = []
  else selectedIds.value = notifications.value.map(item => item.id)
}
const deleteSelected = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await request({ url: '/notification/delete', method: 'delete', data: selectedIds.value })
    ElMessage.success(`已删除 ${selectedIds.value.length} 条通知`)
    editMode.value = false
    selectedIds.value = []
    loadNotifications()
    loadUnreadCount()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 实时消息回调：重新拉取通知列表
const messageListener = () => {
  console.log('通知中心收到实时消息，刷新列表')
  loadNotifications()
  loadUnreadCount()
}

const logout = () => {
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/login')
}

const getAvatarUrl = (avatar: string) => {
  if (avatar && avatar.startsWith('/uploads')) {
    return `/api${avatar}?t=${Date.now()}`
  }
  return ''
}

const formatMessage = (item: any) => {
  let content = item.content || ''
  if (item.type === 'QUESTION' || item.type === 'REPLY') {
    const colonIndex = content.indexOf('：')
    if (colonIndex !== -1 && colonIndex + 1 < content.length) {
      return content.substring(colonIndex + 1)
    }
  }
  if (item.type === 'TASK_CREATE') {
    return content.replace(/你有一个新的任务：/, '')
  }
  if (item.type === 'TASK_STATUS') {
    return content.replace(/任务状态已更新：/, '')
  }
  return content
}

// 确保 WebSocket 连接
const ensureWebSocket = () => {
  if (userStore.info?.id) {
    websocket.connect(userStore.info.id)
  }
}

// ========== 方案一：轮询机制 ==========
let pollingTimer: ReturnType<typeof setInterval> | null = null
let visibilityTimer: ReturnType<typeof setTimeout> | null = null

const startPolling = () => {
  if (pollingTimer) clearInterval(pollingTimer)
  pollingTimer = setInterval(() => {
    // 仅当页面可见且已登录时才拉取，减少无效请求
    if (document.visibilityState === 'visible' && userStore.token) {
      loadNotifications()
      loadUnreadCount()
    }
  }, 30000) // 30秒
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// 页面可见性变化时立即拉取一次
const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible' && userStore.token) {
    // 延迟一点点，避免频繁请求
    if (visibilityTimer) clearTimeout(visibilityTimer)
    visibilityTimer = setTimeout(() => {
      loadNotifications()
      loadUnreadCount()
    }, 200)
  }
}
// ===================================

onMounted(() => {
  fetchLogoUrl()
  loadNotifications()
  loadUnreadCount()
  ensureWebSocket()
  websocket.addMessageListener(messageListener)
  
  // 启动轮询和监听页面可见性
  startPolling()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  websocket.removeMessageListener(messageListener)
  stopPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (visibilityTimer) clearTimeout(visibilityTimer)
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: #409EFF;
  color: #fff;
}
.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}
.logo-img {
  height: 40px;
  max-width: 150px;
  object-fit: contain;
  background: #fff;
  border-radius: 4px;
  padding: 4px;
}
.logo-upload {
  cursor: pointer;
}
.edit-icon {
  font-size: 20px;
  color: #fff;
  background: rgba(0,0,0,0.3);
  border-radius: 50%;
  padding: 4px;
  transition: 0.3s;
}
.edit-icon:hover {
  background: rgba(0,0,0,0.6);
}
.right-box {
  display: flex;
  align-items: center;
  gap: 20px;
}
.notification-box {
  cursor: pointer;
}
.bell-icon {
  font-size: 24px;
  color: #fff;
}
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.notification-title {
  font-size: 16px;
  font-weight: bold;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.notification-list {
  max-height: 420px;
  overflow-y: auto;
}
.notification-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 10px;
  border: 1px solid #eee;
  cursor: pointer;
  position: relative;
  transition: 0.3s;
  background: white;
  align-items: flex-start;
}
.notification-item:hover {
  background: #f5f7fa;
}
.notification-item.unread {
  background: #ecf5ff;
  border-color: #409EFF;
}
.notification-item.selected {
  background: #f0f9eb;
  border-color: #67c23a;
}
.select-checkbox {
  margin-top: 10px;
  flex-shrink: 0;
}
.avatar-area {
  flex-shrink: 0;
}
.content-area {
  flex: 1;
  min-width: 0;
}
.sender-name {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}
.task-info {
  font-size: 11px;
  color: #909399;
  margin-bottom: 6px;
  line-height: 1.4;
}
.message-content {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 6px;
  background: #f0f2f5;
  padding: 8px 12px;
  border-radius: 16px;
  display: inline-block;
  max-width: 100%;
  word-wrap: break-word;
  white-space: normal;
}
.notification-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 2px;
}
.user-info {
  display: flex;
  align-items: center;
}
</style>