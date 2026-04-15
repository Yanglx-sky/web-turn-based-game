import api from './axios'

export const elfApi = {
  // 获取精灵列表
  getElfList: () => api.get(`/api/elves`),
  // 设置出战精灵
  setBattleElf: (elfId, fightOrder) => api.post(`/api/user-elf/set-battle`, { elfId, fightOrder }),
  // 精灵升级
  upgradeElf: (elfId) => api.post(`/api/user-elf/${elfId}/upgrade`),
  // 查看精灵详情
  getElfDetail: (elfId) => api.get(`/api/user-elf/${elfId}`),
  // 获取御三家精灵
  getStarterElves: () => api.get(`/api/elves/starter`)
}