<template>
  <div class="task-container">

    <!-- 统计卡片：仅超级管理员可见 -->
    <div class="cf-stat-grid" v-if="isSuperAdmin">
      <el-card shadow="hover" class="cf-stat-card" :class="{ active: activeStat === 'total' }" @click="filterByStatus('total')">
        <div class="cf-stat-label">任务总数</div>
        <div class="cf-stat-value">{{ statistics.total || 0 }}</div>
      </el-card>
      <el-card shadow="hover" class="cf-stat-card" :class="{ active: activeStat === 'doing' }" @click="filterByStatus('doing')">
        <div class="cf-stat-label">进行中</div>
        <div class="cf-stat-value is-primary">{{ statistics.doing || 0 }}</div>
      </el-card>
      <el-card shadow="hover" class="cf-stat-card" :class="{ active: activeStat === 'done' }" @click="filterByStatus('done')">
        <div class="cf-stat-label">已完成</div>
        <div class="cf-stat-value is-success">{{ statistics.done || 0 }}</div>
      </el-card>
      <el-card shadow="hover" class="cf-stat-card" :class="{ active: activeStat === 'expired' }" @click="filterByStatus('expired')">
        <div class="cf-stat-label">已逾期</div>
        <div class="cf-stat-value is-danger">{{ statistics.expired || 0 }}</div>
      </el-card>
    </div>

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
          <el-button v-if="activeStat !== null" @click="clearFilterAndSearch">清除筛选</el-button>
        </div>
      </div>
    </el-card>

    <!-- 任务表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" border stripe style="width:100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="任务标题" min-width="200" />
        <el-table-column prop="projectName" label="所属项目" width="180" />
        <!-- 修改执行人列：支持多执行人显示 -->
        <el-table-column label="执行人" width="160">
          <template #default="scope">
            <span v-if="scope.row.executorNames && scope.row.executorNames.length">
              {{ scope.row.executorNames.join('、') }}
            </span>
            <span v-else>未指派</span>
          </template>
        </el-table-column>
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
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getTaskPageApi } from '../api/task'
import { useUserStore } from '../store/user'

// 定义任务数据类型（扩展 executorNames）
interface TaskItem {
  id: number
  title: string
  projectName: string
  executorNames?: string[]   // 改为数组
  priority: number
  status: number
  deadline: string | null
  [key: string]: any
}

const route = useRoute()
const userStore = useUserStore()

// 判断当前用户是否是超级管理员
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

// 当前激活的筛选类型（用于高亮）
const activeStat = ref<string | null>(null)

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

const loadTaskList = async () => {
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      priority: priority.value
    }
    if (activeStat.value === 'doing') {
      params.status = 1
    } else if (activeStat.value === 'done') {
      params.status = 2
    } else if (activeStat.value === 'expired') {
      // 逾期筛选不传status，前端过滤
    } else if (activeStat.value === 'total') {
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
    if (activeStat.value === 'expired') {
      const now = new Date().getTime()
      records = records.filter((task: TaskItem) => {
        return task.deadline && new Date(task.deadline).getTime() < now && task.status !== 2
      })
    }
    tableData.value = records
    total.value = activeStat.value === 'expired' ? records.length : res.data.total || 0
    await loadStatistics()
  } catch (error) {
    ElMessage.error('加载任务列表失败')
  }
}

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

const filterByStatus = (type: string) => {
  if (activeStat.value === type) {
    clearFilterAndSearch()
    return
  }
  activeStat.value = type
  pageNum.value = 1
  status.value = null
  loadTaskList()
}

const searchTasks = () => {
  activeStat.value = null
  pageNum.value = 1
  loadTaskList()
}

const clearFilterAndSearch = () => {
  keyword.value = ''
  status.value = null
  priority.value = null
  activeStat.value = null
  pageNum.value = 1
  loadTaskList()
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  loadTaskList()
}

// 监听路由参数变化
watch(() => route.query.filter, (newFilter) => {
  if (newFilter === 'doing' || newFilter === 'done' || newFilter === 'expired' || newFilter === 'total') {
    activeStat.value = newFilter as string
  } else {
    activeStat.value = null
  }
  pageNum.value = 1
  if (activeStat.value) {
    status.value = null
  }
  loadTaskList()
})

onMounted(() => {
  loadTaskList()
})
</script>

<style scoped>
.task-container { display: flex; flex-direction: column; gap: 24px; }

:deep(.el-table) { border-radius: var(--cf-radius-md); overflow: hidden; }
:deep(.el-table td) { padding: 10px 0; }
:deep(.el-table th.el-table__cell) { padding: 10px 0; }

.toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.left { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }

.pagination { margin-top: 24px; display: flex; justify-content: flex-end; }
.pagination :deep(.el-pager li) { border-radius: var(--cf-radius-sm); min-width: 32px; }
.pagination :deep(.btn-prev), .pagination :deep(.btn-next) { border-radius: var(--cf-radius-sm); }

:deep(.el-card) { transition: var(--cf-transition-slow); }
</style>