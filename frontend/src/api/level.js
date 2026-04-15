import api from './axios'

export const levelApi = {
  // 获取关卡列表
  getLevelList: () => api.get('/level/list'),
  // 获取关卡信息
  getLevelInfo: (levelId) => api.get(`/level/info?levelId=${levelId}`),
  // 进入关卡
  enterLevel: (levelId, userElfId) => api.post(`/level/enter?levelId=${levelId}&userElfId=${userElfId}`)
}