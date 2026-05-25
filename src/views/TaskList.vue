<template>
  <div class="task-container">

    <!-- 统计卡片（可点击跳转） -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :class="{ active: activeFilter === 'total' }" @click="goToFilter('total')">
          <div class="stat-card-inner">
            <div class="title">任务总数</div>
            <div class="value">{{ statistics.total || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :class="{ active: activeFilter === 'doing' }" @click="goToFilter('doing')">
          <div class="stat-card-inner">
            <div class="title">进行中</div>
            <div class="value">{{ statistics.doing || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :class="{ active: activeFilter === 'done' }" @click="goToFilter('done')">
          <div class="stat-card-inner">
            <div class="title">已完成</div>
            <div class="value">{{ statistics.done || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :class="{ active: activeFilter === 'expired' }" @click="goToFilter('expired')">
          <div class="stat-card-inner">
            <div class="title">已逾期</div>
            <div class="value">{{ statistics.expired || 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索区域 -->
    <el-card shadow="never">
      <div class="toolbar">
        <div class="left">
          <el-input
            v-model="keyword"
            placeholder="请输入任务名称"
            clearable
            style="width:220px"
            @keyup.enter="searchTasks"
          />
          <el-select
            v-model="status"
            placeholder="任务状态"
            clearable
            style="width:140px"
            @change="searchTasks"
          >
            <el-option label="待开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
          <el-select
            v-model="priority"
            placeholder="优先级"
            clearable
            style="width:140px"
            @change="searchTasks"
          >
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
          <el-button type="primary" @click="searchTasks">搜索</el-button>
          <el-button v-if="activeFilter !== null" @click="clearFilterAndSearch">清除筛选</el-button>
        </div>
      </div>
    </el-card>

    <!-- 任务表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" border stripe style="width:100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="任务标题" min-width="200" />
        <el-table-column prop="projectName" label="所属项目" width="180" />
        <el-table-column prop="executorName" label="执行人" width="140" />
        <el-table-column prop="priority" label="优先级" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.priority === 3" type="danger">高</el-tag>
            <el-tag v-else-if="scope.row.priority === 2" type="warning">中</el-tag>
            <el-tag v-else>低</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="info">待开始</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="primary">进行中</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="success">已完成</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="180">
          <template #default="scope">
            {{ scope.row.deadline ? scope.row.deadline.replace('T', ' ') : '未设置' }}
          </template>
        </el-table-column>
        <el-table-column label="进度" width="180">
          <template #default="scope">
            <el-progress
              :percentage="getTaskProgress(scope.row.status)"
              :status="getProgressStatus(scope.row.status)"
              :show-text="true"
            />
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getTaskPageApi } from '../api/task'
import { useUserStore } from '../store/user'

// 定义任务数据类型
interface TaskItem {
  id: number
  title: string
  projectName: string
  executorName: string
  priority: number
  status: number
  deadline: string | null
  [key: string]: any
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 权限判断
const isSuperAdmin = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员'
})

const currentUserId = computed(() => userStore.info?.id)

// 数据
const tableData = ref<TaskItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const status = ref<number | null>(null)
const priority = ref<number | null>(null)
const statistics = ref<any>({})

// 当前激活的筛选类型
const activeFilter = ref<string | null>(null)

// 辅助函数
const getTaskProgress = (taskStatus: number) => {
  switch (taskStatus) {
    case 0: return 0
    case 1: return 50
    case 2: return 100
    default: return 0
  }
}

const getProgressStatus = (taskStatus: number) => {
  if (taskStatus === 2) return 'success'
  return ''
}

const calculateExpired = (tasks: TaskItem[]) => {
  const now = new Date().getTime()
  return tasks.filter(task => {
    return task.deadline && new Date(task.deadline).getTime() < now && task.status !== 2
  }).length
}

// 加载任务列表
const loadTaskList = async () => {
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      priority: priority.value
    }
    if (activeFilter.value === 'doing') {
      params.status = 1
    } else if (activeFilter.value === 'done') {
      params.status = 2
    } else if (activeFilter.value === 'expired') {
      // 逾期筛选：后端不传status，前端过滤
    } else if (activeFilter.value === 'total') {
      // 全部
    } else {
      if (status.value !== null) {
        params.status = status.value
      }
    }

    // 非管理员只能看到自己的任务
    if (!isSuperAdmin.value && currentUserId.value) {
      params.executorId = currentUserId.value
    }

    const res: any = await getTaskPageApi(params)
    let records: TaskItem[] = res.data.records || []
    // 逾期筛选前端过滤
    if (activeFilter.value === 'expired') {
      const now = new Date().getTime()
      records = records.filter((task: TaskItem) => {
        return task.deadline && new Date(task.deadline).getTime() < now && task.status !== 2
      })
    }
    tableData.value = records
    total.value = activeFilter.value === 'expired' ? records.length : res.data.total || 0
    await loadStatistics()
  } catch (error) {
    ElMessage.error('加载任务列表失败')
  }
}

// 加载统计
const loadStatistics = async () => {
  try {
    const params: any = {}
    if (!isSuperAdmin.value && currentUserId.value) {
      params.executorId = currentUserId.value
    }
    const res: any = await request({ url: '/task/statistics', method: 'get', params })
    statistics.value = res.data || {}
    if (statistics.value.expired === undefined) {
      const allParams: any = { pageNum: 1, pageSize: 999 }
      if (!isSuperAdmin.value && currentUserId.value) {
        allParams.executorId = currentUserId.value
      }
      const allRes: any = await getTaskPageApi(allParams)
      statistics.value.expired = calculateExpired(allRes.data.records || [])
    }
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

// 跳转筛选
const goToFilter = (type: string) => {
  if (activeFilter.value === type) {
    clearFilterAndSearch()
    return
  }
  router.push({ path: '/tasks', query: { filter: type } })
}

// 搜索按钮
const searchTasks = () => {
  router.push({ path: '/tasks', query: {} })
}

// 清除所有筛选
const clearFilterAndSearch = () => {
  keyword.value = ''
  status.value = null
  priority.value = null
  pageNum.value = 1
  router.push({ path: '/tasks', query: {} })
}

// 分页切换
const handlePageChange = (page: number) => {
  pageNum.value = page
  loadTaskList()
}

// 初始化路由参数
const initFromRoute = () => {
  const filter = route.query.filter as string
  if (filter === 'doing' || filter === 'done' || filter === 'expired' || filter === 'total') {
    activeFilter.value = filter
  } else {
    activeFilter.value = null
  }
  pageNum.value = 1
  if (activeFilter.value) {
    status.value = null
  }
  loadTaskList()
}

// 监听路由变化
watch(() => route.query.filter, () => {
  initFromRoute()
})

onMounted(() => {
  initFromRoute()
})
</script>

<style scoped>
.task-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.stat-card.active {
  border: 2px solid #409eff;
  background-color: #ecf5ff;
}
.stat-card-inner {
  text-align: center;
}
.stat-card .title {
  color: #999;
  margin-bottom: 10px;
}
.stat-card .value {
  font-size: 30px;
  font-weight: bold;
  color: #409EFF;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.left {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>