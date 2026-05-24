import request from '../utils/request'

/**
 * 项目分页
 */
export const getProjectPageApi = (params:any) => {
  return request({
    url:'/project/page',
    method:'get',
    params
  })
}

/**
 * 项目列表
 */
export const getProjectListApi = () => {
  return request({
    url:'/project/list',
    method:'get'
  })
}

/**
 * 新增项目
 */
export const addProjectApi = (data:any) => {
  return request({
    url:'/project/add',
    method:'post',
    data
  })
}

/**
 * 修改项目
 */
export const updateProjectApi = (data:any) => {
  return request({
    url:'/project/update',
    method:'put',
    data
  })
}

/**
 * 删除项目
 */
export const deleteProjectApi = (id:number) => {
  return request({
    url:`/project/delete/${id}`,
    method:'delete'
  })
}

/**
 * 项目详情
 */
export const getProjectDetailApi = (id:number) => {
  return request({
    url:`/project/detail/${id}`,
    method:'get'
  })
}

/**
 * 添加项目成员
 */
export const addProjectMemberApi = (data:any) => {
  return request({
    url:'/project/member/add',
    method:'post',
    data
  })
}

/**
 * 项目成员列表
 */
export const getProjectMemberListApi = (projectId:number) => {
  return request({
    url:`/project/member/list/${projectId}`,
    method:'get'
  })
}