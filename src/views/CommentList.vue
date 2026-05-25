<template>
  <div class="chat-room-container">
    <el-card shadow="never" class="chat-room-card">
      <el-row :gutter="0" style="height: 100%;">
        <!-- 左侧：项目列表 -->
        <el-col :span="7" class="project-list-side">
          <div class="project-list-header">
            <span>项目聊天室</span>
          </div>
          <div class="project-list" v-loading="loadingProjects">
            <div
              v-for="project in projectList"
              :key="project.id"
              class="project-item"
              :class="{ active: currentProject?.id === project.id }"
              @click="selectProject(project)"
            >
              <div class="project-title">
                {{ project.name }}
                <el-badge :value="project.unreadCount" :hidden="!project.unreadCount" class="unread-badge" />
              </div>
              <div class="project-desc">{{ project.description || '暂无描述' }}</div>
            </div>
            <el-empty v-if="projectList.length === 0 && !loadingProjects" description="暂无项目，请先加入项目" />
          </div>
        </el-col>

        <!-- 右侧：聊天区域 -->
        <el-col :span="17" class="chat-area" v-if="currentProject">
          <div class="chat-header">
            <div class="project-info">
              <span class="project-name">{{ currentProject.name }}</span>
              <span class="project-desc">{{ currentProject.description }}</span>
            </div>
            <div class="header-actions">
              <!-- 三个点按钮，使用 Popover 显示成员列表 -->
              <el-popover
                placement="bottom-end"
                :width="260"
                trigger="click"
                @show="loadMembers"
              >
                <template #reference>
                  <el-button :icon="MoreFilled" circle plain size="small" />
                </template>
                <div class="member-popover">
                  <div class="member-popover-header">
                    <span>项目成员</span>
                    <span class="member-count">{{ memberList.length }}人</span>
                  </div>
                  <div class="member-popover-list">
                    <div v-for="member in memberList" :key="member.userId" class="member-popover-item">
                      <el-avatar :size="28" :src="getAvatarUrl(member.avatar)" class="member-avatar">
                        <el-icon><User /></el-icon>
                      </el-avatar>
                      <div class="member-info">
                        <div class="member-name">{{ member.nickname || member.username }}</div>
                        <div class="member-role">{{ member.roleName || (member.role === 'admin' ? '管理员' : '成员') }}</div>
                      </div>
                    </div>
                    <el-empty v-if="memberList.length === 0" description="暂无成员" :image-size="60" />
                  </div>
                </div>
              </el-popover>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef" v-loading="loadingMessages">
            <div
              v-for="msg in messageList"
              :key="msg.id"
              class="message-item"
              :class="{ 'is-self': isSelf(msg) }"
            >
              <el-avatar :size="36" :src="getAvatarUrl(msg.avatar)" class="avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="message-bubble">
                <div class="message-info">
                  <span class="nickname">{{ msg.nickname || '匿名' }}</span>
                  <span class="time">{{ msg.createTime }}</span>
                </div>
                <div class="message-content" v-html="formatMessage(msg.content)"></div>
                <div class="message-actions" v-if="canDelete(msg)">
                  <el-button link type="danger" size="small" @click="deleteComment(msg.id)">删除</el-button>
                </div>
              </div>
            </div>
            <el-empty v-if="messageList.length === 0 && !loadingMessages" description="暂无消息，发一条开始聊天吧~" />
          </div>

          <!-- 输入区域 -->
          <div class="input-area">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="3"
              placeholder="输入你的消息... (Ctrl+Enter 发送)"
              resize="none"
              @keyup.ctrl.enter="sendComment"
            />
            <div class="input-actions">
              <el-upload
                :action="uploadUrl"
                :headers="headers"
                :data="{ projectId: currentProject.id }"
                :show-file-list="false"
                :on-success="handleFileUploadSuccess"
                :before-upload="beforeFileUpload"
              >
                <el-button size="small" type="primary" :icon="Picture">上传图片</el-button>
              </el-upload>
              <el-button type="primary" @click="sendComment" :loading="sending">发送</el-button>
            </div>
          </div>
        </el-col>

        <!-- 未选择项目时的占位 -->
        <el-col :span="17" class="chat-placeholder" v-else>
          <el-empty description="请从左侧选择一个项目聊天室" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Picture, MoreFilled } from '@element-plus/icons-vue'
import { getProjectPageApi, getProjectMemberListApi } from '../api/project'
import { getProjectCommentListApi, addProjectCommentApi, deleteCommentApi } from '../api/comment'
import { useUserStore } from '../store/user'
import websocket from '../utils/websocket'

const userStore = useUserStore()
const currentUser = computed(() => userStore.info)

// 项目列表
const projectList = ref<any[]>([])
const loadingProjects = ref(false)

// 当前选中的项目
const currentProject = ref<any>(null)

// 评论列表
const messageList = ref<any[]>([])
const loadingMessages = ref(false)

// 输入框
const newComment = ref('')
const sending = ref(false)

// 成员列表（用于 popover）
const memberList = ref<any[]>([])

const messageListRef = ref<HTMLElement>()

// 上传配置
const uploadUrl = '/api/file/upload'
const headers = {
  Authorization: `Bearer ${localStorage.getItem('token')}`
}

// 格式化消息（渲染图片）
const formatMessage = (content: string) => {
  if (!content) return ''
  return content.replace(/!\[.*?\]\((.*?)\)/g, '<img src="$1" style="max-width: 200px; border-radius: 8px; margin: 4px 0;">')
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

// 图片上传成功回调
const handleFileUploadSuccess = (res: any) => {
  if (res.code === 200 && res.data) {
    const fileUrl = `/api${res.data}`
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

// WebSocket 消息处理
const onWebSocketMessage = (message: any) => {
  if (message.type === 'NEW_PROJECT_COMMENT') {
    const comment = message.comment
    const projectId = message.projectId
    if (currentProject.value && currentProject.value.id === projectId) {
      messageList.value.push(comment)
      scrollToBottom()
    } else {
      const project = projectList.value.find(p => p.id === projectId)
      if (project) {
        project.unreadCount = (project.unreadCount || 0) + 1
      }
    }
  }
}

// 加载用户参与的项目列表
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

// 选择项目，加载评论
const selectProject = async (project: any) => {
  if (currentProject.value?.id === project.id) return
  currentProject.value = project
  project.unreadCount = 0
  await loadComments()
}

// 加载当前项目的评论
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

// 发送评论
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

// 删除评论
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

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 加载项目成员（供 popover 调用）
const loadMembers = async () => {
  if (!currentProject.value) return
  try {
    const res: any = await getProjectMemberListApi(currentProject.value.id)
    memberList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载成员列表失败')
    memberList.value = []
  }
}

onMounted(() => {
  loadProjectList()
  websocket.addMessageListener(onWebSocketMessage)
})

onUnmounted(() => {
  websocket.removeMessageListener(onWebSocketMessage)
})
</script>

<style scoped>
.chat-room-container {
  height: calc(100vh - 100px);
  padding: 20px;
}
.chat-room-card {
  height: 100%;
}
.chat-room-card :deep(.el-card__body) {
  height: 100%;
  padding: 0;
  overflow: hidden;
}

/* 左侧项目列表 */
.project-list-side {
  border-right: 1px solid #ebeef5;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.project-list-header {
  padding: 15px;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
}
.project-list {
  flex: 1;
  overflow-y: auto;
}
.project-item {
  padding: 12px 15px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}
.project-item:hover {
  background: #f5f7fa;
}
.project-item.active {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
}
.project-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 4px;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.project-desc {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unread-badge {
  margin-left: 8px;
}

/* 右侧聊天区域 */
.chat-area {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.project-info .project-name {
  font-size: 16px;
  font-weight: bold;
  margin-right: 12px;
}
.project-info .project-desc {
  font-size: 13px;
  color: #909399;
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafbfc;
}
.message-item {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
}
.message-item.is-self {
  flex-direction: row-reverse;
}
.message-item.is-self .message-bubble {
  background: #9eea6a;
  margin-left: 12px;
  margin-right: 0;
}
.avatar {
  flex-shrink: 0;
}
.message-bubble {
  max-width: 70%;
  background: #fff;
  border-radius: 12px;
  padding: 10px 15px;
  margin-left: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  word-wrap: break-word;
}
.message-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
}
.nickname {
  font-weight: bold;
  color: #409eff;
}
.time {
  color: #999;
  margin-left: 10px;
}
.message-content {
  font-size: 14px;
  line-height: 1.5;
  color: #333;
}
.message-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 4px 0;
}
.message-actions {
  text-align: right;
  margin-top: 6px;
}
.input-area {
  border-top: 1px solid #ebeef5;
  padding: 15px 20px;
  background: #fff;
}
.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}
.chat-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

/* 成员 Popover 样式 */
.member-popover {
  max-height: 400px;
  overflow-y: auto;
}
.member-popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  font-weight: bold;
  color: #333;
}
.member-count {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}
.member-popover-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.member-popover-item {
  display: flex;
  align-items: center;
  padding: 6px 0;
}
.member-avatar {
  margin-right: 12px;
}
.member-info {
  flex: 1;
}
.member-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}
.member-role {
  font-size: 12px;
  color: #909399;
}
</style>