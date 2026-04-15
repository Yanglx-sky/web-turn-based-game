<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useBattleStore } from '@/stores/battleStore'
import { battleApi } from '@/api/battle'
import { userElfApi } from '@/api/userElf'
import { potionApi } from '@/api/potion'
import { NetworkManager } from '@/utils/network'

// 新组件
import BattleScene from '@/components/battle/BattleScene.vue'
import SkillPanel from '@/components/battle/SkillPanel.vue'
import BattlePopup from '@/components/battle/BattlePopup.vue'
import BattleLog from '@/components/battle/BattleLog.vue'
import CountdownTimer from '@/components/battle/CountdownTimer.vue'
import BattleReconnectDialog from '@/components/BattleReconnectDialog.vue'

const router = useRouter()
const route = useRoute()
const battleStore = useBattleStore()

// 网络管理器
let networkManager = null

// ==================== 战斗状态 ====================
const isLoading = ref(true)
const isActing = ref(false)
const error = ref(null)

// 战斗数据
const battleId = ref(null)
const currentRound = ref(1)
const countdown = ref(30)
const battleStatus = ref('active') // 'active', 'victory', 'defeat'
const battleResult = ref(null)

// 精灵数据
const playerSprite = ref(null)
const enemySprite = ref(null)
const availableSprites = ref([])
const currentSkills = ref([])
const potions = ref([])

// 战斗日志
const battleLogs = ref([])

// 弹窗状态
const showBattleStartPopup = ref(false)
const showBattleEndPopup = ref(false)
const popupType = ref('start')

// 动画状态
const animationState = ref({
  isPlayerAttacking: false,
  isEnemyAttacking: false,
  isPlayerHurt: false,
  isEnemyHurt: false,
  activeSkill: null
})

// 倒计时组件引用
const countdownRef = ref(null)

// ==================== 辅助函数 ====================

// 获取精灵图片URL
const getElfImage = (elfId) => {
  const images = {
    1: new URL('@/assets/photo/sasuke/佐助.jpg', import.meta.url).href,
    2: new URL('@/assets/photo/zhaomeiming/照美冥.webp', import.meta.url).href,
    3: new URL('@/assets/photo/qianshouzhujian/千手柱间.jpg', import.meta.url).href
  }
  return images[elfId] || new URL('@/assets/hero.png', import.meta.url).href
}

// 获取怪物图片URL
const getMonsterImage = (name) => {
  const monsterImages = {
    '迪莫': new URL('@/assets/photo/t01ad4c8a8c1b78d5e9.jpg', import.meta.url).href,
    '焰阳火灵': new URL('@/assets/photo/78714861ed8311a9d5af0982af3bf12196230622.jpg', import.meta.url).href,
    '惊涛水灵': new URL('@/assets/photo/7acb0a46f21fbe096b63310c0e2a1b338744ebf807da.png', import.meta.url).href,
    '魔草巫灵': new URL('@/assets/photo/ac6eddc451da81cb39db2c96742dc7160924ab189b1f.png', import.meta.url).href
  }
  return monsterImages[name] || new URL('@/assets/hero.png', import.meta.url).href
}

// 获取系别名称
const getElementType = (type) => {
  const types = { 1: '火系', 2: '水系', 3: '草系', 4: '光系' }
  return types[type] || '未知'
}

// ==================== 倒计时管理 ====================
const startCountdown = () => {
  countdown.value = 30
  countdownRef.value?.start()
}

const pauseCountdown = () => {
  countdownRef.value?.pause()
}

const resumeCountdown = () => {
  countdownRef.value?.resume()
}

const handleTimeout = () => {
  // 超时自动攻击
  executeAction('attack')
}

// ==================== 战斗初始化 ====================
const initBattle = async () => {
  try {
    isLoading.value = true
    error.value = null

    const levelId = route.params.id
    const battleType = route.query.type || 'level'

    // 获取出战精灵
    const battleElvesResponse = await userElfApi.getBattleElves()
    if (battleElvesResponse?.code !== 200 || !battleElvesResponse?.data?.length) {
      error.value = '没有可用的精灵'
      return
    }

    const userElfId = battleElvesResponse.data[0].id

    // 开始战斗
    let response
    if (battleType === 'train') {
      // 训练模式
      response = await battleApi.startBattle(userElfId, null)
    } else {
      response = await battleApi.startBattle(userElfId, levelId)
    }

    if (response?.code === 200 && response?.data) {
      const data = response.data
      battleId.value = data.battleId

      // 设置玩家精灵
      playerSprite.value = {
        id: data.userElfId || battleElvesResponse.data[0].id,
        name: data.elfName || battleElvesResponse.data[0].elfName,
        level: data.elf?.level || battleElvesResponse.data[0].level,
        elementType: data.elf?.elementType || battleElvesResponse.data[0].elementType,
        hp: data.playerElfHp || data.elf?.hp,
        maxHp: data.elf?.maxHp || battleElvesResponse.data[0].maxHp,
        mp: data.elfMp || data.elf?.mp,
        maxMp: data.elf?.maxMp || battleElvesResponse.data[0].maxMp,
        imageUrl: getElfImage(data.elf?.id || battleElvesResponse.data[0].elfId)
      }

      // 设置敌人
      enemySprite.value = {
        id: data.monsterId,
        name: data.monsterName || '敌人',
        level: 1,
        elementType: data.monsterElementType || 1,
        hp: data.monsterHp,
        maxHp: data.monsterMaxHp,
        mp: data.monsterMp || 0,
        maxMp: data.monsterMaxMp || 0,
        imageUrl: getMonsterImage(data.monsterName)
      }

      // 设置技能
      currentSkills.value = data.skills || []

      // 设置可用精灵
      availableSprites.value = (battleElvesResponse.data || []).map(elf => ({
        id: elf.id,
        name: elf.elfName,
        level: elf.level,
        elementType: elf.elementType,
        hp: elf.hp,
        maxHp: elf.maxHp,
        mp: elf.mp,
        maxMp: elf.maxMp,
        imageUrl: getElfImage(elf.elfId),
        status: elf.status
      }))

      // 获取药品
      const potionsResponse = await potionApi.getUserPotions()
      if (potionsResponse?.code === 200) {
        potions.value = (potionsResponse.data || []).map(p => ({
          id: p.id,
          name: p.name,
          count: p.count,
          effect: p.description,
          imageUrl: `/src/assets/photo/equip/${p.name.includes('血') ? '血瓶' : '蓝瓶'}.jpg`
        }))
      }

      // 更新store
      battleStore.initBattle({
        battleId: data.battleId,
        currentRound: 1,
        levelId: levelId,
        elves: availableSprites.value,
        monsters: [enemySprite.value]
      })

      // 显示战斗开始弹窗
      showBattleStartPopup.value = true

      // 添加日志
      battleLogs.value = ['战斗开始！']

      // 开始倒计时
      startCountdown()
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

// ==================== 执行战斗动作 ====================
const executeAction = async (actionType, payload = {}) => {
  if (isActing.value || battleStatus.value !== 'active') return

  isActing.value = true
  pauseCountdown()

  try {
    const requestData = { type: actionType, ...payload }
    const response = await battleApi.executeAction(requestData)

    if (response?.code === 200 && response?.data) {
      const data = response.data
      processBattleResponse(data, actionType, payload)
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

// 处理战斗响应
const processBattleResponse = (data, actionType, payload) => {
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
    const newLogs = data.roundLogs.flatMap(r => r.logs || [r])
    battleLogs.value = [...battleLogs.value, ...newLogs]
  }

  // 触发动画
  triggerAnimation(actionType, payload, data)

  // 检查战斗结果
  checkBattleResult(data)
}

// 触发动画
const triggerAnimation = (actionType, payload, data) => {
  if (actionType === 'attack' || actionType === 'skill') {
    animationState.value.isPlayerAttacking = true

    if (actionType === 'skill') {
      const skill = currentSkills.value.find(s => s.id === payload.skillId)
      animationState.value.activeSkill = skill
    }

    // 延迟后触发受击
    setTimeout(() => {
      animationState.value.isPlayerAttacking = false
      animationState.value.isEnemyHurt = true

      setTimeout(() => {
        animationState.value.isEnemyHurt = false
        animationState.value.activeSkill = null
      }, 500)
    }, 800)
  }

  // 敌人反击
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
      goldReward: data.goldReward || 50
    }
    pauseCountdown()
    showBattleEndPopup.value = true
    popupType.value = 'victory'
  } else if (data.status === 2) {
    battleStatus.value = 'defeat'
    battleResult.value = { reason: '战斗失败' }
    pauseCountdown()
    showBattleEndPopup.value = true
    popupType.value = 'defeat'
  }
}

// ==================== 动作快捷方法 ====================
const attack = () => executeAction('attack')
const useSkill = (skillId) => executeAction('skill', { skillId })
const switchSprite = (elfId) => executeAction('switch', { elfId })
const usePotion = (potionId) => executeAction('item', { potionId })
const flee = () => executeAction('flee')

// ==================== 弹窗处理 ====================
const handlePopupClose = () => {
  showBattleStartPopup.value = false
  showBattleEndPopup.value = false
}

// ==================== 断线重连 ====================
const continueBattle = async () => {
  battleStore.setPendingBattle(null)
  await initBattle()
}

const abandonBattle = async () => {
  try {
    await battleApi.abandonBattle()
  } catch (err) {
    console.error('放弃战斗失败:', err)
  }
  battleStore.clearBattleState()
  router.push('/pve')
}

// ==================== 返回关卡 ====================
const backToPVE = () => {
  battleStore.clearBattleState()
  router.push('/pve')
}

// ==================== 生命周期 ====================
onMounted(async () => {
  // 初始化网络管理器
  networkManager = new NetworkManager(battleStore)

  // 检查是否有待恢复的战斗
  if (battleStore.hasPendingBattle) {
    // 显示重连弹窗
    return
  }

  // 初始化战斗
  await initBattle()
})

onUnmounted(() => {
  pauseCountdown()
  if (networkManager) {
    networkManager.destroy()
  }
})

// ==================== 计算属性 ====================
const isDisabled = computed(() => isActing.value || battleStatus.value !== 'active')

// 计算当前精灵ID
const currentSpriteId = computed(() => playerSprite.value?.id || 0)
</script>

<template>
  <div class="battle-page">
    <!-- 网络异常遮罩 -->
    <div v-if="!battleStore.isOnline" class="network-error-overlay">
      <div class="network-error-content">
        <div class="loading-spinner"></div>
        <p>网络异常，正在重连...</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <p>正在加载战斗...</p>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error && !isLoading" class="error-overlay">
      <div class="error-content">
        <p class="error-text">{{ error }}</p>
        <button class="btn retry-btn" @click="initBattle">重试</button>
        <button class="btn back-btn" @click="backToPVE">返回关卡</button>
      </div>
    </div>

    <!-- 恢复战斗弹窗 -->
    <BattleReconnectDialog
      :visible="battleStore.hasPendingBattle"
      @continue="continueBattle"
      @abandon="abandonBattle"
    />

    <!-- 战斗开始弹窗 -->
    <BattlePopup
      :visible="showBattleStartPopup"
      type="start"
      :duration="2000"
      @close="handlePopupClose"
    />

    <!-- 战斗结束弹窗 -->
    <BattlePopup
      :visible="showBattleEndPopup"
      :type="popupType"
      :duration="3000"
      :resultData="battleResult"
      @close="handlePopupClose"
    />

    <!-- 主战斗界面 -->
    <div v-if="!isLoading && !error" class="battle-main">
      <!-- 顶部导航 -->
      <div class="battle-header">
        <div class="header-left">
          <span class="game-title">洛克王国</span>
        </div>
        <div class="header-right">
          <CountdownTimer
            ref="countdownRef"
            :initialTime="30"
            :isActive="battleStatus === 'active'"
            @timeout="handleTimeout"
          />
        </div>
      </div>

      <!-- 战斗场景 -->
      <BattleScene
        :playerSprite="playerSprite"
        :enemySprite="enemySprite"
        :battleLogs="battleLogs"
        :currentRound="currentRound"
        :isPlayerAttacking="animationState.isPlayerAttacking"
        :isEnemyAttacking="animationState.isEnemyAttacking"
        :isPlayerHurt="animationState.isPlayerHurt"
        :isEnemyHurt="animationState.isEnemyHurt"
        :activeSkill="animationState.activeSkill"
      />

      <!-- 战斗日志（可选显示） -->
      <div class="battle-log-section">
        <BattleLog :logs="battleLogs" :maxVisible="5" />
      </div>

      <!-- 底部技能操作框 -->
      <SkillPanel
        :skills="currentSkills"
        :availableSprites="availableSprites"
        :potions="potions"
        :currentMp="playerSprite?.mp || 0"
        :currentSpriteId="currentSpriteId"
        :isDisabled="isDisabled"
        :countdown="countdown"
        @attack="attack"
        @skill="useSkill"
        @switch="switchSprite"
        @item="usePotion"
        @flee="flee"
      />

      <!-- 战斗结果（胜利/失败后显示） -->
      <div v-if="battleStatus !== 'active'" class="battle-result-overlay">
        <div class="result-panel">
          <h2 class="result-title" :class="`result-${battleStatus}`">
            {{ battleStatus === 'victory' ? '战斗胜利！' : '战斗失败...' }}
          </h2>

          <div v-if="battleResult && battleStatus === 'victory'" class="result-rewards">
            <div class="reward-item">
              <span class="reward-icon">✨</span>
              <span class="reward-label">获得经验</span>
              <span class="reward-value">+{{ battleResult.expReward }}</span>
            </div>
            <div class="reward-item">
              <span class="reward-icon">💰</span>
              <span class="reward-label">获得金币</span>
              <span class="reward-value">+{{ battleResult.goldReward }}</span>
            </div>
          </div>

          <div class="result-actions">
            <button class="btn primary-btn" @click="backToPVE">返回关卡</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import '@/styles/global.css';

.battle-page {
  min-height: 100vh;
  background: var(--gradient-dark);
  position: relative;
  overflow: hidden;
}

/* 加载/错误遮罩 */
.loading-overlay,
.error-overlay,
.network-error-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: var(--z-overlay);
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-content,
.error-content,
.network-error-content {
  text-align: center;
  padding: var(--space-2xl);
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--border-visible);
  border-top-color: var(--water-primary);
  border-radius: 50%;
  animation: loading-spin 1s linear infinite;
  margin: 0 auto var(--space-lg);
}

.error-text {
  color: var(--fire-primary);
  font-size: var(--text-lg);
  margin-bottom: var(--space-xl);
}

/* 主战斗界面 */
.battle-main {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* 顶部导航 */
.battle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) var(--space-xl);
  background: rgba(0, 0, 0, 0.4);
  border-bottom: 1px solid var(--border-subtle);
}

.header-left {
  display: flex;
  align-items: center;
}

.game-title {
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
}

/* 战斗日志区域 */
.battle-log-section {
  padding: var(--space-md) var(--space-xl);
}

/* 战斗结果 */
.battle-result-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: var(--z-modal);
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-panel {
  background: var(--bg-elevated);
  border: 2px solid var(--border-visible);
  border-radius: var(--radius-2xl);
  padding: var(--space-2xl) var(--space-3xl);
  text-align: center;
  max-width: 400px;
  animation: result-appear 0.5s ease-out;
}

@keyframes result-appear {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.result-title {
  font-size: var(--text-3xl);
  font-weight: 900;
  margin-bottom: var(--space-xl);
}

.result-victory {
  color: var(--grass-primary);
  text-shadow: 0 0 20px rgba(34, 197, 94, 0.5);
}

.result-defeat {
  color: var(--fire-primary);
  text-shadow: 0 0 20px rgba(239, 68, 68, 0.5);
}

.result-rewards {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: rgba(0, 0, 0, 0.3);
  border-radius: var(--radius-xl);
  margin-bottom: var(--space-xl);
}

.reward-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.reward-icon {
  font-size: 20px;
}

.reward-label {
  font-size: var(--text-base);
  color: var(--text-secondary);
}

.reward-value {
  font-size: var(--text-lg);
  font-weight: 700;
  color: #fbbf24;
}

.result-actions {
  display: flex;
  gap: var(--space-md);
}

.primary-btn {
  background: var(--water-primary);
  border-color: var(--water-primary);
  color: var(--text-inverse);
  padding: var(--space-lg) var(--space-2xl);
}

.primary-btn:hover {
  background: var(--water-secondary);
  border-color: var(--water-secondary);
}

.retry-btn {
  background: var(--btn-attack);
  border-color: var(--btn-attack);
  color: var(--text-primary);
  margin-right: var(--space-md);
}

.back-btn {
  background: var(--bg-tertiary);
}

/* 响应式 */
@media (max-width: 768px) {
  .battle-header {
    padding: var(--space-sm) var(--space-md);
  }

  .game-title {
    font-size: var(--text-lg);
  }

  .battle-log-section {
    padding: var(--space-sm) var(--space-md);
  }

  .result-panel {
    padding: var(--space-xl);
    max-width: 90vw;
  }

  .result-title {
    font-size: var(--text-2xl);
  }
}
</style>