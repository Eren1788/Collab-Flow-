<template>
  <div>
    <el-button type="primary" @click="openDialog()">新增任务</el-button>
    <el-table :data="list" v-loading="loading">
      <el-table-column prop="id" label="ID"/>
      <el-table-column prop="title" label="任务标题"/>
      <el-table-column prop="content" label="任务内容"/>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">{{ statusText(row.status) }}</template>
      </el-table-column>
      <el-table-column prop="executorName" label="执行人"/>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="mini" @click="openDialog(row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="deleteTask(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible">
      <el-form :model="form">
        <el-form-item label="任务标题"><el-input v-model="form.title"/></el-form-item>
        <el-form-item label="任务内容"><el-input v-model="form.content"/></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option label="待开始" :value="0"/>
            <el-option label="进行中" :value="1"/>
            <el-option label="完成" :value="2"/>
          </el-select>
        </el-form-item>
        <el-form-item label="执行人">
          <el-select v-model="form.executorId" placeholder="选择执行人">
            <el-option v-for="user in users" :key="user.id" :label="user.nickname" :value="user.id"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { requestWithType } from '../utils/request'
import { ElMessage } from 'element-plus'

interface Task {
  id:number
  title:string
  content:string
  status:number
  executorId:number|null
  executorName?:string
}

interface User {
  id:number
  nickname:string
}

const list = ref<Task[]>([])
const users = ref<User[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增任务')
const form = ref<Task>({id:0,title:'',content:'',status:0,executorId:null})

const statusText = (status:number) => { switch(status){ case 0:return '待开始'; case 1:return '进行中'; case 2:return '完成'; default:return '' } }

const fetchList = async () => {
  loading.value=true
  try{
    const res = await requestWithType<{code:number,data:Task[],message:string}>({url:'/task/list',method:'get'})
    if(res.code===200) list.value=res.data
  }finally{loading.value=false}
}

const fetchUsers = async () => {
  try{
    const res = await requestWithType<{code:number,data:User[],message:string}>({url:'/user/list',method:'get'})
    if(res.code===200) users.value=res.data
  }catch(err){console.error(err)}
}

const openDialog = (row?:Task) => {
  if(row){ dialogTitle.value='编辑任务'; form.value={...row} }
  else{ dialogTitle.value='新增任务'; form.value={id:0,title:'',content:'',status:0,executorId:null} }
  dialogVisible.value=true
}

const saveTask = async () => {
  try{
    if(form.value.id) await requestWithType({url:'/task/update',method:'put',data:form.value})
    else await requestWithType({url:'/task/add',method:'post',data:form.value})
    ElMessage.success('操作成功')
    dialogVisible.value=false
    fetchList()
  }catch{ ElMessage.error('操作失败') }
}

const deleteTask = async (id:number) => {
  try{
    await requestWithType({url:`/task/delete/${id}`,method:'delete'})
    ElMessage.success('删除成功')
    fetchList()
  }catch{ ElMessage.error('删除失败') }
}

onMounted(()=>{
  fetchList()
  fetchUsers()
})
</script>