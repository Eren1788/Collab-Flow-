<template>

  <div class="file-container">

    <!-- 顶部操作 -->

    <el-card shadow="never">

      <div class="toolbar">

        <div class="left">

          <el-upload
            :show-file-list="false"
            :http-request="handleUpload"
          >

            <el-button type="primary">

              上传文件

            </el-button>

          </el-upload>

        </div>

        <div class="right">

          <el-button
            type="success"
            @click="loadFileList"
          >
            刷新列表
          </el-button>

        </div>

      </div>

    </el-card>

    <!-- 文件列表 -->

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
          prop="fileName"
          label="文件名称"
          min-width="260"
        />

        <el-table-column
          prop="fileSize"
          label="文件大小"
          width="140"
        >

          <template #default="scope">

            {{ formatFileSize(scope.row.fileSize) }}

          </template>

        </el-table-column>

        <el-table-column
          prop="fileType"
          label="文件类型"
          width="120"
        />

        <el-table-column
          prop="createTime"
          label="上传时间"
          width="180"
        />

        <el-table-column
          label="操作"
          width="220"
          fixed="right"
        >

          <template #default="scope">

            <el-button
              type="primary"
              size="small"
              @click="downloadFile(scope.row.id)"
            >
              下载
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

    </el-card>

  </div>

</template>

<script setup lang="ts">

import {

  ref,
  onMounted

} from 'vue'

import {

  ElMessage,
  ElMessageBox

} from 'element-plus'

import request from '../utils/request'

const tableData = ref([])

/**
 * 文件列表
 */
const loadFileList = async ()=>{

  try{

    const res:any = await request({

      url:'/file/list',

      method:'get'
    })

    tableData.value = res.data || []

  }catch(error){

    console.log(error)
  }
}

/**
 * 上传文件
 */
const handleUpload = async (options:any)=>{

  const formData = new FormData()

  formData.append('file',options.file)

  try{

    await request({

      url:'/file/upload',

      method:'post',

      data:formData,

      headers:{

        'Content-Type':'multipart/form-data'
      }
    })

    ElMessage.success('上传成功')

    loadFileList()

  }catch(error){

    console.log(error)
  }
}

/**
 * 下载文件
 */
const downloadFile = (id:number)=>{

  window.open(

    `http://localhost:8080/file/download/${id}`

  )
}

/**
 * 删除文件
 */
const handleDelete = (id:number)=>{

  ElMessageBox.confirm(

    '确认删除该文件吗？',

    '提示',

    {

      type:'warning'
    }

  ).then(async ()=>{

    await request({

      url:`/file/delete/${id}`,

      method:'delete'
    })

    ElMessage.success('删除成功')

    loadFileList()
  })
}

/**
 * 文件大小格式化
 */
const formatFileSize = (size:number)=>{

  if(!size){

    return '0 B'
  }

  if(size < 1024){

    return size + ' B'
  }

  if(size < 1024 * 1024){

    return (size / 1024).toFixed(2) + ' KB'
  }

  return (size / 1024 / 1024).toFixed(2) + ' MB'
}

onMounted(()=>{

  loadFileList()
})

</script>

<style scoped>

.file-container{

  display:flex;

  flex-direction:column;

  gap:20px;
}

.toolbar{

  display:flex;

  justify-content:space-between;

  align-items:center;
}

</style>