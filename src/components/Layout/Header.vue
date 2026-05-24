<template>
  <el-header height="60px" class="header">

    <!-- Logo区域 -->
    <div class="logo-area">
      <img :src="logoUrl" class="logo-img" alt="Logo" @error="handleLogoError">
      <el-upload v-if="isSuperAdmin" :show-file-list="false" :before-upload="beforeUpload" :http-request="uploadLogo" accept="image/png, image/jpeg, image/jpg" class="logo-upload">
        <el-icon class="edit-icon"><Edit /></el-icon>
      </el-upload>
    </div>

    <div class="right-box">

      <!-- 通知中心 -->
      <el-popover placement="bottom" :width="420" trigger="click">
        <template #reference>
          <div class="notification-box">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0">
              <el-icon class="bell-icon"><Bell /></el-icon>
            </el-badge>
          </div>
        </template>

        <div class="notification-header">
          <span class="notification-title">通知中心</span>
          <el-button link type="primary" @click="readAll">全部已读</el-button>
        </div>

        <!-- 通知列表（美化后） -->
        <div v-if="notifications.length > 0" class="notification-list">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: item.isRead === 0 }"
            @click="readNotification(item)"
          >
            <!-- 左侧头像 -->
            <div class="avatar-area">
              <el-avatar :size="40" :src="getAvatarUrl(item.avatar)">
                <el-icon><User /></el-icon>
              </el-avatar>
            </div>
            <!-- 右侧内容 -->
            <div class="content-area">
              <!-- 发送人姓名 -->
              <div class="sender-name">{{ item.senderName || '系统' }}</div>
              <!-- 项目+任务信息（移到姓名下方） -->
              <div v-if="item.taskTitle" class="task-info">
                {{ item.projectName }} ｜ {{ item.taskTitle }}
              </div>
              <!-- 消息内容（气泡样式） -->
              <div class="message-content">{{ formatMessage(item) }}</div>
              <!-- 时间 -->
              <div class="notification-time">{{ item.createTime }}</div>
            </div>
            <!-- 未读红点 -->
            <div v-if="item.isRead === 0" class="unread-dot" />
          </div>
        </div>
        <el-empty v-else description="暂无通知" :image-size="80" />
      </el-popover>

      <!-- 退出 -->
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

const loadNotifications = async () => {
  try {
    const res: any = await request({ url: '/notification/my', method: 'get' })
    notifications.value = res.data || []
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
const messageListener = () => {
  console.log('通知中心收到实时消息')
  loadNotifications()
  loadUnreadCount()
}
const logout = () => {
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/login')
}

// 辅助函数
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

onMounted(() => {
  fetchLogoUrl()
  loadNotifications()
  loadUnreadCount()
  websocket.addMessageListener(messageListener)
})
onUnmounted(() => {
  websocket.removeMessageListener(messageListener)
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
.notification-list {
  max-height: 420px;
  overflow-y: auto;
}
.notification-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 10px;
  border: 1px solid #eee;
  cursor: pointer;
  position: relative;
  transition: 0.3s;
  background: white;
}
.notification-item:hover {
  background: #f5f7fa;
}
.notification-item.unread {
  background: #ecf5ff;
  border-color: #409EFF;
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
/* 消息内容的气泡样式 */
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
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  position: absolute;
  right: 12px;
  top: 18px;
}
.user-info {
  display: flex;
  align-items: center;
}
</style>