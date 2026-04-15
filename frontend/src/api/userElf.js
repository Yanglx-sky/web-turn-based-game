import api from './axios'

export const userElfApi = {
  // 获取用户精灵列表
  list: () => {
    return api.get('/api/user-elf')
  },
  
  // 获取出战精灵列表
  getBattleElves: () => {
    return api.get('/api/user-elf/battle-elves')
  },
  
  // 设置出战精灵
  setActive: (elfId, fightOrder) => {
    return api.post('/api/user-elf/set-battle', { elfId, fightOrder })
  },
  
  // 精灵升级
  upgrade: (elfId) => {
    return api.post(`/api/user-elf/${elfId}/upgrade`)
  },
  
  // 解锁技能
  unlockSkill: (elfId, skillId) => {
    return api.post('/api/user-elf/unlock-skill', { elfId, skillId })
  },
  
  // 获取精灵详情
  getDetail: (elfId) => {
    return api.get(`/api/user-elf/${elfId}`)
  },
  
  // 创建精灵
  create: (elfId) => {
    return api.post('/api/user-elf', null, { params: { elfId } })
  }
}