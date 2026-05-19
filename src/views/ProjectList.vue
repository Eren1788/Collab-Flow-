<template>
  <div>
    <el-button type="primary" @click="openDialog()">新增项目</el-button>
    <el-table :data="list" v-loading="loading">
      <el-table-column prop="id" label="ID"/>
      <el-table-column prop="name" label="项目名称"/>
      <el-table-column prop="description" label="项目描述"/>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">{{ statusText(row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="mini" @click="openDialog(row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="deleteProject(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible">
      <el-form :model="form">
        <el-form-item label="项目名称"><el-input v-model="form.name"/></el-form-item>
        <el-form-item label="项目描述"><el-input v-model="form.description"/></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option label="未开始" :value="0"/>
            <el-option label="进行中" :value="1"/>
            <el-option label="已完成" :value="2"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveProject">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { requestWithType } from '../utils/request'
import { ElMessage } from 'element-plus'

interface Project {
  id: number
  name: string
  description: string
  status: number
}

const list = ref<Project[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增项目')
const form = ref<Project>({id:0,name:'',description:'',status:0})

const statusText = (status:number) => {
  switch(status){ case 0: return '未开始'; case 1: return '进行中'; case 2: return '已完成'; default: return ''; }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await requestWithType<{ code:number, data:Project[], message:string }>({url:'/project/list', method:'get'})
    if(res.code===200) list.value = res.data
  } finally { loading.value = false }
}

const openDialog = (row?:Project) => {
  if(row){ dialogTitle.value='编辑项目'; form.value={...row} }
  else{ dialogTitle.value='新增项目'; form.value={id:0,name:'',description:'',status:0} }
  dialogVisible.value = true
}

const saveProject = async () => {
  try{
    if(form.value.id) await requestWithType({url:'/project/update', method:'put', data:form.value})
    else await requestWithType({url:'/project/add', method:'post', data:form.value})
    ElMessage.success('操作成功')
    dialogVisible.value=false
    fetchList()
  }catch{ ElMessage.error('操作失败') }
}

const deleteProject = async (id:number) => {
  try{
    await requestWithType({url:`/project/delete/${id}`, method:'delete'})
    ElMessage.success('删除成功')
    fetchList()
  }catch{ ElMessage.error('删除失败') }
}

onMounted(()=>fetchList())
</script>