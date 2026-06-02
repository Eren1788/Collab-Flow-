import request from '../utils/request'

/**
 * 获取指定任务的评论列表
 * @param taskId 任务ID
 */
export function getCommentListApi(taskId: number) {
  return request({
    url: `/comment/list/${taskId}`,
    method: 'get'
  })
}

/**
 * 新增评论
 * @param data { taskId: number, content: string, parentId?: number }
 */
export function addCommentApi(data: any) {
  return request({
    url: '/comment/add',
    method: 'post',
    data
  })
}

/**
 * 删除评论
 * @param id 评论ID
 */
export function deleteCommentApi(id: number) {
  return request({
    url: `/comment/delete/${id}`,
    method: 'delete'
  })
}

/**
 * 标记已读
 */
export function markTaskReadApi(taskId: number) {
  return request({
    url: `/task/read/${taskId}`,
    method: 'put'
  })
}

export function getProjectCommentListApi(projectId: number) {
  return request({
    url: `/comment/project/list/${projectId}`,
    method: 'get'
  })
}

export function addProjectCommentApi(data: { projectId: number; content: string; parentId?: number }) {
  return request({
    url: '/comment/project/add',
    method: 'post',
    data
  })
}