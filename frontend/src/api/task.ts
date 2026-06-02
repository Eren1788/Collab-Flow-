import request from '../utils/request'

/**
 * 任务分页
 */
export const getTaskPageApi = (params:any)=>{
  return request({
    url:'/task/page',
    method:'get',
    params
  })
}

/**
 * 新增任务
 */
export const addTaskApi = (data:any)=>{
  return request({
    url:'/task/add',
    method:'post',
    data
  })
}

/**
 * 修改任务
 */
export const updateTaskApi = (data:any)=>{
  return request({
    url:'/task/update',
    method:'put',
    data
  })
}

/**
 * 删除任务
 */
export const deleteTaskApi = (id:number)=>{
  return request({
    url:`/task/delete/${id}`,
    method:'delete'
  })
}

/**
 * 任务详情
 */
export const getTaskDetailApi = (id:number)=>{
  return request({
    url:`/task/detail/${id}`,
    method:'get'
  })
}

/**
 * 修改任务状态
 */
export const updateTaskStatusApi = (data: any) => {
  return request({
    url: '/task/status',
    method: 'put',
    data
  })
}