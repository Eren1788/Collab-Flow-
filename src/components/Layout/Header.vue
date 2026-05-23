<template>
  <el-header height="60px" class="header">
    <!-- Logo区域：图片 + 上传图标（仅管理员） -->
    <div class="logo-area">
      <img :src="logoUrl" class="logo-img" alt="Logo" @error="handleLogoError" />
      <!-- 上传按钮（仅超级管理员可见） -->
      <el-upload
        v-if="isSuperAdmin"
        :show-file-list="false"
        :before-upload="beforeUpload"
        :http-request="uploadLogo"
        accept="image/png, image/jpeg, image/jpg"
        class="logo-upload"
      >
        <el-icon class="edit-icon"><Edit /></el-icon>
      </el-upload>
    </div>

    <div class="right-box">
      <!-- 通知中心（保持不变） -->
      <el-popover placement="bottom" :width="360" trigger="click">
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
        <div class="notification-list" v-if="notifications.length > 0">
          <div class="notification-item" v-for="item in notifications" :key="item.id" @click="readNotification(item)">
            <div class="notification-content">{{ item.content }}</div>
            <div class="notification-time">{{ item.createTime }}</div>
            <div class="unread-dot" v-if="item.isRead === 0" />
          </div>
        </div>
        <el-empty v-else description="暂无通知" :image-size="80" />
      </el-popover>

      <!-- 退出按钮 -->
      <div class="user-info">
        <el-button type="danger" plain size="small" @click="logout">退出</el-button>
      </div>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Bell, Edit } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import websocket from '../../utils/websocket'
import { useUserStore } from '../../store/user'
// 不再需要 getLogoUrlApi，只保留 uploadLogoApi
import { uploadLogoApi } from '../../api/logo'

const router = useRouter()
const userStore = useUserStore()

// 是否超级管理员
const isSuperAdmin = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员'
})

// Logo URL（直接使用后端图片接口，带时间戳避免缓存）
const logoUrl = ref('')

// 获取Logo图片URL（直接指向 /logo/image 接口）
const fetchLogoUrl = () => {
  // 绝对路径，以 /api 开头
  logoUrl.value = '/api/logo/image?t=' + Date.now()
  console.log('[Header] Logo URL:', logoUrl.value)  // 添加日志查看
}

// 图片加载失败时的处理
const handleLogoError = () => {
  const img = document.querySelector('.logo-img') as HTMLImageElement
  if (img) img.style.display = 'none'
}

// 上传前校验
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB')
    return false
  }
  return true
}

// 自定义上传
const uploadLogo = async (options: any) => {
  try {
    const res: any = await uploadLogoApi(options.file)
    if (res.code === 200) {
      ElMessage.success('Logo更新成功')
      // 重新获取Logo（刷新缓存）
      fetchLogoUrl()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  }
}

// ---------- 通知相关代码（与原代码相同）----------
const unreadCount = ref(0)
const notifications = ref<any[]>([])
const loadNotifications = async () => {
  try {
    const res: any = await request({ url: '/notification/my', method: 'get' })
    notifications.value = res.data
  } catch (error) { console.error(error) }
}
const loadUnreadCount = async () => {
  try {
    const res: any = await request({ url: '/notification/unread/count', method: 'get' })
    unreadCount.value = res.data
  } catch (error) { console.error(error) }
}
const readNotification = async (item: any) => {
  if (item.isRead === 1) return
  try {
    await request({ url: `/notification/read/${item.id}`, method: 'put' })
    item.isRead = 1
    loadUnreadCount()
  } catch (error) { console.error(error) }
}
const readAll = async () => {
  try {
    await request({ url: '/notification/read/all', method: 'put' })
    notifications.value.forEach(item => item.isRead = 1)
    unreadCount.value = 0
    ElMessage.success('全部已读')
  } catch (error) { console.error(error) }
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
  gap: 8px;
  position: relative;
}

.logo-img {
  height: 40px;
  max-width: 150px;
  object-fit: contain;
  background: white;
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
  transition: all 0.3s;
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
  margin-bottom: 10px;
}
.notification-title {
  font-size: 16px;
  font-weight: bold;
}
.notification-list {
  max-height: 400px;
  overflow-y: auto;
}
.notification-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  position: relative;
  transition: all .3s;
}
.notification-item:hover {
  background: #f5f7fa;
}
.notification-content {
  font-size: 14px;
  color: #333;
  margin-bottom: 6px;
}
.notification-time {
  font-size: 12px;
  color: #999;
}
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: red;
  position: absolute;
  right: 10px;
  top: 18px;
}
.user-info {
  display: flex;
  align-items: center;
}
</style>