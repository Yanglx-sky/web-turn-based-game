import api from './axios'

export const battleApi = {
  // 开始战斗
  startBattle: (userElfId, levelId) => {
    return api.post('/api/battle', {}, {
      params: {
        userElfId,
        levelId
      }
    })
  },
  
  // 获取当前战斗状态
  getCurrentBattle: () => {
    return api.get('/api/battle')
  },
  
  // 更新战斗状态
  updateBattleStatus: (status) => {
    return api.put('/api/battle/status', { status })
  },
  
  // 玩家逃跑
  flee: () => {
    return api.put('/api/battle/status', { status: 'flee' })
  },
  
  // 放弃战斗
  abandonBattle: () => {
    return api.put('/api/battle/status', { status: 'abandon' })
  },
  
  // 普通攻击
  normalAttack: () => {
    return api.post('/api/battle/action', { type: 'attack' })
  },
  
  // 使用技能
  useSkill: (skillId) => {
    return api.post('/api/battle/action', { type: 'skill', skillId })
  },
  
  // 执行怪物行动
  executeMonsterTurn: () => {
    return api.post('/api/battle/action', { type: 'monster_turn' })
  },
  
  // 获取战斗中的精灵列表
  getBattleElves: () => {
    return api.get('/api/battle/battle-elves')
  },
  
  // 切换精灵
  switchElf: (elfId) => {
    return api.post('/api/battle/action', { type: 'switch', elfId })
  },
  
  // 战斗结算
  battleSettlement: () => {
    return api.post('/api/battle/settlement')
  },
  
  // 获取AI战报总结
  getBattleSummary: () => {
    return api.get('/api/battle/ai/summary')
  },
  
  // 获取战斗策略推荐
  getStrategyRecommendation: (levelId) => {
    return api.get(`/api/battle/ai/strategy?levelId=${levelId}`)
  },

  // 断线重连
  reconnect: () => {
    return api.get('/api/battle')
  },

  // 玩家离线
  playerOffline: () => {
    return api.put('/api/battle/status', { status: 'offline' })
  },
  
  // 领取战斗奖励
  claimReward: (levelId, battleId) => {
    return api.post('/api/battle/reward', null, {
      params: {
        levelId,
        battleId
      }
    })
  },
  
  // 获取战斗日志
  getBattleLogs: (battleId) => {
    return api.get('/api/battle/logs', {
      params: {
        battleId
      }
    })
  }
}