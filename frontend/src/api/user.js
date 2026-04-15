import api from './axios'

export const userApi = {
  // 注册
  register: (data) => api.post('/user/register', data),
  // 登录
  login: (data) => api.post('/user/login', data),
  // 获取用户信息
  getUserInfo: () => api.get('/user/info'),
  // 修改密码
  updatePassword: (data) => api.post('/user/update-password', data),
  // 修改资料
  updateUserInfo: (data) => api.post('/user/update-info', data),
  // 获取用户精灵数量
  getElfCount: () => api.get('/user/elf-count'),
  // 获取用户资产
  getUserAsset: () => api.get('/user/asset')
}