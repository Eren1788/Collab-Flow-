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
          <el-table :data="taskList" border stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="任务标题" />
            <el-table-column prop="status" label="任务状态">
              <template #default="scope">
                <el-tag v-if="scope.row.status === 0" type="info">待开始</el-tag>
                <el-tag v-else-if="scope.row.status === 1" type="primary">进行中</el-tag>
                <el-tag v-else-if="scope.row.status === 2" type="success">已完成</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级">
              <template #default="scope">
                <el-tag v-if="scope.row.priority === 1" type="danger">紧急</el-tag>
                <el-tag v-else type="info">普通</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止时间" width="180" />
          </el-table>
          <el-empty v-if="taskList.length === 0" description="暂无任务" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const projectId = route.params.id

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
  // 后端任务状态: 0-待开始, 1-进行中, 2-已完成
  statistics.completed = taskList.value.filter((item: any) => item.status === 2).length
  statistics.processing = taskList.value.filter((item: any) => item.status === 1).length
  const now = new Date().getTime()
  statistics.expired = taskList.value.filter((item: any) => {
    return item.deadline && new Date(item.deadline).getTime() < now && item.status !== 2
  }).length
  progress.value = statistics.total ? Math.round(statistics.completed / statistics.total * 100) : 0
}

onMounted(() => {
  loadProjectDetail()
  loadMemberList()
  loadTaskList()
  loadActivityList()
})
</script>

<style scoped>
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
</style>