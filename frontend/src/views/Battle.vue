<template>
  <div class="home-container">
    <GameTopNav :active-path="battleNavPath" />

    <!-- 网络异常遮罩 -->
    <div v-if="!battleStore.isOnline" class="network-error-overlay">
      <div class="network-error-content">
        <p>网络异常，正在重连...</p>
      </div>
    </div>

    <!-- 战斗开始弹窗 -->
    <div v-if="showBattleStartPopup" class="battle-start-popup">
      <div class="popup-content">
        <img src="/src/assets/photo/battle/开始战斗.jpeg" alt="开始战斗" class="start-image">
      </div>
    </div>

    <!-- 胜负已分弹窗 -->
    <div v-if="showBattleEndPopup" class="battle-start-popup">
      <div class="popup-content">
        <img src="/src/assets/photo/battle/胜负已分.jpeg" alt="胜负已分" class="start-image">
      </div>
    </div>

    <!-- 恢复战斗弹窗 -->
    <BattleReconnectDialog
      :visible="battleStore.hasPendingBattle"
      @continue="continueBattle"
      @abandon="abandonBattle"
    />

    <div class="battle-container" :class="`battle-mode-${battleMode}`">
      <div class="battle-status-strip" v-if="!showResult">
        <div class="battle-status-side battle-status-side--left">
          <div class="battle-status-chip battle-status-chip--stage">{{ battleStageLabel }}</div>
          <div class="battle-status-chip battle-status-chip--round">
            <span class="battle-status-chip__label">ROUND</span>
            <span class="battle-status-chip__value">{{ battleRoundDisplay }}</span>
          </div>
        </div>
        <div class="battle-status-chip battle-status-chip--countdown">
          <span class="battle-status-chip__label">决策时间</span>
          <span class="battle-status-chip__value">{{ countdown }}</span>
          <span class="battle-status-chip__unit">秒</span>
        </div>
        <div class="battle-status-side battle-status-side--right">
          <button
            type="button"
            class="battle-log-toggle"
            :class="{ 'is-open': isBattleLogOpen }"
            @click="toggleBattleLog"
          >
            {{ isBattleLogOpen ? '收起记录' : '战斗记录' }}
          </button>
        </div>
      </div>

      <div class="enemy-dialog" v-if="enemyDialog">
        <div class="dialog-content">
          <span class="enemy-name">{{ enemyName }}:</span>
          <span class="dialog-text">{{ enemyDialog }}</span>
        </div>
      </div>

      <div class="battle-shell">
        <div class="battle-field" :class="[battleArenaClass, { 'is-projectile-active': showProjectile }]">
          <div class="battle-field__sky"></div>
          <div class="battle-field__mist"></div>
          <div class="battle-field__spotlight battle-field__spotlight--left"></div>
          <div class="battle-field__spotlight battle-field__spotlight--right"></div>

          <div class="hud player-hud" v-if="playerElf && playerElf.elfId">
            <div class="elf-info-box">
              <div class="info-header">
                <div>
                  <span class="hud-caption">玩家精灵</span>
                  <h4>{{ playerElf.elfName || `精灵 ${playerElf.elfId}` }}</h4>
                </div>
                <span class="level">Lv.{{ playerElf.level }}</span>
              </div>
              <div class="type-badge">{{ getElementType(playerElf.elementType) }}</div>
              <div class="bars">
                <div class="bar-container hp-container">
                  <span class="bar-label">HP</span>
                  <div class="bar-bg">
                    <div class="bar-fill hp-fill" :style="{ width: playerHpPercentage + '%' }"></div>
                  </div>
                  <span class="bar-text">{{ Math.max(0, playerElf.hp) }}/{{ playerElf.maxHp }}</span>
                </div>
                <div class="bar-container mp-container">
                  <span class="bar-label">MP</span>
                  <div class="bar-bg">
                    <div class="bar-fill mp-fill" :style="{ width: playerElf.maxMp ? (playerElf.mp / playerElf.maxMp * 100) + '%' : '0%' }"></div>
                  </div>
                  <span class="bar-text">{{ playerElf.mp }}/{{ playerElf.maxMp }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="hud player-hud empty-hud" v-else>
            <div class="elf-info-box">
              <h4>未选择精灵</h4>
              <p>请进入战斗选择精灵</p>
            </div>
          </div>

          <div class="hud enemy-hud">
            <div class="elf-info-box">
              <div class="info-header">
                <div>
                  <span class="hud-caption">敌方单位</span>
                  <h4>{{ enemyName }}</h4>
                </div>
                <span class="level">Lv.1</span>
              </div>
              <div class="type-badge">{{ getElementType(enemyElementType) }}</div>
              <div class="bars">
                <div class="bar-container hp-container">
                  <span class="bar-label">HP</span>
                  <div class="bar-bg">
                    <div class="bar-fill hp-fill" :style="{ width: enemyHpPercentage + '%' }"></div>
                  </div>
                  <span class="bar-text">{{ Math.max(0, enemyHp) }}/{{ enemyMaxHp }}</span>
                </div>
                <div class="bar-container mp-container">
                  <span class="bar-label">MP</span>
                  <div class="bar-bg">
                    <div class="bar-fill mp-fill" :style="{ width: enemyMaxMp ? (enemyMp / enemyMaxMp * 100) + '%' : '0%' }"></div>
                  </div>
                  <span class="bar-text">{{ enemyMp }}/{{ enemyMaxMp }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="battle-stage">
            <div class="battle-stage__ground">
              <span class="battle-stage__label">{{ battleStageLabel }}</span>
            </div>

            <div
              v-if="playerElf && playerElf.elfId"
              class="sprite-wrapper player-sprite"
              :class="[currentStrikeClass, { attacking: attackingSide === 'player' && attackAnimation, hurt: isPlayerHurt }]"
            >
              <div class="sprite-aura sprite-aura--player"></div>
              <div class="skill-name-float" v-if="attackingSide === 'player' && attackAnimation">
                {{ currentSkill }}
              </div>
              <div class="impact-flash" v-if="isPlayerHurt"></div>
              <img :src="getElfImage(playerElf.elfId)" :alt="playerElf.elfName || `精灵 ${playerElf.elfId}`" class="sprite-img" />
            </div>

            <div v-if="showProjectile" class="particle-track" :class="[projectileMotionClass, projectileElementClass]">
              <div v-for="particleIndex in projectileParticles" :key="particleIndex" class="particle" :class="`particle-${particleIndex}`"></div>
              <div class="particle-core"></div>
            </div>

            <div
              class="sprite-wrapper enemy-sprite"
              :class="[
                currentStrikeClass,
                { attacking: attackingSide === 'enemy' && attackAnimation, hurt: isEnemyHurt, 'is-mannequin': isMannequinEnemy }
              ]"
            >
              <div class="sprite-aura sprite-aura--enemy"></div>
              <div class="skill-name-float" v-if="attackingSide === 'enemy' && attackAnimation">
                {{ currentSkill }}
              </div>
              <div class="impact-flash" v-if="isEnemyHurt"></div>
              <img :src="enemySpriteSrc" :alt="enemySpriteAlt" class="sprite-img" />
            </div>
          </div>
        </div>

        <div class="battle-command-deck">
          <transition name="skill-panel">
            <div v-if="showSkills" class="skills-section">
              <div class="skills-section__header">
                <h4>技能</h4>
                <button type="button" class="skills-close-btn" @click="showSkills = false">收起</button>
              </div>
              <div class="skills-grid">
                <button
                  v-for="skill in skills"
                  :key="skill.id"
                  @click="useSelectedSkill(skill)"
                  class="skill-btn"
                  :class="getElementColorClass(playerElf?.elementType)"
                >
                  <span class="skill-btn__name">{{ skill.skillName }}</span>
                  <span class="skill-btn__cost">{{ skill.costMp }} MP</span>
                </button>
              </div>
            </div>
          </transition>

          <transition name="skill-panel">
            <div v-if="showSwitchElf" class="command-panel command-panel--switch">
              <div class="skills-section__header">
                <h4>更换精灵</h4>
                <button type="button" class="skills-close-btn" @click="closeSwitchElf">收起</button>
              </div>
              <div v-if="loadingBattleElves" class="command-panel__empty">加载中...</div>
              <div v-else class="battle-elves-list inline-battle-elves-list">
                <div v-for="elf in battleElves" :key="elf.id" class="elf-card horizontal" :class="{ 'current': elf && elf.id === playerElf.id }">
                  <template v-if="elf && elf.elfState !== 2">
                    <div class="elf-image">
                      <img :src="getElfImage(elf.elfId)" :alt="elf.elfName || `精灵 ${elf.elfId}`" />
                    </div>
                    <div class="elf-info">
                      <h4>{{ elf.elfName || `精灵 ${elf.elfId}` }}</h4>
                      <div class="elf-stats">
                        <p>等级: {{ elf.level }}</p>
                        <p>系别: {{ getElementType(elf.elementType) }}</p>
                        <div class="hp-bar">
                          <div class="hp-fill" :style="{ width: (Math.max(0, elf.hp) / elf.maxHp) * 100 + '%' }"></div>
                        </div>
                        <p>HP: {{ Math.max(0, elf.hp) }}/{{ elf.maxHp }}</p>
                        <p>MP: {{ elf.mp }}/{{ elf.maxMp }}</p>
                      </div>
                      <div v-if="elf && elf.id !== playerElf.id" class="card-actions">
                        <button 
                          @click="selectElfToSwitch(elf)" 
                          class="select-btn" 
                          :disabled="!elf || elf.elfState === 2 || elf.elfState === 0"
                          :class="{ 'disabled': !elf || elf.elfState === 2 || elf.elfState === 0 }"
                        >
                          {{ !elf ? '未知' : (elf.elfState === 2 ? '已死亡' : elf.elfState === 0 ? '战斗中' : '选择') }}
                        </button>
                      </div>
                      <div v-else class="current-tag">当前出战</div>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </transition>

          <transition name="skill-panel">
            <div v-if="showPotions" class="command-panel command-panel--potions">
              <div class="skills-section__header">
                <h4>药品</h4>
                <button type="button" class="skills-close-btn" @click="showPotions = false">收起</button>
              </div>
              <div v-if="potions.length === 0" class="command-panel__empty">
                <p>没有可用的药品</p>
              </div>
              <div v-else class="potion-list inline-potion-list">
                <div v-for="potion in potions" :key="potion.id" class="potion-item" @click="usePotion(potion)">
                  <div class="potion-image">
                    <img :src="getPotionImage(potion.name)" :alt="potion.name">
                  </div>
                  <div class="potion-info">
                    <h4>{{ potion.name }}</h4>
                    <p>{{ potion.description }}</p>
                    <p class="potion-count">数量: {{ potion.count }}</p>
                  </div>
                </div>
              </div>
            </div>
          </transition>

          <div class="battle-actions">
            <div class="action-buttons">
              <button @click="attack" class="action-btn attack-btn" :disabled="isButtonsDisabled">攻击</button>
              <button
                @click="useSkill"
                class="action-btn skill-action-btn"
                :class="getElementColorClass(playerElf?.elementType)"
                :disabled="isButtonsDisabled"
              >技能</button>
              <button @click="switchElf" class="action-btn switch-btn" :disabled="isButtonsDisabled">更换精灵</button>
              <button @click="flee" class="action-btn flee-btn" :disabled="isButtonsDisabled">逃跑</button>
              <button @click="openPotionsPanel" class="action-btn potion-btn" :disabled="isButtonsDisabled">药品</button>
            </div>
          </div>
        </div>
      </div>

      <transition name="log-drawer">
        <div v-if="isBattleLogOpen" class="battle-log-layer" @click.self="closeBattleLog">
          <aside class="battle-log-drawer" role="dialog" aria-modal="true" aria-label="战斗记录">
            <div class="battle-log__header">
              <h3>战斗记录</h3>
              <button type="button" class="drawer-close-btn" @click="closeBattleLog">关闭</button>
            </div>
            <div class="log-content">
              <div v-for="roundData in battleLogs" :key="roundData.round" class="round-section">
                <div class="round-header">
                  回合{{ roundData.round }}：
                </div>
                <div class="round-logs">
                  <div v-for="(log, index) in roundData.logs" :key="index" class="log-entry">
                    {{ log }}
                  </div>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </transition>
      
      <!-- 战斗结果 -->
      <div v-if="showResult" class="battle-result">
        <h2>{{ battleResult }}</h2>
        <div class="rewards" v-if="battleResult === '战斗胜利！'">
          <h3>战斗奖励</h3>
          <div class="reward-item" v-for="reward in battleRewards" :key="reward.type">
            <span class="reward-label">{{ reward.label }}:</span>
            <span class="reward-value">{{ reward.value }}</span>
          </div>
        </div>
        <div class="ai-summary" v-if="aiSummary">
          <h3>AI战报总结</h3>
          <div class="ai-score" v-if="aiScoreValue !== null && aiScoreValue !== undefined">
            <span class="score-label">训练评分：</span>
            <span class="score-value">{{ aiScoreValue }}分</span>
          </div>
          <div class="summary-content" v-html="aiSummary.replace(/\n/g, '<br>')"></div>
        </div>
        <button @click="getAISummary" class="summary-btn" v-if="!aiSummary">获取AI战报总结</button>
        <button @click="backToPVE" class="result-btn">{{ battleResultActionLabel }}</button>
      </div>
      
      <!-- 御三家选择弹窗 -->
      <div v-if="showStarterSelection" class="modal-overlay">
        <div class="modal-content">
          <h2>{{ ownedElfIds.length === 1 ? '选择你的第二只精灵' : '选择你的第三只精灵' }}</h2>
          <p>{{ ownedElfIds.length === 1 ? '恭喜你通关第一关！现在你可以从以下两只精灵中选择一只作为你的第二只伙伴' : '恭喜你通关第二关！现在你可以从以下两只精灵中选择一只作为你的第三只伙伴' }}</p>
          <div v-if="loadingStarterElves" class="loading">加载中...</div>
          <div v-else class="starter-elves">
            <div v-for="elf in starterElves" :key="elf.id" class="starter-elf" :class="{ 'owned': ownedElfIds.includes(elf.id) }">
              <img :src="getElfImage(elf.id)" :alt="elf.elfName" class="elf-image">
              <h4>{{ elf.elfName }}</h4>
              <p>类型: {{ elf.elementType === 1 ? '火系' : elf.elementType === 2 ? '水系' : '草系' }}</p>
              <p>等级: 1</p>
              <div v-if="ownedElfIds.includes(elf.id)" class="owned-tag">已拥有</div>
              <button v-else @click="selectStarter(elf.id)" class="select-btn">选择</button>
            </div>
          </div>
          <div class="modal-buttons">
            <button @click="closeStarterSelection" class="cancel-btn">关闭</button>
          </div>
        </div>
      </div>
      
      <!-- 精灵死亡切换确认弹窗（只有确定按钮） -->
      <div v-if="showElfDeathDialog" class="modal-overlay">
        <div class="modal-content elf-death-dialog">
          <p class="elf-death-message">{{ elfDeathMessage }}</p>
          <div class="modal-buttons">
            <button @click="confirmElfDeath" class="confirm-btn">确定</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { levelApi } from '../api/level'
import { battleApi } from '../api/battle'
import { userElfApi } from '../api/userElf'
import { userApi } from '../api/user'
import { elfApi } from '../api/elf'
import { potionApi } from '../api/potion'
import { trainApi } from '../api/train'
import { useBattleStore } from '../stores/battleStore'
import { NetworkManager } from '../utils/network'
import BattleReconnectDialog from '../components/BattleReconnectDialog.vue'
import GameTopNav from '../components/GameTopNav.vue'

const router = useRouter()
const route = useRoute()

// 网络管理器实例
let networkManager = null
const battleStore = useBattleStore()

const resolveElementTheme = (elementType, skillName = '') => {
  if (skillName.includes('火')) return 'fire'
  if (skillName.includes('水')) return 'water'
  if (skillName.includes('草') || skillName.includes('叶') || skillName.includes('藤') || skillName.includes('孢')) return 'grass'

  switch (elementType) {
    case 1:
      return 'fire'
    case 2:
      return 'water'
    case 3:
      return 'grass'
    default:
      return 'arcane'
  }
}

const navigateTo = (path) => {
  router.push(path)
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/auth')
}

// 战斗状态
const enemyName = ref('')
const enemyHp = ref(0)
const enemyMaxHp = ref(0)
const enemyMp = ref(0)
const enemyMaxMp = ref(0)
const enemyElementType = ref(null)
const playerElf = ref({})
const showSkills = ref(false)
const showResult = ref(false)
const showStarterSelection = ref(false)
const showSwitchElf = ref(false)
const loadingStarterElves = ref(false)
const loadingBattleElves = ref(false)
const starterElves = ref([])
const battleElves = ref([])
const ownedElfIds = ref([])
const battleResult = ref('')
// 战斗日志 - 按回合组织
const battleLogs = ref([{ round: 1, logs: [] }])
// 战斗奖励
const battleRewards = ref([])
// AI战报总结
const aiSummary = ref('')
// AI评分
const aiScoreValue = ref(null)
// 敌人对话
const enemyDialog = ref('')
// 倒计时相关
const countdown = ref(10)
const countdownTimer = ref(null)
// 攻击视觉效果
const attackingSide = ref(null) // 'player' 或 'enemy'
const currentSkill = ref('')
const attackAnimation = ref(false)
// 受伤效果
const isPlayerHurt = ref(false)
const isEnemyHurt = ref(false)
// 回合计数
const currentRound = computed(() => battleStore.currentRound)
// 战斗开始弹窗
const showBattleStartPopup = ref(false)
// 胜负已分弹窗
const showBattleEndPopup = ref(false)
// 精灵死亡切换确认弹窗
const showElfDeathDialog = ref(false)
const elfDeathMessage = ref('')
const elfDeathCallback = ref(null)
// BGM音频对象
const battleBgm = ref(null)
// 按钮禁用状态
const isButtonsDisabled = ref(false)
const isBattleLogOpen = ref(false)

// 保存战斗类型（在onMounted时初始化，避免onUnmounted时route.query丢失）
let currentBattleType = ''

// 战斗是否已结束（用于onUnmounted中判断是否需要调用离线接口）
let battleEnded = false

// 技能列表
const skills = ref([])
// 药品列表
const potions = ref([])
// 药品使用弹窗
const showPotions = ref(false)
const projectileParticles = [1, 2, 3, 4, 5, 6]

const battleMode = computed(() => route.query.type === 'train' ? 'train' : 'pve')
const battleNavPath = computed(() => battleMode.value === 'train' ? '/train' : '/pve')
const battleStageLabel = computed(() => battleMode.value === 'train' ? '训练擂台' : '冒险战场')
const battleRoundDisplay = computed(() => String(currentRound.value || 1).padStart(2, '0'))
const battleArenaClass = computed(() => `arena-${resolveElementTheme(enemyElementType.value, enemyName.value || '')}`)
const showProjectile = computed(() => attackAnimation.value && !!attackingSide.value && currentSkill.value && currentSkill.value !== '普通攻击')
const projectileMotionClass = computed(() => attackingSide.value === 'player' ? 'move-right' : 'move-left')
const currentAttackElement = computed(() => {
  const attackerElementType = attackingSide.value === 'player'
    ? playerElf.value?.elementType
    : enemyElementType.value
  return resolveElementTheme(attackerElementType, currentSkill.value || '')
})
const projectileElementClass = computed(() => `projectile-${currentAttackElement.value}`)
const currentStrikeClass = computed(() => currentSkill.value === '普通攻击' ? 'is-basic-strike' : 'is-skill-strike')
const isMannequinEnemy = computed(() => (enemyName.value || '').includes('训练人偶'))
const battleResultActionLabel = computed(() => battleMode.value === 'train' ? '返回训练' : '返回关卡')

const closeCommandPanels = () => {
  showSkills.value = false
  showSwitchElf.value = false
  showPotions.value = false
}

const toggleBattleLog = () => {
  isBattleLogOpen.value = !isBattleLogOpen.value
}

const closeBattleLog = () => {
  isBattleLogOpen.value = false
}

const handleBattlePageKeydown = (event) => {
  if (event.key !== 'Escape') return

  closeCommandPanels()

  if (isBattleLogOpen.value) {
    isBattleLogOpen.value = false
  }
}

const enemySpriteSrc = computed(() => {
  if (enemyName.value === '迪莫') {
    return new URL('../assets/photo/t01ad4c8a8c1b78d5e9.jpg', import.meta.url).href
  }
  if (enemyName.value === '焰阳火灵') {
    return new URL('../assets/photo/78714861ed8311a9d5af0982af3bf12196230622.jpg', import.meta.url).href
  }
  if (enemyName.value === '惊涛水灵') {
    return new URL('../assets/photo/7acb0a46f21fbe096b63310c0e2a1b338744ebf807da.png', import.meta.url).href
  }
  if (enemyName.value === '魔草巫灵') {
    return new URL('../assets/photo/ac6eddc451da81cb39db2c96742dc7160924ab189b1f.png', import.meta.url).href
  }
  if ((enemyName.value || '').includes('训练人偶') && enemyElementType.value === 1) {
    return new URL('../assets/photo/火-训练人偶.png', import.meta.url).href
  }
  if ((enemyName.value || '').includes('训练人偶') && enemyElementType.value === 2) {
    return new URL('../assets/photo/水-训练人偶.png', import.meta.url).href
  }
  if ((enemyName.value || '').includes('训练人偶') && enemyElementType.value === 3) {
    return new URL('../assets/photo/草-训练人偶.png', import.meta.url).href
  }
  return new URL('../assets/hero.png', import.meta.url).href
})

const enemySpriteAlt = computed(() => enemyName.value || '敌人')

// 计算HP百分比
const enemyHpPercentage = computed(() => {
  if (!enemyMaxHp.value || enemyMaxHp.value === 0) return 0
  const hp = Math.max(0, enemyHp.value || 0)
  const percentage = (hp / enemyMaxHp.value) * 100
  return Math.max(0, Math.min(100, percentage)) // 限制在0-100之间
})

const playerHpPercentage = computed(() => {
  if (!playerElf.value || !playerElf.value.maxHp || playerElf.value.maxHp === 0) return 0
  const hp = Math.max(0, playerElf.value.hp || 0)
  const percentage = (hp / playerElf.value.maxHp) * 100
  return Math.max(0, Math.min(100, percentage)) // 限制在0-100之间
})

// 根据精灵ID获取图片路径
const getElfImage = (elfId) => {
  switch (elfId) {
    case 1:
      return new URL('../assets/photo/sasuke/佐助.jpg', import.meta.url).href
    case 2:
      return new URL('../assets/photo/zhaomeiming/照美冥.webp', import.meta.url).href
    case 3:
      return new URL('../assets/photo/qianshouzhujian/千手柱间.jpg', import.meta.url).href
    default:
      return new URL('../assets/hero.png', import.meta.url).href
  }
}

// 根据elementType获取系别名称
const getElementType = (elementType) => {
  if (elementType === null || elementType === undefined) {
    return '未知'
  }
  switch (elementType) {
    case 1:
      return '火系'
    case 2:
      return '水系'
    case 3:
      return '草系'
    case 4:
      return '光系'
    default:
      return '未知'
  }
}

// 根据elementType获取CSS颜色类名
const getElementColorClass = (elementType) => {
  switch (elementType) {
    case 1:
      return 'element-fire'
    case 2:
      return 'element-water'
    case 3:
      return 'element-grass'
    default:
      return ''
  }
}

// 获取训练人偶类型名称
const getMannequinTypeName = (type) => {
  switch (type) {
    case 1:
      return '火系训练人偶'
    case 2:
      return '水系训练人偶'
    case 3:
      return '草系训练人偶'
    default:
      return '训练人偶'
  }
}

// 获取御三家精灵
const loadStarterElves = async () => {
  try {
    loadingStarterElves.value = true
    const response = await elfApi.getStarterElves()
    if (response.code === 200) {
      starterElves.value = response.data
    }
  } catch (error) {
    console.error('获取御三家精灵失败:', error)
  } finally {
    loadingStarterElves.value = false
  }
}

// 获取用户已拥有的精灵ID列表
const loadOwnedElves = async () => {
  try {
    const response = await userElfApi.list()
    if (response.code === 200) {
      ownedElfIds.value = response.data.map(elf => elf.elfId)
    }
  } catch (error) {
    console.error('获取用户精灵列表失败:', error)
  }
}

// 选择初始精灵
const selectStarter = async (elfId) => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    const user = JSON.parse(userStr)
    const response = await userElfApi.create(elfId)
    if (response.code === 200) {
      // 更新已拥有精灵列表
      await loadOwnedElves()
      alert('精灵选择成功！')
      // 关闭御三家选择弹窗
      showStarterSelection.value = false
    } else {
      alert('精灵选择失败: ' + response.msg)
    }
  } catch (error) {
    console.error('精灵选择失败:', error)
    alert('精灵选择失败，请稍后重试')
  }
}

// 关闭御三家选择弹窗
const closeStarterSelection = () => {
  showStarterSelection.value = false
  // 开始倒计时
  startCountdown()
}

// 开始倒计时
const startCountdown = () => {
  // 清除之前的定时器
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }
  
  // 重置倒计时
  countdown.value = 10
  
  // 开始新的倒计时
  countdownTimer.value = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      // 倒计时结束，自动使用普攻
      clearInterval(countdownTimer.value)
      if (!showResult.value) {
        attack()
      }
    }
  }, 1000)
}

// 重置倒计时
const resetCountdown = () => {
  startCountdown()
}

// 判断是否是敌人的对话
const isEnemyDialog = (log) => {
  if (!log || typeof log !== 'string' || !enemyName.value) {
    return false
  }
  return log.includes(enemyName.value) && (log.includes('说：') || log.includes('嘲讽道：') || log.includes('冷笑道：') || log.includes('大笑：') || log.includes('不屑地说：') || log.includes('怒道：') || log.includes('咬牙切齿：') || log.includes('狞笑着说：') || log.includes('冷声道：') || log.includes('怒吼：') || log.includes('惊慌失措：') || log.includes('垂死挣扎：') || log.includes('哀求：'))
}

// 从战斗日志中提取敌人的对话
const extractEnemyDialog = (battleLog) => {
  if (!battleLog || battleLog.length === 0) {
    return
  }
  
  // 遍历战斗日志，查找最新的敌人对话
  for (let i = battleLog.length - 1; i >= 0; i--) {
    const log = battleLog[i]
    // 检查日志是否包含敌人的名称
    if (isEnemyDialog(log)) {
      // 只有当是新的嘲讽时才更新（避免重复显示同一条）
      if (enemyDialog.value !== log) {
        enemyDialog.value = log
        console.log('[DEBUG] 显示怪物嘲讽:', log)
        
        // 5秒后自动隐藏嘲讽
        setTimeout(() => {
          if (enemyDialog.value === log) {
            enemyDialog.value = ''
          }
        }, 5000)
      }
      break
    }
  }
}

// 检查攻击记录并触发动画
const triggerAttackAnimations = (battleLog, battleData) => {
  if (!battleLog || battleLog.length === 0) {
    return
  }
  
  // 收集当前回合的攻击记录（按后端返回的顺序）
  const attackLogs = []
  
  console.log('[DEBUG] 处理战斗日志:', battleLog)
  
  // 按顺序遍历，收集所有攻击记录（保持后端返回的顺序）
  for (let i = 0; i < battleLog.length; i++) {
    const log = battleLog[i]
    
    // 检查是否是攻击记录
    if (typeof log === 'string' && (log.includes('攻击了') || log.includes('使用技能') || log.includes('普通攻击') || log.includes('使用普通攻击'))) {
      console.log('[DEBUG] 发现攻击记录:', log)
      attackLogs.push(log) // 保持原始顺序
    }
  }
  
  // 去重，避免重复的攻击记录
  const uniqueAttackLogs = []
  const seenLogs = new Set()
  for (const log of attackLogs) {
    if (!seenLogs.has(log)) {
      seenLogs.add(log)
      uniqueAttackLogs.push(log)
    }
  }
  
  // 依次触发攻击动画
  console.log('[DEBUG] 攻击动画队列:', uniqueAttackLogs)
  console.log('[DEBUG] 攻击动画队列长度:', uniqueAttackLogs.length)
  if (uniqueAttackLogs.length > 0) {
    let delay = 0
    
    for (const log of uniqueAttackLogs) {
      console.log('[DEBUG] 准备触发动画，延迟:', delay, '日志:', log)
      console.log('[DEBUG] 动画索引:', uniqueAttackLogs.indexOf(log), '/', uniqueAttackLogs.length)
      
      // 使用立即执行的异步函数，确保每个动画独立执行
      ;((currentLog, currentDelay) => {
        setTimeout(async () => {
          console.log('[DEBUG] === 开始触发动画 ===')
          console.log('[DEBUG] 日志内容:', currentLog)
          console.log('[DEBUG] attackingSide 当前值:', attackingSide.value)
                
          // 先重置所有动画状态，确保干净的状态
          attackAnimation.value = false
          attackingSide.value = null
          currentSkill.value = ''
          await nextTick() // 等待DOM更新
          await new Promise(resolve => setTimeout(resolve, 50)) // 额外等待50ms确保DOM完全更新
                
          // 判断是玩家还是敌人攻击
          if (currentLog.includes('你的精灵')) {
            // 玩家攻击
            console.log('[DEBUG] ✓ 匹配到玩家攻击')
            console.log('[DEBUG] 设置 attackingSide = player')
            attackingSide.value = 'player'
            // 提取技能名称
            let skillName = '普通攻击'
            if (currentLog.includes('使用技能')) {
              // 匹配“使用技能 技能名称”格式
              const match = currentLog.match(/使用技能 (.+?)，/)
              if (match && match[1]) {
                skillName = match[1].trim()
              }
            }
            currentSkill.value = skillName
            console.log('[DEBUG] 玩家攻击，技能:', skillName)
                                      
            // 播放技能音频（只有玩家攻击时才播放）
            if (playerElf.value.elfName === '宇智波佐助' && skillName === '豪火球之术') {
              try {
                const audio = new Audio('/src/assets/audio/sasuke/豪火球.mp3')
                audio.play().catch(error => {
                  console.error('技能音频播放失败，需要用户交互:', error)
                })
              } catch (audioError) {
                console.error('播放技能音频失败:', audioError)
              }
            } else if (playerElf.value.elfName === '照美冥' && skillName === '水龙弹之术') {
              try {
                const audio = new Audio('/src/assets/audio/sasuke/水龙弹.mp3')
                audio.play().catch(error => {
                  console.error('技能音频播放失败，需要用户交互:', error)
                })
              } catch (audioError) {
                console.error('播放技能音频失败:', audioError)
              }
            }
          } else if (currentLog.includes('怪物') || currentLog.includes('训练人偶') || currentLog.includes('迪莫') || currentLog.includes('焰阳火灵') || currentLog.includes('惊涛水灵') || currentLog.includes('魔草巫灵')) {
            // 敌人攻击（包括怪物和训练人偶）
            console.log('[DEBUG] ✓ 匹配到敌人攻击')
            console.log('[DEBUG] 触发敌人攻击动画:', currentLog)
                  
            // 区分训练人偶和怪物
            if (currentLog.includes('训练人偶')) {
              console.log('[DEBUG] 检测到训练人偶攻击')
            } else {
              console.log('[DEBUG] 检测到怪物攻击')
            }
                  
            console.log('[DEBUG] 设置 attackingSide = enemy')
            attackingSide.value = 'enemy'
            // 提取技能名称
            let skillName = '普通攻击'
            if (currentLog.includes('使用技能')) {
              // 匹配“使用技能 技能名称”格式
              const match = currentLog.match(/使用技能 (.+?)，/)
              if (match && match[1]) {
                skillName = match[1].trim()
              }
            }
            currentSkill.value = skillName
            console.log('[DEBUG] 敌人攻击，技能:', skillName, 'attackingSide:', attackingSide.value)
            // 注意：敌人攻击时不播放玩家的技能音频
          } else {
            console.log('[DEBUG] ✗ 未匹配到攻击方，日志:', currentLog)
          }
                
          console.log('[DEBUG] 最终 attackingSide 值:', attackingSide.value)
          console.log('[DEBUG] 最终 currentSkill 值:', currentSkill.value)
                
          // 等待Vue更新DOM后再触发动画
          await nextTick()
                
          console.log('[DEBUG] 触发 attackAnimation = true')
          // 触发动画
          attackAnimation.value = true
          
          // 动画持续1秒
          setTimeout(() => {
            attackAnimation.value = false
            // 延迟重置攻击方，确保动画显示完整
            setTimeout(() => {
              attackingSide.value = null // 重置攻击方
            }, 100)
            
            // 触发受伤动画
            if (currentLog.includes('你的精灵')) {
              // 玩家攻击，敌人受伤
              isEnemyHurt.value = true
              setTimeout(() => {
                isEnemyHurt.value = false
                // 结算玩家攻击的UI变化
                if (battleData) {
                  // 更新敌人状态
                  enemyHp.value = battleData.monsterHp ?? battleData.mannequinHp ?? enemyHp.value
                  // 更新敌人MP（怪物使用技能后）
                  enemyMp.value = battleData.monsterMp ?? battleData.mannequinMp ?? enemyMp.value
                  // 检查敌人是否战败
                  if (enemyHp.value <= 0) {
                    // 停止BGM
                    if (battleBgm.value) {
                      battleBgm.value.pause()
                      battleBgm.value = null
                    }
                  }
                  // 更新玩家精灵的MP（如果使用了技能）
                  playerElf.value.mp = battleData.elfMp ?? battleData.playerElf?.mp ?? playerElf.value.mp
                }
              }, 500)
            } else if (currentLog.includes('怪物') || currentLog.includes('训练人偶') || currentLog.includes('迪莫') || currentLog.includes('焰阳火灵') || currentLog.includes('惊涛水灵') || currentLog.includes('魔草巫灵')) {
              // 敌人攻击，玩家受伤
              isPlayerHurt.value = true
              setTimeout(() => {
                isPlayerHurt.value = false
                // 结算敌人攻击的UI变化
                if (battleData) {
                  playerElf.value.hp = battleData.playerElfHp ?? battleData.elfCurrentHp ?? playerElf.value.hp
                  playerElf.value.mp = battleData.elfMp ?? battleData.playerElf?.mp ?? playerElf.value.mp
                  // 更新敌人MP（怪物使用技能后）
                  enemyMp.value = battleData.monsterMp ?? battleData.mannequinMp ?? enemyMp.value
                  // 检查玩家精灵是否战败
                  if (playerElf.value.hp <= 0) {
                    // 停止BGM
                    if (battleBgm.value) {
                      battleBgm.value.pause()
                      battleBgm.value = null
                    }
                  }
                }
              }, 500)
            }
          }, 1000)
        }, currentDelay)
      })(log, delay)
      
      // 每个攻击之间间隔1.3秒，确保动画完整播放（1秒动画 + 0.3秒间隔）
      delay += 1300
    }
  }
}

// 兼容旧的函数名，防止错误
const checkEnemyAttack = (battleLog) => {
  triggerAttackAnimations(battleLog)
}

// 加载药品列表
const loadPotions = async () => {
  try {
    const response = await potionApi.getUserPotions()
    if (response.code === 200) {
      potions.value = response.data
      console.log('药品数据结构:', potions.value)
    }
  } catch (error) {
    console.error('加载药品失败:', error)
  }
}

// 获取药品图片
const getPotionImage = (name) => {
  if (name === '血瓶') {
    return new URL('../assets/photo/equip/血瓶.jpg', import.meta.url).href
  } else if (name === '蓝瓶') {
    return new URL('../assets/photo/equip/蓝瓶.jpg', import.meta.url).href
  } else {
    return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=health%20potion%20blue%20potion%20fantasy%20style&image_size=square'
  }
}

// 进入战斗
const enterBattle = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      router.push('/auth')
      return
    }
    const user = JSON.parse(userStr)
    const battleType = route.query.type
    const levelId = route.params.id
    
    // 训练模式：从URL参数中读取训练人偶属性
    if (battleType === 'train') {
      const mannequinAttack = parseInt(route.query.mannequinAttack) || 50
      const mannequinDefense = parseInt(route.query.mannequinDefense) || 30
      const mannequinHp = parseInt(route.query.mannequinHp) || 200
      const mannequinMp = parseInt(route.query.mannequinMp) || 100
      const mannequinSpeed = parseInt(route.query.mannequinSpeed) || 50
      const mannequinType = parseInt(route.query.mannequinType) || 1
      const mannequinIsAttack = parseInt(route.query.mannequinIsAttack) || 1
      
      // 调用后端初始化训练状态
      const startTrainResponse = await trainApi.startTrain(
        mannequinAttack,
        mannequinDefense,
        mannequinHp,
        mannequinMp,
        mannequinSpeed,
        mannequinType,
        mannequinIsAttack
      )
      
      if (startTrainResponse.code !== 200) {
        alert('开始训练失败: ' + startTrainResponse.msg)
        router.push('/train')
        return
      }
      
      const data = startTrainResponse.data
      
      // 设置本地状态
      playerElf.value = data.playerElf || {}
      playerElf.value.elfName = data.playerElf?.elfName || ''
      playerElf.value.elementType = data.playerElf?.elementType || 0
      playerElf.value.hp = data.playerElfHp
      playerElf.value.maxHp = data.playerElf?.maxHp || 0
      playerElf.value.mp = data.elfMp
      playerElf.value.maxMp = data.playerElf?.maxMp || 0
      
      // 设置训练人偶状态
      enemyName.value = getMannequinTypeName(data.mannequin?.type || mannequinType)
      enemyHp.value = data.mannequinHp
      enemyMaxHp.value = data.mannequin?.hp || mannequinHp
      enemyMp.value = data.mannequinMp
      enemyMaxMp.value = data.mannequin?.mp || mannequinMp
      enemyElementType.value = data.mannequin?.type || mannequinType
      
      // 初始化battleStore
      battleStore.initBattle({
        battleId: 'train_' + Date.now(),
        currentRound: 1,
        levelId: null,
        status: 0,
        elves: [{
          elf_id: data.playerElf?.id,
          current_hp: data.playerElfHp,
          current_mp: data.elfMp,
          elf_state: 0,
          level: data.playerElf?.level,
          maxHp: data.playerElf?.maxHp || 0,
          maxMp: data.playerElf?.maxMp || 0,
          elfName: data.playerElf?.elfName || '',
          elementType: data.playerElf?.elementType || 0,
          speed: data.playerElf?.speed || 0
        }],
        monsters: [{
          monster_id: null,
          current_hp: data.mannequinHp,
          current_mp: data.mannequinMp,
          is_alive: 1,
          maxHp: data.mannequin?.hp || mannequinHp,
          maxMp: data.mannequin?.mp || mannequinMp,
          attack: data.mannequin?.attack || mannequinAttack,
          defense: data.mannequin?.defense || mannequinDefense,
          speed: data.mannequin?.speed || mannequinSpeed,
          type: data.mannequin?.type || mannequinType,
          isAttack: data.mannequin?.isAttack || mannequinIsAttack
        }]
      })
      
      // 处理战斗日志
      if (data.roundLogs) {
        battleLogs.value = data.roundLogs
      } else {
        battleLogs.value = [{
          round: 1,
          logs: data.trainLog || []
        }]
      }
      
      // 获取精灵技能
      const elfDetailResponse = await userElfApi.getDetail(data.playerElf?.id)
      if (elfDetailResponse.code === 200) {
        const elfDetailData = elfDetailResponse.data
        skills.value = elfDetailData.unlockedSkills || []
      }
      
      // 加载药品列表
      await loadPotions()
      
      // 显示战斗开始弹窗
      showBattleStartPopup.value = true
      
      // 播放战斗开始语音
      try {
        const audio = new Audio('/src/assets/audio/battle/战斗开始.mp3')
        audio.play().catch(error => {
          console.error('战斗开始音频播放失败，需要用户交互:', error)
        })
      } catch (audioError) {
        console.error('播放语音失败:', audioError)
      }
      
      // 预加载BGM
      try {
        const bgmFiles = [
          '/src/assets/audio/battle/青鸟.mp3',
          '/src/assets/audio/battle/重燃季.mp3'
        ]
        bgmFiles.forEach(file => {
          try {
            const audio = new Audio(file)
            audio.load()
          } catch (error) {
            console.error('预加载BGM失败:', error)
          }
        })
      } catch (audioError) {
        console.error('预加载BGM失败:', audioError)
      }
      
      // 1.5秒后自动关闭弹窗
      setTimeout(() => {
        showBattleStartPopup.value = false
        startCountdown()
        
        const bgmFiles = [
          '/src/assets/audio/battle/青鸟.mp3',
          '/src/assets/audio/battle/重燃季.mp3'
        ]
        const randomBgm = bgmFiles[Math.floor(Math.random() * bgmFiles.length)]
        
        try {
          battleBgm.value = new Audio(randomBgm)
          battleBgm.value.loop = true
          battleBgm.value.play().catch(error => {
            console.error('BGM自动播放失败，将在用户交互后播放:', error)
          })
        } catch (audioError) {
          console.error('播放BGM失败:', audioError)
        }
      }, 1500)
      
      return
    }
    
    // 关卡模式（后端自动读取玩家的出战精灵列表）
    const response = await levelApi.enterLevel(levelId)
    
    if (response.code === 200) {
      const data = response.data
      console.log('完整的战斗数据:', data)
      
      // 初始化战斗状态到battleStore
      const battleData = {
        battleId: data.battleId,
        currentRound: 1,
        levelId: levelId,
        status: 0,
        elves: [{
          elf_id: data.elf?.id || data.userElf?.id,
          current_hp: data.playerElfHp || (data.elf?.hp || data.userElf?.hp || 0),
          current_mp: data.elfMp || (data.elf?.mp || data.userElf?.mp || 0),
          elf_state: 0,
          level: data.elf?.level || data.userElf?.level,
          maxHp: data.userElf?.maxHp || (data.elf?.maxHp || 0),
          maxMp: data.userElf?.maxMp || (data.elf?.maxMp || 0),
          elfName: data.elfName || (data.userElf?.elfName || ''),
          elementType: data.elfElementType || (data.userElf?.elementType || 0)
        }],
        monsters: [{
          monster_id: data.monsterId || data.mannequin?.id,
          current_hp: data.monsterHp || (data.mannequinHp || 0),
          current_mp: data.monsterMp || (data.mannequinMp || data.mannequin?.mp || 0),
          is_alive: 1,
          maxHp: data.monsterMaxHp || (data.mannequinHp || 0),
          maxMp: data.monsterMaxMp || (data.mannequin?.mp || 0)
        }]
      }
      
      // 保存到store
      battleStore.initBattle(battleData)
      
      // 设置本地状态
      playerElf.value = data.elf || data.userElf || data.playerElf || {}
      // 添加精灵名字
      playerElf.value.elfName = data.elfName || (data.userElf?.elfName || '')
      // 添加精灵系别
      playerElf.value.elementType = data.elfElementType || (data.userElf?.elementType || 0)
      // 设置精灵血量
      playerElf.value.hp = data.playerElfHp || (data.elf?.hp || data.userElf?.hp || 0)
      // 设置精灵最大血量
      playerElf.value.maxHp = data.playerElfMaxHp || data.userElf?.maxHp || (data.elf?.maxHp || 0)
      // 设置精灵蓝量
      playerElf.value.mp = data.elfMp || (data.elf?.mp || data.userElf?.mp || 0)
      // 设置精灵最大蓝量
      playerElf.value.maxMp = data.playerElfMaxMp || data.userElf?.maxMp || (data.elf?.maxMp || 0)
      enemyName.value = data.monsterName || `训练人偶 ${data.mannequin?.type === 1 ? '火系' : data.mannequin?.type === 2 ? '水系' : '草系'}`
      enemyHp.value = data.monsterHp || (data.mannequinHp || 0)
      enemyMaxHp.value = data.monsterMaxHp || data.mannequinMaxHp || (data.mannequinHp || 0)
      enemyMp.value = data.monsterMp || (data.mannequinMp || data.mannequin?.mp || 0)
      enemyMaxMp.value = data.monsterMaxMp || data.mannequinMaxMp || (data.mannequin?.mp || 0)
      enemyElementType.value = data.monsterElementType || (data.mannequin?.type || 0)
      console.log('精灵系别:', data.elfElementType || data.userElf?.elementType || 0, '敌人系别:', data.monsterElementType || (data.mannequin?.type || 0))
      console.log('敌人名字:', enemyName.value)
      
      // 处理战斗日志
      if (data.roundLogs) {
        // 使用后端返回的按回合组织的战斗日志
        battleLogs.value = data.roundLogs
        // 更新当前回合
        if (data.roundLogs.length > 0) {
          battleStore.currentRound = data.roundLogs[data.roundLogs.length - 1].round
        }
      } else {
        // 兼容旧格式
        const logs = data.battleLog || data.trainLog || []
        const initialLogs = logs.filter(log => !isEnemyDialog(log))
        // 将初始日志添加到第一回合
        battleLogs.value[0].logs.push(...initialLogs)
      }
      
      // 获取精灵技能
      const elfDetailResponse = await userElfApi.getDetail(playerElf.value.id)
      if (elfDetailResponse.code === 200) {
        const detailData = elfDetailResponse.data
        skills.value = detailData.unlockedSkills || []
      }
      
      // 加载药品列表
      await loadPotions()
      
      // 显示战斗开始弹窗
      showBattleStartPopup.value = true
      
      // 播放战斗开始语音
      try {
        const audio = new Audio('/src/assets/audio/battle/战斗开始.mp3')
        audio.play().catch(error => {
          console.error('战斗开始音频播放失败，需要用户交互:', error)
        })
      } catch (audioError) {
        console.error('播放语音失败:', audioError)
      }
      
      // 预加载BGM
      try {
        const bgmFiles = [
          '/src/assets/audio/battle/青鸟.mp3',
          '/src/assets/audio/battle/重燃季.mp3'
        ]
        bgmFiles.forEach(file => {
          try {
            const audio = new Audio(file)
            audio.load()
          } catch (error) {
            console.error('预加载BGM失败:', error)
          }
        })
      } catch (audioError) {
        console.error('预加载BGM失败:', audioError)
      }
      
      // 1.5秒后自动关闭弹窗
      setTimeout(() => {
        showBattleStartPopup.value = false
        // 开始倒计时
        startCountdown()
        
        // 随机播放BGM（需要用户交互后才能播放）
        const bgmFiles = [
          '/src/assets/audio/battle/青鸟.mp3',
          '/src/assets/audio/battle/重燃季.mp3'
        ]
        const randomBgm = bgmFiles[Math.floor(Math.random() * bgmFiles.length)]
        
        try {
          battleBgm.value = new Audio(randomBgm)
          battleBgm.value.loop = true
          // 尝试播放BGM，如果失败则在用户交互后再播放
          battleBgm.value.play().catch(error => {
            console.error('BGM自动播放失败，将在用户交互后播放:', error)
          })
        } catch (audioError) {
          console.error('播放BGM失败:', audioError)
        }
      }, 1500)
    } else {
      alert('进入战斗失败: ' + response.msg)
      if (battleType === 'train') {
        router.push('/train')
      } else {
        router.push('/pve')
      }
    }
  } catch (error) {
    console.error('进入战斗失败:', error)
    alert('进入战斗失败，请重试')
    const battleType = route.query.type
    if (battleType === 'train') {
      router.push('/train')
    } else {
      router.push('/pve')
    }
  }
}

// 使用药品
const usePotion = async (potion) => {
  try {
    const battleType = route.query.type
    
    // 根据战斗类型调用不同的API
    let response
    if (battleType === 'train') {
      // 训练模式：调用训练专门的药品使用接口
      response = await trainApi.usePotion(playerElf.value.id, potion.potionConfigId)
    } else {
      // 关卡模式：调用普通药品使用接口
      response = await potionApi.usePotion(playerElf.value.id, potion.potionConfigId)
    }
    
    if (response.code === 200) {
      const data = response.data
      // 更新精灵状态
      playerElf.value.hp = data.elf.hp
      playerElf.value.mp = data.elf.mp
      
      // 更新battleStore中的状态
      if (battleStore.elves.length > 0) {
        battleStore.elves[0].current_hp = data.elf.hp
        battleStore.elves[0].current_mp = data.elf.mp
      }
      
      // 添加战斗日志
      if (potion.type === 1) {
        // 血瓶
        battleLogs.value[battleLogs.value.length - 1].logs.push(`使用了${potion.name}，恢复了${potion.healValue}点生命值`)
      } else if (potion.type === 2) {
        // 蓝瓶
        battleLogs.value[battleLogs.value.length - 1].logs.push(`使用了${potion.name}，恢复了${potion.healValue}点魔法值`)
      }
      
      // 重新加载药品列表，更新数量
      await loadPotions()
      // 关闭药品弹窗
      showPotions.value = false
    } else {
      alert('使用药品失败: ' + response.msg)
    }
  } catch (error) {
    console.error('使用药品失败:', error)
    alert('使用药品失败，请重试')
  } finally {
    // 重置倒计时
    if (!showResult.value) {
      resetCountdown()
    }
  }
}

// 战斗操作
const attack = async () => {
  try {
    // 检查是否已经显示战斗结果
    if (showResult.value) {
      alert('战斗已经结束，无法再次攻击')
      return
    }
    
    // 禁用按钮
    isButtonsDisabled.value = true
    
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    const user = JSON.parse(userStr)
    const battleType = route.query.type
    
    // 根据战斗类型调用相应的API
    let response
    if (battleType === 'train') {
      response = await trainApi.normalAttack()
    } else {
      response = await battleApi.normalAttack()
    }
    if (response.code === 200) {
        const data = response.data
        console.log('[DEBUG] 攻击响应完整数据:', data)
        console.log('[DEBUG] playerElfId:', data.playerElfId, '当前精灵ID:', playerElf.value.id)
        
        // 立即更新MP值，确保UI及时反映MP消耗（训练模式下，如果发生了精灵切换，延迟更新）
        if (data.elfMp !== undefined) {
          // 训练模式：如果elfSwitched为true，说明后端已自动切换，暂不更新UI，等弹窗确认后再更新
          if (battleType === 'train' && data.elfSwitched) {
            console.log('[DEBUG] attack函数-检测到elfSwitched，暂不更新MP，等待弹窗确认')
          } else {
            playerElf.value.mp = data.elfMp
            // 更新battleStore中的状态
            if (battleStore.elves.length > 0) {
              battleStore.elves[0].current_mp = data.elfMp
            }
          }
        } else if (data.userElf?.mp !== undefined) {
          if (battleType === 'train' && data.elfSwitched) {
            console.log('[DEBUG] attack函数-检测到elfSwitched，暂不更新MP(userElf)，等待弹窗确认')
          } else {
            playerElf.value.mp = data.userElf.mp
            // 更新battleStore中的状态
            if (battleStore.elves.length > 0) {
              battleStore.elves[0].current_mp = data.userElf.mp
            }
          }
        }
        
        // 处理精灵切换（需要立即更新）
        if (data.status === 3 || data.trainResult === 'switch') {
          // 精灵战败提示
          alert(`${playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`} 战败！`)
          // 切换到新精灵
          playerElf.value = data.elf || data.userElf || {}
          playerElf.value.elfName = data.elfName || (data.userElf?.elfName || '')
          playerElf.value.elementType = data.elfElementType || (data.userElf?.elementType || 0)
          // 新精灵登场提示
          alert(`${playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`} 登场！`)
          
          // 更新battleStore中的状态
          battleStore.elves = [{
            elf_id: playerElf.value.id,
            current_hp: playerElf.value.hp,
            current_mp: playerElf.value.mp,
            elf_state: 0,
            level: playerElf.value.level,
            maxHp: playerElf.value.maxHp,
            maxMp: playerElf.value.maxMp,
            elfName: playerElf.value.elfName,
            elementType: playerElf.value.elementType
          }]
          
          // 更新新精灵的技能列表
          const elfDetailResponse = await userElfApi.getDetail(playerElf.value.id)
          if (elfDetailResponse.code === 200) {
            const detailData = elfDetailResponse.data
            skills.value = detailData.unlockedSkills || []
          }
        } else {
          // 处理战斗日志
          if (data.roundLogs) {
            // 使用后端返回的按回合组织的战斗日志
            battleLogs.value = data.roundLogs
            // 更新当前回合
            if (data.roundLogs.length > 0) {
              battleStore.currentRound = data.roundLogs[data.roundLogs.length - 1].round
            }
            // 提取敌人的对话
            data.roundLogs.forEach(round => {
              extractEnemyDialog(round.logs)
            })
            
            // 只有当战斗结果不是switch时才触发攻击动画
            if (data.status !== 3 && data.trainResult !== 'switch') {
              const latestRound = data.roundLogs[data.roundLogs.length - 1]
              if (latestRound) {
                triggerAttackAnimations(latestRound.logs, data)
              }
            }
          } else {
            // 兼容旧格式
            // 从战斗日志中提取敌人的对话
            extractEnemyDialog(data.battleLog || data.trainLog)
            
            // 保存当前回合数，避免异步操作导致的问题
            const currentRoundNum = battleStore.currentRound
            // 确保battleLogs数组存在
            if (!battleLogs.value) {
              battleLogs.value = []
            }
            // 确保当前回合对象存在
            if (!battleLogs.value[currentRoundNum - 1]) {
              battleLogs.value.push({ round: currentRoundNum, logs: [] })
            }
            // 确保logs数组存在
            if (!battleLogs.value[currentRoundNum - 1].logs) {
              battleLogs.value[currentRoundNum - 1].logs = []
            }
            const currentRoundLogs = battleLogs.value[currentRoundNum - 1].logs
            const newLogs = (data.battleLog || data.trainLog)?.filter(log => 
              !currentRoundLogs.includes(log) && 
              !isEnemyDialog(log)
            ) || []
            if (newLogs.length > 0) {
              battleLogs.value[currentRoundNum - 1].logs.push(...newLogs)
            }
            
            // 只有当战斗结果不是switch时才触发攻击动画
            if (data.status !== 3 && data.trainResult !== 'switch') {
              triggerAttackAnimations(newLogs, data)
            }
          }
          
          // 更新怪物状态
          if (data.monsterHp !== undefined) {
            enemyHp.value = data.monsterHp
            if (battleStore.monsters.length > 0) {
              battleStore.monsters[0].current_hp = data.monsterHp
            }
          }
          if (data.monsterMp !== undefined) {
            enemyMp.value = data.monsterMp
            if (battleStore.monsters.length > 0) {
              battleStore.monsters[0].current_mp = data.monsterMp
            }
          }
          
          // 更新精灵状态（训练模式下，如果发生了精灵切换，延迟更新UI）
          if (data.playerElfHp !== undefined) {
            // 训练模式：如果elfSwitched为true，说明后端已自动切换，暂不更新UI，等弹窗确认后再更新
            if (battleType === 'train' && data.elfSwitched) {
              console.log('[DEBUG] attack函数-检测到elfSwitched，暂不更新HP，等待弹窗确认')
            } else {
              playerElf.value.hp = data.playerElfHp
              if (battleStore.elves.length > 0) {
                battleStore.elves[0].current_hp = data.playerElfHp
              }
            }
          }
          if (data.playerElfMp !== undefined) {
            // 训练模式：如果elfSwitched为true，说明后端已自动切换，暂不更新UI，等弹窗确认后再更新
            if (battleType === 'train' && data.elfSwitched) {
              console.log('[DEBUG] attack函数-检测到elfSwitched，暂不更新MP(playerElfMp)，等待弹窗确认')
            } else {
              playerElf.value.mp = data.playerElfMp
              if (battleStore.elves.length > 0) {
                battleStore.elves[0].current_mp = data.playerElfMp
              }
            }
          }
          
          // 检查精灵是否切换（仅训练模式）
          // 注意：如果elfSwitched为true，说明后端已自动切换，我们等待弹窗确认后再更新UI
          if (battleType === 'train' && data.playerElfId !== undefined && playerElf.value.id !== data.playerElfId) {
            // 如果elfSwitched为true，跳过这里的自动更新，等待弹窗确认
            if (data.elfSwitched) {
              console.log('[DEBUG] 训练模式-检测到精灵切换，但elfSwitched=true，等待弹窗确认后再更新UI')
            } else {
              console.log('[DEBUG] 训练模式-检测到精灵切换，旧ID:', playerElf.value.id, '新ID:', data.playerElfId)
              // 精灵ID变了，说明发生了自动切换
              const oldElfName = playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`
              
              // 重新获取新精灵的详细信息
              const elfDetailResponse = await userElfApi.getDetail(data.playerElfId)
              if (elfDetailResponse.code === 200) {
                const elfData = elfDetailResponse.data
                const elf = elfData.elf || elfData // 兼容两种数据结构
                console.log('[DEBUG] 获取新精灵详情:', elfData)
                console.log('[DEBUG] elf对象:', elf)
                
                // 更新playerElf（保持与enterBattle相同的数据结构）
                playerElf.value.id = elf.id
                playerElf.value.elfId = elf.elfId
                // 优先使用返回数据中的elfName，如果没有则从elf对象中获取
                playerElf.value.elfName = data.elfName || elf.elfName || ''
                playerElf.value.elementType = data.elfElementType || elf.elementType || 0
                playerElf.value.level = elf.level
                playerElf.value.maxHp = elf.maxHp
                playerElf.value.maxMp = elf.maxMp
                playerElf.value.hp = data.playerElfHp !== undefined ? data.playerElfHp : (elf.hp || elf.maxHp)
                playerElf.value.mp = data.elfMp !== undefined ? data.elfMp : (elf.mp || elf.maxMp)
                
                console.log('[DEBUG] playerElf更新后:', {
                  id: playerElf.value.id,
                  elfId: playerElf.value.elfId,
                  elfName: playerElf.value.elfName,
                  elementType: playerElf.value.elementType,
                  hp: playerElf.value.hp,
                  maxHp: playerElf.value.maxHp,
                  mp: playerElf.value.mp,
                  maxMp: playerElf.value.maxMp
                })
                
                // 更新battleStore
                battleStore.elves[0].elf_id = playerElf.value.id
                battleStore.elves[0].elfId = playerElf.value.elfId
                battleStore.elves[0].current_hp = playerElf.value.hp
                battleStore.elves[0].current_mp = playerElf.value.mp
                battleStore.elves[0].level = playerElf.value.level
                battleStore.elves[0].maxHp = playerElf.value.maxHp
                battleStore.elves[0].maxMp = playerElf.value.maxMp
                battleStore.elves[0].elfName = playerElf.value.elfName
                battleStore.elves[0].elementType = playerElf.value.elementType
                
                // 更新技能列表
                skills.value = elfData.unlockedSkills || []
                
                const newElfName = playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`
                console.log('[DEBUG] 精灵切换完成，新精灵:', newElfName, 'elfId:', playerElf.value.elfId)
                
                // 显示新精灵登场提示
                setTimeout(() => {
                  alert(`${newElfName} 登场！\n\nHP: ${playerElf.value.hp}/${playerElf.value.maxHp}\nMP: ${playerElf.value.mp}/${playerElf.value.maxMp}`)
                }, 100)
              }
            }
          }
          
          // 检查精灵是否死亡（PVE模式和训练模式）
          console.log('[DEBUG] attack函数-检查精灵死亡 - battleType:', battleType, 'playerElfHp:', data.playerElfHp, 'needSwitch:', data.needSwitch, 'nextElfId:', data.nextElfId, 'elfSwitched:', data.elfSwitched)
          
          // 训练模式：检查elfSwitched标记（后端已自动切换精灵，需要显示弹窗）
          if (battleType === 'train' && data.elfSwitched) {
            console.log('[DEBUG] attack函数-检测到elfSwitched标记，显示精灵死亡弹窗')
            // 保存旧精灵信息（用于弹窗显示）
            const deadElfName = playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`
            const oldElfData = { ...playerElf.value }  // 保存当前精灵数据
            
            // 使用setTimeout确保在动画播放完成后显示弹窗
            setTimeout(async () => {
              console.log('[DEBUG] attack函数-显示精灵死亡弹窗')
              showElfDeathSwitchDialog(
                `${deadElfName} 被击败了！\n\n点击下方按钮确认切换到下一只精灵`,
                async () => {
                  console.log('[DEBUG] attack函数-用户确认切换精灵，开始更新UI')
                  // 用户点击确认后，才更新前端显示为新精灵
                  if (data.playerElf) {
                    playerElf.value.id = data.playerElf.id
                    playerElf.value.elfId = data.playerElf.elfId
                    playerElf.value.elfName = data.elfName
                    playerElf.value.elementType = data.elfElementType
                    playerElf.value.level = data.playerElf.level
                    playerElf.value.maxHp = data.playerElf.maxHp
                    playerElf.value.maxMp = data.playerElf.maxMp
                    playerElf.value.hp = data.playerElfHp !== null && data.playerElfHp !== undefined ? data.playerElfHp : data.playerElf.maxHp
                    playerElf.value.mp = data.elfMp !== null && data.elfMp !== undefined ? data.elfMp : data.playerElf.maxMp
                    playerElf.value.attack = data.playerElf.attack
                    playerElf.value.defense = data.playerElf.defense
                    playerElf.value.speed = data.playerElf.speed
                    
                    // 更新battleStore
                    if (battleStore.elves.length > 0) {
                      battleStore.elves[0].elf_id = playerElf.value.id
                      battleStore.elves[0].elfId = playerElf.value.elfId
                      battleStore.elves[0].current_hp = playerElf.value.hp
                      battleStore.elves[0].current_mp = playerElf.value.mp
                      battleStore.elves[0].level = playerElf.value.level
                      battleStore.elves[0].maxHp = playerElf.value.maxHp
                      battleStore.elves[0].maxMp = playerElf.value.maxMp
                      battleStore.elves[0].elfName = playerElf.value.elfName
                      battleStore.elves[0].elementType = playerElf.value.elementType
                    }
                    
                    // 更新技能列表
                    try {
                      const elfDetailResponse = await userElfApi.getDetail(playerElf.value.id)
                      if (elfDetailResponse.code === 200) {
                        skills.value = elfDetailResponse.data.unlockedSkills || []
                      }
                    } catch (error) {
                      console.error('[DEBUG] 获取技能列表失败:', error)
                    }
                    
                    // 显示新精灵登场提示
                    const newElfName = data.elfName || `精灵 ${data.playerElf.elfId}`
                    setTimeout(() => {
                      alert(`${newElfName} 登场！\n\nHP: ${playerElf.value.hp}/${playerElf.value.maxHp}\nMP: ${playerElf.value.mp}/${playerElf.value.maxMp}`)
                    }, 100)
                  }
                }
              )
            }, 2500) // 延迟2.5秒，等待受击动画播放完成
          }
          // PVE模式：检查needSwitch标记（前端需要调用切换接口）
          else if (battleType !== 'train' && data.playerElfHp !== undefined && data.playerElfHp <= 0) {
            console.log('[DEBUG] attack函数-当前精灵已死亡，HP:', data.playerElfHp)
            
            // 检查是否需要切换精灵
            if (data.needSwitch && data.nextElfId) {
              console.log('[DEBUG] attack函数-需要切换精灵，下一只精灵ID:', data.nextElfId)
              const deadElfName = playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`
              
              // 使用setTimeout确保在动画播放完成后显示弹窗
              setTimeout(async () => {
                console.log('[DEBUG] attack函数-显示精灵死亡弹窗')
                showElfDeathSwitchDialog(
                  `${deadElfName} 被击败了！\n\n点击下方按钮切换下一只精灵继续战斗`,
                  async () => {
                    console.log('[DEBUG] attack函数-用户确认切换精灵')
                    await switchToNextElf(data.nextElfId, data.nextElfHp, data.nextElfMp)
                  }
                )
              }, 2500) // 延迟2.5秒，等待受击动画播放完成
            } else {
              console.log('[DEBUG] attack函数-精灵死亡但不需要切换（可能没有其他可用精灵）')
            }
          }
        }
        
        // 处理战斗结果
        if (data.status === 1 || data.trainResult === '胜利' || data.status === 2 || data.trainResult === '失败' || data.trainResult === '逃跑') {
          // 立即停止BGM
          if (battleBgm.value) {
            battleBgm.value.pause()
            battleBgm.value = null
          }
          
          // 显示胜负已分弹窗
          showBattleEndPopup.value = true
          
          // 播放结束战斗语音
          try {
            const audio = new Audio('/src/assets/audio/battle/结束战斗.mp3')
            audio.play().catch(error => {
              console.error('结束战斗音频播放失败，需要用户交互:', error)
            })
          } catch (audioError) {
            console.error('播放语音失败:', audioError)
          }
          
          // 延迟显示战斗结果
          setTimeout(async () => {
            // 关闭胜负已分弹窗
            showBattleEndPopup.value = false
            
            if (data.status === 1 || data.trainResult === '胜利') {
              // 确保敌人HP显示为0
              enemyHp.value = 0
              if (battleStore.monsters.length > 0) {
                battleStore.monsters[0].current_hp = 0
              }
              // 保存AI战报总结（训练模式）
              if (data.aiReport) {
                aiSummary.value = data.aiReport
              }
              
              // 显示战斗结果
              battleResult.value = '战斗胜利！'
              showResult.value = true
              // 禁用所有按钮，防止重复点击
              isButtonsDisabled.value = true
              // 清除倒计时
              if (countdownTimer.value) {
                clearInterval(countdownTimer.value)
              }
              
              // 战斗胜利后自动领取奖励（仅PVE模式）
              if (battleType !== 'train' && battleStore.battleId && battleStore.levelId) {
                try {
                  console.log('[DEBUG] 普通攻击胜利，自动领取奖励, battleId:', battleStore.battleId, 'levelId:', battleStore.levelId)
                  const rewardResponse = await battleApi.claimReward(battleStore.levelId, battleStore.battleId)
                  if (rewardResponse.code === 200) {
                    console.log('[DEBUG] 奖励领取成功:', rewardResponse.data)
                    battleRewards.value = [
                      { type: 'exp', label: '经验', value: rewardResponse.data.actualExp || 0 },
                      { type: 'gold', label: '金币', value: rewardResponse.data.actualGold || 0 }
                    ]
                    // 如果有升级提示
                    if (rewardResponse.data.levelUp) {
                      alert(`精灵升级到 ${rewardResponse.data.levelUp} 级！`)
                    }
                  } else {
                    console.warn('[DEBUG] 奖励领取失败:', rewardResponse.msg)
                  }
                } catch (error) {
                  console.error('[DEBUG] 领取奖励异常:', error)
                }
              }
              
              // 设置战斗奖励
              battleRewards.value = []
              if (data.rewardGold) {
                battleRewards.value.push({ type: 'gold', label: '金币', value: data.rewardGold })
              }
              if (data.rewardExp) {
                battleRewards.value.push({ type: 'exp', label: '经验', value: data.rewardExp })
              }
              // 添加评分和星级
              if (data.score !== undefined && data.star !== undefined) {
                battleRewards.value.push({ type: 'score', label: '评分', value: data.score })
                battleRewards.value.push({ type: 'star', label: '星级', value: data.star + '星' })
              }
              // 战斗胜利后更新用户信息
              const userStr = localStorage.getItem('user')
              if (userStr) {
                userApi.getUserInfo().then(userInfoResponse => {
                  if (userInfoResponse.code === 200) {
                    // 更新本地存储的用户信息
                    localStorage.setItem('user', JSON.stringify(userInfoResponse.data))
                  }
                })
                // 刷新用户金币数据
                userApi.getUserAsset().then(userAssetResponse => {
                  if (userAssetResponse.code === 200) {
                    // 这里可以更新本地存储的金币数据
                    console.log('用户金币更新:', userAssetResponse.data)
                  }
                })
              }
              
              // 检查是否需要显示御三家选择
              if (data.showStarterSelection) {
                // 加载御三家精灵和用户已拥有的精灵
                loadStarterElves().then(() => {
                  loadOwnedElves().then(() => {
                    showStarterSelection.value = true
                  })
                })
              }
              // 更新battleStore中的状态
              battleStore.status = 1 // 1=胜利
              // 清空战斗状态
              battleStore.clearBattleState()
            } else if (data.status === 2 || data.trainResult === '失败' || data.trainResult === '逃跑') {
              // 确保玩家精灵HP显示为0（如果战败）
              if (data.status === 2 || data.trainResult === '失败') {
                playerElf.value.hp = 0
                if (battleStore.elves.length > 0) {
                  battleStore.elves[0].current_hp = 0
                }
              }
              // 保存AI战报总结（训练模式）
              if (data.aiReport) {
                aiSummary.value = data.aiReport
              }
              
              // 显示战斗结果
              battleResult.value = data.trainResult === '逃跑' ? '你逃跑了' : '战斗失败！'
              showResult.value = true
              // 禁用所有按钮，防止重复点击
              isButtonsDisabled.value = true
              // 清除倒计时
              if (countdownTimer.value) {
                clearInterval(countdownTimer.value)
              }
              // 更新battleStore中的状态
              battleStore.status = 2 // 2=失败
              // 清空战斗状态
              battleStore.clearBattleState()
            }
          }, 1500) // 1.5秒后关闭弹窗并显示结果
        } else {
          // 战斗继续
          // 训练模式：根据needMannequinTurn标志位决定是否需要调用executeMannequinTurn
          if (battleType === 'train' && data.needMannequinTurn) {
            // 玩家先手，需要调用executeMannequinTurn执行训练人偶反击
            console.log('[DEBUG] 玩家先手，自动调用executeMannequinTurn')
            await executeMonsterAction()
          } else {
            // 训练人偶先手或不需要反击，直接启用按钮
            isButtonsDisabled.value = false
          }
        }
      } else {
        console.error('攻击失败:', response.msg)
        // 特别处理MP不足的错误
        if (response.msg.includes('MP不足')) {
          // 确保 battleLogs数组存在且至少有一个回合对象
          if (!battleLogs.value || battleLogs.value.length === 0) {
            battleLogs.value = [{ round: 1, logs: [] }]
          }
          // 确保当前回合对象存在
          if (!battleLogs.value[battleLogs.value.length - 1]) {
            battleLogs.value.push({ round: battleLogs.value.length + 1, logs: [] })
          }
          // 确保logs数组存在
          if (!battleLogs.value[battleLogs.value.length - 1].logs) {
            battleLogs.value[battleLogs.value.length - 1].logs = []
          }
          battleLogs.value[battleLogs.value.length - 1].logs.push('MP不足，无法使用技能')
          alert('MP不足，无法使用技能')
        } else {
          alert('攻击失败: ' + response.msg)
        }
        // 攻击失败时重新启用按钮
        isButtonsDisabled.value = false
      }
  } catch (error) {
    console.error('攻击失败:', error)
    alert('攻击失败，请重试')
    // 攻击异常时重新启用按钮
    isButtonsDisabled.value = false
  } finally {
    // 重置倒计时
    if (!showResult.value) {
      resetCountdown()
    }
    // 注意：不在这里重新启用按钮，因为:
    // 1. 战斗继续时已经在上面启用了
    // 2. 战斗结束时按钮应该保持禁用
    // 3. 只有异常时才需要在catch中启用
  }
}

const useSkill = () => {
  closeCommandPanels()
  showSkills.value = true
}

const useSelectedSkill = async (skill) => {
  try {
    // 检查是否已经显示战斗结果
    if (showResult.value) {
      alert('战斗已经结束，无法使用技能')
      showSkills.value = false
      return
    }
    
    if (playerElf.value.mp < skill.costMp) {
      // 确保battleLogs数组存在且至少有一个回合对象
      if (!battleLogs.value || battleLogs.value.length === 0) {
        battleLogs.value = [{ round: 1, logs: [] }]
      }
      // 确保当前回合对象存在
      if (!battleLogs.value[battleLogs.value.length - 1]) {
        battleLogs.value.push({ round: battleLogs.value.length + 1, logs: [] })
      }
      // 确保logs数组存在
      if (!battleLogs.value[battleLogs.value.length - 1].logs) {
        battleLogs.value[battleLogs.value.length - 1].logs = []
      }
      battleLogs.value[battleLogs.value.length - 1].logs.push('MP不足，无法使用技能')
      alert('MP不足，无法使用技能')
      showSkills.value = false
      return
    }
    
    // 禁用按钮
    isButtonsDisabled.value = true
    
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    const user = JSON.parse(userStr)
    const battleType = route.query.type
    
    // 根据战斗类型调用相应的API
    console.log('使用技能，skill.id:', skill.id)
    let response
    if (battleType === 'train') {
      response = await trainApi.useSkill(skill.id)
    } else {
      response = await battleApi.useSkill(skill.id)
    }
    if (response.code === 200) {
        const data = response.data
        
        // 立即更新MP值，确保UI及时反映MP消耗（训练模式下，如果发生了精灵切换，延迟更新）
        if (data.elfMp !== undefined) {
          // 训练模式：如果elfSwitched为true，说明后端已自动切换，暂不更新UI，等弹窗确认后再更新
          if (battleType === 'train' && data.elfSwitched) {
            console.log('[DEBUG] useSelectedSkill函数-检测到elfSwitched，暂不更新MP，等待弹窗确认')
          } else {
            playerElf.value.mp = data.elfMp
            // 更新battleStore中的状态
            if (battleStore.elves.length > 0) {
              battleStore.elves[0].current_mp = data.elfMp
            }
          }
        } else if (data.userElf?.mp !== undefined) {
          if (battleType === 'train' && data.elfSwitched) {
            console.log('[DEBUG] useSelectedSkill函数-检测到elfSwitched，暂不更新MP(userElf)，等待弹窗确认')
          } else {
            playerElf.value.mp = data.userElf.mp
            // 更新battleStore中的状态
            if (battleStore.elves.length > 0) {
              battleStore.elves[0].current_mp = data.userElf.mp
            }
          }
        }
        
        // 处理精灵切换（需要立即更新）
        if (data.status === 3 || data.trainResult === 'switch') {
          // 精灵战败提示
          alert(`${playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`} 战败！`)
          // 切换到新精灵
          playerElf.value = data.elf || data.userElf || {}
          playerElf.value.elfName = data.elfName || (data.userElf?.elfName || '')
          playerElf.value.elementType = data.elfElementType || (data.userElf?.elementType || 0)
          // 新精灵登场提示
          alert(`${playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`} 登场！`)
          
          // 更新battleStore中的状态
          battleStore.elves = [{
            elf_id: playerElf.value.id,
            current_hp: playerElf.value.hp,
            current_mp: playerElf.value.mp,
            elf_state: 0,
            level: playerElf.value.level,
            maxHp: playerElf.value.maxHp,
            maxMp: playerElf.value.maxMp,
            elfName: playerElf.value.elfName,
            elementType: playerElf.value.elementType
          }]
          
          // 更新新精灵的技能列表
          const elfDetailResponse = await userElfApi.getDetail(playerElf.value.id)
          if (elfDetailResponse.code === 200) {
            const detailData = elfDetailResponse.data
            skills.value = detailData.unlockedSkills || []
          }
        } else {
          // 处理战斗日志
          if (data.roundLogs) {
            // 使用后端返回的按回合组织的战斗日志
            battleLogs.value = data.roundLogs
            // 更新当前回合
            if (data.roundLogs.length > 0) {
              battleStore.currentRound = data.roundLogs[data.roundLogs.length - 1].round
            }
            // 提取敌人的对话
            data.roundLogs.forEach(round => {
              extractEnemyDialog(round.logs)
            })
            
            // 只有当战斗结果不是switch时才触发攻击动画
            if (data.status !== 3 && data.trainResult !== 'switch') {
              const latestRound = data.roundLogs[data.roundLogs.length - 1]
              if (latestRound) {
                triggerAttackAnimations(latestRound.logs, data)
              }
            }
          } else {
            // 兼容旧格式
            // 从战斗日志中提取敌人的对话
            extractEnemyDialog(data.battleLog || data.trainLog)
            
            // 保存当前回合数，避免异步操作导致的问题
            const currentRoundNum = battleStore.currentRound
            // 确保battleLogs数组存在
            if (!battleLogs.value) {
              battleLogs.value = []
            }
            // 确保当前回合对象存在
            if (!battleLogs.value[currentRoundNum - 1]) {
              battleLogs.value.push({ round: currentRoundNum, logs: [] })
            }
            // 确保logs数组存在
            if (!battleLogs.value[currentRoundNum - 1].logs) {
              battleLogs.value[currentRoundNum - 1].logs = []
            }
            const currentRoundLogs = battleLogs.value[currentRoundNum - 1].logs
            const newLogs = (data.battleLog || data.trainLog)?.filter(log => 
              !currentRoundLogs.includes(log) && 
              !isEnemyDialog(log)
            ) || []
            if (newLogs.length > 0) {
              battleLogs.value[currentRoundNum - 1].logs.push(...newLogs)
            }
            
            // 只有当战斗结果不是switch时才触发攻击动画
            if (data.status !== 3 && data.trainResult !== 'switch') {
              triggerAttackAnimations(newLogs, data)
            }
          }
          
          // 更新battleStore中的怪物状态
          if (data.monsterHp !== undefined) {
            if (battleStore.monsters.length > 0) {
              battleStore.monsters[0].current_hp = data.monsterHp
            }
          }
          if (data.monsterMp !== undefined) {
            if (battleStore.monsters.length > 0) {
              battleStore.monsters[0].current_mp = data.monsterMp
            }
          }
          
          // 更新battleStore中的精灵状态
          if (data.playerElfHp !== undefined) {
            if (battleStore.elves.length > 0) {
              battleStore.elves[0].current_hp = data.playerElfHp
            }
          }
          
          // 检查精灵是否死亡（PVE模式和训练模式）
          console.log('[DEBUG] useSelectedSkill函数-检查精灵死亡 - battleType:', battleType, 'playerElfHp:', data.playerElfHp, 'needSwitch:', data.needSwitch, 'nextElfId:', data.nextElfId, 'elfSwitched:', data.elfSwitched)
          
          // 训练模式：检查elfSwitched标记
          if (battleType === 'train' && data.elfSwitched) {
            console.log('[DEBUG] useSelectedSkill函数-检测到elfSwitched标记，显示精灵死亡弹窗')
            const deadElfName = playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`
            
            setTimeout(async () => {
              console.log('[DEBUG] useSelectedSkill函数-显示精灵死亡弹窗')
              showElfDeathSwitchDialog(
                `${deadElfName} 被击败了！\n\n点击下方按钮确认切换到下一只精灵`,
                async () => {
                  console.log('[DEBUG] useSelectedSkill函数-用户确认切换精灵')
                  // 后端已经自动切换了，只需要更新前端显示
                  if (data.playerElf) {
                    playerElf.value.id = data.playerElf.id
                    playerElf.value.elfId = data.playerElf.elfId
                    playerElf.value.elfName = data.elfName
                    playerElf.value.elementType = data.elfElementType
                    playerElf.value.level = data.playerElf.level
                    playerElf.value.maxHp = data.playerElf.maxHp
                    playerElf.value.maxMp = data.playerElf.maxMp
                    playerElf.value.hp = data.playerElfHp !== null && data.playerElfHp !== undefined ? data.playerElfHp : data.playerElf.maxHp
                    playerElf.value.mp = data.elfMp !== null && data.elfMp !== undefined ? data.elfMp : data.playerElf.maxMp
                    playerElf.value.attack = data.playerElf.attack
                    playerElf.value.defense = data.playerElf.defense
                    playerElf.value.speed = data.playerElf.speed
                    
                    if (battleStore.elves.length > 0) {
                      battleStore.elves[0].elf_id = playerElf.value.id
                      battleStore.elves[0].elfId = playerElf.value.elfId
                      battleStore.elves[0].current_hp = playerElf.value.hp
                      battleStore.elves[0].current_mp = playerElf.value.mp
                      battleStore.elves[0].level = playerElf.value.level
                      battleStore.elves[0].maxHp = playerElf.value.maxHp
                      battleStore.elves[0].maxMp = playerElf.value.maxMp
                      battleStore.elves[0].elfName = playerElf.value.elfName
                      battleStore.elves[0].elementType = playerElf.value.elementType
                    }
                    
                    try {
                      const elfDetailResponse = await userElfApi.getDetail(playerElf.value.id)
                      if (elfDetailResponse.code === 200) {
                        skills.value = elfDetailResponse.data.unlockedSkills || []
                      }
                    } catch (error) {
                      console.error('[DEBUG] 获取技能列表失败:', error)
                    }
                  }
                }
              )
            }, 2500)
          }
          // PVE模式：检查needSwitch标记
          else if (battleType !== 'train' && data.playerElfHp !== undefined && data.playerElfHp <= 0) {
            console.log('[DEBUG] useSelectedSkill函数-当前精灵已死亡，HP:', data.playerElfHp)
            
            if (data.needSwitch && data.nextElfId) {
              console.log('[DEBUG] useSelectedSkill函数-需要切换精灵，下一只精灵ID:', data.nextElfId)
              const deadElfName = playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`
              
              setTimeout(async () => {
                console.log('[DEBUG] useSelectedSkill函数-显示精灵死亡弹窗')
                showElfDeathSwitchDialog(
                  `${deadElfName} 被击败了！\n\n点击下方按钮切换下一只精灵继续战斗`,
                  async () => {
                    console.log('[DEBUG] useSelectedSkill函数-用户确认切换精灵')
                    await switchToNextElf(data.nextElfId, data.nextElfHp, data.nextElfMp)
                  }
                )
              }, 2500)
            } else {
              console.log('[DEBUG] useSelectedSkill函数-精灵死亡但不需要切换（可能没有其他可用精灵）')
            }
          }
        }
        
        // 处理战斗结果
        if (data.status === 1 || data.trainResult === '胜利') {
          // 停止BGM
          if (battleBgm.value) {
            battleBgm.value.pause()
            battleBgm.value = null
          }
          
          // 显示胜负已分弹窗
          showBattleEndPopup.value = true
          
          // 播放结束战斗语音
          try {
            const audio = new Audio('/src/assets/audio/battle/结束战斗.mp3')
            audio.play().catch(error => {
              console.error('结束战斗音频播放失败，需要用户交互:', error)
            })
          } catch (audioError) {
            console.error('播放语音失败:', audioError)
          }
          
          // 延迟显示战斗结果
          setTimeout(async () => {
            // 关闭胜负已分弹窗
            showBattleEndPopup.value = false
            
            // 训练模式：从数据库查询AI评分和战报
            if (currentBattleType === 'train') {
              try {
                console.log('[DEBUG] 战斗胜利 - 训练模式，从数据库查询训练记录')
                const recordsResponse = await trainApi.getTrainRecords()
                console.log('[DEBUG] 战斗胜利 - 训练记录响应:', recordsResponse)
                if (recordsResponse.code === 200 && recordsResponse.data.records && recordsResponse.data.records.length > 0) {
                  const latestRecord = recordsResponse.data.records[recordsResponse.data.records.length - 1]
                  console.log('[DEBUG] 战斗胜利 - 最新训练记录:', latestRecord)
                  
                  if (latestRecord.aiReport && !aiSummary.value) {
                    aiSummary.value = latestRecord.aiReport
                    console.log('[DEBUG] 战斗胜利 - 已获取aiReport:', aiSummary.value)
                  }
                  if (latestRecord.aiScore !== null && latestRecord.aiScore !== undefined) {
                    aiScoreValue.value = latestRecord.aiScore
                    console.log('[DEBUG] 战斗胜利 - 已获取aiScore:', aiScoreValue.value)
                  }
                }
              } catch (error) {
                console.error('[DEBUG] 战斗胜利 - 查询训练记录失败:', error)
              }
            }
            
            // 显示战斗结果
            battleResult.value = '战斗胜利！'
            showResult.value = true
            // 禁用所有按钮，防止重复点击
            isButtonsDisabled.value = true
            // 清除倒计时
            if (countdownTimer.value) {
              clearInterval(countdownTimer.value)
            }
            
            // 战斗胜利后自动领取奖励（仅PVE模式）
            if (battleType !== 'train' && battleStore.battleId && battleStore.levelId) {
              try {
                console.log('[DEBUG] 技能胜利，自动领取奖励, battleId:', battleStore.battleId, 'levelId:', battleStore.levelId)
                const rewardResponse = await battleApi.claimReward(battleStore.levelId, battleStore.battleId)
                if (rewardResponse.code === 200) {
                  console.log('[DEBUG] 奖励领取成功:', rewardResponse.data)
                  battleRewards.value = [
                    { type: 'exp', label: '经验', value: rewardResponse.data.actualExp || 0 },
                    { type: 'gold', label: '金币', value: rewardResponse.data.actualGold || 0 }
                  ]
                  
                  // 检查每日收益上限提示
                  const limitMessages = []
                  if (rewardResponse.data.expLimitReached) {
                    limitMessages.push('今日经验收益已达上限（1500/天），超出部分无法获得')
                  }
                  if (rewardResponse.data.goldLimitReached) {
                    limitMessages.push('今日金币收益已达上限（1500/天），超出部分无法获得')
                  }
                  
                  // 如果有升级提示
                  if (rewardResponse.data.levelUp) {
                    alert(`精灵升级到 ${rewardResponse.data.levelUp} 级！`)
                  }
                  
                  // 显示每日上限提示
                  if (limitMessages.length > 0) {
                    alert(limitMessages.join('\n'))
                  }
                } else {
                  console.warn('[DEBUG] 奖励领取失败:', rewardResponse.msg)
                }
              } catch (error) {
                console.error('[DEBUG] 领取奖励异常:', error)
              }
            }
            
            // 设置战斗奖励
            battleRewards.value = []
            if (data.rewardGold) {
              battleRewards.value.push({ type: 'gold', label: '金币', value: data.rewardGold })
            }
            if (data.rewardExp) {
              battleRewards.value.push({ type: 'exp', label: '经验', value: data.rewardExp })
            }
            // 战斗胜利后更新用户信息
            const userStr = localStorage.getItem('user')
            if (userStr) {
              userApi.getUserInfo().then(userInfoResponse => {
                if (userInfoResponse.code === 200) {
                  // 更新本地存储的用户信息
                  localStorage.setItem('user', JSON.stringify(userInfoResponse.data))
                }
              })
            }
            
            // 检查是否需要显示御三家选择
            if (data.showStarterSelection) {
              // 加载御三家精灵和用户已拥有的精灵
              loadStarterElves().then(() => {
                loadOwnedElves().then(() => {
                  showStarterSelection.value = true
                })
              })
            }
            // 更新battleStore中的状态
            battleStore.status = 1 // 1=胜利
            
            // 标记战斗已结束
            battleEnded = true
            console.log('[DEBUG] 战斗胜利，设置 battleEnded = true')
            
            // 清空战斗状态
            battleStore.clearBattleState()
          }, 1500) // 1.5秒后关闭弹窗并显示结果
        } else if (data.status === 2 || data.trainResult === '失败' || data.trainResult === '逃跑') {
          // 保存AI战报总结（训练模式）
          if (data.aiReport) {
            aiSummary.value = data.aiReport
          }
          // 停止BGM
          if (battleBgm.value) {
            battleBgm.value.pause()
            battleBgm.value = null
          }
          
          // 显示胜负已分弹窗
          showBattleEndPopup.value = true
          
          // 播放结束战斗语音
          try {
            const audio = new Audio('/src/assets/audio/battle/结束战斗.mp3')
            audio.play().catch(error => {
              console.error('结束战斗音频播放失败，需要用户交互:', error)
            })
          } catch (audioError) {
            console.error('播放语音失败:', audioError)
          }
          
          // 延迟显示战斗结果
          setTimeout(async () => {
            // 关闭胜负已分弹窗
            showBattleEndPopup.value = false
            
            // 训练模式：从数据库查询AI评分和战报
            if (currentBattleType === 'train') {
              try {
                console.log('[DEBUG] 战斗失败 - 训练模式，从数据库查询训练记录')
                const recordsResponse = await trainApi.getTrainRecords()
                console.log('[DEBUG] 战斗失败 - 训练记录响应:', recordsResponse)
                if (recordsResponse.code === 200 && recordsResponse.data.records && recordsResponse.data.records.length > 0) {
                  const latestRecord = recordsResponse.data.records[recordsResponse.data.records.length - 1]
                  console.log('[DEBUG] 战斗失败 - 最新训练记录:', latestRecord)
                  
                  if (latestRecord.aiReport && !aiSummary.value) {
                    aiSummary.value = latestRecord.aiReport
                    console.log('[DEBUG] 战斗失败 - 已获取aiReport:', aiSummary.value)
                  }
                  if (latestRecord.aiScore !== null && latestRecord.aiScore !== undefined) {
                    aiScoreValue.value = latestRecord.aiScore
                    console.log('[DEBUG] 战斗失败 - 已获取aiScore:', aiScoreValue.value)
                  }
                }
              } catch (error) {
                console.error('[DEBUG] 战斗失败 - 查询训练记录失败:', error)
              }
            }
            
            // 显示战斗结果
            battleResult.value = data.trainResult === '逃跑' ? '你逃跑了' : '战斗失败！'
            showResult.value = true
            // 禁用所有按钮，防止重复点击
            isButtonsDisabled.value = true
            // 清除倒计时
            if (countdownTimer.value) {
              clearInterval(countdownTimer.value)
            }
            // 更新battleStore中的状态
            battleStore.status = 2 // 2=失败
            
            // 标记战斗已结束
            battleEnded = true
            console.log('[DEBUG] 战斗失败/逃跑，设置 battleEnded = true')
            
            // 清空战斗状态
            battleStore.clearBattleState()
          }, 1500) // 1.5秒后关闭弹窗并显示结果
        } else {
          // 战斗继续
          // 训练模式：根据needMannequinTurn标志位决定是否需要调用executeMannequinTurn
          if (battleType === 'train' && data.needMannequinTurn) {
            // 玩家先手，需要调用executeMannequinTurn执行训练人偶反击
            console.log('[DEBUG] 玩家先手，自动调用executeMannequinTurn')
            await executeMonsterAction()
          } else {
            // 训练人偶先手或不需要反击，直接启用按钮
            isButtonsDisabled.value = false
          }
        }
      } else {
        console.error('技能使用失败:', response.msg)
        // 特别处理MP不足的错误
        if (response.msg.includes('MP不足')) {
          // 确保 battleLogs数组存在且至少有一个回合对象
          if (!battleLogs.value || battleLogs.value.length === 0) {
            battleLogs.value = [{ round: 1, logs: [] }]
          }
          // 确保当前回合对象存在
          if (!battleLogs.value[battleLogs.value.length - 1]) {
            battleLogs.value.push({ round: battleLogs.value.length + 1, logs: [] })
          }
          // 确保logs数组存在
          if (!battleLogs.value[battleLogs.value.length - 1].logs) {
            battleLogs.value[battleLogs.value.length - 1].logs = []
          }
          battleLogs.value[battleLogs.value.length - 1].logs.push('MP不足，无法使用技能')
          alert('MP不足，无法使用技能')
        } else {
          alert('技能使用失败: ' + response.msg)
        }
        // 技能使用失败时重新启用按钮
        isButtonsDisabled.value = false
      }
    
    showSkills.value = false
  } catch (error) {
    console.error('使用技能失败:', error)
    // 使用技能异常时重新启用按钮
    isButtonsDisabled.value = false
  } finally {
    // 重置倒计时
    if (!showResult.value) {
      resetCountdown()
    }
  }
}

// 执行怪物行动
const executeMonsterAction = async () => {
  try {
    const battleType = route.query.type
    
    // 根据战斗类型调用相应的API
    let response
    if (battleType === 'train') {
      response = await trainApi.executeMannequinTurn()
    } else {
      response = await battleApi.executeMonsterTurn()
    }
    
    if (response.code === 200) {
      const data = response.data
      console.log('[DEBUG] 怪物行动响应:', data)
      
      // 先更新怪物状态（MP消耗）
      if (data.monsterMp !== undefined) {
        enemyMp.value = data.monsterMp
        if (battleStore.monsters.length > 0) {
          battleStore.monsters[0].current_mp = data.monsterMp
        }
      }
      
      // 先更新玩家状态（HP扣减）- 必须在动画之前更新（训练模式下，如果发生了精灵切换，延迟更新）
      if (data.playerElfHp !== undefined) {
        // 训练模式：如果elfSwitched为true，说明后端已自动切换，暂不更新UI，等弹窗确认后再更新
        if (battleType === 'train' && data.elfSwitched) {
          console.log('[DEBUG] executeMonsterAction-检测到elfSwitched，暂不更新HP，等待弹窗确认')
        } else {
          playerElf.value.hp = data.playerElfHp
          if (battleStore.elves.length > 0) {
            battleStore.elves[0].current_hp = data.playerElfHp
          }
        }
      }
      if (data.playerElfMp !== undefined) {
        // 训练模式：如果elfSwitched为true，说明后端已自动切换，暂不更新UI，等弹窗确认后再更新
        if (battleType === 'train' && data.elfSwitched) {
          console.log('[DEBUG] executeMonsterAction-检测到elfSwitched，暂不更新MP，等待弹窗确认')
        } else {
          playerElf.value.mp = data.playerElfMp
          if (battleStore.elves.length > 0) {
            battleStore.elves[0].current_mp = data.playerElfMp
          }
        }
      }
      
      // 处理战斗日志和动画
      if (data.roundLogs) {
        battleLogs.value = data.roundLogs
        if (data.roundLogs.length > 0) {
          battleStore.currentRound = data.roundLogs[data.roundLogs.length - 1].round
        }
        data.roundLogs.forEach(round => {
          extractEnemyDialog(round.logs)
        })
        
        const latestRound = data.roundLogs[data.roundLogs.length - 1]
        if (latestRound) {
          // 训练模式：触发所有攻击动画（包括训练人偶和玩家）
          if (battleType === 'train') {
            console.log('[DEBUG] 训练人偶行动-触发所有攻击动画:', latestRound.logs)
            // 触发所有动画，保持日志顺序
            triggerAttackAnimations(latestRound.logs, data)
          } else {
            // PVE模式：触发所有动画
            triggerAttackAnimations(latestRound.logs, data)
          }
        }
      }
      
      // 检查精灵是否死亡（在动画播放后弹出切换提示）
      console.log('[DEBUG] 检查精灵死亡 - playerElfHp:', data.playerElfHp, 'needSwitch:', data.needSwitch, 'nextElfId:', data.nextElfId, 'elfSwitched:', data.elfSwitched)
      
      // 训练模式：检查elfSwitched标记
      if (battleType === 'train' && data.elfSwitched) {
        console.log('[DEBUG] 检测到elfSwitched标记，显示精灵死亡弹窗')
        const deadElfName = playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`
        
        setTimeout(async () => {
          console.log('[DEBUG] 显示精灵死亡弹窗')
          showElfDeathSwitchDialog(
            `${deadElfName} 被击败了！\n\n点击下方按钮确认切换到下一只精灵`,
            async () => {
              console.log('[DEBUG] 用户确认切换精灵')
              // 后端已经自动切换了，只需要更新前端显示
              if (data.playerElf) {
                playerElf.value.id = data.playerElf.id
                playerElf.value.elfId = data.playerElf.elfId
                playerElf.value.elfName = data.elfName
                playerElf.value.elementType = data.elfElementType
                playerElf.value.level = data.playerElf.level
                playerElf.value.maxHp = data.playerElf.maxHp
                playerElf.value.maxMp = data.playerElf.maxMp
                playerElf.value.hp = data.playerElfHp !== null && data.playerElfHp !== undefined ? data.playerElfHp : data.playerElf.maxHp
                playerElf.value.mp = data.elfMp !== null && data.elfMp !== undefined ? data.elfMp : data.playerElf.maxMp
                playerElf.value.attack = data.playerElf.attack
                playerElf.value.defense = data.playerElf.defense
                playerElf.value.speed = data.playerElf.speed
                
                if (battleStore.elves.length > 0) {
                  battleStore.elves[0].elf_id = playerElf.value.id
                  battleStore.elves[0].elfId = playerElf.value.elfId
                  battleStore.elves[0].current_hp = playerElf.value.hp
                  battleStore.elves[0].current_mp = playerElf.value.mp
                  battleStore.elves[0].level = playerElf.value.level
                  battleStore.elves[0].maxHp = playerElf.value.maxHp
                  battleStore.elves[0].maxMp = playerElf.value.maxMp
                  battleStore.elves[0].elfName = playerElf.value.elfName
                  battleStore.elves[0].elementType = playerElf.value.elementType
                }
                
                try {
                  const elfDetailResponse = await userElfApi.getDetail(playerElf.value.id)
                  if (elfDetailResponse.code === 200) {
                    skills.value = elfDetailResponse.data.unlockedSkills || []
                  }
                } catch (error) {
                  console.error('[DEBUG] 获取技能列表失败:', error)
                }
              }
            }
          )
        }, 2500)
      }
      // PVE模式：检查needSwitch标记
      else if (battleType !== 'train' && data.playerElfHp !== undefined && data.playerElfHp <= 0) {
        console.log('[DEBUG] 当前精灵已死亡，HP:', data.playerElfHp)
        
        // 检查是否需要切换精灵
        if (data.needSwitch && data.nextElfId) {
          console.log('[DEBUG] 需要切换精灵，下一只精灵ID:', data.nextElfId)
          // 显示精灵死亡弹窗（只有确定按钮）
          const deadElfName = playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`
          
          // 使用setTimeout确保在动画播放完成后显示弹窗
          setTimeout(async () => {
            // 使用自定义弹窗，只有确定按钮
            console.log('[DEBUG] 显示精灵死亡弹窗')
            showElfDeathSwitchDialog(
              `${deadElfName} 被击败了！\n\n点击下方按钮切换下一只精灵继续战斗`,
              async () => {
                // 用户确认切换，自动切换到下一只精灵
                console.log('[DEBUG] 用户确认切换精灵')
                await switchToNextElf(data.nextElfId, data.nextElfHp, data.nextElfMp)
              }
            )
          }, 2500) // 延迟2.5秒，等待受击动画播放完成
        } else {
          console.log('[DEBUG] 精灵死亡但不需要切换（可能没有其他可用精灵）')
        }
      }
      
      // 检查战斗是否结束
      if (data.status === 1 || data.status === 2) {
        // 停止BGM
        if (battleBgm.value) {
          battleBgm.value.pause()
          battleBgm.value = null
        }
        
        // 显示胜负已分弹窗
        showBattleEndPopup.value = true
        
        // 播放结束战斗语音
        try {
          const audio = new Audio('/src/assets/audio/battle/结束战斗.mp3')
          audio.play().catch(error => {
            console.error('结束战斗音频播放失败，需要用户交互:', error)
          })
        } catch (audioError) {
          console.error('播放语音失败:', audioError)
        }
        
        // 1.5秒后自动关闭弹窗，然后显示战斗结果
        setTimeout(async () => {
          console.log('[DEBUG] === 进入战斗结束setTimeout ===')
          console.log('[DEBUG] data.status:', data.status)
          console.log('[DEBUG] showBattleEndPopup.value:', showBattleEndPopup.value)
          
          showBattleEndPopup.value = false
          
          // 保存AI战报总结和评分（训练模式）
          console.log('[DEBUG] 战斗结束数据:', data)
          console.log('[DEBUG] aiReport:', data.aiReport)
          console.log('[DEBUG] aiScore:', data.aiScore)
          console.log('[DEBUG] currentBattleType:', currentBattleType)
          
          // 训练模式：直接从数据库查询最新的训练记录获取AI评分和战报
          if (currentBattleType === 'train') {
            try {
              console.log('[DEBUG] 训练模式 - 从数据库查询训练记录')
              const recordsResponse = await trainApi.getTrainRecords()
              console.log('[DEBUG] 训练记录响应:', recordsResponse)
              if (recordsResponse.code === 200 && recordsResponse.data.records && recordsResponse.data.records.length > 0) {
                // 获取最新的训练记录（数组最后一条）
                const latestRecord = recordsResponse.data.records[recordsResponse.data.records.length - 1]
                console.log('[DEBUG] 最新训练记录:', latestRecord)
                console.log('[DEBUG] latestRecord.aiScore:', latestRecord.aiScore)
                console.log('[DEBUG] latestRecord.aiReport:', latestRecord.aiReport)
                
                if (latestRecord.aiReport) {
                  aiSummary.value = latestRecord.aiReport
                  console.log('[DEBUG] 已从数据库获取aiReport:', aiSummary.value)
                }
                if (latestRecord.aiScore !== null && latestRecord.aiScore !== undefined) {
                  aiScoreValue.value = latestRecord.aiScore
                  console.log('[DEBUG] 已从数据库获取aiScore:', aiScoreValue.value)
                } else {
                  console.warn('[DEBUG] aiScore 为 null 或 undefined')
                }
              } else {
                console.warn('[DEBUG] 未查询到训练记录')
                console.warn('[DEBUG] recordsResponse.data:', recordsResponse.data)
              }
            } catch (error) {
              console.error('[DEBUG] 查询训练记录失败:', error)
            }
          } else {
            console.log('[DEBUG] 关卡模式 - 使用后端返回的数据')
            // 关卡模式：使用后端返回的数据
            if (data.aiReport) {
              aiSummary.value = data.aiReport
            }
            if (data.aiScore !== undefined && data.aiScore !== null) {
              aiScoreValue.value = data.aiScore
              console.log('[DEBUG] 已保存aiScoreValue:', aiScoreValue.value)
            }
          }
          
          if (data.status === 1) {
            battleResult.value = '战斗胜利！'
            
            // 战斗胜利后自动领取奖励（仅PVE模式）
            const battleType = route.query.type
            console.log('[DEBUG] 战斗胜利, battleType:', battleType, 'battleId:', battleStore.battleId, 'levelId:', battleStore.levelId)
            console.log('[DEBUG] 条件判断:', battleType !== 'train', '&&', battleStore.battleId, '&&', battleStore.levelId)
            
            if (battleType !== 'train' && battleStore.battleId && battleStore.levelId) {
              try {
                console.log('[DEBUG] 自动领取战斗奖励, battleId:', battleStore.battleId, 'levelId:', battleStore.levelId)
                const rewardResponse = await battleApi.claimReward(battleStore.levelId, battleStore.battleId)
                if (rewardResponse.code === 200) {
                  console.log('[DEBUG] 奖励领取成功:', rewardResponse.data)
                  battleRewards.value = [
                    { type: 'exp', label: '经验', value: rewardResponse.data.actualExp || 0 },
                    { type: 'gold', label: '金币', value: rewardResponse.data.actualGold || 0 }
                  ]
                  
                  // 检查每日收益上限提示
                  const limitMessages = []
                  if (rewardResponse.data.expLimitReached) {
                    limitMessages.push('今日经验收益已达上限（1500/天），超出部分无法获得')
                  }
                  if (rewardResponse.data.goldLimitReached) {
                    limitMessages.push('今日金币收益已达上限（1500/天），超出部分无法获得')
                  }
                  
                  // 如果有升级提示
                  if (rewardResponse.data.levelUp) {
                    alert(`精灵升级到 ${rewardResponse.data.levelUp} 级！`)
                  }
                  
                  // 显示每日上限提示
                  if (limitMessages.length > 0) {
                    alert(limitMessages.join('\n'))
                  }
                } else {
                  console.warn('[DEBUG] 奖励领取失败:', rewardResponse.msg)
                }
              } catch (error) {
                console.error('[DEBUG] 领取奖励异常:', error)
              }
            }
          } else {
            battleResult.value = '战斗失败！'
          }
          showResult.value = true
          // 禁用所有按钮，防止重复点击
          isButtonsDisabled.value = true
          if (countdownTimer.value) {
            clearInterval(countdownTimer.value)
          }
          battleStore.status = data.status
          battleStore.clearBattleState()
          
          // 标记战斗已结束
          battleEnded = true
          console.log('[DEBUG] 战斗已结束，设置 battleEnded = true')
        }, 1500)
      }
    } else {
      console.error('怪物行动失败:', response.msg)
      alert('怪物行动失败: ' + response.msg)
    }
  } catch (error) {
    console.error('怪物行动失败:', error)
    alert('怪物行动失败，请重试')
  } finally {
    // 重新启用按钮
    isButtonsDisabled.value = false
    // 重置倒计时
    if (!showResult.value) {
      resetCountdown()
    }
  }
}

// 切换到下一只精灵
const switchToNextElf = async (nextElfId, nextElfHp, nextElfMp) => {
  try {
    console.log('[DEBUG] 切换到下一只精灵，ID:', nextElfId, 'HP:', nextElfHp, 'MP:', nextElfMp)
    
    const battleType = route.query.type
    
    // 调用后端switchElf接口，真正完成切换
    let response
    if (battleType === 'train') {
      response = await trainApi.switchElf(nextElfId)
    } else {
      response = await battleApi.switchElf(nextElfId)
    }
    
    if (response.code === 200) {
      const data = response.data
      
      const newElfName = data.elfName || `精灵 ${data.elf?.elfId}`
      
      // 更新playerElf（确保HP和MP不为空）
      playerElf.value.id = data.elf.id
      playerElf.value.elfId = data.elf.elfId
      playerElf.value.elfName = data.elfName
      playerElf.value.elementType = data.elfElementType
      playerElf.value.level = data.elf.level
      playerElf.value.maxHp = data.elf.maxHp
      playerElf.value.maxMp = data.elf.maxMp
      // 优先使用battle_record_elf中的HP/MP，如果为空则使用maxHp/maxMp
      playerElf.value.hp = data.elf.hp !== null && data.elf.hp !== undefined ? data.elf.hp : data.elf.maxHp
      playerElf.value.mp = data.elf.mp !== null && data.elf.mp !== undefined ? data.elf.mp : data.elf.maxMp
      playerElf.value.attack = data.elf.attack
      playerElf.value.defense = data.elf.defense
      playerElf.value.speed = data.elf.speed
      
      console.log('[DEBUG] 新精灵数据:', {
        name: newElfName,
        hp: playerElf.value.hp,
        maxHp: playerElf.value.maxHp,
        mp: playerElf.value.mp,
        maxMp: playerElf.value.maxMp
      })
      
      // 更新battleStore
      if (battleStore.elves.length > 0) {
        battleStore.elves[0].elf_id = playerElf.value.id
        battleStore.elves[0].elfId = playerElf.value.elfId
        battleStore.elves[0].current_hp = playerElf.value.hp
        battleStore.elves[0].current_mp = playerElf.value.mp
        battleStore.elves[0].level = playerElf.value.level
        battleStore.elves[0].maxHp = playerElf.value.maxHp
        battleStore.elves[0].maxMp = playerElf.value.maxMp
        battleStore.elves[0].elfName = playerElf.value.elfName
        battleStore.elves[0].elementType = playerElf.value.elementType
      }
      
      // 等待Vue更新DOM后再执行后续操作
      await nextTick()
      
      console.log('[DEBUG] DOM更新完成，当前显示HP:', playerElf.value.hp)
      
      // 更新技能列表
      const elfDetailResponse = await userElfApi.getDetail(playerElf.value.id)
      if (elfDetailResponse.code === 200) {
        const detailData = elfDetailResponse.data
        skills.value = detailData.unlockedSkills || []
      }
      
      // 添加战斗日志
      if (!battleLogs.value || battleLogs.value.length === 0) {
        battleLogs.value = [{ round: 1, logs: [] }]
      }
      battleLogs.value[battleLogs.value.length - 1].logs.push(`${newElfName} 登场！`)
      
      // 显示新精灵登场弹窗
      setTimeout(() => {
        alert(`${newElfName} 登场！\n\nHP: ${playerElf.value.hp}/${playerElf.value.maxHp}\nMP: ${playerElf.value.mp}/${playerElf.value.maxMp}`)
      }, 100)
    } else {
      console.error('切换精灵失败:', response.msg)
      alert('切换精灵失败: ' + response.msg)
    }
  } catch (error) {
    console.error('切换精灵失败:', error)
    alert('切换精灵失败，请重试')
  }
}

// 加载出战精灵列表
const loadBattleElves = async () => {
  try {
    loadingBattleElves.value = true
    const battleType = route.query.type
    
    let response
    if (battleType === 'train') {
      // 训练模式使用专门的API，从train_record_elf查询实时HP/MP
      response = await trainApi.getBattleElves()
    } else {
      // PVE战斗使用新的API，从battle_record_elf查询
      response = await battleApi.getBattleElves()
    }
    
    if (response.code === 200) {
      battleElves.value = response.data
    }
  } catch (error) {
    console.error('获取出战精灵列表失败:', error)
  } finally {
    loadingBattleElves.value = false
  }
}

// 打开精灵切换弹窗
const switchElf = async () => {
  closeCommandPanels()
  await loadBattleElves()
  showSwitchElf.value = true
}

// 关闭精灵切换弹窗
const closeSwitchElf = () => {
  showSwitchElf.value = false
}

const openPotionsPanel = async () => {
  closeCommandPanels()
  await loadPotions()
  showPotions.value = true
}

// 显示精灵死亡切换弹窗
const showElfDeathSwitchDialog = (message, callback) => {
  elfDeathMessage.value = message
  elfDeathCallback.value = callback
  showElfDeathDialog.value = true
}

// 确认精灵死亡切换
const confirmElfDeath = async () => {
  showElfDeathDialog.value = false
  if (elfDeathCallback.value) {
    await elfDeathCallback.value()
    elfDeathCallback.value = null
  }
}

// 选择要切换的精灵
const selectElfToSwitch = async (elf) => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    const user = JSON.parse(userStr)
    const battleType = route.query.type
    
    // 根据战斗类型调用相应的API
    let response
    if (battleType === 'train') {
      response = await trainApi.switchElf(elf.id)
    } else {
      response = await battleApi.switchElf(elf.id)
    }
    if (response.code === 200) {
      const data = response.data
      
      // 更新playerElf（确保HP和MP不为空）
      playerElf.value.id = data.elf.id
      playerElf.value.elfId = data.elf.elfId
      playerElf.value.elfName = data.elfName
      playerElf.value.elementType = data.elfElementType
      playerElf.value.level = data.elf.level
      playerElf.value.maxHp = data.elf.maxHp
      playerElf.value.maxMp = data.elf.maxMp
      // 优先使用battle_record_elf中的HP/MP，如果为空则使用maxHp/maxMp
      playerElf.value.hp = data.elf.hp !== null && data.elf.hp !== undefined ? data.elf.hp : data.elf.maxHp
      playerElf.value.mp = data.elf.mp !== null && data.elf.mp !== undefined ? data.elf.mp : data.elf.maxMp
      playerElf.value.attack = data.elf.attack
      playerElf.value.defense = data.elf.defense
      playerElf.value.speed = data.elf.speed
      
      // 更新battleStore
      if (battleStore.elves.length > 0) {
        battleStore.elves[0].elf_id = playerElf.value.id
        battleStore.elves[0].elfId = playerElf.value.elfId
        battleStore.elves[0].current_hp = playerElf.value.hp
        battleStore.elves[0].current_mp = playerElf.value.mp
        battleStore.elves[0].level = playerElf.value.level
        battleStore.elves[0].maxHp = playerElf.value.maxHp
        battleStore.elves[0].maxMp = playerElf.value.maxMp
        battleStore.elves[0].elfName = playerElf.value.elfName
        battleStore.elves[0].elementType = playerElf.value.elementType
      }
      
      // 等待Vue更新DOM后再执行后续操作
      await nextTick()
      
      // 更新技能列表
      const elfDetailResponse = await userElfApi.getDetail(playerElf.value.id)
      if (elfDetailResponse.code === 200) {
        const detailData = elfDetailResponse.data
        skills.value = detailData.unlockedSkills || []
      }
      // 添加战斗日志
      if (!battleLogs.value || battleLogs.value.length === 0) {
        battleLogs.value = [{ round: 1, logs: [] }]
      }
      battleLogs.value[battleLogs.value.length - 1].logs.push(`你更换了精灵为 ${playerElf.value.elfName || `精灵 ${playerElf.value.elfId}`}`)
      // 关闭弹窗
      showSwitchElf.value = false
    }
  } catch (error) {
    console.error('切换精灵失败:', error)
  } finally {
    // 重置倒计时
    if (!showResult.value) {
      resetCountdown()
    }
  }
}

const flee = async () => {
  try {
    // 检查是否已经显示战斗结果
    if (showResult.value) {
      alert('战斗已经结束，无法再次逃跑')
      return
    }
    
    // 立即禁用所有按钮，防止重复点击
    isButtonsDisabled.value = true
    
    // 立即显示胜负已分字幕，不等待API返回
    showBattleEndPopup.value = true
    // 播放结束战斗语音
    try {
      const audio = new Audio('/src/assets/audio/battle/结束战斗.mp3')
      audio.play()
    } catch (audioError) {
      console.error('播放语音失败:', audioError)
    }
    
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    const user = JSON.parse(userStr)
    const battleType = route.query.type
    
    // 根据战斗类型调用相应的API
    let response
    if (battleType === 'train') {
      response = await trainApi.flee()
    } else {
      response = await battleApi.flee()
    }
    
    if (response.code === 200) {
      const data = response.data
      // 处理战斗日志
      if (data.roundLogs) {
        // 使用后端返回的按回合组织的战斗日志
        battleLogs.value = data.roundLogs
        // 更新当前回合
        if (data.roundLogs.length > 0) {
          battleStore.currentRound = data.roundLogs[data.roundLogs.length - 1].round
        }
      } else {
        // 兼容旧格式
        battleLogs.value[battleLogs.value.length - 1].logs.push(...(data.battleLog || data.trainLog))
      }
      // 保存AI战报总结（训练模式）
      if (data.aiReport) {
        aiSummary.value = data.aiReport
      }
      // 停止BGM
      if (battleBgm.value) {
        battleBgm.value.pause()
        battleBgm.value = null
      }
      // 1.5秒后自动关闭弹窗，然后显示战斗结果
      setTimeout(async () => {
        showBattleEndPopup.value = false
        
        battleResult.value = '你逃跑了'
        showResult.value = true
        // 禁用所有按钮，防止重复点击
        isButtonsDisabled.value = true
        // 清除倒计时
        if (countdownTimer.value) {
          clearInterval(countdownTimer.value)
        }
        
        // 训练模式：从数据库查询AI评分和战报
        if (currentBattleType === 'train') {
          try {
            console.log('[DEBUG] 逃跑 - 训练模式，从数据库查询训练记录')
            const recordsResponse = await trainApi.getTrainRecords()
            console.log('[DEBUG] 逃跑 - 训练记录响应:', recordsResponse)
            if (recordsResponse.code === 200 && recordsResponse.data.records && recordsResponse.data.records.length > 0) {
              const latestRecord = recordsResponse.data.records[recordsResponse.data.records.length - 1]
              console.log('[DEBUG] 逃跑 - 最新训练记录:', latestRecord)
              
              if (latestRecord.aiReport && !aiSummary.value) {
                aiSummary.value = latestRecord.aiReport
                console.log('[DEBUG] 逃跑 - 已获取aiReport:', aiSummary.value)
              }
              if (latestRecord.aiScore !== null && latestRecord.aiScore !== undefined) {
                aiScoreValue.value = latestRecord.aiScore
                console.log('[DEBUG] 逃跑 - 已获取aiScore:', aiScoreValue.value)
              }
            }
          } catch (error) {
            console.error('[DEBUG] 逃跑 - 查询训练记录失败:', error)
          }
        }
        
        // 标记战斗已结束
        battleEnded = true
        console.log('[DEBUG] 逃跑成功，设置 battleEnded = true')
      }, 1500)
    } else {
      console.error('逃跑失败:', response.msg)
      // 逃跑失败时关闭弹窗并恢复按钮
      showBattleEndPopup.value = false
      isButtonsDisabled.value = false
      alert('逃跑失败: ' + response.msg)
    }
  } catch (error) {
    console.error('逃跑失败:', error)
    // 逃跑异常时关闭弹窗并恢复按钮
    showBattleEndPopup.value = false
    isButtonsDisabled.value = false
    alert('逃跑失败，请重试')
  }
}

const backToPVE = () => {
  const battleType = route.query.type
  if (battleType === 'train') {
    router.push('/train')
  } else {
    router.push('/pve')
  }
}

// 获取AI战报总结
const getAISummary = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    const user = JSON.parse(userStr)
    
    // 检查是否已经有AI战报总结
    if (aiSummary.value) {
      return
    }
    
    console.log('[DEBUG] getAISummary - currentBattleType:', currentBattleType)
    
    // 根据战斗类型调用相应的API
    let response
    if (currentBattleType === 'train') {
      // 训练模式下，直接从数据库查询最新的训练记录获取AI总结
      console.log('[DEBUG] getAISummary - 训练模式，从数据库查询训练记录')
      response = await trainApi.getTrainRecords()
      console.log('[DEBUG] getAISummary - 训练记录响应:', response)
      if (response.code === 200 && response.data.records && response.data.records.length > 0) {
        // 获取最新的训练记录（数组最后一条）
        const latestRecord = response.data.records[response.data.records.length - 1]
        console.log('[DEBUG] getAISummary - 最新训练记录:', latestRecord)
        
        if (latestRecord.aiReport) {
          aiSummary.value = latestRecord.aiReport
          console.log('[DEBUG] getAISummary - 已获取aiReport:', aiSummary.value)
        }
        if (latestRecord.aiScore !== null && latestRecord.aiScore !== undefined) {
          aiScoreValue.value = latestRecord.aiScore
          console.log('[DEBUG] getAISummary - 已获取aiScore:', aiScoreValue.value)
        }
      } else {
        console.warn('[DEBUG] getAISummary - 未查询到训练记录')
        alert('未找到训练记录')
      }
    } else {
      // 关卡模式
      response = await battleApi.getBattleSummary()
      if (response.code === 200) {
        aiSummary.value = response.data.summary
      } else {
        alert('获取战报总结失败: ' + response.msg)
      }
    }
  } catch (error) {
    console.error('获取战报总结失败:', error)
    alert('获取战报总结失败，请重试')
  }
}

// 检测断线重连
const checkReconnect = async () => {
  try {
    const response = await battleApi.reconnect()
    if (response.code === 200 && response.data) {
      // 有未结束的战斗
      battleStore.setPendingBattle(response.data)
    } else if (response.code === 500 && response.msg.includes('战斗已超时')) {
      // 战斗已超时
      alert('战斗超时，判定失败')
      // 清空战斗状态
      battleStore.clearBattleState()
    } else if (response.code === 403) {
      // 权限错误，忽略，继续进入新战斗
      console.warn('重连接口权限错误，继续进入新战斗')
    }
  } catch (error) {
    console.error('检测断线重连失败:', error)
    // 忽略错误，继续进入新战斗
  }
}

// 继续战斗
const continueBattle = async () => {
  try {
    const battleData = battleStore.pendingBattleData
    if (!battleData) return
    
    // 初始化战斗状态
    battleStore.initBattle({
      battleId: battleData.battleId,
      currentRound: battleData.currentRound,
      levelId: battleData.levelId,
      status: battleData.status,
      elves: battleData.elves,
      monsters: battleData.monsters
    })
    
    // 设置本地状态
    if (battleData.elves && battleData.elves.length > 0) {
      const elf = battleData.elves[0]
      console.log('[重连] 精灵数据:', elf)
      
      playerElf.value = {
        id: elf.userElfId || 0,  // 用户精灵ID，用于查询技能
        elfId: elf.elfId || 0,   // 精灵模板ID，用于显示图片
        elfName: elf.elfName || '',
        elementType: elf.elementType || 0,
        hp: elf.currentHp || 0,
        maxHp: elf.maxHp || 0,
        mp: elf.currentMp || 0,
        maxMp: elf.maxMp || 0,
        level: elf.level || 1,
        attack: elf.attack || 0,
        defense: elf.defense || 0,
        speed: elf.speed || 0
      }
      
      console.log('[重连] 设置玩家精灵:', playerElf.value)
      console.log('[重连] 精灵图片路径:', getElfImage(playerElf.value.elfId))
    }
    
    // 设置敌人信息
    enemyName.value = battleData.monsterName || '敌人'
    enemyElementType.value = battleData.monsterElementType || 0
    
    if (battleData.monsters && battleData.monsters.length > 0) {
      const monster = battleData.monsters[0]
      console.log('[重连] 怪物数据:', monster)
      
      enemyHp.value = monster.currentHp || 0
      enemyMaxHp.value = monster.maxHp || 0
      enemyMp.value = monster.currentMp || 0
      enemyMaxMp.value = monster.maxMp || 0
      
      console.log('[重连] 设置敌人血量:', enemyHp.value, '/', enemyMaxHp.value)
      console.log('[重连] 设置敌人蓝量:', enemyMp.value, '/', enemyMaxMp.value)
    }
    
    // 获取精灵技能
    if (playerElf.value.id) {
      const elfDetailResponse = await userElfApi.getDetail(playerElf.value.id)
      if (elfDetailResponse.code === 200) {
        const detailData = elfDetailResponse.data
        skills.value = detailData.unlockedSkills || []
      }
    }
    
    // 加载药品列表
    await loadPotions()
    
    // 显示战斗已恢复提示
    alert('战斗已恢复')
    
    // 开始倒计时
    startCountdown()
    
    // 随机播放BGM
    const bgmFiles = [
      '/src/assets/audio/battle/青鸟.mp3',
      '/src/assets/audio/battle/重燃季.mp3'
    ]
    const randomBgm = bgmFiles[Math.floor(Math.random() * bgmFiles.length)]
    
    try {
      battleBgm.value = new Audio(randomBgm)
      battleBgm.value.loop = true
      battleBgm.value.play().catch(error => {
        console.error('BGM自动播放失败，将在用户交互后播放:', error)
      })
    } catch (audioError) {
      console.error('播放BGM失败:', audioError)
    }
  } catch (error) {
    console.error('恢复战斗失败:', error)
    alert('恢复战斗失败，请重试')
    // 清空战斗状态
    battleStore.clearBattleState()
  }
}

// 放弃战斗
const abandonBattle = async () => {
  try {
    // 调用后端API放弃战斗
    const response = await battleApi.abandonBattle()
    if (response.code === 200) {
      // 清空战斗状态
      battleStore.clearBattleState()
      
      // 标记战斗已结束
      battleEnded = true
      console.log('[DEBUG] 放弃战斗成功，设置 battleEnded = true')
      
      // 跳转到关卡选择页面
      router.push('/pve')
    } else {
      console.error('放弃战斗失败:', response.msg)
      alert('放弃战斗失败: ' + response.msg)
    }
  } catch (error) {
    console.error('放弃战斗失败:', error)
    alert('放弃战斗失败，请重试')
  }
}

onMounted(async () => {
  // 保存战斗类型（在路由query丢失前保存）
  currentBattleType = route.query.type || ''
  console.log('[DEBUG] onMounted - currentBattleType:', currentBattleType)
  
  window.addEventListener('keydown', handleBattlePageKeydown)

  // 创建网络管理器实例并初始化
  networkManager = new NetworkManager()
  networkManager.init()
  
  // 检测是否有断线重连的战斗
  await checkReconnect()
  
  // 如果没有待恢复的战斗，则进入新战斗
  if (!battleStore.hasPendingBattle) {
    await enterBattle()
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleBattlePageKeydown)

  // 清除倒计时
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
    countdownTimer.value = null
  }
  
  // 停止BGM
  if (battleBgm.value) {
    battleBgm.value.pause()
    battleBgm.value = null
  }
  
  // 清理网络管理器
  if (networkManager) {
    networkManager.destroy()
    networkManager = null
  }
  
  // 通知后端保存战斗状态（只有战斗还在进行中时才调用）
  console.log('[DEBUG] onUnmounted - currentBattleType:', currentBattleType, 'battleEnded:', battleEnded)
  if (battleEnded) {
    console.log('[DEBUG] 战斗已结束，不需要调用离线接口')
    return
  }
  
  if (currentBattleType === 'train') {
    // 训练模式：调用训练离线接口
    console.log('[DEBUG] 调用 trainApi.playerOffline()')
    trainApi.playerOffline().catch(error => {
      console.error('通知后端保存训练状态失败:', error)
    })
  } else if (currentBattleType === '') {
    // 关卡模式：调用关卡离线接口（战斗已结束时会失败，忽略错误）
    console.log('[DEBUG] 调用 battleApi.playerOffline()')
    battleApi.playerOffline().catch(error => {
      // 战斗已结束时后端会返回400，这是正常的，忽略即可
      if (error.response && error.response.status === 400) {
        console.log('[DEBUG] 战斗已结束，离线接口返回400（正常）')
      } else {
        console.error('通知后端保存战斗状态失败:', error)
      }
    })
  }
})
</script>

<style scoped>
/* 网络错误遮罩 */
.network-error-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.network-error-content {
  background-color: #fff;
  padding: 2rem;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.network-error-content p {
  font-size: 1.2rem;
  color: #333;
  margin: 0;
}

.home-container {
  min-height: 100vh;
  background: white;
  padding: 20px;
  position: relative;
}

/* 战斗开始弹窗 */
.battle-start-popup {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  pointer-events: none;
}

.popup-content {
  text-align: center;
  pointer-events: none;
}

.start-image {
  max-width: 80%;
  max-height: 80%;
  border-radius: 0;
  box-shadow: none;
  animation: popupFade 1.5s ease-in-out;
  border: none;
  outline: none;
}

@keyframes popupFade {
  0% { opacity: 0; transform: scale(0.8); }
  20% { opacity: 1; transform: scale(1.1); }
  80% { opacity: 1; transform: scale(1); }
  100% { opacity: 0; transform: scale(0.9); }
}

.nav-bar {
  display: flex;
  align-items: center;
  background: rgba(255, 140, 0, 0.9);
  padding: 15px 30px;
  border-radius: 10px;
  margin-bottom: 30px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
  justify-content: center;
  gap: 30px;
}

.nav-logo {
  font-size: 24px;
  font-weight: bold;
  color: white;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
}

/* 导航栏中的倒计时样式 */
.nav-bar .countdown {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  padding: 8px 16px;
  border-radius: 20px;
  border: 2px solid white;
  max-width: 200px;
}

.nav-bar .countdown-text {
  font-size: 14px;
  font-weight: bold;
  color: white;
  margin-right: 8px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
}

.nav-bar .countdown-number {
  font-size: 18px;
  font-weight: bold;
  color: #ffeb3b;
  margin: 0 4px;
  min-width: 24px;
  text-align: center;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
}

.nav-bar .countdown-unit {
  font-size: 14px;
  font-weight: bold;
  color: white;
  margin-left: 4px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
}

.main-content {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

h1 {
  text-align: center;
  margin-bottom: 1rem;
  color: #ff8c00;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

/* 回合显示 */
.round-display {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  padding: 10px 20px;
  background: rgba(255, 140, 0, 0.1);
  border-radius: 20px;
  border: 2px solid #ff8c00;
  max-width: 200px;
  margin-left: auto;
  margin-right: auto;
}

.round-text {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-right: 10px;
}

.round-number {
  font-size: 20px;
  font-weight: bold;
  color: #ff8c00;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

/* 敌人对话区域 */
.enemy-dialog {
  background: #f9f9f9;
  border: 2px solid #ff8c00;
  border-radius: 10px;
  padding: 15px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.dialog-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.enemy-name {
  font-weight: bold;
  color: #f44336;
  font-size: 16px;
}

.dialog-text {
  flex: 1;
  font-size: 16px;
  color: #333;
  line-height: 1.4;
}

/* 战斗场景 HUD化重构 */
.battle-field {
  position: relative;
  width: 100%;
  min-height: 50vh;
  margin-bottom: 20px;
  background: transparent;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* HUD面板定位 */
.hud {
  position: absolute;
  top: 10px;
  width: 320px;
  z-index: 10;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  border-radius: 12px;
  background: rgba(20, 20, 20, 0.85);
  border: 2px solid #555;
  color: white;
  padding: 10px 15px;
  backdrop-filter: blur(5px);
}
.player-hud {
  left: 10px;
  border-color: #4caf50;
  border-left-width: 6px;
}
.enemy-hud {
  right: 10px;
  border-color: #f44336;
  border-right-width: 6px;
}
.empty-hud {
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-color: #9e9e9e;
  height: 80px;
}

/* 内部面板样式 */
.elf-info-box .info-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 5px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  padding-bottom: 5px;
}
.elf-info-box h4 {
  margin: 0;
  font-size: 1.2rem;
  color: #fff;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.8);
}
.elf-info-box .level {
  font-weight: bold;
  font-size: 0.9rem;
  color: #ffca28;
}
.type-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 0.8rem;
  border-radius: 12px;
  background: rgba(255,255,255,0.1);
  margin-bottom: 10px;
}

/* 状态条 */
.bars .bar-container {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 0.85rem;
}
.bar-bg {
  flex: 1;
  height: 12px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 6px;
  overflow: hidden;
  margin-right: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.5);
}
.bar-fill {
  height: 100%;
  transition: width 0.3s ease-out;
}
.hp-fill {
  background: linear-gradient(90deg, #e53935, #ef5350);
}
.mp-fill {
  background: linear-gradient(90deg, #1e88e5, #42a5f5);
}
.bar-text {
  min-width: 60px;
  text-align: right;
  font-family: monospace;
  font-weight: bold;
}

/* 战斗主舞台 */
.battle-stage {
  flex: 1;
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  padding-bottom: 40px;
  margin-top: 100px; /* Leave space for HUD */
  position: relative;
}

/* 精灵立绘去除圆形束缚 */
.sprite-wrapper {
  position: relative;
  text-align: center;
  transition: transform 0.3s ease;
  z-index: 5;
}
.sprite-img {
  max-width: 200px;
  max-height: 250px;
  object-fit: contain;
  filter: drop-shadow(0 15px 15px rgba(0,0,0,0.4));
  transition: filter 0.3s ease;
}
/* 人偶大小稍微调小一点 */
.mannequin-img {
  max-width: 150px;
  max-height: 180px;
}

/* 技能名称浮动 */
.skill-name-float {
  position: absolute;
  top: -40px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #ff9800, #ff5722);
  color: white;
  padding: 5px 15px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 1.1rem;
  box-shadow: 0 4px 10px rgba(255, 87, 34, 0.5);
  animation: floatUpFade 1.2s forwards;
  pointer-events: none;
  white-space: nowrap;
}

@keyframes floatUpFade {
  0% { opacity: 0; top: -10px; transform: translateX(-50%) scale(0.8); }
  20% { opacity: 1; top: -40px; transform: translateX(-50%) scale(1.1); }
  80% { opacity: 1; top: -60px; transform: translateX(-50%) scale(1); }
  100% { opacity: 0; top: -80px; transform: translateX(-50%) scale(0.9); }
}

/* 粒子特出容器(例如火球) */
.particle-track {
  position: absolute;
  bottom: 80px;
  left: 15%;
  right: 15%;
  height: 50px;
  pointer-events: none;
  z-index: 6;
}

.particle {
  position: absolute;
  top: 0;
  width: 40px;
  height: 40px;
  background: radial-gradient(circle, #fff 10%, #ffeb3b 40%, #ff5722 80%);
  border-radius: 50%;
  box-shadow: 0 0 20px 10px rgba(255, 87, 34, 0.6);
  opacity: 0;
}

.particle.move-right {
  animation: shootRight 0.8s ease-in forwards;
}

.particle.move-left {
  right: 0;
  animation: shootLeft 0.8s ease-in forwards;
}

@keyframes shootRight {
  0% { left: 0%; opacity: 0; transform: scale(0.5); }
  10% { left: 10%; opacity: 1; transform: scale(1); }
  90% { left: 90%; opacity: 1; transform: scale(1.2); }
  100% { left: 100%; opacity: 0; transform: scale(2); }
}

@keyframes shootLeft {
  0% { right: 0%; opacity: 0; transform: scale(0.5); }
  10% { right: 10%; opacity: 1; transform: scale(1); }
  90% { right: 90%; opacity: 1; transform: scale(1.2); }
  100% { right: 100%; opacity: 0; transform: scale(2); }
}

/* 攻击动画红白闪烁 (Flash Red/White) */
.sprite-wrapper.attacking .sprite-img {
  animation: attackFlash 1s ease-in-out;
}

@keyframes attackFlash {
  0% { filter: drop-shadow(0 15px 15px rgba(0,0,0,0.4)) brightness(1); }
  20% { filter: drop-shadow(0 0 20px rgba(255, 255, 255, 0.8)) brightness(1.5); }
  40% { filter: drop-shadow(0 0 20px rgba(255, 0, 0, 0.8)) brightness(0.8) sepia(1) hue-rotate(-50deg) saturate(5); }
  60% { filter: drop-shadow(0 0 20px rgba(255, 255, 255, 0.8)) brightness(1.5); }
  80% { filter: drop-shadow(0 0 20px rgba(255, 0, 0, 0.8)) brightness(0.8) sepia(1) hue-rotate(-50deg) saturate(5); }
  100% { filter: drop-shadow(0 15px 15px rgba(0,0,0,0.4)) brightness(1); }
}

/* 受击击退动画 (Knockback Effect) */
.sprite-wrapper.hurt {
  animation: knockbackEffect 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}

@keyframes knockbackEffect {
  0% { transform: translateX(0); filter: brightness(1); }
  20% { transform: translateX(-30px) rotate(-5deg); filter: brightness(2) contrast(2); } /* Assuming player hit, we will use general shake instead since we don't know side */
  40% { transform: translateX(20px) rotate(3deg); filter: brightness(0.5) sepia(1) hue-rotate(-50deg) saturate(5); }
  60% { transform: translateX(-10px) rotate(-2deg); }
  80% { transform: translateX(5px) rotate(1deg); }
  100% { transform: translateX(0); filter: brightness(1); }
}

.hp-bar {
  width: 100%;
  height: 10px;
  background: var(--color-neutral-300);
  border-radius: 5px;
  overflow: hidden;
  margin: 10px 0;
}

.hp-fill {
  height: 100%;
  background: var(--color-success);
  transition: width 0.5s ease;
}

.mp-bar {
  width: 100%;
  height: 10px;
  background: var(--color-neutral-300);
  border-radius: 5px;
  overflow: hidden;
  margin: 10px 0;
}

.mp-fill {
  height: 100%;
  background: var(--color-stat-mp);
  transition: width 0.5s ease;
}

/* 战斗操作 */
.battle-actions {
  position: sticky;
  bottom: 0;
  width: 100%;
  margin-top: auto;
  padding: 15px 20px;
  background: rgba(20, 20, 20, 0.9);
  border-radius: 15px 15px 0 0;
  border-top: 3px solid #ff8c00;
  box-shadow: 0 -5px 15px rgba(0,0,0,0.3);
  z-index: 100;
}

.battle-actions h3 {
  color: #fff;
  margin: 0 0 10px 0;
  font-size: 1.1rem;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  justify-content: center;
}

.action-btn {
  flex: 1;
  min-width: 120px;
  padding: 12px 20px;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 30px;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.attack-btn {
  background: linear-gradient(135deg, #d32f2f, #f44336);
  color: white;
  box-shadow: 0 4px 10px rgba(244, 67, 54, 0.4);
}

.skill-btn {
  background: linear-gradient(135deg, #1976d2, #2196f3);
  color: white;
  box-shadow: 0 4px 10px rgba(33, 150, 243, 0.4);
}

.switch-btn {
  background: linear-gradient(135deg, #fbc02d, #ffeb3b);
  color: #333;
  box-shadow: 0 4px 10px rgba(255, 235, 59, 0.4);
}

.flee-btn {
  background: linear-gradient(135deg, #757575, #9e9e9e);
  color: white;
  box-shadow: 0 4px 10px rgba(158, 158, 158, 0.4);
}

.potion-btn {
  background: linear-gradient(135deg, #388e3c, #4caf50);
  color: white;
  box-shadow: 0 4px 10px rgba(76, 175, 80, 0.4);
}

.action-btn:hover:not(:disabled) {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.5);
  filter: brightness(1.1);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(1);
}

.skills-section {
  margin-top: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.skills-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 10px;
}

.skill-btn {
  padding: 10px;
  background: #e3f2fd;
  border: 1px solid #2196f3;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.skill-btn:hover {
  background: #bbdefb;
}

/* 技能元素类型颜色 */
.skill-btn.element-fire {
  background: var(--color-fire-bg);
  border-color: var(--color-fire);
  color: var(--color-fire-text);
}

.skill-btn.element-fire:hover {
  background: oklch(0.85 0.08 30);
}

.skill-btn.element-water {
  background: var(--color-water-bg);
  border-color: var(--color-water);
  color: var(--color-water-text);
}

.skill-btn.element-water:hover {
  background: oklch(0.85 0.06 240);
}

.skill-btn.element-grass {
  background: var(--color-grass-bg);
  border-color: var(--color-grass);
  color: var(--color-grass-text);
}

.skill-btn.element-grass:hover {
  background: oklch(0.85 0.06 150);
}

/* 战斗日志 */
.battle-log {
  margin-bottom: 30px;
  padding: 20px;
  background: rgba(245, 245, 245, 0.8);
  border-radius: 10px;
}

.log-content {
  max-height: 200px;
  overflow-y: auto;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
}

.log-entry {
  margin: 5px 0;
  font-size: 0.9rem;
  color: #333;
  margin-left: 20px;
}

.round-section {
  margin-bottom: 15px;
  border-left: 3px solid #ff8c00;
  padding-left: 10px;
}

.round-header {
  font-weight: bold;
  color: #ff8c00;
  font-size: 1rem;
  margin-bottom: 5px;
  padding: 5px 0;
}

/* 战斗结果 */
.battle-result {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  border-radius: 15px;
  padding: 40px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  z-index: 1000;
}

.battle-result h2 {
  color: #ff8c00;
  margin-bottom: 20px;
}

.result-btn, .summary-btn {
  padding: 10px 30px;
  border: none;
  border-radius: 25px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin: 0 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.result-btn {
  background: #4caf50;
  color: white;
  box-shadow: 0 4px 8px rgba(76, 175, 80, 0.3);
}

.result-btn:hover {
  background: #43a047;
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(76, 175, 80, 0.4);
}

.summary-btn {
  background: #2196f3;
  color: white;
  box-shadow: 0 4px 8px rgba(33, 150, 243, 0.3);
}

.summary-btn:hover {
  background: #1976d2;
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(33, 150, 243, 0.4);
}

.battle-result button {
  display: inline-block;
  margin: 10px 5px;
}

/* AI战报总结 */
.ai-summary {
  margin: 20px 0;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: left;
}

.ai-summary h3 {
  color: #ff8c00;
  margin-bottom: 10px;
  text-align: center;
}

/* AI评分样式 */
.ai-score {
  margin: 15px 0;
  padding: 12px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.score-label {
  font-size: 16px;
  color: #fff;
  font-weight: bold;
  margin-right: 8px;
}

.score-value {
  font-size: 24px;
  color: #ffd700;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.summary-content {
  line-height: 1.8;
  color: #333;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 10px;
}

.summary-content::-webkit-scrollbar {
  width: 6px;
}

.summary-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.summary-content::-webkit-scrollbar-thumb {
  background: #ff8c00;
  border-radius: 3px;
}

.summary-content::-webkit-scrollbar-thumb:hover {
  background: #ff6600;
}

.summary-content br {
  margin-bottom: 15px;
}

/* 御三家选择弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.modal-content {
  background: white;
  border-radius: 15px;
  padding: 30px;
  max-width: 800px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(255, 140, 0, 0.4);
  text-align: center;
}

/* 精灵死亡切换弹窗样式 */
.elf-death-dialog {
  max-width: 500px;
  padding: 40px;
}

.elf-death-message {
  font-size: 18px;
  line-height: 1.8;
  color: #333;
  margin: 20px 0 30px 0;
  white-space: pre-line;
}

.elf-death-dialog .confirm-btn {
  background: linear-gradient(135deg, var(--color-brand) 0%, var(--color-brand-light) 100%);
  color: white;
  border: none;
  padding: 12px 40px;
  border-radius: 25px;
  cursor: pointer;
  font-size: 16px;
  font-weight: bold;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px oklch(0.65 0.18 50 / 0.4);
}

.elf-death-dialog .confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px oklch(0.65 0.18 50 / 0.6);
}

.potion-list {
  margin: 20px 0;
  text-align: left;
}

.potion-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.potion-item:hover {
  background: #f5f5f5;
  transform: translateY(-2px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.potion-image {
  width: 60px;
  height: 60px;
  margin-right: 15px;
}

.potion-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.potion-info {
  flex: 1;
}

.potion-info h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #333;
}

.potion-info p {
  margin: 5px 0;
  font-size: 14px;
  color: #666;
}

.potion-count {
  font-weight: bold;
  color: #4caf50;
}

.no-items {
  margin: 20px 0;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 8px;
  text-align: center;
}

.battle-elves-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin: 1rem 0;
}

.elf-card.horizontal {
  display: flex;
  align-items: center;
  padding: 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  background: #f9f9f9;
  transition: all 0.3s ease;
  height: auto;
  min-height: 80px;
}

.elf-card.horizontal:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.elf-card.horizontal.current {
  border-color: #4CAF50;
  background: rgba(76, 175, 80, 0.1);
}

.elf-card.horizontal .elf-image {
  width: 60px;
  height: 60px;
  margin-right: 1rem;
}

.elf-card.horizontal .elf-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.elf-card.horizontal .elf-info {
  flex: 1;
  text-align: left;
}

.elf-card.horizontal .elf-info h4 {
  margin: 0 0 0.25rem 0;
  font-size: 1rem;
}

.elf-card.horizontal .elf-stats {
  display: flex;
  gap: 1rem;
  margin: 0.25rem 0;
  flex-wrap: wrap;
}

.elf-card.horizontal .elf-stats p {
  margin: 0;
  font-size: 0.8rem;
  white-space: nowrap;
}

.elf-card.horizontal .card-actions {
  margin-top: 0.25rem;
}

.elf-card.horizontal .select-btn {
  background: #4CAF50;
  color: white;
  border: none;
  padding: 0.3rem 0.8rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

.elf-card.horizontal .select-btn:hover {
  background: #45a049;
}

.elf-card.horizontal .select-btn.disabled {
  background: #9e9e9e;
  cursor: not-allowed;
}

.elf-card.horizontal .select-btn.disabled:hover {
  background: #9e9e9e;
  transform: none;
  box-shadow: none;
}

.elf-card.horizontal .current-tag {
  display: inline-block;
  background: #4CAF50;
  color: white;
  padding: 0.2rem 0.6rem;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: bold;
  margin-top: 0.25rem;
}

.elf-card.horizontal .hp-bar {
  width: 100%;
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
  margin: 0.25rem 0;
}

.elf-card.horizontal .hp-fill {
  height: 100%;
  background: #4CAF50;
  transition: width 0.3s ease;
}

.modal-content h2 {
  color: #ff8c00;
  margin-bottom: 20px;
  font-size: 1.8rem;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.modal-content p {
  color: #666;
  margin-bottom: 30px;
  font-size: 1.1rem;
}

.starter-elves {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 30px;
}

.starter-elf {
  background: white;
  padding: 20px;
  border-radius: 15px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  text-align: center;
  min-width: 200px;
  transition: all 0.3s ease;
  border: 2px solid rgba(255, 140, 0, 0.2);
  position: relative;
  overflow: hidden;
}

.starter-elf:hover {
  transform: translateY(-10px);
  box-shadow: 0 10px 25px rgba(255, 140, 0, 0.3);
  border-color: #ff8c00;
}

.starter-elf.owned {
  opacity: 0.7;
  border-color: #9e9e9e;
}

.starter-elf .elf-image {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 15px;
  border: 3px solid #ff8c00;
  box-shadow: 0 4px 15px rgba(255, 140, 0, 0.4);
  transition: all 0.3s ease;
}

.starter-elf:hover .elf-image {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(255, 140, 0, 0.6);
}

.starter-elf h4 {
  color: #333;
  margin-bottom: 10px;
  font-size: 1.2rem;
}

.starter-elf p {
  color: #666;
  margin: 5px 0;
  font-size: 0.9rem;
}

.owned-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #9e9e9e;
  color: white;
  padding: 5px 10px;
  border-radius: 15px;
  font-size: 0.8rem;
  font-weight: bold;
}

.select-btn {
  margin-top: 15px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #ff8c00 0%, #ffb74d 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(255, 140, 0, 0.4);
}

.select-btn:hover {
  background: linear-gradient(135deg, #e67e00 0%, #ff9800 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 140, 0, 0.6);
}

.modal-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}

.cancel-btn {
  padding: 10px 30px;
  background: #9e9e9e;
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  background: #757575;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(158, 158, 158, 0.4);
}

.loading {
  text-align: center;
  padding: 30px;
  color: #666;
  font-size: 1.2rem;
}

/* 攻击动画 */
.elf-card.attacking .elf-image {
  animation: attackAnimation 1s ease-in-out;
}

/* 受伤效果 */
.elf-card.hurt {
  animation: hurtAnimation 0.5s ease-in-out;
}

.elf-card.hurt .elf-image {
  animation: hurtShake 0.5s ease-in-out;
}

@keyframes attackAnimation {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

@keyframes hurtAnimation {
  0% { background-color: rgba(255, 0, 0, 0); }
  25% { background-color: rgba(255, 0, 0, 0.5); }
  50% { background-color: rgba(255, 0, 0, 0.3); }
  75% { background-color: rgba(255, 0, 0, 0.5); }
  100% { background-color: rgba(255, 0, 0, 0); }
}

@keyframes hurtShake {
  0% { transform: translateX(0); }
  20% { transform: translateX(-5px); }
  40% { transform: translateX(5px); }
  60% { transform: translateX(-5px); }
  80% { transform: translateX(5px); }
  100% { transform: translateX(0); }
}

/* Game-style battle scene overrides */
.home-container {
  min-height: 100vh;
  padding: 0 20px 20px;
  overflow-x: hidden;
  background:
    radial-gradient(circle at top, rgba(255, 153, 51, 0.14), transparent 28%),
    linear-gradient(180deg, #05070d 0%, #09111f 32%, #101a29 100%);
  color: #f8f2e9;
}

.battle-container {
  max-width: 1400px;
  margin: 10px auto 0;
  min-height: calc(100vh - 108px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 10px;
}

.battle-status-strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 42px;
  width: 100%;
}

.battle-status-side {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.battle-status-side--left {
  justify-content: flex-start;
}

.battle-status-side--right {
  justify-content: flex-end;
}

.battle-status-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 196, 112, 0.16);
  background: rgba(8, 12, 22, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
  color: rgba(255, 238, 214, 0.84);
}

.battle-status-chip--stage {
  color: rgba(255, 220, 162, 0.9);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.battle-status-chip--countdown {
  gap: 8px;
}

.battle-status-chip__label {
  color: rgba(255, 222, 184, 0.72);
  letter-spacing: 0.16em;
  font-size: 0.74rem;
  font-weight: 700;
}

.battle-status-chip__value {
  color: #ffd07b;
  font-size: 1.2rem;
  font-weight: 800;
  text-shadow: 0 0 18px rgba(255, 191, 80, 0.3);
}

.battle-status-chip__unit {
  color: rgba(255, 231, 196, 0.72);
  font-size: 0.76rem;
  font-weight: 700;
}

.round-display {
  margin: 0;
  justify-content: flex-start;
  padding: 0;
  max-width: none;
  border: none;
  background: none;
}

.round-text {
  margin-right: 12px;
  color: rgba(255, 222, 184, 0.72);
  letter-spacing: 0.2em;
  font-size: 0.82rem;
}

.round-number {
  color: #ffd07b;
  font-size: 2rem;
  text-shadow: 0 0 20px rgba(255, 191, 80, 0.4);
}

.enemy-dialog {
  max-width: min(760px, 100%);
  margin: 0;
  padding: 10px 14px;
  border: 1px solid rgba(255, 181, 77, 0.22);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(12, 15, 27, 0.88), rgba(38, 14, 18, 0.78));
  box-shadow: 0 12px 24px rgba(5, 7, 13, 0.26);
  animation: dialogReveal 260ms ease-out;
}

.dialog-content {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.enemy-name {
  color: #ffb299;
}

.dialog-text {
  color: rgba(255, 244, 224, 0.84);
}

.battle-shell {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 12px;
  min-height: 0;
}

.battle-field {
  position: relative;
  height: clamp(380px, calc(100vh - 300px), 560px);
  min-height: 380px;
  border: 1px solid rgba(255, 194, 107, 0.16);
  border-radius: 32px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 196, 127, 0.06), transparent 28%),
    linear-gradient(180deg, rgba(8, 12, 22, 0.98) 0%, rgba(13, 21, 36, 0.96) 100%);
  box-shadow:
    0 24px 50px rgba(4, 8, 15, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.battle-field.arena-fire {
  background:
    radial-gradient(circle at 50% 18%, rgba(255, 133, 61, 0.28), transparent 28%),
    linear-gradient(180deg, rgba(255, 120, 34, 0.08), rgba(20, 16, 19, 0.96));
}

.battle-field.arena-water {
  background:
    radial-gradient(circle at 50% 18%, rgba(89, 182, 255, 0.28), transparent 28%),
    linear-gradient(180deg, rgba(64, 148, 255, 0.08), rgba(11, 18, 34, 0.96));
}

.battle-field.arena-grass {
  background:
    radial-gradient(circle at 50% 18%, rgba(96, 212, 128, 0.24), transparent 28%),
    linear-gradient(180deg, rgba(76, 175, 80, 0.08), rgba(12, 21, 20, 0.96));
}

.battle-field__sky,
.battle-field__mist,
.battle-field__spotlight {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.battle-field__sky {
  background:
    radial-gradient(circle at 20% 18%, rgba(255, 255, 255, 0.08), transparent 20%),
    radial-gradient(circle at 78% 14%, rgba(255, 255, 255, 0.08), transparent 18%);
}

.battle-field__mist {
  inset: auto 5% 12%;
  height: 180px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.12), transparent 70%);
  filter: blur(24px);
  opacity: 0.55;
}

.battle-field__spotlight {
  width: 38%;
  height: 72%;
  top: auto;
  bottom: 14%;
  filter: blur(8px);
  opacity: 0.38;
}

.battle-field__spotlight--left {
  left: 2%;
  background: radial-gradient(circle at 50% 50%, rgba(88, 160, 255, 0.26), transparent 65%);
}

.battle-field__spotlight--right {
  right: 2%;
  background: radial-gradient(circle at 50% 50%, rgba(255, 115, 77, 0.26), transparent 65%);
}

.hud {
  top: 10px;
  width: min(270px, calc(50% - 20px));
  padding: 10px 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 210, 145, 0.16);
  background: linear-gradient(180deg, rgba(18, 22, 34, 0.9), rgba(12, 15, 24, 0.84));
  box-shadow: 0 10px 18px rgba(4, 8, 14, 0.24);
}

.player-hud {
  left: 10px;
  border-left: 3px solid rgba(99, 202, 122, 0.92);
}

.enemy-hud {
  right: 10px;
  border-right: 3px solid rgba(255, 118, 82, 0.92);
}

.hud-caption {
  display: block;
  margin-bottom: 1px;
  color: rgba(255, 225, 179, 0.68);
  font-size: 0.64rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.elf-info-box .info-header {
  align-items: flex-start;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.elf-info-box h4 {
  font-size: 0.96rem;
}

.elf-info-box .level {
  font-size: 0.78rem;
}

.type-badge {
  margin-top: 6px;
  padding: 4px 8px;
  font-size: 0.72rem;
  background: rgba(255, 255, 255, 0.08);
  color: #ffeccc;
}

.bars {
  display: grid;
  gap: 6px;
  margin-top: 8px;
}

.bars .bar-container {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 0;
}

.bar-label {
  color: rgba(255, 237, 207, 0.72);
  font-size: 0.66rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.bar-bg {
  margin-right: 0;
  height: 9px;
  border-radius: 999px;
  border-color: rgba(255, 255, 255, 0.08);
}

.bar-fill {
  border-radius: 999px;
}

.hp-fill {
  background: linear-gradient(90deg, #f44336, #ff8a65);
}

.mp-fill {
  background: linear-gradient(90deg, #4d8cff, #7cc9ff);
}

.bar-text {
  min-width: 64px;
  color: rgba(255, 244, 224, 0.88);
  font-size: 0.7rem;
}

.battle-stage {
  margin-top: 0;
  position: relative;
  height: 100%;
  align-items: flex-end;
  justify-content: center;
  gap: clamp(56px, 15vw, 180px);
  min-height: 0;
  padding: 124px 6% 96px;
}

.battle-stage__ground {
  position: absolute;
  left: 50%;
  bottom: 28px;
  width: min(72%, 860px);
  height: 110px;
  transform: translateX(-50%);
  border-radius: 50%;
  background:
    radial-gradient(circle at center, rgba(255, 234, 193, 0.22), rgba(77, 59, 47, 0.1) 38%, rgba(5, 7, 13, 0) 72%);
  box-shadow: 0 0 40px rgba(255, 176, 78, 0.12);
}

.battle-stage__label {
  position: absolute;
  left: 50%;
  bottom: 22px;
  transform: translateX(-50%);
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(12, 15, 24, 0.7);
  border: 1px solid rgba(255, 205, 125, 0.18);
  color: rgba(255, 237, 207, 0.8);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.sprite-wrapper {
  z-index: 2;
}

.player-sprite,
.enemy-sprite {
  min-width: 220px;
}

.player-sprite {
  transform-origin: center bottom;
}

.enemy-sprite {
  transform-origin: center bottom;
}

.sprite-aura {
  position: absolute;
  inset: 12% auto auto 50%;
  width: 70%;
  height: 70%;
  transform: translateX(-50%);
  border-radius: 50%;
  filter: blur(22px);
  opacity: 0.55;
  pointer-events: none;
}

.sprite-aura--player {
  background: radial-gradient(circle, rgba(107, 173, 255, 0.36), rgba(107, 173, 255, 0));
}

.sprite-aura--enemy {
  background: radial-gradient(circle, rgba(255, 125, 89, 0.34), rgba(255, 125, 89, 0));
}

.sprite-img {
  position: relative;
  max-width: clamp(180px, 20vw, 260px);
  max-height: clamp(230px, 32vh, 320px);
  filter: drop-shadow(0 22px 24px rgba(0, 0, 0, 0.42));
}

.enemy-sprite .sprite-img {
  transform: scaleX(-1);
}

.enemy-sprite.is-mannequin .sprite-img {
  max-width: clamp(145px, 13vw, 190px);
  max-height: clamp(180px, 24vh, 230px);
}

.skill-name-float {
  top: -24px;
  background: linear-gradient(135deg, rgba(255, 232, 180, 0.94), rgba(255, 131, 48, 0.98));
  color: #3c1600;
  box-shadow: 0 16px 30px rgba(255, 136, 56, 0.24);
}

.particle-track {
  left: 16%;
  right: 16%;
  bottom: 154px;
  height: 84px;
  z-index: 5;
}

.particle-core,
.particle {
  position: absolute;
  top: 50%;
  border-radius: 999px;
  transform: translateY(-50%);
}

.particle-core {
  width: 52px;
  height: 52px;
  left: 0;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.98), rgba(255, 210, 120, 0.96) 35%, rgba(255, 118, 44, 0.7) 70%, rgba(255, 118, 44, 0));
  box-shadow: 0 0 28px rgba(255, 148, 51, 0.65);
}

.particle {
  width: 22px;
  height: 8px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.9), rgba(255, 190, 120, 0));
  opacity: 0.8;
}

.particle-track.move-right .particle-core {
  animation: projectileMoveRight 0.82s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.particle-track.move-left .particle-core {
  animation: projectileMoveLeft 0.82s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.particle-track.move-right .particle {
  animation: projectileSparkRight 0.82s ease-out forwards;
}

.particle-track.move-left .particle {
  animation: projectileSparkLeft 0.82s ease-out forwards;
}

.particle-1 { animation-delay: 0.02s; top: 20%; }
.particle-2 { animation-delay: 0.08s; top: 34%; }
.particle-3 { animation-delay: 0.14s; top: 50%; }
.particle-4 { animation-delay: 0.2s; top: 62%; }
.particle-5 { animation-delay: 0.26s; top: 76%; }
.particle-6 { animation-delay: 0.32s; top: 44%; }

.particle-track.projectile-fire .particle-core {
  background: radial-gradient(circle, rgba(255, 255, 255, 0.98), rgba(255, 208, 102, 0.95) 30%, rgba(255, 94, 58, 0.76) 72%, rgba(255, 94, 58, 0));
  box-shadow: 0 0 30px rgba(255, 112, 70, 0.7);
}

.particle-track.projectile-water .particle-core {
  background: radial-gradient(circle, rgba(255, 255, 255, 0.98), rgba(167, 232, 255, 0.95) 30%, rgba(68, 154, 255, 0.74) 72%, rgba(68, 154, 255, 0));
  box-shadow: 0 0 30px rgba(87, 171, 255, 0.68);
}

.particle-track.projectile-grass .particle-core {
  background: radial-gradient(circle, rgba(255, 255, 255, 0.98), rgba(220, 255, 193, 0.96) 30%, rgba(98, 206, 109, 0.74) 72%, rgba(98, 206, 109, 0));
  box-shadow: 0 0 30px rgba(88, 198, 112, 0.7);
}

.particle-track.projectile-arcane .particle-core {
  background: radial-gradient(circle, rgba(255, 255, 255, 0.98), rgba(219, 215, 255, 0.96) 30%, rgba(152, 120, 255, 0.72) 72%, rgba(152, 120, 255, 0));
  box-shadow: 0 0 30px rgba(152, 120, 255, 0.66);
}

.impact-flash {
  position: absolute;
  inset: 20% 10% 8%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0));
  mix-blend-mode: screen;
  animation: impactFlash 320ms ease-out;
  pointer-events: none;
}

.sprite-wrapper.attacking .sprite-img {
  animation: attackFlash 0.96s ease-in-out;
}

.player-sprite.hurt {
  animation: playerKnockback 0.46s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}

.enemy-sprite.hurt {
  animation: enemyKnockback 0.46s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}

.battle-command-deck {
  position: relative;
}

.skills-section,
.command-panel,
.battle-actions,
.battle-log-drawer {
  border-radius: 26px;
  border: 1px solid rgba(255, 191, 112, 0.14);
  background: rgba(11, 15, 24, 0.86);
  box-shadow: 0 16px 28px rgba(4, 8, 15, 0.24);
}

.skills-section,
.command-panel,
.battle-log-drawer {
  padding: 18px 20px;
}

.skills-section__header,
.battle-log__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
}

.skills-section__header h4,
.battle-log__header h3 {
  margin: 0;
  color: #fff2d6;
}

.skills-close-btn,
.drawer-close-btn {
  padding: 8px 12px;
  border: 1px solid rgba(255, 197, 112, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 242, 214, 0.84);
  font-size: 0.84rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 160ms ease, background 160ms ease;
}

.skills-close-btn:hover,
.drawer-close-btn:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.09);
}

.skills-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.skill-btn {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  padding: 14px 16px;
  border-radius: 16px;
  font-weight: 700;
}

.skill-btn__name {
  font-size: 1rem;
}

.skill-btn__cost {
  color: inherit;
  opacity: 0.78;
  font-size: 0.82rem;
}

.skills-section {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(100% + 10px);
  z-index: 30;
  max-height: min(260px, 36vh);
  overflow-y: auto;
}

.command-panel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(100% + 10px);
  z-index: 30;
  max-height: min(320px, 46vh);
  overflow-y: auto;
}

.command-panel__empty {
  display: grid;
  place-items: center;
  min-height: 120px;
  color: rgba(247, 237, 220, 0.72);
  text-align: center;
}

.inline-potion-list {
  margin: 0;
  display: grid;
  gap: 12px;
}

.inline-potion-list .potion-item {
  border: 1px solid rgba(255, 194, 107, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  padding: 12px 14px;
}

.inline-potion-list .potion-item:hover {
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 18px rgba(4, 8, 15, 0.18);
}

.inline-potion-list .potion-info h4 {
  color: #fff0d5;
}

.inline-potion-list .potion-info p {
  color: rgba(247, 237, 220, 0.72);
}

.inline-potion-list .potion-count {
  color: #9ee184;
}

.inline-battle-elves-list {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
  align-items: stretch;
}

.inline-battle-elves-list .elf-card.horizontal {
  height: 100%;
  min-height: 108px;
  border-color: rgba(255, 194, 107, 0.14);
  background: rgba(255, 255, 255, 0.04);
}

.inline-battle-elves-list .elf-card.horizontal:hover {
  box-shadow: 0 10px 20px rgba(4, 8, 15, 0.16);
}

.inline-battle-elves-list .elf-card.horizontal.current {
  border-color: rgba(99, 202, 122, 0.44);
  background: rgba(99, 202, 122, 0.08);
}

.inline-battle-elves-list .elf-card.horizontal .elf-info h4,
.inline-battle-elves-list .elf-card.horizontal .elf-stats p {
  color: rgba(247, 237, 220, 0.82);
}

.inline-battle-elves-list .elf-card.horizontal .elf-stats {
  gap: 0.7rem;
}

.battle-actions {
  position: relative;
  bottom: auto;
  padding: 14px 16px 16px;
  border-top: none;
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(24, 30, 45, 0.96), rgba(10, 13, 21, 0.98)),
    rgba(11, 15, 24, 0.92);
  z-index: 20;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.action-btn {
  min-width: 0;
  min-height: 52px;
  border-radius: 16px;
  text-transform: none;
  letter-spacing: 0.06em;
  font-size: 0.94rem;
}

.skill-action-btn {
  background: linear-gradient(135deg, #2b74ff, #38a9ff);
  color: white;
  box-shadow: 0 10px 24px rgba(56, 169, 255, 0.24);
}

.battle-log-toggle {
  flex-shrink: 0;
  min-width: 118px;
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid rgba(255, 196, 112, 0.2);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(26, 31, 48, 0.94), rgba(11, 14, 22, 0.96));
  color: #fff0d2;
  font-size: 0.9rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  box-shadow: 0 10px 20px rgba(4, 8, 15, 0.28);
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.battle-log-toggle:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(4, 8, 15, 0.38);
}

.battle-log-toggle.is-open {
  background: linear-gradient(180deg, rgba(255, 145, 61, 0.94), rgba(201, 77, 28, 0.94));
}

.battle-log-layer {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: flex;
  justify-content: flex-end;
  padding: 88px 20px 20px;
  background: rgba(4, 7, 13, 0.34);
  backdrop-filter: blur(4px);
}

.battle-log-drawer {
  width: min(380px, calc(100vw - 28px));
  height: min(calc(100vh - 108px), 780px);
  display: flex;
  flex-direction: column;
}

.log-content {
  flex: 1;
  max-height: none;
  overflow-y: auto;
  border: 1px solid rgba(255, 196, 112, 0.1);
  background: rgba(6, 9, 15, 0.76);
  border-radius: 18px;
  padding: 16px;
}

.round-logs {
  display: grid;
  gap: 6px;
}

.round-section {
  border-left-color: rgba(255, 177, 75, 0.72);
}

.round-header {
  color: #ffcf82;
}

.log-entry {
  margin-left: 12px;
  color: rgba(248, 240, 226, 0.78);
}

.battle-result {
  width: min(92vw, 700px);
  border: 1px solid rgba(255, 198, 111, 0.24);
  background: linear-gradient(180deg, rgba(18, 22, 34, 0.98), rgba(9, 12, 19, 0.98));
  color: #fff4df;
}

.battle-result h2,
.ai-summary h3 {
  color: #ffd07b;
}

.ai-summary {
  border-color: rgba(255, 198, 111, 0.14);
  background: rgba(255, 255, 255, 0.04);
}

.summary-content {
  color: rgba(247, 239, 224, 0.82);
}

@keyframes dialogReveal {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes projectileMoveRight {
  0% {
    left: 4%;
    opacity: 0;
    transform: translateY(-50%) scale(0.4);
  }
  12% {
    opacity: 1;
  }
  100% {
    left: 92%;
    opacity: 0;
    transform: translateY(-50%) scale(1.24);
  }
}

@keyframes projectileMoveLeft {
  0% {
    left: 92%;
    opacity: 0;
    transform: translateY(-50%) scale(0.4);
  }
  12% {
    opacity: 1;
  }
  100% {
    left: 4%;
    opacity: 0;
    transform: translateY(-50%) scale(1.24);
  }
}

@keyframes projectileSparkRight {
  0% {
    left: 8%;
    opacity: 0;
    transform: translateY(-50%) scaleX(0.4);
  }
  18% {
    opacity: 0.95;
  }
  100% {
    left: 86%;
    opacity: 0;
    transform: translateY(-50%) scaleX(1.6);
  }
}

@keyframes projectileSparkLeft {
  0% {
    left: 86%;
    opacity: 0;
    transform: translateY(-50%) scaleX(0.4);
  }
  18% {
    opacity: 0.95;
  }
  100% {
    left: 8%;
    opacity: 0;
    transform: translateY(-50%) scaleX(1.6);
  }
}

@keyframes playerKnockback {
  0% { transform: translateX(0) scale(1); }
  25% { transform: translateX(-24px) translateY(-8px) scale(0.98); }
  50% { transform: translateX(10px) translateY(2px) scale(1.02); }
  100% { transform: translateX(0) scale(1); }
}

@keyframes enemyKnockback {
  0% { transform: translateX(0) scale(1); }
  25% { transform: translateX(24px) translateY(-8px) scale(0.98); }
  50% { transform: translateX(-10px) translateY(2px) scale(1.02); }
  100% { transform: translateX(0) scale(1); }
}

@keyframes impactFlash {
  0% { opacity: 0; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1.1); }
  100% { opacity: 0; transform: scale(1.3); }
}

.skill-panel-enter-active,
.skill-panel-leave-active,
.log-drawer-enter-active,
.log-drawer-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.skill-panel-enter-from,
.skill-panel-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.log-drawer-enter-from,
.log-drawer-leave-to {
  opacity: 0;
}

.log-drawer-enter-from .battle-log-drawer,
.log-drawer-leave-to .battle-log-drawer {
  transform: translateX(24px);
}

@media (max-width: 980px) {
  .home-container {
    padding: 0 14px 24px;
  }

  .battle-container {
    min-height: auto;
  }

  .battle-field {
    height: auto;
    min-height: 700px;
  }

  .hud {
    width: calc(100% - 36px);
  }

  .player-hud,
  .enemy-hud {
    left: 18px;
    right: 18px;
  }

  .enemy-hud {
    top: 136px;
    border-right-width: 1px;
    border-left: 4px solid rgba(255, 118, 82, 0.92);
  }

  .battle-stage {
    min-height: 560px;
    padding: 292px 6% 132px;
  }

  .player-sprite,
  .enemy-sprite {
    min-width: 0;
  }

  .action-buttons {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .skills-section {
    max-height: min(300px, 42vh);
  }

  .command-panel {
    max-height: min(340px, 48vh);
  }

  .inline-battle-elves-list {
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  }
}

@media (max-width: 640px) {
  .battle-container {
    margin-top: 12px;
  }

  .battle-field {
    min-height: 760px;
    border-radius: 24px;
  }

  .battle-stage {
    min-height: 520px;
    padding: 292px 8% 126px;
  }

  .sprite-img {
    max-width: 170px;
    max-height: 220px;
  }

  .battle-status-strip {
    grid-template-columns: 1fr;
  }

  .battle-status-chip {
    min-height: 38px;
  }

  .battle-status-side,
  .battle-status-side--left,
  .battle-status-side--right {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .battle-command-deck {
    padding-bottom: 0;
  }

  .battle-actions {
    padding: 16px;
  }

  .action-buttons {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .action-btn {
    flex: unset;
    width: 100%;
    min-height: 54px;
    font-size: 0.92rem;
  }

  .skills-section,
  .command-panel,
  .battle-log-layer {
    padding-left: 10px;
    padding-right: 10px;
  }

  .skills-section__header,
  .battle-log__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .skills-section {
    left: 0;
    right: 0;
    bottom: calc(100% + 8px);
  }

  .command-panel {
    left: 0;
    right: 0;
    bottom: calc(100% + 8px);
  }

  .inline-battle-elves-list {
    display: grid;
    grid-auto-flow: column;
    grid-auto-columns: minmax(220px, 82vw);
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .battle-log-toggle {
    min-width: 104px;
    min-height: 42px;
    padding: 0 14px;
    font-size: 0.82rem;
  }

  .battle-log-layer {
    align-items: flex-end;
    padding: 72px 10px 10px;
  }

  .battle-log-drawer {
    width: 100%;
    height: min(calc(100vh - 82px), 78vh);
  }
}
</style>
