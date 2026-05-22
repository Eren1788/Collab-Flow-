import request from '../utils/request'

/**
 * 用户登录
 */
export function loginApi(data: any) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

/**
 * 用户列表
 */
export function getUserListApi(params?: any) {
  return request({
    url: '/user/list',
    method: 'get',
    params
  })
}

/**
 * 用户分页
 */
export function getUserPageApi(params: any) {
  return request({
    url: '/user/page',
    method: 'get',
    params
  })
}

/**
 * 修改用户
 */
export function updateUserApi(data: any) {
  return request({
    url: '/user/update',
    method: 'put',
    data
  })
}

/**
 * 删除用户
 */
export function deleteUserApi(id: number) {
  return request({
    url: `/user/delete/${id}`,
    method: 'delete'
  })
}

/**
 * 用户注册
 */
export function registerApi(data: any) {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

/**
 * 修改用户状态（管理员权限）
 */
export function updateUserStatusApi(id: number, status: number) {
  return updateUserApi({
    id,
    status
  })
}

/**
 * 获取角色列表
 */
export function getRoleListApi() {
  return request({
    url: '/role/list',
    method: 'get'
  })
}