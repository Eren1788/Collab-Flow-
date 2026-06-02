import request from '../utils/request'

/**
 * 获取系统Logo的URL（公开接口，无需token）
 */
export function getLogoUrlApi() {
  return request({
    url: '/logo/url',
    method: 'get'
  })
}

/**
 * 上传Logo（需要管理员权限）
 * @param file 图片文件
 */
export function uploadLogoApi(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/logo/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}