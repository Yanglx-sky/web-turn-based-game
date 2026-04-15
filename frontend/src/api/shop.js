import api from './axios'

export const shopApi = {
  // 获取所有商品
  getAllItems: () => {
    return api.get('/api/equip/all')
  },
  
  // 根据类型获取商品
  getItemsByType: (type) => {
    return api.get(`/api/equip/type/${type}`)
  },
  
  // 购买商品
  buyItem: (shopItemId) => {
    return api.post('/api/equip/buy', {}, {
      params: {
        equipId: shopItemId
      }
    })
  },
  
  // 获取用户拥有的指定类型装备
  getUserEquipsByType: (userId, type) => {
    return api.get('/api/equip/user/type', {
      params: {
        type
      }
    })
  }
}