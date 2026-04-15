import api from './axios'

export const potionApi = {
  // 获取所有药品
  getAllPotions: () => {
    return api.get('/api/potions')
  },
  
  // 获取药品详情
  getPotionById: (id) => {
    return api.get(`/api/potions/detail/${id}`)
  },
  
  // 获取用户拥有的药品
  getUserPotions: () => {
    return api.get('/api/potions/user')
  },
  
  // 使用药品
  usePotion: (elfId, potionConfigId) => {
    return api.post('/api/potions/use', null, {
      params: {
        elfId,
        potionId: potionConfigId
      }
    })
  },
  
  // 增加用户药品数量
  addUserPotion: (potionConfigId, count) => {
    return api.post('/api/potions/add', null, {
      params: {
        potionConfigId,
        count
      }
    })
  },
  
  // 减少用户药品数量
  reduceUserPotion: (potionConfigId, count) => {
    return api.post('/api/potions/reduce', null, {
      params: {
        potionConfigId,
        count
      }
    })
  }
}