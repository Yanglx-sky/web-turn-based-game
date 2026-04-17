import api from './axios'

export const trainApi = {
  // 创建训练人偶
  createMannequin: (attack, defense, hp, mp, speed, type, isAttack) => api.post(`/api/train/create`, { attack, defense, hp, mp, speed, type, isAttack }),
  // 开始训练（支持直接传递训练人偶属性或mannequinId）
  startTrain: (attackOrMannequinId, defense, hp, mp, speed, type, isAttack) => {
    // 如果只传了一个参数，当作mannequinId
    if (defense === undefined && hp === undefined) {
      return api.post(`/api/train/start`, { mannequinId: attackOrMannequinId })
    }
    // 否则传递训练人偶属性
    return api.post(`/api/train/start`, { 
      attack: attackOrMannequinId, 
      defense, 
      hp, 
      mp,
      speed,
      type, 
      isAttack 
    })
  },
  // 普通攻击
  normalAttack: () => api.post(`/api/train/attack`),
  // 使用技能
  useSkill: (skillId) => api.post(`/api/train/skill`, { skillId }),
  // 执行人偶行动
  executeMannequinTurn: () => api.post(`/api/train/monster_turn`),
  // 训练逃跑
  flee: () => api.post(`/api/train/flee`),
  // 切换精灵
  switchElf: (elfId) => api.post(`/api/train/switch`, { elfId }),
  // 训练结算
  trainSettlement: () => api.post(`/api/train/settlement`),
  // 获取训练记录
  getTrainRecords: () => api.get(`/api/train/records`),
  // 获取训练中的出战精灵
  getBattleElves: () => api.get(`/api/train/battle_elves`),
  // 获取训练日志
  getTrainLogs: (trainId) => api.get(`/api/train/logs`, {
    params: { trainId }
  }),
  // 训练使用药品
  usePotion: (elfId, potionId) => api.post(`/api/train/use_potion`, null, {
    params: { elfId, potionId }
  }),
  // 训练玩家离线
  playerOffline: () => api.put(`/api/train/offline`)
}