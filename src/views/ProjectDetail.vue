<template>
  <div class="detail-container">
    <!-- 项目基础信息 -->
    <el-card shadow="never">
      <div class="header">
        <div>
          <div class="title">{{ projectInfo.name }}</div>
          <div class="description">{{ projectInfo.description || '暂无描述' }}</div>
        </div>
        <el-tag v-if="projectInfo.status === 1" type="primary">进行中</el-tag>
        <el-tag v-else-if="projectInfo.status === 2" type="success">已完成</el-tag>
        <el-tag v-else type="warning">未开始</el-tag>
      </div>
    </el-card>

    <!-- 项目统计 -->
    <div class="statistics">
      <el-card shadow="hover">
        <div class="card-title">总任务数</div>
        <div class="card-value">{{ statistics.total }}</div>
      </el-card>
      <el-card shadow="hover">
        <div class="card-title">已完成</div>
        <div class="card-value success">{{ statistics.completed }}</div>
      </el-card>
      <el-card shadow="hover">
        <div class="card-title">进行中</div>
        <div class="card-value primary">{{ statistics.processing }}</div>
      </el-card>
      <el-card shadow="hover">
        <div class="card-title">已逾期</div>
        <div class="card-value danger">{{ statistics.expired }}</div>
      </el-card>
    </div>

    <!-- 项目进度 -->
    <el-card shadow="never">
      <div class="progress-header">
        <span>项目完成进度</span>
        <span>{{ progress }}%</span>
      </div>
      <el-progress :percentage="progress" :status="progress === 100 ? 'success' : ''" />
    </el-card>

    <!-- 项目动态 -->
    <el-card shadow="never">
      <template #header>
        <div class="activity-title">项目动态</div>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="item in activityList"
          :key="item.id"
          :timestamp="item.createTime"
          placement="top"
        >
          <el-card>
            <div class="activity-content">{{ item.content }}</div>
          </el-card>
        </el-timeline-item>
        <el-empty v-if="activityList.length === 0" description="暂无动态" />
      </el-timeline>
    </el-card>

    <!-- Tabs -->
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="项目成员" name="member">
          <el-table :data="memberList" border stripe>
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="nickname" label="昵称" />
            <el-table-column prop="role" label="项目角色" />
          </el-table>
          <el-empty v-if="memberList.length === 0" description="暂无成员" />
        </el-tab-pane>

        <el-tab-pane label="项目任务" name="task">
          <div class="task-toolbar">
            <el-button
              v-if="canManage"
              type="primary"
              @click="openAddTaskDialog"
            >
              新增任务
            </el-button>
          </div>
          <el-table :data="taskList" border stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="任务标题" min-width="220">
              <template #default="scope">
                <el-link type="primary" @click="$router.push(`/task/detail/${scope.row.id}`)">
                  {{ scope.row.title }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="任务状态" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.status === 0" type="info">待开始</el-tag>
                <el-tag v-else-if="scope.row.status === 1" type="primary">进行中</el-tag>
                <el-tag v-else type="success">已完成</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.priority === 3" type="danger">高</el-tag>
                <el-tag v-else-if="scope.row.priority === 2" type="warning">中</el-tag>
                <el-tag v-else>低</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executorName" label="执行人" width="140" />
            <el-table-column label="截止时间" width="180">
              <template #default="scope">
                {{ scope.row.deadline ? scope.row.deadline.replace('T', ' ') : '' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="scope">
                <template v-if="canManage">
                  <el-button type="primary" size="small" @click="handleEditTask(scope.row)">编辑</el-button>
                  <el-button type="danger" size="small" @click="handleDeleteTask(scope.row.id)">删除</el-button>
                  <el-button type="success" size="small" @click="openStatusDialog(scope.row)">状态</el-button>
                  <el-button type="warning" size="small" @click="openReplyDialog(scope.row)">回复</el-button>
                </template>
                <template v-else>
                  <el-button type="primary" size="small" @click="openQuestionDialog(scope.row)">疑问</el-button>
                  <el-button type="info" size="small" @click="$router.push(`/task/detail/${scope.row.id}`)">详情</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="taskList.length === 0" description="暂无任务" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增/编辑任务弹窗（保持不变） -->
    <el-dialog v-model="taskDialogVisible" :title="isEditTask ? '编辑任务' : '新增任务'" width="650px">
      <el-form :model="taskForm" label-width="100px">
        <el-form-item label="任务标题">
          <el-input v-model="taskForm.title" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="taskForm.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="taskForm.priority" style="width:100%">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行人" v-if="canManage">
          <el-select v-model="taskForm.executorId" placeholder="请选择执行人" clearable style="width:100%">
            <el-option
              v-for="member in memberList"
              :key="member.userId"
              :label="member.nickname || member.username"
              :value="member.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="taskForm.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改状态弹窗 -->
    <el-dialog v-model="statusDialogVisible" title="修改任务状态" width="400px">
      <el-form label-width="80px">
        <el-form-item label="新状态">
          <el-select v-model="newStatus" placeholder="请选择状态" style="width:100%">
            <el-option label="待开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmStatusChange">确定</el-button>
      </template>
    </el-dialog>

    <!-- 发送疑问弹窗（接收人仅限项目经理） -->
    <el-dialog v-model="questionDialogVisible" title="发送疑问" width="500px">
      <el-form :model="questionForm" label-width="80px">
        <el-form-item label="发送给" required>
          <el-select v-model="questionForm.receiverId" placeholder="请选择接收人" style="width:100%">
            <el-option
              v-for="user in questionReceivers"
              :key="user.userId"
              :label="user.nickname || user.username"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="疑问内容">
          <el-input
            v-model="questionForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入您的问题..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendQuestion">发送</el-button>
      </template>
    </el-dialog>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyDialogVisible" title="回复疑问" width="500px">
      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="4"
        placeholder="请输入回复内容..."
      />
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendReply">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { addTaskApi, updateTaskApi, deleteTaskApi, updateTaskStatusApi } from '../api/task'
import { useUserStore } from '../store/user'

const route = useRoute()
const projectId = route.params.id
const userStore = useUserStore()

// 权限判断：管理员或项目经理
const canManage = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员' || info?.roleId === 2 || info?.roleName === '项目经理'
})

// 可接收疑问的用户（仅项目经理）
const questionReceivers = computed(() => {
  // memberList 中的 role 字段已经是转换后的中文（项目经理 / 普通成员）
  return memberList.value.filter(m => m.role === '项目经理')
})

const projectInfo = reactive<any>({
  id: null,
  name: '',
  description: '',
  status: 0
})

const activeTab = ref('member')
const memberList = ref<any[]>([])
const taskList = ref<any[]>([])
const activityList = ref<any[]>([])

const statistics = reactive({
  total: 0,
  completed: 0,
  processing: 0,
  expired: 0
})

const progress = ref(0)
const taskDialogVisible = ref(false)
const isEditTask = ref(false)

const taskForm = reactive<any>({
  id: null,
  title: '',
  content: '',
  projectId: Number(projectId),
  priority: 1,
  status: 0,
  executorId: null,
  endTime: ''
})

// 状态修改相关
const statusDialogVisible = ref(false)
const currentTask = ref<any>(null)
const newStatus = ref<number>(0)

// 疑问相关
const questionDialogVisible = ref(false)
const questionForm = reactive({
  taskId: null as number | null,
  receiverId: null as number | null,
  content: ''
})

// 回复相关
const replyDialogVisible = ref(false)
const replyTask = ref<any>(null)
const replyContent = ref('')

const loadProjectDetail = async () => {
  const res: any = await request({ url: `/project/detail/${projectId}`, method: 'get' })
  Object.assign(projectInfo, res.data)
}

const loadMemberList = async () => {
  const res: any = await request({ url: `/project/member/list/${projectId}`, method: 'get' })
  memberList.value = res.data || []
}

const loadTaskList = async () => {
  const res: any = await request({
    url: '/task/page',
    method: 'get',
    params: { pageNum: 1, pageSize: 999, projectId }
  })
  taskList.value = res.data.records || []
  calculateStatistics()
}

const loadActivityList = async () => {
  try {
    const res: any = await request({ url: `/project/activity/list/${projectId}`, method: 'get' })
    activityList.value = res.data || []
  } catch (error) {
    console.error('加载项目动态失败', error)
    activityList.value = []
  }
}

const calculateStatistics = () => {
  statistics.total = taskList.value.length
  statistics.completed = taskList.value.filter((item: any) => item.status === 2).length
  statistics.processing = taskList.value.filter((item: any) => item.status === 1).length
  const now = new Date().getTime()
  statistics.expired = taskList.value.filter((item: any) => {
    return item.deadline && new Date(item.deadline).getTime() < now && item.status !== 2
  }).length
  progress.value = statistics.total ? Math.round(statistics.completed / statistics.total * 100) : 0
}

const openAddTaskDialog = () => {
  isEditTask.value = false
  resetTaskForm()
  taskDialogVisible.value = true
}

const handleEditTask = (row: any) => {
  isEditTask.value = true
  Object.assign(taskForm, row)
  taskDialogVisible.value = true
}

const submitTask = async () => {
  if (isEditTask.value) {
    await updateTaskApi(taskForm)
    ElMessage.success('修改成功')
  } else {
    await addTaskApi(taskForm)
    ElMessage.success('新增成功')
  }
  taskDialogVisible.value = false
  loadTaskList()
}

const handleDeleteTask = (id: number) => {
  ElMessageBox.confirm('确认删除该任务吗？', '提示', { type: 'warning' }).then(async () => {
    await deleteTaskApi(id)
    ElMessage.success('删除成功')
    loadTaskList()
  })
}

const resetTaskForm = () => {
  taskForm.id = null
  taskForm.title = ''
  taskForm.content = ''
  taskForm.projectId = Number(projectId)
  taskForm.priority = 1
  taskForm.status = 0
  taskForm.executorId = null
  taskForm.endTime = ''
}

// 状态修改
const openStatusDialog = (row: any) => {
  currentTask.value = row
  newStatus.value = row.status
  statusDialogVisible.value = true
}

const confirmStatusChange = async () => {
  if (!currentTask.value) return
  try {
    await updateTaskStatusApi({
      id: currentTask.value.id,
      status: newStatus.value
    })
    ElMessage.success('状态修改成功')
    statusDialogVisible.value = false
    loadTaskList()
  } catch (error) {
    console.error('状态修改失败', error)
  }
}

// 发送疑问
const openQuestionDialog = (row: any) => {
  questionForm.taskId = row.id
  questionForm.receiverId = null
  questionForm.content = ''
  questionDialogVisible.value = true
}

const sendQuestion = async () => {
  if (!questionForm.receiverId) {
    ElMessage.warning('请选择接收人')
    return
  }
  if (!questionForm.content.trim()) {
    ElMessage.warning('请输入疑问内容')
    return
  }
  try {
    await request({
      url: '/task/question',
      method: 'post',
      data: {
        taskId: questionForm.taskId,
        receiverId: questionForm.receiverId,
        content: questionForm.content
      }
    })
    ElMessage.success('疑问已发送')
    questionDialogVisible.value = false
  } catch (error) {
    console.error('发送疑问失败', error)
    ElMessage.error('发送失败，请稍后重试')
  }
}

// 回复疑问
const openReplyDialog = (row: any) => {
  replyTask.value = row
  replyContent.value = ''
  replyDialogVisible.value = true
}

const sendReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  if (!replyTask.value.executorId) {
    ElMessage.warning('该任务暂无执行人，无法回复')
    return
  }
  try {
    await request({
      url: '/task/reply',
      method: 'post',
      data: {
        taskId: replyTask.value.id,
        receiverId: replyTask.value.executorId,
        content: replyContent.value
      }
    })
    ElMessage.success('回复已发送')
    replyDialogVisible.value = false
  } catch (error) {
    console.error('发送回复失败', error)
    ElMessage.error('回复失败，请稍后重试')
  }
}

onMounted(() => {
  loadProjectDetail()
  loadMemberList()
  loadTaskList()
  loadActivityList()
})
</script>

<style scoped>
/* 样式与之前相同，省略，请保留原有样式 */
.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
}
.description {
  color: #666;
}
.statistics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.card-title {
  color: #999;
  margin-bottom: 15px;
}
.card-value {
  font-size: 32px;
  font-weight: bold;
}
.success {
  color: #67c23a;
}
.primary {
  color: #409eff;
}
.danger {
  color: #f56c6c;
}
.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}
.activity-title {
  font-size: 18px;
  font-weight: bold;
}
.activity-content {
  line-height: 24px;
}
.task-toolbar {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>