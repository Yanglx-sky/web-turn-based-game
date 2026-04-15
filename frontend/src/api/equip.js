import api from './axios'

export const equipApi = {
  // 获取所有装备
  getAllEquips: () => {
    return api.get('/api/equips')
  },
  
  // 根据类型获取装备
  getEquipsByType: (type) => {
    return api.get(`/api/equips/type/${type}`)
  },
  
  // 获取装备详情
  getEquipById: (id) => {
    return api.get(`/api/equips/detail/${id}`)
  },
  
  // 购买装备
  buyEquip: (equipId) => {
    return api.post('/api/equips', null, {
      params: {
        equipId
      }
    })
  },
  
  // 装备武器
  equipWeapon: (elfId, weaponId) => {
    return api.put('/api/equips/weapon', null, {
      params: {
        elfId,
        weaponId
      }
    })
  },
  
  // 装备防具
  equipArmor: (elfId, armorId) => {
    return api.put('/api/equips/armor', null, {
      params: {
        elfId,
        userBagId: armorId
      }
    })
  },
  
  // 卸下武器
  unequipWeapon: (elfId) => {
    return api.delete('/api/equips/weapon', {
      params: {
        elfId
      }
    })
  },
  
  // 卸下防具
  unequipArmor: (elfId) => {
    return api.delete('/api/equips/armor', {
      params: {
        elfId
      }
    })
  },
  
  // 获取用户拥有的指定类型装备
  getUserEquipsByType: (type) => {
    return api.get('/api/equips/user/type', {
      params: {
        type
      }
    })
  }
}