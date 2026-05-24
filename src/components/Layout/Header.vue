<template>
  <el-header height="60px" class="header">

    <!-- Logo区域 -->
    <div class="logo-area">

      <img
        :src="logoUrl"
        class="logo-img"
        alt="Logo"
        @error="handleLogoError"
      >

      <!-- 上传按钮 -->
      <el-upload
        v-if="isSuperAdmin"
        :show-file-list="false"
        :before-upload="beforeUpload"
        :http-request="uploadLogo"
        accept="image/png, image/jpeg, image/jpg"
        class="logo-upload"
      >

        <el-icon class="edit-icon">

          <Edit />

        </el-icon>

      </el-upload>

    </div>

    <div class="right-box">

      <!-- 通知中心 -->
      <el-popover
        placement="bottom"
        :width="420"
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

        <!-- 通知头部 -->
        <div class="notification-header">

          <span class="notification-title">

            通知中心

          </span>

          <el-button
            link
            type="primary"
            @click="readAll"
          >
            全部已读
          </el-button>

        </div>

        <!-- 通知列表 -->
        <div
          v-if="notifications.length > 0"
          class="notification-list"
        >

          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread:item.isRead === 0 }"
            @click="readNotification(item)"
          >

            <div class="notification-content">

              {{ item.content }}

            </div>

            <div class="notification-time">

              {{ item.createTime }}

            </div>

            <div
              v-if="item.isRead === 0"
              class="unread-dot"
            />

          </div>

        </div>

        <el-empty
          v-else
          description="暂无通知"
          :image-size="80"
        />

      </el-popover>

      <!-- 退出 -->
      <div class="user-info">

        <el-button
          type="danger"
          plain
          size="small"
          @click="logout"
        >
          退出
        </el-button>

      </div>

    </div>

  </el-header>
</template>

<script setup lang="ts">

import {

  ref,
  onMounted,
  onUnmounted,
  computed

} from 'vue'

import {

  Bell,
  Edit

} from '@element-plus/icons-vue'

import {

  useRouter

} from 'vue-router'

import {

  ElMessage

} from 'element-plus'

import request from '../../utils/request'

import websocket from '../../utils/websocket'

import { useUserStore } from '../../store/user'

import { uploadLogoApi } from '../../api/logo'

const router = useRouter()

const userStore = useUserStore()

/**
 * 是否超级管理员
 */
const isSuperAdmin = computed(()=>{

  const info = userStore.info

  return info?.roleId === 1 || info?.roleName === '超级管理员'
})

/**
 * Logo
 */
const logoUrl = ref('')

/**
 * 获取Logo
 */
const fetchLogoUrl = ()=>{

  logoUrl.value = '/api/logo/image?t=' + Date.now()

  console.log('[Header] Logo URL =>', logoUrl.value)
}

/**
 * Logo加载失败
 */
const handleLogoError = ()=>{

  console.error('Logo加载失败:',logoUrl.value)
}

/**
 * 上传前校验
 */
const beforeUpload = (file:File)=>{

  const isImage = file.type.startsWith('image/')

  const isLt5M = file.size / 1024 / 1024 < 5

  if(!isImage){

    ElMessage.error('只能上传图片')

    return false
  }

  if(!isLt5M){

    ElMessage.error('图片不能超过5MB')

    return false
  }

  return true
}

/**
 * 上传Logo
 */
const uploadLogo = async (options:any)=>{

  try{

    const res:any = await uploadLogoApi(options.file)

    if(res.code === 200){

      ElMessage.success('Logo更新成功')

      fetchLogoUrl()

    }else{

      ElMessage.error(res.message || '上传失败')
    }

  }catch(error:any){

    ElMessage.error(error.message || '上传失败')
  }
}

/**
 * 通知
 */
const unreadCount = ref(0)

const notifications = ref<any[]>([])

/**
 * 加载通知
 */
const loadNotifications = async ()=>{

  try{

    const res:any = await request({

      url:'/notification/my',

      method:'get'
    })

    notifications.value = res.data || []

  }catch(error){

    console.error(error)
  }
}

/**
 * 未读数量
 */
const loadUnreadCount = async ()=>{

  try{

    const res:any = await request({

      url:'/notification/unread/count',

      method:'get'
    })

    unreadCount.value = res.data || 0

  }catch(error){

    console.error(error)
  }
}

/**
 * 点击通知
 */
const readNotification = async (item:any)=>{

  try{

    /**
     * 未读 -> 已读
     */
    if(item.isRead === 0){

      await request({

        url:`/notification/read/${item.id}`,

        method:'put'
      })

      item.isRead = 1

      loadUnreadCount()
    }

    /**
     * 跳转业务
     */
    if(
      item.type === 'TASK_ASSIGN'
      ||
      item.type === 'TASK_COMMENT'
      ||
      item.type === 'TASK_FILE'
    ){

      router.push(`/task/detail/${item.businessId}`)
    }

  }catch(error){

    console.error(error)
  }
}

/**
 * 全部已读
 */
const readAll = async ()=>{

  try{

    await request({

      url:'/notification/read/all',

      method:'put'
    })

    notifications.value.forEach(item=>{

      item.isRead = 1
    })

    unreadCount.value = 0

    ElMessage.success('全部已读')

  }catch(error){

    console.error(error)
  }
}

/**
 * websocket监听
 */
const messageListener = ()=>{

  console.log('通知中心收到实时消息')

  loadNotifications()

  loadUnreadCount()
}

/**
 * 退出登录
 */
const logout = ()=>{

  userStore.logout()

  ElMessage.success('已退出')

  router.push('/login')
}

onMounted(()=>{

  /**
   * Logo
   */
  fetchLogoUrl()

  /**
   * 通知
   */
  loadNotifications()

  loadUnreadCount()

  /**
   * websocket
   */
  websocket.addMessageListener(messageListener)
})

onUnmounted(()=>{

  websocket.removeMessageListener(messageListener)
})

</script>

<style scoped>

.header{

  display:flex;

  justify-content:space-between;

  align-items:center;

  padding:0 20px;

  background:#409EFF;

  color:#fff;
}

/**
 * Logo区域
 */
.logo-area{

  display:flex;

  align-items:center;

  gap:10px;

  position:relative;
}

.logo-img{

  height:40px;

  max-width:150px;

  object-fit:contain;

  background:#fff;

  border-radius:4px;

  padding:4px;
}

.logo-upload{

  cursor:pointer;
}

.edit-icon{

  font-size:20px;

  color:#fff;

  background:rgba(0,0,0,0.3);

  border-radius:50%;

  padding:4px;

  transition:0.3s;
}

.edit-icon:hover{

  background:rgba(0,0,0,0.6);
}

/**
 * 右侧
 */
.right-box{

  display:flex;

  align-items:center;

  gap:20px;
}

/**
 * 通知
 */
.notification-box{

  cursor:pointer;
}

.bell-icon{

  font-size:24px;

  color:#fff;
}

.notification-header{

  display:flex;

  justify-content:space-between;

  align-items:center;

  margin-bottom:12px;
}

.notification-title{

  font-size:16px;

  font-weight:bold;
}

.notification-list{

  max-height:420px;

  overflow-y:auto;
}

.notification-item{

  padding:12px;

  border-radius:8px;

  margin-bottom:10px;

  border:1px solid #eee;

  cursor:pointer;

  position:relative;

  transition:0.3s;
}

.notification-item:hover{

  background:#f5f7fa;
}

.notification-item.unread{

  background:#ecf5ff;

  border-color:#409EFF;
}

.notification-content{

  font-size:14px;

  color:#333;

  line-height:22px;

  margin-bottom:8px;
}

.notification-time{

  font-size:12px;

  color:#999;
}

.unread-dot{

  width:8px;

  height:8px;

  border-radius:50%;

  background:red;

  position:absolute;

  right:12px;

  top:18px;
}

.user-info{

  display:flex;

  align-items:center;
}

</style>