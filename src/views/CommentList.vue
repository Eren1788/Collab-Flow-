<template>
  <div class="chat-wrapper">
    <!-- 左侧：群聊列表面板 (QQ风格) -->
    <div class="chat-sidebar">
      <div class="sidebar-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索群聊"
          size="small"
          clearable
          :prefix-icon="Search"
        />
      </div>
      <div class="sidebar-list" v-loading="loadingProjects">
        <div
          v-for="project in filteredProjectList"
          :key="project.id"
          class="chat-item"
          :class="{ active: currentProject?.id === project.id }"
          @click="selectProject(project)"
        >
          <el-avatar :size="36" class="chat-item-avatar">
            <el-icon><ChatDotRound /></el-icon>
          </el-avatar>
          <div class="chat-item-info">
            <div class="chat-item-top">
              <span class="chat-item-name">{{ project.name }}</span>
              <el-badge :value="project.unreadCount" :hidden="!project.unreadCount" />
            </div>
            <div class="chat-item-desc">{{ project.description || '' }}</div>
          </div>
        </div>
        <el-empty v-if="filteredProjectList.length === 0 && !loadingProjects" description="暂无群聊" :image-size="60" />
      </div>
    </div>

    <!-- 中间：聊天区域 + 右侧：成员面板 -->
    <template v-if="currentProject">
      <div class="chat-main">
        <!-- 顶部栏 -->
        <div class="chat-main-header">
          <span class="chat-main-title">{{ currentProject.name }}</span>
          <span class="chat-main-desc">{{ currentProject.description }}</span>
          <el-button
            :icon="showMembers ? ArrowRight : ArrowLeft"
            link
            size="small"
            class="toggle-members-btn"
            @click="showMembers = !showMembers"
          />
        </div>

        <!-- 消息列表 (含日期分隔线) -->
        <div class="chat-main-messages" ref="messageListRef" v-loading="loadingMessages">
          <template v-for="item in groupedMessages" :key="item.key">
            <div v-if="item.isDivider" class="message-divider">
              <span>{{ item.label }}</span>
            </div>
            <div v-else class="message-item" :class="{ 'is-self': isSelf(item) }">
              <el-avatar :size="32" :src="getAvatarUrl(item.avatar)" class="msg-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="msg-body">
                <div class="msg-header">
                  <span class="msg-nickname">{{ item.nickname || '匿名' }}</span>
                </div>
                <div class="msg-bubble">
                  <div class="msg-content" v-html="formatMessage(item.content)"></div>
                </div>
                <div class="msg-actions" v-if="canDelete(item)">
                  <el-button link type="danger" size="small" @click="deleteComment(item.id)">删除</el-button>
                </div>
              </div>
              <div class="msg-time">{{ formatMsgTime(item.createTime) }}</div>
            </div>
          </template>
          <el-empty v-if="messageList.length === 0 && !loadingMessages" description="暂无消息，发一条开始聊天吧~" />
        </div>

        <!-- 输入区域 -->
        <div class="chat-main-input">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="3"
            placeholder="输入消息... (Ctrl+Enter 发送)"
            resize="none"
            @keyup.ctrl.enter="sendComment"
          />
          <div class="input-toolbar">
            <el-upload
              :action="uploadUrl"
              :headers="headers"
              :data="{ projectId: currentProject.id }"
              :show-file-list="false"
              :on-success="handleFileUploadSuccess"
              :before-upload="beforeFileUpload"
            >
              <el-button size="small" :icon="Picture" link>图片</el-button>
            </el-upload>
            <el-button type="primary" size="small" @click="sendComment" :loading="sending">发送</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：群成员面板 (QQ风格，可收起) -->
      <div class="chat-members" v-if="showMembers">
        <div class="members-header">
          <span>群成员</span>
          <span class="members-count">{{ memberList.length }}人</span>
        </div>
        <div class="members-list" v-loading="loadingMembers">
          <div v-for="member in memberList" :key="member.userId" class="member-item">
            <el-avatar :size="32" :src="getAvatarUrl(member.avatar)">
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="member-info">
              <div class="member-name">{{ member.nickname || member.username }}</div>
              <div class="member-role">{{ getRoleLabel(member) }}</div>
            </div>
          </div>
          <el-empty v-if="memberList.length === 0 && !loadingMembers" description="暂无成员" :image-size="60" />
        </div>
      </div>
    </template>

    <!-- 未选项目时的占位 -->
    <div class="chat-placeholder" v-else>
      <el-icon :size="56" color="#ccc"><ChatDotRound /></el-icon>
      <p>选择一个群聊开始聊天</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Picture, ChatDotRound, Search, ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import { getProjectPageApi, getProjectMemberListApi } from '../api/project'
import { getProjectCommentListApi, addProjectCommentApi, deleteCommentApi } from '../api/comment'
import { useUserStore } from '../store/user'
import websocket from '../utils/websocket'

const userStore = useUserStore()
const currentUser = computed(() => userStore.info)

// ===== 项目列表 =====
const projectList = ref<any[]>([])
const loadingProjects = ref(false)

// ===== 当前选中的项目 =====
const currentProject = ref<any>(null)

// ===== 搜索关键词 =====
const searchKeyword = ref('')

// ===== 根据搜索关键词过滤项目列表 =====
const filteredProjectList = computed(() => {
  if (!searchKeyword.value.trim()) return projectList.value
  const keyword = searchKeyword.value.toLowerCase()
  return projectList.value.filter(
    (p) =>
      p.name?.toLowerCase().includes(keyword) ||
      p.description?.toLowerCase().includes(keyword)
  )
})

// ===== 评论列表 =====
const messageList = ref<any[]>([])
const loadingMessages = ref(false)

// ===== 按日期分组消息，插入日期分隔线 =====
const groupedMessages = computed(() => {
  const result: any[] = []
  let lastDate = ''
  for (const msg of messageList.value) {
    const dateStr = getDateLabel(msg.createTime)
    if (dateStr !== lastDate) {
      result.push({ isDivider: true, label: dateStr, key: `div-${dateStr}` })
      lastDate = dateStr
    }
    result.push({ ...msg, isDivider: false, key: `msg-${msg.id}` })
  }
  return result
})

// ===== 输入框 =====
const newComment = ref('')
const sending = ref(false)

// ===== 成员列表 =====
const memberList = ref<any[]>([])
const loadingMembers = ref(false)

// ===== 右侧成员面板显隐开关 =====
const showMembers = ref(true)

const messageListRef = ref<HTMLElement>()

// ===== 上传配置 =====
const uploadUrl = '/api/file/upload'
const headers = {
  Authorization: `Bearer ${localStorage.getItem('token')}`
}

// ===== 格式化消息时间 =====
const formatMsgTime = (time: string) => {
  if (!time) return ''
  try {
    const d = new Date(time.replace(/-/g, '/'))
    const hours = d.getHours().toString().padStart(2, '0')
    const minutes = d.getMinutes().toString().padStart(2, '0')
    return `${hours}:${minutes}`
  } catch {
    return time
  }
}

// ===== 获取日期标签 =====
const getDateLabel = (time: string) => {
  if (!time) return ''
  try {
    const d = new Date(time.replace(/-/g, '/'))
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const msgDate = new Date(d.getFullYear(), d.getMonth(), d.getDate())
    const diffDays = Math.floor((today.getTime() - msgDate.getTime()) / (1000 * 60 * 60 * 24))

    if (diffDays === 0) return '今天'
    if (diffDays === 1) return '昨天'
    if (diffDays === 2) return '前天'
    const year = d.getFullYear()
    const month = (d.getMonth() + 1).toString().padStart(2, '0')
    const day = d.getDate().toString().padStart(2, '0')
    if (year === now.getFullYear()) return `${month}-${day}`
    return `${year}-${month}-${day}`
  } catch {
    return ''
  }
}

// ===== 获取角色中文标签 =====
const getRoleLabel = (member: any) => {
  if (member.roleName) return member.roleName
  if (member.role === 'admin') return '管理员'
  if (member.roleId === 1) return '超级管理员'
  if (member.roleId === 2) return '项目经理'
  return '成员'
}

// 格式化消息（渲染图片）
const formatMessage = (content: string) => {
  if (!content) return ''
  return content.replace(
    /!\[.*?\]\((.*?)\)/g,
    '<img src="$1" style="max-width: 200px; border-radius: 8px; margin: 4px 0;">'
  )
}

// 获取头像URL
const getAvatarUrl = (avatar: string) => {
  if (avatar && avatar.startsWith('/uploads')) {
    return `/api${avatar}?t=${Date.now()}`
  }
  return ''
}

const isSelf = (msg: any) => {
  return msg.userId === currentUser.value?.id
}

const canDelete = (msg: any) => {
  return isSelf(msg) || currentUser.value?.roleId === 1
}

// ===== 图片上传成功回调（修复版）=====
const handleFileUploadSuccess = (res: any) => {
  if (res.code === 200 && res.data) {
    // 后端返回 { id, fileName, url }，url 是文件名如 "abc.png"
    const fileName = res.data.url
    // 构建可访问的图片URL：/api/uploads/文件名
    const fileUrl = `/api/uploads/${fileName}`
    newComment.value += ` ![图片](${fileUrl}) `
    ElMessage.success('图片已添加到输入框')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const beforeFileUpload = (file: File) => {
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

// ===== WebSocket 消息处理 =====
const onWebSocketMessage = (message: any) => {
  if (message.type === 'NEW_PROJECT_COMMENT') {
    const comment = message.comment
    const projectId = message.projectId
    if (currentProject.value && currentProject.value.id === projectId) {
      messageList.value.push(comment)
      scrollToBottom()
    } else {
      const project = projectList.value.find((p) => p.id === projectId)
      if (project) {
        project.unreadCount = (project.unreadCount || 0) + 1
      }
    }
  }
}

// ===== 加载项目列表（后端根据角色自动过滤）=====
const loadProjectList = async () => {
  loadingProjects.value = true
  try {
    const res: any = await getProjectPageApi({ pageNum: 1, pageSize: 100 })
    projectList.value = (res.data.records || []).map((project: any) => ({
      ...project,
      unreadCount: 0
    }))
    if (projectList.value.length > 0 && !currentProject.value) {
      selectProject(projectList.value[0])
    }
  } catch (error) {
    ElMessage.error('加载项目列表失败')
  } finally {
    loadingProjects.value = false
  }
}

// ===== 选择项目 =====
const selectProject = async (project: any) => {
  if (currentProject.value?.id === project.id) return
  currentProject.value = project
  project.unreadCount = 0
  await loadComments()
  loadMembers()
}

// ===== 加载当前项目的评论 =====
const loadComments = async () => {
  if (!currentProject.value) return
  loadingMessages.value = true
  try {
    const res: any = await getProjectCommentListApi(currentProject.value.id)
    messageList.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载消息失败')
  } finally {
    loadingMessages.value = false
  }
}

// ===== 发送评论 =====
const sendComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }
  if (!currentProject.value) {
    ElMessage.warning('请先选择项目聊天室')
    return
  }
  sending.value = true
  try {
    await addProjectCommentApi({
      projectId: currentProject.value.id,
      content: newComment.value
    })
    ElMessage.success('发送成功')
    newComment.value = ''
    await loadComments()
  } catch (error) {
    ElMessage.error('发送失败，请重试')
  } finally {
    sending.value = false
  }
}

// ===== 删除评论 =====
const deleteComment = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该消息吗？', '提示', { type: 'warning' })
    await deleteCommentApi(id)
    ElMessage.success('删除成功')
    await loadComments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// ===== 滚动到底部 =====
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// ===== 加载项目成员 =====
const loadMembers = async () => {
  if (!currentProject.value) return
  loadingMembers.value = true
  try {
    const res: any = await getProjectMemberListApi(currentProject.value.id)
    memberList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载成员列表失败')
    memberList.value = []
  } finally {
    loadingMembers.value = false
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadProjectList()
  websocket.addMessageListener(onWebSocketMessage)
})

onUnmounted(() => {
  websocket.removeMessageListener(onWebSocketMessage)
})
</script>

<style scoped>
/* ===== QQ风格三栏布局容器 ===== */
.chat-wrapper {
  display: flex;
  height: calc(100vh - 100px);
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}

/* ===== 左侧：群聊列表 (280px) ===== */
.chat-sidebar {
  width: 280px;
  min-width: 240px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e5e5e5;
  background: #fafafa;
}

.sidebar-search {
  padding: 12px;
  border-bottom: 1px solid #e5e5e5;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f0f0f0;
}

.chat-item:hover {
  background: #f0f0f0;
}

.chat-item.active {
  background: #d6e7ff;
}

.chat-item-avatar {
  flex-shrink: 0;
  margin-right: 10px;
  background: #409eff;
  color: #fff;
}

.chat-item-info {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.chat-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-item-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-item-desc {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 中间：聊天主区域 ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-main-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #e5e5e5;
  background: #fff;
  gap: 8px;
}

.chat-main-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.chat-main-desc {
  font-size: 12px;
  color: #999;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toggle-members-btn {
  flex-shrink: 0;
}

/* 消息滚动区 */
.chat-main-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: #f5f6fa;
}

/* 日期分隔线 */
.message-divider {
  text-align: center;
  margin: 12px 0;
}

.message-divider span {
  display: inline-block;
  background: #e0e0e0;
  color: #666;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
}

/* 消息项 */
.message-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 8px;
}

.message-item.is-self {
  flex-direction: row-reverse;
}

.message-item.is-self .msg-body {
  align-items: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-body {
  display: flex;
  flex-direction: column;
  max-width: 55%;
}

/* 自己的消息隐藏昵称行（QQ风格） */
.message-item.is-self .msg-header {
  display: none;
}

.msg-header {
  margin-bottom: 2px;
}

.msg-nickname {
  font-size: 12px;
  color: #999;
}

/* 消息气泡 */
.msg-bubble {
  padding: 8px 12px;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  word-wrap: break-word;
  word-break: break-all;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

/* 自己的消息蓝色气泡（QQ风格） */
.message-item.is-self .msg-bubble {
  background: #52a2f2;
  color: #fff;
}

.msg-content :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 4px 0;
}

/* 操作按钮移到气泡下方 */
.msg-actions {
  margin-top: 2px;
}

.msg-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 2px;
  flex-shrink: 0;
  align-self: flex-end;
}

/* 输入区域 */
.chat-main-input {
  border-top: 1px solid #e5e5e5;
  padding: 12px 16px;
  background: #fff;
}

.input-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
}

/* ===== 右侧：群成员面板 (220px) ===== */
.chat-members {
  width: 220px;
  min-width: 180px;
  display: flex;
  flex-direction: column;
  border-left: 1px solid #e5e5e5;
  background: #fafafa;
}

.members-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #e5e5e5;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.members-count {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

.members-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  gap: 10px;
  transition: background 0.15s;
}

.member-item:hover {
  background: #f0f0f0;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-role {
  font-size: 11px;
  color: #999;
}

/* ===== 占位 ===== */
.chat-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
  color: #999;
  font-size: 14px;
  gap: 8px;
}
</style>