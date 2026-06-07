<template>
  <div class="chat-wrapper">
    <!-- 左侧面板 -->
    <div class="chat-sidebar">
      <div class="sidebar-search">
        <el-input v-model="searchKeyword" placeholder="搜索群聊或好友" size="small" clearable :prefix-icon="Search" />
      </div>
      <div class="sidebar-tabs">
        <div class="sidebar-tab" :class="{ active: sidebarTab === 'chat' }" @click="switchTab('chat')">
          <el-icon><ChatDotRound /></el-icon><span>聊天</span>
        </div>
        <div class="sidebar-tab" :class="{ active: sidebarTab === 'friends' }" @click="switchTab('friends')">
          <el-icon><User /></el-icon><span>好友</span>
        </div>
      </div>

      <div class="sidebar-list" v-loading="loadingProjects" v-show="sidebarTab === 'chat'">
        <div v-for="project in filteredProjectList" :key="project.id" class="chat-item" :class="{ active: currentProject?.id === project.id }" @click="selectProject(project)">
          <el-avatar :size="36" class="chat-item-avatar"><el-icon><ChatDotRound /></el-icon></el-avatar>
          <div class="chat-item-info">
            <div class="chat-item-top">
              <span class="chat-item-name">{{ project.name }}</span>
              <el-badge :value="project.unreadCount" :hidden="!project.unreadCount" />
            </div>
            <div class="chat-item-desc">{{ project.description || '' }}</div>
          </div>
        </div>
        <el-empty v-if="filteredProjectList.length === 0 && !loadingProjects" description="暂无可聊项目" :image-size="60" />
      </div>

      <div class="sidebar-list" v-loading="loadingFriends" v-show="sidebarTab === 'friends'">
        <div v-for="user in filteredFriendList" :key="user.userId || user.id" class="friend-item" :class="{ active: (friendChatUser?.userId || friendChatUser?.id) === (user.userId || user.id) }" @click="selectFriend(user)">
          <el-avatar :size="36" :src="getAvatarUrl(user.avatar)"><el-icon><User /></el-icon></el-avatar>
          <div class="friend-info">
            <div class="friend-name">{{ user.nickname || user.username }}</div>
            <div class="friend-role">{{ getRoleLabel(user) }}</div>
          </div>
        </div>
        <el-empty v-if="filteredFriendList.length === 0 && !loadingFriends" description="暂无好友" :image-size="60" />
      </div>
    </div>

    <!-- ===== 项目聊天 ===== -->
    <template v-if="sidebarTab === 'chat' && currentProject">
      <div class="chat-main">
        <div class="chat-main-header">
          <span class="chat-main-title">{{ currentProject.name }}</span>
          <span class="chat-main-desc">{{ currentProject.description }}</span>
          <div class="chat-header-actions">
            <el-dropdown trigger="click" @command="handleMoreCommand">
              <el-button class="more-btn" size="small" circle><el-icon><MoreFilled /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="deleteMode" :icon="deleteModeActive ? Close : Delete">{{ deleteModeActive ? '退出删除模式' : '删除消息' }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button :icon="showMembers ? ArrowRight : ArrowLeft" link size="small" class="toggle-members-btn" @click="showMembers = !showMembers" />
          </div>
        </div>
        <div v-if="deleteModeActive" class="delete-mode-bar">
          <el-icon><InfoFilled /></el-icon><span>已选中 <strong>{{ deleteSelectedIds.length }}</strong> 条消息</span>
          <el-button type="danger" size="small" :disabled="deleteSelectedIds.length === 0" @click="deleteSelectedMessages"><el-icon><Delete /></el-icon>删除选中</el-button>
          <el-button size="small" @click="exitDeleteMode"><el-icon><Close /></el-icon>取消</el-button>
        </div>
        <div class="chat-main-messages" ref="messageListRef" v-loading="loadingMessages">
          <template v-for="item in groupedMessages" :key="item.key">
            <div v-if="item.isDivider" class="message-divider"><span>{{ item.label }}</span></div>
            <div v-else class="message-item" :class="{ 'is-self': isSelf(item), 'is-deletable': canDelete(item), 'is-selected': deleteSelectedIds.includes(item.id) }" @click="deleteModeActive && canDelete(item) ? toggleDeleteSelect(item.id) : null">
              <div v-if="deleteModeActive && canDelete(item)" class="msg-check-col" @click.stop><el-checkbox v-model="deleteSelectedIds" :label="item.id" size="small" /></div>
              <el-avatar :size="32" :src="getAvatarUrl(item.avatar)" class="msg-avatar"><el-icon><User /></el-icon></el-avatar>
              <div class="msg-body">
                <div class="msg-header">
                  <span class="msg-nickname">{{ item.nickname || '匿名' }}</span>
                  <span v-if="canDelete(item) && !deleteModeActive" class="msg-delete-hover" @click.stop="deleteSingleMessage(item.id)"><el-icon><Delete /></el-icon></span>
                </div>
                <div class="msg-bubble"><div class="msg-content" v-html="formatMessage(item.content)"></div></div>
              </div>
              <div class="msg-time">{{ formatMsgTime(item.createTime) }}</div>
            </div>
          </template>
          <el-empty v-if="messageList.length === 0 && !loadingMessages" description="暂无消息，发一条开始聊天吧~" />
        </div>
        <div class="chat-main-input">
          <el-input v-model="newComment" type="textarea" :rows="3" placeholder="输入消息... (Ctrl+Enter 发送)" resize="none" @keyup.ctrl.enter="sendComment" />
          <div class="input-toolbar">
            <el-upload :action="uploadUrl" :headers="headers" :data="{ projectId: currentProject.id }" :show-file-list="false" :on-success="handleFileUploadSuccess" :before-upload="beforeFileUpload">
              <el-button size="small" :icon="Picture" link>图片</el-button>
            </el-upload>
            <el-button type="primary" size="small" @click="sendComment" :loading="sending">发送</el-button>
          </div>
        </div>
      </div>
      <div class="chat-members" v-if="showMembers">
        <div class="members-header"><span>群成员</span><span class="members-count">{{ memberList.length }}人</span></div>
        <div class="members-list" v-loading="loadingMembers">
          <div v-for="member in memberList" :key="member.userId" class="member-item">
            <el-avatar :size="32" :src="getAvatarUrl(member.avatar)"><el-icon><User /></el-icon></el-avatar>
            <div class="member-info">
              <div class="member-name">{{ member.nickname || member.username }}</div>
              <div class="member-role">{{ getRoleLabel(member) }}</div>
            </div>
          </div>
          <el-empty v-if="memberList.length === 0 && !loadingMembers" description="暂无成员" :image-size="60" />
        </div>
      </div>
    </template>

    <!-- ===== 好友聊天（基于项目评论） ===== -->
    <template v-else-if="sidebarTab === 'friends' && friendChatUser">
      <div class="chat-main">
        <div class="chat-main-header">
          <el-avatar :size="28" :src="getAvatarUrl(friendChatUser.avatar)" style="flex-shrink:0"><el-icon><User /></el-icon></el-avatar>
          <span class="chat-main-title">{{ friendChatUser.nickname || friendChatUser.username }}</span>
          <span class="chat-main-desc">{{ friendProject ? `项目：${friendProject.name}` : '' }}</span>
          <div class="chat-header-actions">
            <el-tooltip content="好友信息" placement="bottom">
              <el-button class="more-btn" size="small" circle @click="showFriendInfo = !showFriendInfo">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
        <div class="chat-main-messages" ref="friendMsgListRef" v-loading="loadingFriendMessages">
          <template v-for="item in groupedFriendMessages" :key="item.key">
            <div v-if="item.isDivider" class="message-divider"><span>{{ item.label }}</span></div>
            <div v-else class="message-item" :class="{ 'is-self': item.userId === currentUser?.id }">
              <el-avatar :size="32" :src="item.userId === currentUser?.id ? '' : getAvatarUrl(item.avatar)" class="msg-avatar"><el-icon><User /></el-icon></el-avatar>
              <div class="msg-body">
                <div class="msg-header">
                  <span class="msg-nickname">{{ item.nickname || item.userName || '匿名' }}</span>
                </div>
                <div class="msg-bubble" :class="{ 'is-friend': item.userId !== currentUser?.id }">
                  <div class="msg-content" v-html="formatMessage(item.content)"></div>
                </div>
              </div>
              <div class="msg-time">{{ formatMsgTime(item.createTime) }}</div>
            </div>
          </template>
          <el-empty v-if="friendMessages.length === 0 && !loadingFriendMessages" description="开始聊天吧~" :image-size="60" />
        </div>
        <div class="chat-main-input">
          <el-input v-model="friendInput" type="textarea" :rows="3" placeholder="输入消息... (Enter 发送)" resize="none" @keyup.enter="sendFriendMessage" />
          <div class="input-toolbar">
            <el-button type="primary" size="small" @click="sendFriendMessage">发送</el-button>
          </div>
        </div>
      </div>

      <!-- 好友信息侧栏 -->
      <div class="chat-members" v-if="showFriendInfo">
        <div class="members-header"><span>好友信息</span></div>
        <div class="friend-info-panel">
          <el-avatar :size="72" :src="getAvatarUrl(friendChatUser.avatar)" class="friend-info-avatar"><el-icon><User /></el-icon></el-avatar>
          <div class="friend-info-name">{{ friendChatUser.nickname || friendChatUser.username }}</div>
          <div class="friend-info-role">{{ getRoleLabel(friendChatUser) }}</div>
          <el-divider style="margin:12px 0" />
          <div class="friend-info-detail">
            <div class="detail-row"><span class="detail-label">用户名</span><span class="detail-value">{{ friendChatUser.username }}</span></div>
            <div class="detail-row"><span class="detail-label">昵称</span><span class="detail-value">{{ friendChatUser.nickname || '-' }}</span></div>
            <div class="detail-row"><span class="detail-label">邮箱</span><span class="detail-value">{{ friendChatUser.email || '-' }}</span></div>
            <div class="detail-row"><span class="detail-label">手机号</span><span class="detail-value">{{ friendChatUser.phone || '-' }}</span></div>
          </div>
        </div>
      </div>
    </template>

    <!-- 占位 -->
    <div class="chat-placeholder" v-else-if="sidebarTab === 'chat'">
      <el-icon :size="56" color="#ccc"><ChatDotRound /></el-icon><p>选择一个群聊开始聊天</p>
    </div>
    <div class="chat-placeholder" v-else>
      <el-icon :size="56" color="#ccc"><User /></el-icon><p>选择一个好友开始聊天</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Picture, ChatDotRound, Search, ArrowRight, ArrowLeft, MoreFilled, Delete, Close, InfoFilled } from '@element-plus/icons-vue'
import { getProjectPageApi, getProjectMemberListApi } from '../api/project'
import { getProjectCommentListApi, addProjectCommentApi, deleteCommentApi } from '../api/comment'
import { getUserPageApi } from '../api/user'
import { useUserStore } from '../store/user'
import websocket from '../utils/websocket'
import request from '../utils/request'

const userStore = useUserStore()
const currentUser = computed(() => userStore.info)

// ===== Tab =====
const sidebarTab = ref<'chat' | 'friends'>('chat')
const searchKeyword = ref('')
const switchTab = (tab: 'chat' | 'friends') => {
  sidebarTab.value = tab; searchKeyword.value = ''
  if (tab === 'friends' && friendsList.value.length === 0) loadFriendsList()
}

// ===== 项目列表 =====
const projectList = ref<any[]>([])
const loadingProjects = ref(false)
const currentProject = ref<any>(null)
const filteredProjectList = computed(() => {
  if (!searchKeyword.value.trim()) return projectList.value
  const kw = searchKeyword.value.toLowerCase()
  return projectList.value.filter(p => p.name?.toLowerCase().includes(kw) || p.description?.toLowerCase().includes(kw))
})

// ===== 好友列表 =====
const friendsList = ref<any[]>([])
const loadingFriends = ref(false)
const filteredFriendList = computed(() => {
  if (!searchKeyword.value.trim()) return friendsList.value
  const kw = searchKeyword.value.toLowerCase()
  return friendsList.value.filter(u => (u.nickname || u.username || '').toLowerCase().includes(kw))
})
const loadFriendsList = async () => {
  loadingFriends.value = true
  try {
    const cur = userStore.info
    const isAdmin = cur?.roleId === 1 || cur?.roleName === '超级管理员'

    if (isAdmin) {
      // 管理员：显示所有用户
      const res: any = await getUserPageApi({ pageNum: 1, pageSize: 999 })
      friendsList.value = (res.data.records || []).filter((u: any) => u.id !== cur?.id)
    } else {
      // 项目经理/普通成员：显示同项目成员 + 管理员
      const projRes: any = await getProjectPageApi({ pageNum: 1, pageSize: 100, memberId: cur?.id })
      const projects: any[] = projRes.data?.records || []
      const uidSet = new Set<number>()
      const result: any[] = []
      // 收集同项目成员
      for (const p of projects) {
        try {
          const mr: any = await getProjectMemberListApi(p.id)
          for (const m of (mr.data || [])) {
            if (m.userId !== cur?.id && !uidSet.has(m.userId)) {
              uidSet.add(m.userId); result.push(m)
            }
          }
        } catch { /* 跳过 */ }
      }
      // 从 /user/page 结果中找管理员（按 roleId 或 roleName 判断，不硬编码 ID=1）
      try {
        const allRes: any = await getUserPageApi({ pageNum: 1, pageSize: 999 })
        const admin = (allRes.data?.records || []).find((u: any) => (u.roleId === 1 || u.roleName === '超级管理员') && u.id !== cur?.id)
        if (admin && !uidSet.has(admin.id)) { result.unshift(admin); uidSet.add(admin.id) }
      } catch { /* 忽略 */ }
      friendsList.value = result
    }
  } catch { /* ignore */ }
  finally { loadingFriends.value = false }
}

// ===== 好友聊天（基于项目评论实现消息持久化）=======
const friendChatUser = ref<any>(null)
const friendProject = ref<any>(null)
const showFriendInfo = ref(false)
const friendInput = ref('')
const friendMsgListRef = ref<HTMLElement>()
const friendMessages = ref<any[]>([])
const loadingFriendMessages = ref(false)

const groupedFriendMessages = computed(() => {
  const r: any[] = []; let ld = ''
  for (const msg of friendMessages.value) {
    const ds = getDateLabel(msg.createTime)
    if (ds !== ld) { r.push({ isDivider: true, label: ds, key: `fd-${ds}` }); ld = ds }
    r.push({ ...msg, isDivider: false, key: `fm-${msg.id}` })
  }
  return r
})

const selectFriend = async (user: any) => {
  showFriendInfo.value = false
  friendChatUser.value = user
  // 尝试补全信息
  try {
    const detailId = user.userId || user.id
    if (detailId) {
      const res: any = await request({ url: `/user/detail/${detailId}`, method: 'get' })
      if (res.data) friendChatUser.value = { ...user, ...res.data }
    }
  } catch { /* ignore */ }
  // 使用第一个共享项目作为聊天载体（双方在同一个项目里才能聊天）
  if (projectList.value.length > 0) {
    friendProject.value = projectList.value[0]
    await loadFriendMessages()
  }
  nextTick(() => { if (friendMsgListRef.value) friendMsgListRef.value.scrollTop = friendMsgListRef.value.scrollHeight })
}

const loadFriendMessages = async () => {
  loadingFriendMessages.value = true
  try {
    const myId = userStore.info?.id
    const friendId = friendChatUser.value?.id || friendChatUser.value?.userId
    if (!myId || !friendId) { friendMessages.value = []; return }
    // 扫描所有项目，收集双方之间的私聊（按 发送者+内容 去重，因为同一条消息存在多个项目中）
    const seenKeys = new Set<string>()
    const allDMs: any[] = []
    for (const project of projectList.value) {
      try {
        const res: any = await getProjectCommentListApi(project.id)
        for (const m of (res.data || [])) {
          if (!m.content || !m.content.startsWith('[DM:')) continue
          const match = m.content.match(/^\[DM:(\d+)\]/)
          if (!match) continue
          const targetId = Number(match[1])
          if ((m.userId === myId && targetId === friendId) || (m.userId === friendId && targetId === myId)) {
            const key = `${m.userId}_${m.content}`
            if (seenKeys.has(key)) continue
            seenKeys.add(key)
            allDMs.push({ ...m, content: m.content.replace(/^\[DM:\d+\]\s*/, '') })
          }
        }
      } catch { /* 跳过无权限项目 */ }
    }
    // 按时间排序
    allDMs.sort((a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime())
    friendMessages.value = allDMs
  } catch { friendMessages.value = [] }
  finally { loadingFriendMessages.value = false }
  nextTick(() => { if (friendMsgListRef.value) friendMsgListRef.value.scrollTop = friendMsgListRef.value.scrollHeight })
}

let sendingFriendMsg = false

const sendFriendMessage = async () => {
  if (!friendInput.value.trim() || !friendChatUser.value || sendingFriendMsg) return
  sendingFriendMsg = true
  const friendId = friendChatUser.value.id || friendChatUser.value.userId
  const content = `[DM:${friendId}] ${friendInput.value.trim()}`
  try {
    // 优先找一个双方都在的项目发（对方才能收到）
    let sharedProjectId: number | null = null
    for (const project of projectList.value) {
      try {
        const mr: any = await getProjectMemberListApi(project.id)
        if ((mr.data || []).some((m: any) => m.userId === friendId)) {
          sharedProjectId = project.id; break
        }
      } catch { /* 跳过 */ }
    }
    // 优先发到共同项目
    if (sharedProjectId) {
      await addProjectCommentApi({ projectId: sharedProjectId, content })
    }
    // 也发到所有自己的项目（确保自己能看到）
    for (const project of projectList.value) {
      if (project.id === sharedProjectId) continue
      try { await addProjectCommentApi({ projectId: project.id, content }) }
      catch { /* 跳过 */ }
    }
    // 没有共同项目时，发到自己的所有项目（管理员能看到所有项目，不提示）
    friendInput.value = ''
    await loadFriendMessages()
  } catch { ElMessage.error('发送失败') }
  finally { sendingFriendMsg = false }
}

// ===== 项目聊天消息 =====
const messageList = ref<any[]>([])
const loadingMessages = ref(false)
const groupedMessages = computed(() => {
  const r: any[] = []; let ld = ''
  for (const msg of messageList.value) {
    const ds = getDateLabel(msg.createTime)
    if (ds !== ld) { r.push({ isDivider: true, label: ds, key: `d-${ds}` }); ld = ds }
    r.push({ ...msg, isDivider: false, key: `m-${msg.id}` })
  }
  return r
})
const newComment = ref('')
const sending = ref(false)
const memberList = ref<any[]>([])
const loadingMembers = ref(false)
const showMembers = ref(true)
const messageListRef = ref<HTMLElement>()
const uploadUrl = '/api/file/upload'
const headers = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const formatMsgTime = (t: string) => {
  if (!t) return ''
  try { const d = new Date(t.replace(/-/g,'/')); return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` }
  catch { return t }
}
const getDateLabel = (t: string) => {
  if (!t) return ''
  try {
    const d = new Date(t.replace(/-/g,'/')); const n = new Date()
    const td = new Date(n.getFullYear(),n.getMonth(),n.getDate()); const md = new Date(d.getFullYear(),d.getMonth(),d.getDate())
    const diff = Math.floor((td.getTime()-md.getTime())/86400000)
    if (diff===0) return '今天'; if (diff===1) return '昨天'; if (diff===2) return '前天'
    const y=d.getFullYear(),m=String(d.getMonth()+1).padStart(2,'0'),da=String(d.getDate()).padStart(2,'0')
    return y===n.getFullYear()?`${m}-${da}`:`${y}-${m}-${da}`
  } catch { return '' }
}
const getRoleLabel = (m: any) => {
  if (m.roleName) return m.roleName; if (m.role==='admin'||m.projectRole==='admin') return '管理员'
  if (m.roleId===1) return '超级管理员'; if (m.roleId===2) return '项目经理'; return '成员'
}
const formatMessage = (c: string) => c ? c.replace(/!\[.*?\]\((.*?)\)/g,'<img src="$1" style="max-width:200px;border-radius:8px;margin:4px 0;">') : ''
const getAvatarUrl = (a: string) => {
  if (!a) return ''
  if (a.startsWith('/uploads')) return `/api${a}?t=${Date.now()}`
  if (a.startsWith('http')) return a
  return `/api/uploads/${a}?t=${Date.now()}`
}
const isSelf = (msg: any) => msg.userId === currentUser.value?.id
const canDelete = (msg: any) => isSelf(msg) || currentUser.value?.roleId === 1

// ===== 删除模式 =====
const deleteModeActive = ref(false)
const deleteSelectedIds = ref<number[]>([])
const handleMoreCommand = (c: string) => { if (c==='deleteMode') deleteModeActive.value ? exitDeleteMode() : (deleteModeActive.value=true, deleteSelectedIds.value=[]) }
const toggleDeleteSelect = (id: number) => { const i=deleteSelectedIds.value.indexOf(id); i>-1?deleteSelectedIds.value.splice(i,1):deleteSelectedIds.value.push(id) }
const deleteSingleMessage = async (id: number) => {
  try { await ElMessageBox.confirm('确定删除该消息吗？','提示',{type:'warning',confirmButtonText:'删除',cancelButtonText:'取消'})
    await deleteCommentApi(id); ElMessage.success('删除成功'); await loadComments() } catch(e) { if(e!=='cancel') ElMessage.error('删除失败') }
}
const deleteSelectedMessages = async () => {
  if (!deleteSelectedIds.value.length) return
  try { await ElMessageBox.confirm(`确定删除选中的 ${deleteSelectedIds.value.length} 条消息吗？`,'提示',{type:'warning',confirmButtonText:'删除',cancelButtonText:'取消'})
    for (const id of deleteSelectedIds.value) await deleteCommentApi(id)
    ElMessage.success(`成功删除 ${deleteSelectedIds.value.length} 条消息`); exitDeleteMode(); await loadComments()
  } catch(e) { if(e!=='cancel') ElMessage.error('删除失败') }
}
const exitDeleteMode = () => { deleteModeActive.value=false; deleteSelectedIds.value=[] }

// ===== 图片上传 =====
const handleFileUploadSuccess = (res: any) => {
  if (res.code===200 && res.data) { newComment.value+=` ![图片](/api/uploads/${res.data.url}) `; ElMessage.success('图片已添加到输入框') }
  else ElMessage.error(res.message||'上传失败')
}
const beforeFileUpload = (f: File) => { if (!f.type.startsWith('image/')){ElMessage.error('只能上传图片');return false} if (f.size/1048576>5){ElMessage.error('图片不能超过5MB');return false} return true }

// ===== WebSocket =====
const onWebSocketMessage = (msg: any) => {
  if (msg.type==='NEW_PROJECT_COMMENT' && msg.comment) {
    const isDm = msg.comment.content?.startsWith('[DM:')
    // 群聊消息才推送到群聊界面
    if (!isDm && currentProject.value && currentProject.value.id===msg.projectId) { messageList.value.push(msg.comment); scrollToBottom() }
    else if (!isDm) { const p=projectList.value.find((x:any)=>x.id===msg.projectId); if (p) p.unreadCount=(p.unreadCount||0)+1 }
    // 私聊消息实时刷新好友聊天
    if (isDm && friendChatUser.value) loadFriendMessages()
  }
}

// ===== 项目数据加载 =====
const loadProjectList = async () => {
  loadingProjects.value=true
  try { const r:any=await getProjectPageApi({pageNum:1,pageSize:100}); projectList.value=(r.data.records||[]).map((p:any)=>({...p,unreadCount:0}))
    if (projectList.value.length>0 && !currentProject.value) selectProject(projectList.value[0])
  } catch { ElMessage.error('加载项目列表失败') } finally { loadingProjects.value=false }
}
const selectProject = async (project: any) => {
  if (currentProject.value?.id===project.id) return; currentProject.value=project; project.unreadCount=0
  await loadComments(); loadMembers()
}
const loadComments = async () => {
  if (!currentProject.value) return; loadingMessages.value=true
  try {
    const r:any=await getProjectCommentListApi(currentProject.value.id)
    // 过滤掉私聊消息 [DM:xxx]，群聊界面只显示群聊消息
    messageList.value=(r.data||[]).filter((m:any)=>!m.content?.startsWith('[DM:'))
    await nextTick(); scrollToBottom()
  }
  catch { ElMessage.error('加载消息失败') } finally { loadingMessages.value=false }
}
const sendComment = async () => {
  if (!newComment.value.trim()){ElMessage.warning('请输入消息内容');return}
  if (!currentProject.value){ElMessage.warning('请先选择项目聊天室');return}
  sending.value=true
  try { await addProjectCommentApi({projectId:currentProject.value.id,content:newComment.value}); ElMessage.success('发送成功'); newComment.value=''; await loadComments() }
  catch { ElMessage.error('发送失败，请重试') } finally { sending.value=false }
}
const scrollToBottom = () => { if (messageListRef.value) messageListRef.value.scrollTop=messageListRef.value.scrollHeight }
const loadMembers = async () => {
  if (!currentProject.value) return; loadingMembers.value=true
  try { const r:any=await getProjectMemberListApi(currentProject.value.id); memberList.value=r.data||[] }
  catch { ElMessage.error('加载成员列表失败'); memberList.value=[] } finally { loadingMembers.value=false }
}

// ===== 生命周期 =====
onMounted(() => { loadProjectList(); websocket.addMessageListener(onWebSocketMessage) })
onUnmounted(() => { websocket.removeMessageListener(onWebSocketMessage) })
</script>

<style scoped>
.chat-wrapper {display:flex;height:calc(100vh - 112px);border:1px solid var(--cf-border);border-radius:var(--cf-radius);overflow:hidden;background:var(--cf-surface);box-shadow:var(--cf-shadow-sm)}
.chat-sidebar {width:280px;min-width:240px;display:flex;flex-direction:column;border-right:1px solid var(--cf-border);background:var(--cf-bg-soft)}
.sidebar-search {padding:12px;border-bottom:1px solid var(--cf-border)}
.sidebar-tabs {display:flex;border-bottom:1px solid var(--cf-border);flex-shrink:0}
.sidebar-tab {flex:1;display:flex;align-items:center;justify-content:center;gap:6px;padding:10px 0;font-size:13px;font-weight:500;color:var(--cf-text-muted);cursor:pointer;transition:var(--cf-transition);border-bottom:2px solid transparent}
.sidebar-tab:hover{color:var(--cf-text);background:var(--cf-bg)}
.sidebar-tab.active{color:var(--cf-primary);border-bottom-color:var(--cf-primary)}
.sidebar-tab .el-icon{font-size:16px}
.sidebar-list{flex:1;overflow-y:auto}
.chat-item,.friend-item{display:flex;align-items:center;padding:10px 12px;cursor:pointer;transition:var(--cf-transition);border-bottom:1px solid var(--cf-border-light)}
.chat-item:hover,.friend-item:hover{background:var(--cf-primary-bg)}
.chat-item.active,.friend-item.active{background:var(--cf-primary-bg);border-left:3px solid var(--cf-primary)}
.chat-item-avatar{flex-shrink:0;margin-right:10px;background:var(--cf-primary);color:#fff}
.chat-item-info{flex:1;min-width:0;overflow:hidden}
.chat-item-top{display:flex;justify-content:space-between;align-items:center}
.chat-item-name,.friend-name{font-size:14px;font-weight:500;color:var(--cf-text-heading);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.chat-item-desc,.friend-role{font-size:12px;color:var(--cf-text-muted);margin-top:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.friend-info{flex:1;min-width:0;margin-left:10px}

/* 好友信息侧栏 */
.friend-info-panel{display:flex;flex-direction:column;align-items:center;padding:20px 16px}
.friend-info-avatar{border:3px solid var(--cf-primary-border);margin-bottom:12px}
.friend-info-name{font-size:16px;font-weight:600;color:var(--cf-text-heading)}
.friend-info-role{font-size:13px;color:var(--cf-text-muted);margin-top:4px}
.friend-info-detail{width:100%}
.detail-row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--cf-border-light);font-size:13px}
.detail-label{color:var(--cf-text-muted)}
.detail-value{color:var(--cf-text-heading);font-weight:500;text-align:right}

.chat-main{flex:1;display:flex;flex-direction:column;min-width:0}
.chat-main-header{display:flex;align-items:center;padding:10px 16px;border-bottom:1px solid var(--cf-border);background:var(--cf-surface);gap:8px}
.chat-main-title{font-size:16px;font-weight:600;color:var(--cf-text-heading)}
.chat-main-desc{font-size:12px;color:var(--cf-text-muted);flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.chat-header-actions{display:flex;align-items:center;gap:4px;flex-shrink:0}
.more-btn{border:none!important;background:transparent!important;color:var(--cf-text-muted)!important;font-size:16px!important;transition:var(--cf-transition)!important}
.more-btn:hover{background:var(--cf-bg)!important;color:var(--cf-primary)!important}
.toggle-members-btn{flex-shrink:0}

.delete-mode-bar{display:flex;align-items:center;gap:10px;padding:8px 16px;background:#fef2f2;border-bottom:1px solid #fecaca;font-size:13px;color:var(--cf-text);flex-shrink:0}
.delete-mode-bar .el-icon{font-size:16px;color:var(--cf-danger)}
.delete-mode-bar strong{color:var(--cf-danger)}

.chat-main-messages{flex:1;overflow-y:auto;padding:16px 20px;background:var(--cf-bg)}
.message-divider{text-align:center;margin:12px 0}
.message-divider span{display:inline-block;background:var(--cf-border);color:var(--cf-text-muted);font-size:11px;padding:2px 12px;border-radius:10px}

.message-item{display:flex;align-items:flex-start;margin-bottom:16px;gap:8px;transition:var(--cf-transition);position:relative}
.message-item.is-self{flex-direction:row-reverse}
.message-item.is-self .msg-body{align-items:flex-end}
.message-item.is-deletable{cursor:pointer;border-radius:var(--cf-radius-sm);padding:4px 4px 4px 0;margin:0 -4px 12px}
.message-item.is-deletable:hover{background:var(--cf-primary-bg)}
.message-item.is-selected{background:#fef2f2!important;border-radius:var(--cf-radius-sm);padding:4px 4px 4px 0;margin:0 -4px 12px}

.msg-check-col{display:flex;align-items:center;padding-top:6px;flex-shrink:0}
.msg-avatar{flex-shrink:0}
.msg-body{display:flex;flex-direction:column;max-width:55%}
.message-item.is-self .msg-header{display:none}
.msg-header{margin-bottom:2px;display:flex;align-items:center;gap:8px}
.msg-nickname{font-size:12px;color:var(--cf-text-muted)}
.msg-delete-hover{display:none;font-size:14px;color:var(--cf-text-muted);cursor:pointer;transition:var(--cf-transition);padding:2px;border-radius:4px;line-height:1}
.msg-delete-hover:hover{color:var(--cf-danger);background:#fef2f2}
.message-item:hover .msg-delete-hover{display:inline-flex}

.msg-bubble{padding:8px 14px;border-radius:8px;background:var(--cf-surface);font-size:14px;line-height:1.5;color:var(--cf-text);word-wrap:break-word;word-break:break-all;box-shadow:var(--cf-shadow-sm)}
.message-item.is-self .msg-bubble{background:var(--cf-primary);color:#fff}
.msg-bubble.is-friend{background:var(--cf-surface);color:var(--cf-text)}
.msg-content :deep(img){max-width:100%;border-radius:6px;margin:4px 0}

.msg-time{font-size:11px;color:var(--cf-text-muted);margin-top:2px;flex-shrink:0;align-self:flex-end}

.chat-main-input{border-top:1px solid var(--cf-border);padding:12px 16px;background:var(--cf-surface)}
.input-toolbar{display:flex;justify-content:flex-end;gap:8px;margin-top:8px;align-items:center}

.chat-members{width:220px;min-width:180px;display:flex;flex-direction:column;border-left:1px solid var(--cf-border);background:var(--cf-bg-soft)}
.members-header{display:flex;justify-content:space-between;align-items:center;padding:12px;border-bottom:1px solid var(--cf-border);font-size:14px;font-weight:600;color:var(--cf-text-heading)}
.members-count{font-size:12px;color:var(--cf-text-muted);font-weight:normal}
.members-list{flex:1;overflow-y:auto;padding:8px 0}
.member-item{display:flex;align-items:center;padding:8px 12px;gap:10px;transition:var(--cf-transition)}
.member-item:hover{background:var(--cf-primary-bg)}
.member-info{flex:1;min-width:0}
.member-name{font-size:13px;font-weight:500;color:var(--cf-text-heading);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.member-role{font-size:11px;color:var(--cf-text-muted)}

.chat-placeholder{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;background:var(--cf-bg);color:var(--cf-text-muted);font-size:14px;gap:8px}
</style>
