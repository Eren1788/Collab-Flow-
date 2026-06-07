<template>
  <el-header height="60px" class="header">
    <div class="header-left">
      <span class="header-brand">Collab Flow</span>
    </div>
    <div class="right-box">
      <el-popover placement="bottom-end" :width="420" trigger="click" :popper-style="{ padding: '0' }" @show="onNotifOpen">
        <template #reference>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notif-badge">
            <el-button circle size="small" class="header-icon-btn">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
        </template>
        <div class="notif-popover">
          <div class="notif-header">
            <span class="notif-title">通知中心</span>
            <div class="notif-actions">
              <template v-if="editMode">
                <el-button class="notif-action-btn" size="small" @click="toggleSelectAll">{{ selectAll ? '取消全选' : '全选' }}</el-button>
                <el-button class="notif-action-btn is-danger" size="small" :disabled="selectedIds.length === 0" @click="deleteSelected"><el-icon><Delete /></el-icon>删除{{ selectedIds.length ? ` (${selectedIds.length})` : '' }}</el-button>
                <el-button class="notif-action-btn" size="small" @click="editMode = false; selectedIds = []"><el-icon><Close /></el-icon>完成</el-button>
              </template>
              <template v-else>
                <el-button class="notif-action-btn" size="small" @click="toggleEditMode"><el-icon><EditPen /></el-icon>管理</el-button>
                <el-button class="notif-action-btn is-primary" size="small" @click="readAll"><el-icon><Check /></el-icon>全部已读</el-button>
              </template>
            </div>
          </div>
          <div class="notif-list" v-if="notifications.length > 0">
            <div
              v-for="item in notifications"
              :key="item.id"
              class="notif-item"
              :class="{ 'is-unread': item.isRead === 0, 'is-selected': editMode && selectedIds.includes(item.id) }"
              @click="editMode ? null : readNotification(item)"
            >
              <el-checkbox v-if="editMode" v-model="selectedIds" :label="item.id" @click.stop class="notif-checkbox" />
              <el-avatar :size="36" :src="getAvatarUrl(item.avatar)" class="notif-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="notif-body">
                <div class="notif-sender">{{ ['TASK_CREATE','TASK_ASSIGN','TASK_STATUS'].includes(item.type) ? '系统' : (item.senderName || '系统') }}</div>
                <div class="notif-text">{{ formatMessage(item) }}</div>
                <div class="notif-time">{{ item.createTime }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无通知" :image-size="60" style="padding: 40px 0" />
        </div>
      </el-popover>

      <!-- Logo 上传（管理员） -->
      <el-upload v-if="isSuperAdmin" :show-file-list="false" :before-upload="beforeUpload" :http-request="uploadLogo" accept="image/png, image/jpeg, image/jpg" class="header-icon-btn-wrapper">
        <el-tooltip content="上传 Logo" placement="bottom">
          <el-button circle size="small" class="header-icon-btn">
            <el-icon><PictureFilled /></el-icon>
          </el-button>
        </el-tooltip>
      </el-upload>

      <!-- 退出 -->
      <el-tooltip content="退出登录" placement="bottom">
        <el-button circle size="small" class="header-icon-btn" @click="logout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </el-tooltip>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Bell, User, Delete, Close, EditPen, Check, PictureFilled, SwitchButton } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import websocket from '../../utils/websocket'
import { useUserStore } from '../../store/user'
import { uploadLogoApi } from '../../api/logo'
import { getProjectPageApi } from '../../api/project'
import { getProjectCommentListApi } from '../../api/comment'

const router = useRouter()
const userStore = useUserStore()

const isSuperAdmin = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员'
})

// 添加通知并立即去重（防止同一来源多次添加导致重复）
const notifKey = (n: any) => {
  const s = (n.content || '').replace(/^.*?[：:]\s*/, '').replace(/^\[DM:\d+\]\s*/, '')
  return `${n.senderName}_${s}`
}
const dedupNotifs = () => {
  const seen = new Set<string>()
  const deduped: any[] = []
  for (const n of notifications.value) {
    const key = notifKey(n)
    if (!seen.has(key)) { seen.add(key); deduped.push(n) }
  }
  notifications.value = deduped
  unreadCount.value = deduped.filter((n: any) => n.isRead === 0).length
}
const addNotif = (n: any) => {
  // 先检查是否已有相同内容的通知
  const key = notifKey(n)
  for (const existing of notifications.value) {
    if (notifKey(existing) === key) return // 已存在，跳过
  }
  notifications.value.unshift(n)
  if (!n.isRead) unreadCount.value++
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
    const raw: any[] = (res.data || []).filter((n: any) => {
      // 私聊通知只显示给接收者
      const c = n.content || ''
      if (c.indexOf('[DM:') !== -1) {
        const dmId = Number(c.match(/\[DM:(\d+)\]/)?.[1])
        if (dmId && dmId !== userStore.info?.id) return false
      }
      return true
    })
    // 后端可能存了重复通知，按 发送者+归一化内容 去重
    const seen = new Set<string>()
    const apiNotifs: any[] = []
    for (const n of raw) {
      const rawContent = n.content || ''
      // 归一化：去掉后端加的前缀 和 [DM:xxx] 标记，统一比对
      const normalized = rawContent.replace(/^.*?[：:]\s*/, '').replace(/^\[DM:\d+\]\s*/, '')
      const key = `${n.senderName || ''}_${normalized}`
      if (!seen.has(key)) { seen.add(key); apiNotifs.push(n) }
    }
    // 保留本地轮询/WebSocket添加的通知（id 以 poll_ 或 local_ 开头）
    const localNotifs = notifications.value.filter((n: any) =>
      typeof n.id === 'string' && (n.id.startsWith('poll_') || n.id.startsWith('local_') || n.id.startsWith('pa_') || n.id.startsWith('pr_')))
    // 去重：API 已有内容的不保留本地版
    const norm = (s: string) => (s || '').replace(/^.*?[：:]\s*/, '').replace(/^\[DM:\d+\]\s*/, '')
    const apiKeys = new Set(apiNotifs.map((n: any) => `${n.senderName}_${norm(n.content)}`))
    const keepLocal = localNotifs.filter((n: any) => !apiKeys.has(`${n.senderName}_${norm(n.content)}`))
    notifications.value = [...keepLocal, ...apiNotifs]
    dedupNotifs() // 最终去重保险
    if (!editMode.value) selectedIds.value = []
  } catch (error) {
    console.error(error)
  }
}
const loadUnreadCount = () => {
  // 从未读数从去重后的列表计算，不与 API 耦合
  unreadCount.value = notifications.value.filter((n: any) => n.isRead === 0).length
}
const readNotification = async (item: any) => {
  try {
    // 本地通知（id 为字符串）不调后端 API，只本地标记已读
    const isLocal = typeof item.id === 'string'
    if (item.isRead === 0 && !isLocal) {
      await request({ url: `/notification/read/${item.id}`, method: 'put' })
    }
    item.isRead = 1
    loadUnreadCount()
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
    // 只传数字ID给后端，字符串ID（本地通知）直接删掉
    const apiIds = selectedIds.value.filter((id: any) => typeof id === 'number')
    const localIds = selectedIds.value.filter((id: any) => typeof id === 'string')
    if (apiIds.length > 0) {
      await request({ url: '/notification/delete', method: 'delete', data: apiIds })
    }
    if (localIds.length > 0) {
      notifications.value = notifications.value.filter((n: any) => !localIds.includes(n.id))
    }
    ElMessage.success(`已删除 ${selectedIds.value.length} 条通知`)
    editMode.value = false
    selectedIds.value = []
    loadNotifications()
    loadUnreadCount()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 实时消息回调：收到新消息时刷新通知列表
const pendingNotifs: any[] = []
const messageListener = async (data?: any) => {
  // 缓存 WebSocket 推送的项目评论，等 API 返回后对比补漏
  if (data?.type === 'NEW_PROJECT_COMMENT') {
    const src = data.comment || data
    if (src.userId && src.userId !== userStore.info?.id) {
      // 私聊：只缓存发送者和接收者的消息
      const rawContent = src.content || ''
      if (rawContent.indexOf('[DM:') !== -1) {
        const dmId = Number(rawContent.match(/\[DM:(\d+)\]/)?.[1])
        if (dmId && dmId !== userStore.info?.id) return
      }
      pendingNotifs.push({
        senderName: src.nickname || src.userName || src.username || '未知',
        content: rawContent,
        projectName: data.projectName || '',
        avatar: src.avatar || '',
        createTime: src.createTime || nowStr()
      })
    }
  }
  // 等 API 加载完成（await 确保不重复）
  await loadNotifications()
  loadUnreadCount()
  // API 没有的（管理员情况）→ 从缓存补上
  if (pendingNotifs.length > 0) {
    const normalize = (s: string) => (s || '').replace(/^.*?[：:]\s*/, '').replace(/^\[DM:\d+\]\s*/, '')
    const existKeys = new Set(notifications.value.map((n: any) => `${n.senderName}_${normalize(n.content)}`))
    const needAdd = pendingNotifs.filter((n: any) => !existKeys.has(`${n.senderName}_${normalize(n.content)}`))
    for (const n of needAdd) {
      addNotif({
        id: `local_${Date.now()}_${Math.random()}`,
        type: 'PROJECT_COMMENT',
        content: n.content.indexOf('[DM:') !== -1 ? n.content.replace(/^.*?\[DM:\d+\]\s*/, '') : n.content,
        senderName: n.senderName,
        projectName: n.projectName || '',
        avatar: n.avatar,
        createTime: n.createTime,
        isRead: 0,
        businessId: null
      })
    }
    pendingNotifs.length = 0
  }
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
  // 私聊消息去掉 [DM:id] 标记（可能在内容开头或中间）
  if (content.indexOf('[DM:') !== -1) {
    return content.replace(/^.*?\[DM:\d+\]\s*/, '')
  }
  if (item.type === 'QUESTION' || item.type === 'REPLY') {
    const colonIndex = content.indexOf('：')
    if (colonIndex !== -1 && colonIndex + 1 < content.length) {
      return content.substring(colonIndex + 1)
    }
  }
  if (item.type === 'TASK_CREATE') {
    // 如果API有taskTitle就用，没有就从内容里提取
    const title = item.taskTitle || content.replace(/你有一个新的任务：/, '')
    return `你被${item.senderName || '管理员'}安排了一个${title}的任务`
  }
  if (item.type === 'TASK_ASSIGN') {
    const title = item.taskTitle || content
    return `你被${item.senderName || '管理员'}指派了任务：${title}`
  }
  if (item.type === 'TASK_STATUS') {
    if (content.includes('取消') || content.includes('删除')) {
      const title = item.taskTitle || content.replace(/.*?(取消|删除).*?[：:]?\s*/, '')
      return `你被${item.senderName || '管理员'}取消了任务：${title}`
    }
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

// 项目评论轮询 + 项目指派检测
let lastCommentCheck = Date.now() - 60000
// 按用户区分 localStorage key（不同用户登录不会串数据）
const pidKey = () => `cf_pids_${userStore.info?.id || ''}`
const pnameKey = () => `cf_pnames_${userStore.info?.id || ''}`
const nowStr = () => new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
let lastProjectIds = localStorage.getItem(pidKey()) || ''
let lastProjectNames: Record<number, string> = JSON.parse(localStorage.getItem(pnameKey()) || '{}')

const checkProjectComments = async () => {
  if (!userStore.token) return
  const seenIds = new Set<number>()
  // 收集现有通知中的 comment id，避免重复添加
  for (const n of notifications.value) {
    const id = Number(n.businessId || n.id)
    if (id) seenIds.add(id)
  }
  try {
    const projRes: any = await getProjectPageApi({ pageNum: 1, pageSize: 100 })
    const projects: any[] = projRes.data?.records || []
    // 检测项目变动（新增 / 移除）
    const curIds = projects.map((p: any) => p.id).sort().join(',')
    if (curIds !== lastProjectIds) {
      // 首次加载或检测到变化（无历史但用户有项目 → 首次登录，也检测）
      if (lastProjectIds || projects.length > 0) {
        const oldList = lastProjectIds.split(',').map(Number).filter(Boolean)
        const oldSet = new Set(oldList)
        const curIdList = projects.map((p: any) => p.id)
        const newOnes = projects.filter((p: any) => !oldSet.has(p.id))
        const removed = oldList.filter((id: number) => !curIdList.includes(id))
        if (newOnes.length > 0) {
          const names = newOnes.map((p: any) => p.name).join('、')
          const key = `pa_${names}`
          if (!notifications.value.some((n: any) => n.id === key)) {
            notifications.value.unshift({
              id: key, type: 'PROJECT_COMMENT',
              content: `你被添加到了新项目：${names}`,
              senderName: '系统', projectName: '', avatar: '',
              createTime: nowStr(), isRead: 0, businessId: null
            })
            unreadCount.value++
            ElMessage.success(`检测到新项目：${names}`)
          }
        }
        // 保存当前项目名映射（用于下次检测移除时获取名称）
        for (const p of projects) { lastProjectNames[p.id] = p.name }
        if (removed.length > 0) {
          const rNames = removed.map((id: number) => lastProjectNames[id] || `项目#${id}`).join('、')
          const key = `pr_${rNames}`
          if (!notifications.value.some((n: any) => n.id === key)) {
            notifications.value.unshift({
              id: key, type: 'PROJECT_COMMENT',
              content: `你被移除了项目：${rNames}`,
              senderName: '系统', projectName: '', avatar: '',
              createTime: nowStr(), isRead: 0, businessId: null
            })
            unreadCount.value++
            ElMessage.warning(`检测到项目移除：${rNames}`)
          }
        }
        localStorage.setItem(pnameKey(), JSON.stringify(lastProjectNames))
      }
      lastProjectIds = curIds
      localStorage.setItem(pidKey(), curIds)
    }
    for (const proj of projects) {
      try {
        const cmtRes: any = await getProjectCommentListApi(proj.id)
        const comments: any[] = cmtRes.data || []
        for (const c of comments) {
          const ct = new Date(c.createTime).getTime()
          if (ct <= lastCommentCheck || c.userId === userStore.info?.id) continue
          if (seenIds.has(c.id)) continue
          seenIds.add(c.id)
          const rawContent = c.content || ''
          const isDm = rawContent.indexOf('[DM:') !== -1
          // 私聊：只推给发送者和接收者
          if (isDm) {
            const dmRecipientId = Number(rawContent.match(/\[DM:(\d+)\]/)?.[1])
            if (dmRecipientId && dmRecipientId !== userStore.info?.id) continue
          }
          const displayContent = isDm ? rawContent.replace(/^.*?\[DM:\d+\]\s*/, '') : `项目聊天室有新消息：${rawContent}`
          // 用归一化内容做去重键，与 loadNotifications 一致
          const normalized = rawContent.replace(/^.*?[：:]\s*/, '').replace(/^\[DM:\d+\]\s*/, '')
          const existKeys = new Set(notifications.value.map((n: any) =>
            `${n.senderName}_${(n.content || '').replace(/^.*?[：:]\s*/, '').replace(/^\[DM:\d+\]\s*/, '')}`
          ))
          if (!existKeys.has(`${c.nickname || c.userName || '未知'}_${normalized}`)) {
            addNotif({
              id: `poll_${c.id}_${Date.now()}`,
              type: 'PROJECT_COMMENT',
              content: displayContent,
              senderName: c.nickname || c.userName || c.username || '未知',
              projectName: proj.name || '',
              avatar: c.avatar || '',
              createTime: c.createTime,
              isRead: 0,
              businessId: proj.id
            })
          }
        }
      } catch { /* 忽略 */ }
    }
    lastCommentCheck = Date.now()
  } catch { /* 忽略 */ }
}

// ========== 轮询机制 ==========
let pollingTimer: ReturnType<typeof setInterval> | null = null
let visibilityTimer: ReturnType<typeof setTimeout> | null = null

const startPolling = () => {
  if (pollingTimer) clearInterval(pollingTimer)
  // 立即执行一次（不等30秒）
  if (document.visibilityState === 'visible' && userStore.token) {
    loadNotifications()
    loadUnreadCount()
    checkProjectComments()
  }
  pollingTimer = setInterval(() => {
    if (document.visibilityState === 'visible' && userStore.token) {
      loadNotifications()
      loadUnreadCount()
      checkProjectComments()
    }
  }, 30000)
}

// 通知弹窗展开时立即检查
const onNotifOpen = () => {
  loadNotifications()
  loadUnreadCount()
  checkProjectComments()
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
  loadNotifications()
  loadUnreadCount()
  ensureWebSocket()
  websocket.addMessageListener(messageListener)
  
  // 启动轮询和监听页面可见性
  startPolling()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  // 延迟再清一次残留重复
  setTimeout(dedupNotifs, 500)
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
  padding: 0 24px;
  background: var(--cf-primary);
  color: #fff;
  border-bottom: none;
}

.header-left { display: flex; align-items: center; }
.header-brand {
  font-family: var(--cf-font);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #fff;
}

.header-icon-btn-wrapper {
  display: inline-flex;
}
.header-icon-btn-wrapper :deep(.el-upload) { display: flex; }

.header-icon-btn {
  border: none;
  background: rgba(255,255,255,0.12);
  color: #fff;
  font-size: 18px;
  transition: var(--cf-transition);
}
.header-icon-btn:hover {
  background: rgba(255,255,255,0.25);
  color: #fff;
}

.right-box {
  display: flex;
  align-items: center;
  gap: 8px;
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
.notif-badge :deep(.el-badge__content) {
  background: var(--cf-danger);
}

/* 通知弹窗样式 */
.notif-popover {
  max-height: 480px;
  display: flex;
  flex-direction: column;
}
.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px 12px;
  border-bottom: 1px solid var(--cf-border-light);
  flex-shrink: 0;
}
.notif-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cf-text-heading);
}
.notif-actions {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.notif-action-btn {
  border: 1px solid var(--cf-border) !important;
  border-radius: 6px !important;
  background: #fff !important;
  color: var(--cf-text-secondary) !important;
  font-size: 12px !important;
  padding: 4px 10px !important;
  height: 28px !important;
  transition: var(--cf-transition) !important;
  white-space: nowrap;
}
.notif-action-btn:hover {
  border-color: var(--cf-primary-border) !important;
  background: var(--cf-primary-bg) !important;
  color: var(--cf-primary) !important;
}
.notif-action-btn .el-icon { margin-right: 3px; font-size: 13px; }
.notif-action-btn.is-primary {
  border-color: var(--cf-primary) !important;
  color: var(--cf-primary) !important;
}
.notif-action-btn.is-primary:hover {
  background: var(--cf-primary) !important;
  color: #fff !important;
}
.notif-action-btn.is-danger {
  border-color: var(--cf-danger) !important;
  color: var(--cf-danger) !important;
}
.notif-action-btn.is-danger:hover:not(:disabled) {
  background: var(--cf-danger) !important;
  color: #fff !important;
}
.notif-action-btn.is-danger:disabled { opacity: 0.4; cursor: not-allowed; }

.notif-list { flex: 1; overflow-y: auto; padding: 8px; }
.notif-item {
  display: flex; gap: 10px; padding: 10px 12px;
  border-radius: 8px; cursor: pointer; transition: var(--cf-transition);
  align-items: flex-start; margin-bottom: 4px;
}
.notif-item:hover { background: var(--cf-bg); }
.notif-item.is-unread { background: var(--cf-primary-bg); }
.notif-item.is-unread:hover { background: #dde4ff; }
.notif-item.is-selected { background: #f0f9eb; }
.notif-checkbox { margin-top: 6px; }
.notif-avatar { flex-shrink: 0; }
.notif-body { flex: 1; min-width: 0; }
.notif-sender { font-size: 13px; font-weight: 600; color: var(--cf-text-heading); margin-bottom: 2px; }
.notif-text {
  font-size: 12px; color: var(--cf-text); line-height: 1.4;
  background: var(--cf-bg); padding: 6px 10px; border-radius: 8px;
  display: inline-block; max-width: 100%; word-break: break-all;
}
.notif-item.is-unread .notif-text { background: #fff; }
.notif-time { font-size: 11px; color: var(--cf-text-muted); margin-top: 4px; }

.header-icon-btn {
  border: none; background: rgba(255,255,255,0.15);
  color: #fff; font-size: 18px; transition: var(--cf-transition);
}
.header-icon-btn:hover { background: rgba(255,255,255,0.3); color: #fff; }
</style>