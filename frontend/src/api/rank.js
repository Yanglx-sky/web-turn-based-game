import api from './axios'

export const rankApi = {
  // 获取排名配置
  getConfigs: () => api.get('/api/rank/configs'),
  // 获取用户排名
  getUserRank: (rankType) => api.get('/api/rank/user', { params: { rankType } }),
  // 获取排行榜数据
  getRankList: (type) => api.get('/api/rank/data', { params: { rankType: type, limit: 10 } })
}