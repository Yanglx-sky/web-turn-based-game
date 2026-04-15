import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { battleApi } from '@/api/battle'

/**
 * 战斗逻辑Hook
 * 处理战斗初始化、动作执行、状态更新
 */
export function useBattle(levelId, battleType = 'level') {
  // 加载状态
  const isLoading = ref(true)
  const isActing = ref(false)
  const error = ref(null)

  // 战斗数据
  const battleId = ref(null)
  const currentRound = ref(1)
  const countdown = ref(30)
  const battleStatus = ref('active') // 'active', 'victory', 'defeat', 'flee'

  // 精灵数据
  const playerSprite = ref(null)
  const enemySprite = ref(null)
  const availableSprites = ref([])
  const currentSkills = ref([])
  const potions = ref([])

  // 战斗日志
  const battleLogs = ref([])

  // 结果数据
  const battleResult = ref(null)

  // 动画状态（用于通知组件）
  const animationState = ref({
    isPlayerAttacking: false,
    isEnemyAttacking: false,
    isPlayerHurt: false,
    isEnemyHurt: false,
    activeSkill: null
  })

  // 倒计时管理
  let countdownTimer = null

  const startCountdown = () => {
    countdown.value = 30
    countdownTimer = setInterval(() => {
      if (countdown.value > 0 && battleStatus.value === 'active') {
        countdown.value--
      } else if (countdown.value <= 0) {
        // 超时自动攻击
        executeAction('attack')
      }
    }, 1000)
  }

  const pauseCountdown = () => {
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }

  const resumeCountdown = () => {
    if (battleStatus.value === 'active') {
      startCountdown()
    }
  }

  // 初始化战斗
  const initBattle = async (userElfId) => {
    try {
      isLoading.value = true
      error.value = null

      let response
      if (battleType === 'level') {
        response = await battleApi.startBattle(userElfId, levelId)
      }

      if (response?.code === 200 && response?.data) {
        const data = response.data
        battleId.value = data.battleId

        // 设置玩家精灵数据
        playerSprite.value = {
          id: data.elf?.id || data.userElfId,
          name: data.elfName || '我的精灵',
          level: data.elf?.level || 1,
          elementType: data.elf?.elementType || 1,
          hp: data.playerElfHp || data.elf?.hp,
          maxHp: data.elf?.maxHp || 100,
          mp: data.elfMp || data.elf?.mp,
          maxMp: data.elf?.maxMp || 50,
          attack: data.elf?.attack || 30,
          defense: data.elf?.defense || 20
        }

        // 设置敌人数据
        enemySprite.value = {
          id: data.monsterId,
          name: data.monsterName || '敌人',
          level: data.monsterLevel || 1,
          elementType: data.monsterElementType || 1,
          hp: data.monsterHp,
          maxHp: data.monsterMaxHp,
          mp: data.monsterMp || 0,
          maxMp: data.monsterMaxMp || 0
        }

        // 设置技能列表
        currentSkills.value = data.skills || []

        // 设置可用精灵
        availableSprites.value = data.elves || []

        // 设置药品
        potions.value = data.potions || []

        // 设置回合数
        currentRound.value = data.currentRound || 1

        // 启动倒计时
        startCountdown()

        battleLogs.value = ['战斗开始！']
      } else {
        error.value = response?.msg || '战斗初始化失败'
      }
    } catch (err) {
      error.value = err.message || '网络错误'
      console.error('战斗初始化失败:', err)
    } finally {
      isLoading.value = false
    }
  }

  // 重连战斗
  const reconnectBattle = async () => {
    try {
      isLoading.value = true
      const response = await battleApi.getCurrentBattle()

      if (response?.code === 200 && response?.data) {
        // 使用与initBattle相同的数据处理逻辑
        initBattleState(response.data)
        startCountdown()
        return true
      }
      return false
    } catch (err) {
      console.error('重连失败:', err)
      return false
    } finally {
      isLoading.value = false
    }
  }

  // 初始化战斗状态（用于重连）
  const initBattleState = (data) => {
    battleId.value = data.battleId
    currentRound.value = data.currentRound || 1
    battleStatus.value = data.status === 1 ? 'victory' : data.status === 2 ? 'defeat' : 'active'

    if (data.elf) {
      playerSprite.value = {
        ...playerSprite.value,
        hp: data.playerElfHp || data.elf.hp,
        mp: data.elfMp || data.elf.mp
      }
    }

    if (data.monster) {
      enemySprite.value = {
        ...enemySprite.value,
        hp: data.monsterHp
      }
    }
  }

  // 执行战斗动作
  const executeAction = async (actionType, payload = {}) => {
    if (isActing.value || battleStatus.value !== 'active') return

    isActing.value = true
    pauseCountdown()

    try {
      const requestData = { type: actionType, ...payload }
      const response = await battleApi.executeAction(requestData)

      if (response?.code === 200 && response?.data) {
        const data = response.data

        // 更新状态
        updateBattleState(data)

        // 触发动画
        triggerAnimationSequence(actionType, data)

        // 检查战斗结果
        checkBattleResult(data)
      } else {
        error.value = response?.msg || '动作执行失败'
      }
    } catch (err) {
      error.value = err.message || '网络错误'
      console.error('动作执行失败:', err)
    } finally {
      isActing.value = false
      if (battleStatus.value === 'active') {
        resumeCountdown()
      }
    }
  }

  // 更新战斗状态
  const updateBattleState = (data) => {
    // 更新回合数
    if (data.currentRound) {
      currentRound.value = data.currentRound
    }

    // 更新血量
    if (playerSprite.value) {
      playerSprite.value.hp = data.playerElfHp || playerSprite.value.hp
      playerSprite.value.mp = data.elfMp || playerSprite.value.mp
    }

    if (enemySprite.value) {
      enemySprite.value.hp = data.monsterHp || enemySprite.value.hp
      enemySprite.value.mp = data.monsterMp || enemySprite.value.mp
    }

    // 更新日志
    if (data.roundLogs) {
      const newLogs = data.roundLogs.flatMap(log => log.logs || [log])
      battleLogs.value = [...battleLogs.value, ...newLogs]
    }
  }

  // 触发动画序列
  const triggerAnimationSequence = (actionType, data) => {
    // 简单动画触发逻辑
    // 实际动画由组件处理，这里只设置状态
    if (actionType === 'attack' || actionType === 'skill') {
      animationState.value.isPlayerAttacking = true

      if (actionType === 'skill' && payload.skillId) {
        const skill = currentSkills.value.find(s => s.id === payload.skillId)
        animationState.value.activeSkill = skill
      }

      // 500ms后触发受击
      setTimeout(() => {
        animationState.value.isPlayerAttacking = false
        animationState.value.isEnemyHurt = true

        setTimeout(() => {
          animationState.value.isEnemyHurt = false
          animationState.value.activeSkill = null
        }, 500)
      }, 800)
    }

    // 敌人反击动画
    if (data.enemyAttack) {
      setTimeout(() => {
        animationState.value.isEnemyAttacking = true

        setTimeout(() => {
          animationState.value.isEnemyAttacking = false
          animationState.value.isPlayerHurt = true

          setTimeout(() => {
            animationState.value.isPlayerHurt = false
          }, 500)
        }, 800)
      }, 1500)
    }
  }

  // 检查战斗结果
  const checkBattleResult = (data) => {
    if (data.status === 1) {
      battleStatus.value = 'victory'
      battleResult.value = {
        expReward: data.expReward || 100,
        goldReward: data.goldReward || 50,
        levelUp: data.levelUp || false
      }
      pauseCountdown()
    } else if (data.status === 2) {
      battleStatus.value = 'defeat'
      battleResult.value = { reason: data.reason || '战斗失败' }
      pauseCountdown()
    }
  }

  // 各种动作的快捷方法
  const attack = () => executeAction('attack')
  const useSkill = (skillId) => executeAction('skill', { skillId })
  const switchSprite = (elfId) => executeAction('switch', { elfId })
  const usePotion = (potionId) => executeAction('item', { potionId })
  const flee = () => executeAction('flee')

  // 清理战斗状态
  const clearBattle = () => {
    pauseCountdown()
    battleId.value = null
    currentRound.value = 1
    battleStatus.value = 'active'
    playerSprite.value = null
    enemySprite.value = null
    availableSprites.value = []
    currentSkills.value = []
    potions.value = []
    battleLogs.value = []
    battleResult.value = null
    animationState.value = {
      isPlayerAttacking: false,
      isEnemyAttacking: false,
      isPlayerHurt: false,
      isEnemyHurt: false,
      activeSkill: null
    }
  }

  // 清理
  onUnmounted(() => {
    pauseCountdown()
  })

  return {
    // 状态
    isLoading,
    isActing,
    error,
    battleId,
    currentRound,
    countdown,
    battleStatus,
    battleResult,

    // 数据
    playerSprite,
    enemySprite,
    availableSprites,
    currentSkills,
    potions,
    battleLogs,
    animationState,

    // 方法
    initBattle,
    reconnectBattle,
    executeAction,
    attack,
    useSkill,
    switchSprite,
    usePotion,
    flee,
    clearBattle,
    pauseCountdown,
    resumeCountdown
  }
}