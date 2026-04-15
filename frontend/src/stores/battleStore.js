import { defineStore } from 'pinia'

export const useBattleStore = defineStore('battle', {
  state: () => ({
    // 战斗信息
    battleId: null,
    currentRound: 1,
    levelId: null,
    status: 0, // 0=战斗中 1=胜利 2=失败 3=断线暂停
    
    // 精灵列表
    elves: [],
    
    // 怪物列表
    monsters: [],
    
    // 网络状态
    isOnline: true,
    reconnecting: false,
    
    // 断线重连相关
    hasPendingBattle: false,
    pendingBattleData: null
  }),
  
  getters: {
    // 获取当前战斗的精灵
    currentElf: (state) => {
      return state.elves.find(elf => elf.elf_state === 0) || null
    },
    
    // 获取所有存活的怪物
    aliveMonsters: (state) => {
      return state.monsters.filter(monster => monster.is_alive === 1)
    }
  },
  
  actions: {
    // 初始化战斗状态
    initBattle(battleData) {
      this.battleId = battleData.battleId
      this.currentRound = battleData.currentRound || 1
      this.levelId = battleData.levelId
      this.status = battleData.status || 0
      this.elves = battleData.elves || []
      this.monsters = battleData.monsters || []
      this.hasPendingBattle = false
      this.pendingBattleData = null
    },
    
    // 保存战斗状态
    saveBattleState(state) {
      this.currentRound = state.currentRound
      this.elves = state.elves
      this.monsters = state.monsters
      this.status = state.status
    },
    
    // 设置网络状态
    setOnlineStatus(status) {
      this.isOnline = status
    },
    
    // 设置重连状态
    setReconnecting(status) {
      this.reconnecting = status
    },
    
    // 设置待恢复的战斗
    setPendingBattle(data) {
      this.hasPendingBattle = !!data
      this.pendingBattleData = data
    },
    
    // 清空战斗状态
    clearBattleState() {
      this.battleId = null
      this.currentRound = 1
      this.levelId = null
      this.status = 0
      this.elves = []
      this.monsters = []
      this.hasPendingBattle = false
      this.pendingBattleData = null
    }
  },
  
  // 持久化存储
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'battle-state',
        storage: localStorage
      }
    ]
  }
})