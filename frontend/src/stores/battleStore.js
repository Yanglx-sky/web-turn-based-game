import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useBattleStore = defineStore('battle', () => {
  // === 核心状态 ===
  const battleId = ref(null)
  const battleType = ref('level') // 'level' | 'train'
  const currentRound = ref(1)
  const levelId = ref(null)
  const status = ref(0) // 0=战斗中, 1=胜利, 2=失败, 3=断线暂停
  const countdown = ref(30)

  // === 精灵状态 ===
  const elves = ref([])
  const monsters = ref([])
  const playerSprite = ref(null)
  const enemySprite = ref(null)

  // === 技能/物品状态 ===
  const skills = ref([])
  const potions = ref([])

  // === 战斗日志 ===
  const battleLogs = ref([])

  // === 网络状态 ===
  const isOnline = ref(true)
  const reconnecting = ref(false)
  const hasPendingBattle = ref(false)
  const pendingBattleData = ref(null)

  // === 计算属性 ===
  const currentElf = computed(() => {
    return elves.value.find(elf => elf.elf_state === 0) || elves.value[0] || null
  })

  const aliveMonsters = computed(() => {
    return monsters.value.filter(monster => monster.is_alive === 1)
  })

  const isBattleActive = computed(() => status.value === 0)

  const playerHpPercent = computed(() => {
    if (!playerSprite.value) return 0
    return Math.max(0, Math.min(100, (playerSprite.value.hp / playerSprite.value.maxHp) * 100))
  })

  const enemyHpPercent = computed(() => {
    if (!enemySprite.value) return 0
    return Math.max(0, Math.min(100, (enemySprite.value.hp / enemySprite.value.maxHp) * 100))
  })

  // === Actions ===

  function initBattle(battleData) {
    battleId.value = battleData.battleId
    battleType.value = battleData.type || 'level'
    currentRound.value = battleData.currentRound || 1
    levelId.value = battleData.levelId
    status.value = battleData.status || 0

    elves.value = battleData.elves || []
    monsters.value = battleData.monsters || []

    playerSprite.value = battleData.playerSprite || null
    enemySprite.value = battleData.enemySprite || (monsters.value[0] || null)

    skills.value = battleData.skills || []
    potions.value = battleData.potions || []

    battleLogs.value = battleData.logs || ['战斗开始！']
    hasPendingBattle.value = false
    pendingBattleData.value = null
  }

  function updateBattleState(state) {
    currentRound.value = state.currentRound || currentRound.value
    status.value = state.status || status.value

    // 更新精灵状态
    if (state.elves) {
      elves.value = state.elves
    }

    // 更新怪物状态
    if (state.monsters) {
      monsters.value = state.monsters
    }

    // 更新血量
    if (playerSprite.value && state.playerElfHp !== undefined) {
      playerSprite.value.hp = state.playerElfHp
      playerSprite.value.mp = state.elfMp || playerSprite.value.mp
    }

    if (enemySprite.value && state.monsterHp !== undefined) {
      enemySprite.value.hp = state.monsterHp
      enemySprite.value.mp = state.monsterMp || enemySprite.value.mp
    }

    // 更新日志
    if (state.roundLogs) {
      const newLogs = state.roundLogs.flatMap(r => r.logs || [r])
      battleLogs.value = [...battleLogs.value, ...newLogs]
    }

    // 精灵切换
    if (state.switchedElf) {
      playerSprite.value = state.switchedElf
      skills.value = state.skills || []
    }
  }

  function setOnlineStatus(online) {
    isOnline.value = online
  }

  function setReconnecting(reconnect) {
    reconnecting.value = reconnect
  }

  function setPendingBattle(data) {
    hasPendingBattle.value = !!data
    pendingBattleData.value = data
  }

  function addBattleLog(log) {
    battleLogs.value.push(log)
  }

  function clearBattleState() {
    battleId.value = null
    battleType.value = 'level'
    currentRound.value = 1
    levelId.value = null
    status.value = 0
    countdown.value = 30
    elves.value = []
    monsters.value = []
    playerSprite.value = null
    enemySprite.value = null
    skills.value = []
    potions.value = []
    battleLogs.value = []
    hasPendingBattle.value = false
    pendingBattleData.value = null
    isOnline.value = true
    reconnecting.value = false
  }

  // === 断线重连 ===
  function markOffline() {
    status.value = 3
    if (battleId.value) {
      hasPendingBattle.value = true
      pendingBattleData.value = {
        battleId: battleId.value,
        currentRound: currentRound.value,
        levelId: levelId.value,
        elves: elves.value,
        monsters: monsters.value
      }
    }
  }

  function recoverFromOffline() {
    status.value = 0
    hasPendingBattle.value = false
    pendingBattleData.value = null
  }

  return {
    // State
    battleId,
    battleType,
    currentRound,
    levelId,
    status,
    countdown,
    elves,
    monsters,
    playerSprite,
    enemySprite,
    skills,
    potions,
    battleLogs,
    isOnline,
    reconnecting,
    hasPendingBattle,
    pendingBattleData,

    // Computed
    currentElf,
    aliveMonsters,
    isBattleActive,
    playerHpPercent,
    enemyHpPercent,

    // Actions
    initBattle,
    updateBattleState,
    setOnlineStatus,
    setReconnecting,
    setPendingBattle,
    addBattleLog,
    clearBattleState,
    markOffline,
    recoverFromOffline
  }
}, {
  persist: {
    key: 'battle-state',
    storage: localStorage,
    paths: ['battleId', 'battleType', 'currentRound', 'levelId', 'status', 'elves', 'monsters', 'hasPendingBattle', 'pendingBattleData']
  }
})