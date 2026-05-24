<template>
  <div class="project-container">
    <!-- 顶部工具栏 -->
    <el-card shadow="never">
      <div class="toolbar">
        <div class="left">
          <el-input
            v-model="keyword"
            placeholder="请输入项目名称"
            clearable
            style="width:220px"
          />
          <el-select
            v-model="status"
            placeholder="项目状态"
            clearable
            style="width:150px"
          >
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已暂停" :value="3" />
          </el-select>
          <el-button type="primary" @click="loadProjectList">搜索</el-button>
        </div>
        <div class="right">
          <el-button type="primary" @click="openAddDialog">新增项目</el-button>
        </div>
      </div>
    </el-card>

    <!-- 项目表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" border stripe style="width:100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="项目名称" />
        <el-table-column prop="description" label="项目描述" show-overflow-tooltip />
        <el-table-column prop="status" label="项目状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="primary">进行中</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="success">已完成</el-tag>
            <el-tag v-else type="warning">已暂停</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="项目进度" width="220">
          <template #default="scope">
            <el-progress
              :percentage="scope.row.progress || 0"
              :status="scope.row.progress === 100 ? 'success' : ''"
            />
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="180" />

        <!-- 操作列：两行布局，居中 -->
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button type="success" size="small" @click="handleMember(scope.row)">成员</el-button>
            </div>
            <div class="action-buttons">
              <el-button type="warning" size="small" @click="handleDetail(scope.row)">详情</el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
            </div>
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

    <!-- 新增/编辑项目弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑项目' : '新增项目'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="项目状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已暂停" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 项目成员弹窗 -->
    <el-dialog v-model="memberDialogVisible" title="项目成员" width="600px">
      <div class="member-toolbar">
        <el-input v-model="memberForm.userId" placeholder="请输入用户ID" style="width:200px" />
        <el-button type="primary" @click="addMember">添加成员</el-button>
      </div>
      <el-table :data="memberList" border stripe>
        <el-table-column prop="userId" label="用户ID" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button type="danger" size="small" @click="handleDeleteMember(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const router = useRouter()

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const status = ref()
const dialogVisible = ref(false)
const memberDialogVisible = ref(false)
const isEdit = ref(false)
const currentProjectId = ref()
const memberList = ref([])

const form = reactive<any>({
  id: null,
  name: '',
  description: '',
  status: 1
})

const memberForm = reactive<any>({
  projectId: null,
  userId: ''
})

const loadProjectList = async () => {
  try {
    const res: any = await request({
      url: '/project/page',
      method: 'get',
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: keyword.value,
        status: status.value
      }
    })
    tableData.value = (res.data.records || []).map((item: any) => ({
      ...item,
      progress: item.taskCount ? Math.min(item.taskCount * 10, 100) : 0
    }))
    total.value = res.data.total || 0
  } catch (error) {
    console.log(error)
  }
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  loadProjectList()
}

const openAddDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    if (isEdit.value) {
      await request({
        url: '/project/update',
        method: 'put',
        data: form
      })
      ElMessage.success('修改成功')
    } else {
      await request({
        url: '/project/add',
        method: 'post',
        data: form
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadProjectList()
  } catch (error) {
    console.log(error)
  }
}

const handleDetail = (row: any) => {
  router.push(`/project/detail/${row.id}`)
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确认删除该项目吗？', '提示', { type: 'warning' }).then(async () => {
    await request({ url: `/project/delete/${id}`, method: 'delete' })
    ElMessage.success('删除成功')
    loadProjectList()
  })
}

const handleMember = async (row: any) => {
  currentProjectId.value = row.id
  memberForm.projectId = row.id
  memberDialogVisible.value = true
  loadMemberList()
}

const loadMemberList = async () => {
  const res: any = await request({
    url: `/project/member/list/${currentProjectId.value}`,
    method: 'get'
  })
  memberList.value = res.data || []
}

const addMember = async () => {
  await request({
    url: '/project/member/add',
    method: 'post',
    data: memberForm
  })
  ElMessage.success('添加成员成功')
  memberForm.userId = ''
  loadMemberList()
}

const handleDeleteMember = (id: number) => {
  ElMessageBox.confirm('确认删除该成员吗？', '提示', { type: 'warning' }).then(async () => {
    await request({ url: `/project/member/delete/${id}`, method: 'delete' })
    ElMessage.success('删除成员成功')
    loadMemberList()
  })
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.description = ''
  form.status = 1
}

onMounted(() => {
  loadProjectList()
})
</script>

<style scoped>
.project-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.left {
  display: flex;
  gap: 10px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.member-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin: 4px 0;
}
.action-buttons .el-button {
  margin: 0;
}
</style>