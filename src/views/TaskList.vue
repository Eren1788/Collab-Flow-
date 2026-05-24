<template>

  <div class="task-container">

    <!-- 统计卡片 -->

    <el-row :gutter="20">

      <el-col :span="6">

        <el-card shadow="hover">

          <div class="stat-card">

            <div class="title">任务总数</div>

            <div class="value">
              <!-- {{ statistics.total || 0 }} -->
                {{ statistics.total || 0 }}
            </div>

          </div>

        </el-card>

      </el-col>

      <el-col :span="6">

        <el-card shadow="hover">

          <div class="stat-card">

            <div class="title">进行中</div>

            <div class="value">
              // 任务总数
              <!-- {{ statistics.processing || 0 }} -->
              {{ statistics.doing || 0 }}
            </div>

          </div>

        </el-card>

      </el-col>

      <el-col :span="6">

        <el-card shadow="hover">

          <div class="stat-card">

            <div class="title">已完成</div>

            <div class="value">
              
              <!-- {{ statistics.finished || 0 }} -->
                {{ statistics.done || 0 }}
            </div>

          </div>

        </el-card>

      </el-col>

      <el-col :span="6">

        <el-card shadow="hover">

          <div class="stat-card">

            <div class="title">已逾期</div>

            <div class="value">
              
              <!-- {{ statistics.timeout || 0 }} -->
                {{ statistics.todo || 0 }}
            </div>

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
          />

          <el-select
            v-model="status"
            placeholder="任务状态"
            clearable
            style="width:140px"
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
          >

            <el-option label="低" :value="1" />

            <el-option label="中" :value="2" />

            <el-option label="高" :value="3" />

          </el-select>

          <el-button
            type="primary"
            @click="loadTaskList"
          >
            搜索
          </el-button>

        </div>

        <div class="right">

          <el-button
            type="primary"
            @click="openAddDialog"
          >
            新增任务
          </el-button>

        </div>

      </div>

    </el-card>

    <!-- 表格 -->

    <el-card shadow="never">

      <el-table
        :data="tableData"
        border
        stripe
        style="width:100%"
      >

        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />

        <el-table-column
          prop="title"
          label="任务标题"
          min-width="200"
        />

        <el-table-column
          prop="projectName"
          label="所属项目"
          width="180"
        />

        <el-table-column
          prop="executorName"
          label="执行人"
          width="140"
        />

        <el-table-column
          prop="priority"
          label="优先级"
          width="120"
        >

          <template #default="scope">

            <el-tag
              v-if="scope.row.priority === 3"
              type="danger"
            >
              高
            </el-tag>

            <el-tag
              v-else-if="scope.row.priority === 2"
              type="warning"
            >
              中
            </el-tag>

            <el-tag v-else>
              低
            </el-tag>

          </template>

        </el-table-column>

        <el-table-column
          prop="status"
          label="状态"
          width="120"
        >

          <template #default="scope">

        <el-tag
          v-if="scope.row.status === 0"
          type="info"
        >
          待开始
        </el-tag>

        <el-tag
          v-else-if="scope.row.status === 1"
          type="primary"
        >
          进行中
        </el-tag>

        <el-tag
          v-else-if="scope.row.status === 2"
          type="success"
        >
          已完成
        </el-tag>

</template>

        </el-table-column>

        <el-table-column
          prop="endTime"
          label="截止时间"
          width="180"
        />

        <el-table-column
          label="操作"
          width="360"
          fixed="right"
        >

          <template #default="scope">

            <el-button
              type="primary"
              size="small"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>

            <el-button
              type="success"
              size="small"
              @click="handleAssign(scope.row)"
            >
              指派
            </el-button>

            <el-button
              type="warning"
              size="small"
              @click="handleStatus(scope.row)"
            >
              状态
            </el-button>

            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row.id)"
            >
              删除
            </el-button>

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

    <!-- 新增/编辑 -->

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑任务' : '新增任务'"
      width="650px"
    >

      <el-form
        :model="form"
        label-width="100px"
      >

        <el-form-item label="任务标题">

          <el-input v-model="form.title" />

        </el-form-item>

        <el-form-item label="任务描述">

          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
          />

        </el-form-item>

        <el-form-item label="项目ID">

          <el-input v-model="form.projectId" />

        </el-form-item>

        <el-form-item label="优先级">

          <el-select
            v-model="form.priority"
            style="width:100%"
          >

            <el-option label="低" :value="1" />

            <el-option label="中" :value="2" />

            <el-option label="高" :value="3" />

          </el-select>

        </el-form-item>

        <el-form-item label="截止时间">

          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:100%"
          />

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button @click="dialogVisible = false">
          取消
        </el-button>

        <el-button
          type="primary"
          @click="submitForm"
        >
          确定
        </el-button>

      </template>

    </el-dialog>

    <!-- 指派任务 -->

    <el-dialog
      v-model="assignDialogVisible"
      title="指派任务"
      width="450px"
    >

      <el-form
        :model="assignForm"
        label-width="100px"
      >

        <el-form-item label="执行人ID">

          <el-input v-model="assignForm.executorId" />

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button @click="assignDialogVisible = false">
          取消
        </el-button>

        <el-button
          type="primary"
          @click="submitAssign"
        >
          确定
        </el-button>

      </template>

    </el-dialog>

    <!-- 修改状态 -->

    <el-dialog
      v-model="statusDialogVisible"
      title="修改状态"
      width="450px"
    >

      <el-form
        :model="statusForm"
        label-width="100px"
      >

        <el-form-item label="任务状态">

          <el-select
            v-model="statusForm.status"
            style="width:100%"
          >

            <el-option label="待开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />

          </el-select>

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button @click="statusDialogVisible = false">
          取消
        </el-button>

        <el-button
          type="primary"
          @click="submitStatus"
        >
          确定
        </el-button>

      </template>

    </el-dialog>

  </div>

</template>

<script setup lang="ts">

import {

  ref,
  reactive,
  onMounted

} from 'vue'

import {

  ElMessage,
  ElMessageBox

} from 'element-plus'

import request from '../utils/request'

const tableData = ref([])

const total = ref(0)

const pageNum = ref(1)

const pageSize = ref(10)

const keyword = ref('')

const status = ref()

const priority = ref()

const statistics = ref<any>({})

const dialogVisible = ref(false)

const assignDialogVisible = ref(false)

const statusDialogVisible = ref(false)

const isEdit = ref(false)

const form = reactive<any>({
  id:null,
  title:'',
  content:'',
  projectId:'',
  priority:1,
  endTime:''
})

const assignForm = reactive<any>({
  taskId:null,
  executorId:''
})

const statusForm = reactive<any>({
  taskId:null,
  status:1
})

/**
 * 任务分页
 */
const loadTaskList = async ()=>{

  const res:any = await request({

    url:'/task/page',

    method:'get',

    params:{

      pageNum:pageNum.value,

      pageSize:pageSize.value,

      keyword:keyword.value,

      status:status.value,

      priority:priority.value
    }
  })

  tableData.value = res.data.records || []

  total.value = res.data.total || 0
}

/**
 * 任务统计
 */
const loadStatistics = async ()=>{

  const res:any = await request({

    url:'/task/statistics',

    method:'get'
  })

  statistics.value = res.data || {}
}

/**
 * 分页
 */
const handlePageChange = (page:number)=>{

  pageNum.value = page

  loadTaskList()
}

/**
 * 新增
 */
const openAddDialog = ()=>{

  isEdit.value = false

  resetForm()

  dialogVisible.value = true
}

/**
 * 编辑
 */
const handleEdit = (row:any)=>{

  isEdit.value = true

  Object.assign(form,row)

  dialogVisible.value = true
}

/**
 * 提交
 */
const submitForm = async ()=>{

  if(isEdit.value){

    await request({

      url:'/task/update',

      method:'put',

      data:form
    })

    ElMessage.success('修改成功')

  }else{

    await request({

      url:'/task/add',

      method:'post',

      data:form
    })

    ElMessage.success('新增成功')
  }

  dialogVisible.value = false

  loadTaskList()

  loadStatistics()
}

/**
 * 删除
 */
const handleDelete = (id:number)=>{

  ElMessageBox.confirm(

    '确认删除该任务吗？',

    '提示',

    {

      type:'warning'
    }

  ).then(async ()=>{

    await request({

      url:`/task/delete/${id}`,

      method:'delete'
    })

    ElMessage.success('删除成功')

    loadTaskList()

    loadStatistics()
  })
}

/**
 * 指派
 */
const handleAssign = (row:any)=>{

  assignForm.taskId = row.id

  assignForm.executorId = row.executorId

  assignDialogVisible.value = true
}

/**
 * 提交指派
 */
const submitAssign = async ()=>{

  await request({

    url:'/task/assign',

    method:'put',

    data:assignForm
  })

  ElMessage.success('指派成功')

  assignDialogVisible.value = false

  loadTaskList()
}

/**
 * 修改状态
 */
const handleStatus = (row:any)=>{

  statusForm.taskId = row.id

  statusForm.status = row.status

  statusDialogVisible.value = true
}

/**
 * 提交状态
 */
const submitStatus = async ()=>{

  await request({

    url:'/task/status',

    method:'put',

    data:statusForm
  })

  ElMessage.success('状态修改成功')

  statusDialogVisible.value = false

  loadTaskList()

  loadStatistics()
}

/**
 * 重置
 */
const resetForm = ()=>{

  form.id = null

  form.title = ''

  form.content = ''

  form.projectId = ''

  form.priority = 1

  form.endTime = ''
}

onMounted(()=>{

  loadTaskList()

  loadStatistics()
})

</script>

<style scoped>

.task-container{

  display:flex;

  flex-direction:column;

  gap:20px;
}

.stat-card{

  text-align:center;
}

.stat-card .title{

  color:#999;

  margin-bottom:10px;
}

.stat-card .value{

  font-size:30px;

  font-weight:bold;

  color:#409EFF;
}

.toolbar{

  display:flex;

  justify-content:space-between;

  align-items:center;
}

.left{

  display:flex;

  gap:10px;
}

.pagination{

  margin-top:20px;

  display:flex;

  justify-content:flex-end;
}

</style>