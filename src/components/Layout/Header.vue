<template>
  <el-header height="60px" class="header">
    <!-- LOGO -->
    <div class="logo">
      Collab Flow
    </div>

    <div class="right-box">
      <!-- 通知中心 -->
      <el-popover
        placement="bottom"
        :width="360"
        trigger="click"
      >
        <template #reference>
          <div class="notification-box">
            <el-badge
              :value="unreadCount"
              :hidden="unreadCount === 0"
            >
              <el-icon class="bell-icon">
                <Bell />
              </el-icon>
            </el-badge>
          </div>
        </template>

        <div class="notification-header">
          <span class="notification-title">通知中心</span>
          <el-button link type="primary" @click="readAll">全部已读</el-button>
        </div>

        <div class="notification-list" v-if="notifications.length > 0">
          <div
            class="notification-item"
            v-for="item in notifications"
            :key="item.id"
            @click="readNotification(item)"
          >
            <div class="notification-content">{{ item.content }}</div>
            <div class="notification-time">{{ item.createTime }}</div>
            <div class="unread-dot" v-if="item.isRead === 0" />
          </div>
        </div>
        <el-empty v-else description="暂无通知" :image-size="80" />
      </el-popover>

      <!-- 用户信息区域：只显示退出按钮，不显示用户名 -->
      <div class="user-info">
        <el-button type="danger" plain size="small" @click="logout">退出</el-button>
      </div>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import websocket from '../../utils/websocket'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()

/**
 * 未读数量
 */
const unreadCount = ref(0)

/**
 * 通知列表
 */
const notifications = ref<any[]>([])

/**
 * 获取通知列表
 */
const loadNotifications = async () => {
  try {
    const res: any = await request({
      url: '/notification/my',
      method: 'get'
    })
    notifications.value = res.data
  } catch (error) {
    console.error(error)
  }
}

/**
 * 获取未读数量
 */
const loadUnreadCount = async () => {
  try {
    const res: any = await request({
      url: '/notification/unread/count',
      method: 'get'
    })
    unreadCount.value = res.data
  } catch (error) {
    console.error(error)
  }
}

/**
 * 已读通知
 */
const readNotification = async (item: any) => {
  if (item.isRead === 1) return
  try {
    await request({
      url: `/notification/read/${item.id}`,
      method: 'put'
    })
    item.isRead = 1
    loadUnreadCount()
  } catch (error) {
    console.error(error)
  }
}

/**
 * 全部已读
 */
const readAll = async () => {
  try {
    await request({
      url: '/notification/read/all',
      method: 'put'
    })
    notifications.value.forEach(item => {
      item.isRead = 1
    })
    unreadCount.value = 0
    ElMessage.success('全部已读')
  } catch (error) {
    console.error(error)
  }
}

/**
 * websocket实时监听
 */
const messageListener = () => {
  console.log('通知中心收到实时消息')
  loadNotifications()
  loadUnreadCount()
}

/**
 * 退出登录
 */
const logout = () => {
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/login')
}

/**
 * 页面初始化
 */
onMounted(() => {
  loadNotifications()
  loadUnreadCount()
  websocket.addMessageListener(messageListener)
})

/**
 * 页面销毁
 */
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

.logo {
  font-size: 22px;
  font-weight: bold;
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
  transition: all 0.3s;
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