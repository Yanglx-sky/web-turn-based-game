<template>
  <div class="home-container">
    <div class="nav-bar">
      <div class="nav-logo">洛克王国</div>
      <div class="countdown" v-if="!showResult">
        <span class="countdown-text">决策时间:</span>
        <span class="countdown-number">{{ countdown }}</span>
        <span class="countdown-unit">秒</span>
      </div>
    </div>

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

    <div class="battle-container">
      <h1>战斗</h1>
      
      <!-- 回合显示 -->
      <div class="round-display">
        <span class="round-text">当前回合：</span>
        <span class="round-number">{{ currentRound }}</span>
      </div>
      
      <!-- 敌人对话区域 -->
      <div class="enemy-dialog" v-if="enemyDialog">
        <div class="dialog-content">
          <span class="enemy-name">{{ enemyName }}:</span>
          <span class="dialog-text">{{ enemyDialog }}</span>
        </div>
      </div>
      
      <!-- 战斗场景 -->
      <div class="battle-field">
        <!-- 玩家精灵 -->
        <div class="player-section">
          <h3>我的精灵</h3>
          <div class="elf-card player-elf" :class="{ 'attacking': attackingSide === 'player' && attackAnimation, 'hurt': isPlayerHurt }" v-if="playerElf && playerElf.elfId">
            <div class="elf-image">
              <img :src="getElfImage(playerElf.elfId)" :alt="playerElf.elfName || `精灵 ${playerElf.elfId}`" />
            </div>
            <div class="elf-info">
              <h4>{{ playerElf.elfName || `精灵 ${playerElf.elfId}` }}</h4>
              <p>等级: {{ playerElf.level }}</p>
              <p>系别: {{ getElementType(playerElf.elementType) }}</p>
              <div class="hp-bar">
                <div class="hp-fill" :style="{ width: playerHpPercentage + '%' }"></div>
              </div>
              <p>HP: {{ Math.max(0, playerElf.hp) }}/{{ playerElf.maxHp }}</p>
              <p>MP: {{ playerElf.mp }}/{{ playerElf.maxMp }}</p>
              <div class="skill-name" v-if="attackingSide === 'player' && attackAnimation">
                {{ currentSkill }}
              </div>
            </div>
          </div>
          <div v-else class="elf-card player-elf">
            <div class="elf-info">
              <h4>未选择精灵</h4>
              <p>请进入战斗选择精灵</p>
            </div>
          </div>
        </div>
        
        <!-- 敌人精灵 -->
        <div class="enemy-section">
          <h3>敌人</h3>
          <div class="elf-card enemy-elf" :class="{ 'attacking': attackingSide === 'enemy' && attackAnimation, 'hurt': isEnemyHurt }">
            <div class="elf-image">
              <img src="/src/assets/photo/t01ad4c8a8c1b78d5e9.jpg" alt="迪莫" v-if="enemyName === '迪莫'" />
              <img src="/src/assets/photo/78714861ed8311a9d5af0982af3bf12196230622.jpg" alt="焰阳火灵" v-else-if="enemyName === '焰阳火灵'" />
              <img src="/src/assets/photo/7acb0a46f21fbe096b63310c0e2a1b338744ebf807da.png" alt="惊涛水灵" v-else-if="enemyName === '惊涛水灵'" />
              <img src="/src/assets/photo/ac6eddc451da81cb39db2c96742dc7160924ab189b1f.png" alt="魔草巫灵" v-else-if="enemyName === '魔草巫灵'" />
              <img src="/src/assets/photo/火-训练人偶.png" alt="火系训练人偶" v-else-if="enemyName.includes('训练人偶') && enemyElementType === 1" />
              <img src="/src/assets/photo/水-训练人偶.png" alt="水系训练人偶" v-else-if="enemyName.includes('训练人偶') && enemyElementType === 2" />
              <img src="/src/assets/photo/草-训练人偶.png" alt="草系训练人偶" v-else-if="enemyName.includes('训练人偶') && enemyElementType === 3" />
              <img src="/src/assets/hero.png" alt="敌人" v-else />
            </div>
            <div class="elf-info">
              <h4>{{ enemyName }}</h4>
              <p>等级: 1</p>
              <p>系别: {{ getElementType(enemyElementType) }}</p>
              <div class="hp-bar">
                <div class="hp-fill" :style="{ width: enemyHpPercentage + '%' }"></div>
              </div>
              <p>HP: {{ Math.max(0, enemyHp) }}/{{ enemyMaxHp }}</p>
              <p>MP: {{ enemyMp }}/{{ enemyMaxMp }}</p>
              <div class="skill-name" v-if="attackingSide === 'enemy' && attackAnimation">
                {{ currentSkill }}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 技能选择 -->
      <div v-if="showSkills" class="skills-section">
        <h4>选择技能</h4>
        <div class="skills-grid">
          <button v-for="skill in skills" :key="skill.id" @click="useSelectedSkill(skill)" class="skill-btn">
            {{ skill.skillName }} (MP: {{ skill.costMp }})
          </button>
        </div>
      </div>
      
      <!-- 战斗操作 -->
      <div class="battle-actions">
        <h3>战斗操作</h3>
        <div class="action-buttons">
          <button @click="attack" class="action-btn attack-btn">攻击</button>
          <button @click="useSkill" class="action-btn skill-btn">技能</button>
          <button @click="switchElf" class="action-btn switch-btn">更换精灵</button>
          <button @click="flee" class="action-btn flee-btn">逃跑</button>
          <button @click="showPotions = true" class="action-btn potion-btn">药品</button>
        </div>
      </div>
      
      <!-- 战斗日志 -->
      <div class="battle-log">
        <h3>战斗日志</h3>
        <div class="log-content">
          <div v-for="roundData in battleLogs" :key="roundData.round" class="round-section">
            <div class="round-header">
              回合{{ roundData.round }}：
            </div>
            <div class="round-logs">
              <div 
                v-for="(log, index) in roundData.logs" 
                :key="index" 
                class="log-entry"
              >
                {{ log }}
              </div>
            </div>
          </div>
        </div>
      </div>
      
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
          <div class="summary-content" v-html="aiSummary.replace(/\n/g, '<br>')"></div>
        </div>
        <button @click="getAISummary" class="summary-btn" v-if="!aiSummary">获取AI战报总结</button>
        <button @click="backToPVE" class="result-btn">返回关卡</button>
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
      
      <!-- 精灵切换弹窗 -->
      <div v-if="showSwitchElf" class="modal-overlay">
        <div class="modal-content switch-elf-modal">
          <h2>选择精灵</h2>
          <p>选择一只精灵出战</p>
          <div v-if="loadingBattleElves" class="loading">加载中...</div>
          <div v-else class="battle-elves-list">
            <div v-for="elf in battleElves" :key="elf.id" class="elf-card horizontal" :class="{ 'current': elf && elf.id === playerElf.id }">
              <template v-if="elf && elf.status !== 2">
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
                      :disabled="!elf || elf.status === 2 || elf.status === 1"
                      :class="{ 'disabled': !elf || elf.status === 2 || elf.status === 1 }"
                    >
                      {{ !elf ? '未知' : (elf.status === 2 ? '已死亡' : elf.status === 1 ? '战斗中' : '选择') }}
                    </button>
                  </div>
                  <div v-else class="current-tag">当前出战</div>
                </div>
              </template>
            </div>
          </div>
          <div class="modal-buttons">
            <button @click="closeSwitchElf" class="cancel-btn">取消</button>
          </div>
        </div>
      </div>
      
      <!-- 药品使用弹窗 -->
      <div v-if="showPotions" class="modal-overlay">
        <div class="modal-content">
          <h2>使用药品</h2>
          <div v-if="potions.length === 0" class="no-items">
            <p>没有可用的药品</p>
          </div>
          <div v-else class="potion-list">
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
          <div class="modal-buttons">
            <button @click="showPotions = false" class="cancel-btn">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
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

const router = useRouter()
const route = useRoute()

// 网络管理器实例
let networkManager = null
const battleStore = useBattleStore()

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
// BGM音频对象
const battleBgm = ref(null)

// 技能列表
const skills = ref([])
// 药品列表
const potions = ref([])
// 药品使用弹窗
const showPotions = ref(false)

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
  
  // 清空之前的对话
  enemyDialog.value = ''
  
  // 遍历战斗日志，查找敌人的对话
  for (let i = battleLog.length - 1; i >= 0; i--) {
    const log = battleLog[i]
    // 检查日志是否包含敌人的名称
    if (isEnemyDialog(log)) {
      // 提取敌人的对话
      enemyDialog.value = log
      // 不再从战斗日志中移除这条对话，避免影响攻击动画的触发
      break
    }
  }
}

// 检查攻击记录并触发动画
const triggerAttackAnimations = (battleLog, battleData) => {
  if (!battleLog || battleLog.length === 0) {
    return
  }
  
  // 收集当前回合的攻击记录（只处理最新的攻击记录）
  const attackLogs = []
  
  // 从后向前遍历，收集攻击记录
  // 收集最新的玩家攻击和敌人攻击
  let foundPlayerAttack = false
  let foundEnemyAttack = false
  
  console.log('[DEBUG] 处理战斗日志:', battleLog)
  
  for (let i = battleLog.length - 1; i >= 0; i--) {
    const log = battleLog[i]
    
    // 检查是否是攻击记录
    if (typeof log === 'string' && (log.includes('攻击了') || log.includes('使用技能') || log.includes('普通攻击') || log.includes('使用普通攻击'))) {
      console.log('[DEBUG] 发现攻击记录:', log)
      // 检查是否是玩家攻击
      if (log.includes('你的精灵')) {
        if (!foundPlayerAttack) {
          attackLogs.unshift(log) // 插入到数组开头，保持攻击顺序
          foundPlayerAttack = true
          console.log('[DEBUG] 添加玩家攻击记录')
        }
      } 
      // 检查是否是敌人攻击
      else if (log.includes('怪物') || log.includes('训练人偶') || log.includes('迪莫') || log.includes('焰阳火灵') || log.includes('惊涛水灵') || log.includes('魔草巫灵')) {
        if (!foundEnemyAttack) {
          attackLogs.push(log) // 敌人攻击在玩家攻击之后
          foundEnemyAttack = true
          console.log('[DEBUG] 添加敌人攻击记录')
        }
      }
    }
  }
  
  // 确保攻击记录按顺序排列：玩家攻击在前，敌人攻击在后
  // 如果只有敌人攻击，确保它被添加到数组中
  if (attackLogs.length === 0 && foundEnemyAttack) {
    // 重新遍历找到第一个敌人攻击记录
    for (let i = battleLog.length - 1; i >= 0; i--) {
      const log = battleLog[i]
      if (typeof log === 'string' && (log.includes('怪物') || log.includes('训练人偶') || log.includes('迪莫') || log.includes('焰阳火灵') || log.includes('惊涛水灵') || log.includes('魔草巫灵')) && (log.includes('攻击了') || log.includes('使用技能') || log.includes('普通攻击') || log.includes('使用普通攻击'))) {
        attackLogs.push(log)
        break
      }
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
  if (uniqueAttackLogs.length > 0) {
    let delay = 0
    
    for (const log of uniqueAttackLogs) {
      console.log('[DEBUG] 准备触发动画，延迟:', delay, '日志:', log)
      setTimeout(() => {
        // 判断是玩家还是敌人攻击
        if (log.includes('你的精灵')) {
          // 玩家攻击
          attackingSide.value = 'player'
          // 提取技能名称
          let skillName = '普通攻击'
          if (log.includes('使用技能')) {
            // 匹配"使用技能 技能名称"格式
            const match = log.match(/使用技能 (.+?)，/)
            if (match && match[1]) {
              skillName = match[1].trim()
            }
          }
          currentSkill.value = skillName
          
          // 播放技能音频
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
        } else if (log.includes('怪物') || log.includes('训练人偶') || log.includes('迪莫') || log.includes('焰阳火灵') || log.includes('惊涛水灵') || log.includes('魔草巫灵')) {
          // 敌人攻击
          console.log('[DEBUG] 触发敌人攻击动画:', log)
          attackingSide.value = 'enemy'
          // 提取技能名称
          let skillName = '普通攻击'
          if (log.includes('使用技能')) {
            // 匹配"使用技能 技能名称"格式
            const match = log.match(/使用技能 (.+?)，/)
            if (match && match[1]) {
              skillName = match[1].trim()
            }
          }
          currentSkill.value = skillName
        }
        
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
          if (log.includes('你的精灵')) {
            // 玩家攻击，敌人受伤
            isEnemyHurt.value = true
            setTimeout(() => {
              isEnemyHurt.value = false
              // 结算玩家攻击的UI变化
              if (battleData) {
                // 更新敌人状态
                enemyHp.value = battleData.monsterHp ?? battleData.mannequinHp ?? enemyHp.value
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
          } else if (log.includes('怪物') || log.includes('训练人偶') || log.includes('迪莫') || log.includes('焰阳火灵') || log.includes('惊涛水灵') || log.includes('魔草巫灵')) {
            // 敌人攻击，玩家受伤
            isPlayerHurt.value = true
            setTimeout(() => {
              isPlayerHurt.value = false
              // 结算敌人攻击的UI变化
              if (battleData) {
                playerElf.value.hp = battleData.playerElfHp ?? battleData.elfCurrentHp ?? playerElf.value.hp
                playerElf.value.mp = battleData.elfMp ?? battleData.playerElf?.mp ?? playerElf.value.mp
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
      }, delay)
      
      // 每个攻击之间间隔1秒，减少等待时间
      delay += 1000
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
    const mannequinId = route.query.mannequinId
    const userElfId = route.query.userElfId
    
    let response
    if (battleType === 'train' && mannequinId) {
      // 训练模式
      response = await trainApi.startTrain(mannequinId)
    } else {
      // 关卡模式
      response = await levelApi.enterLevel(levelId, userElfId)
    }
    
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
      playerElf.value = data.elf || data.userElf || {}
      // 添加精灵名字
      playerElf.value.elfName = data.elfName || (data.userElf?.elfName || '')
      // 添加精灵系别
      playerElf.value.elementType = data.elfElementType || (data.userElf?.elementType || 0)
      // 设置精灵血量
      playerElf.value.hp = data.playerElfHp || (data.elf?.hp || data.userElf?.hp || 0)
      // 设置精灵最大血量
      playerElf.value.maxHp = data.userElf?.maxHp || (data.elf?.maxHp || 0)
      // 设置精灵蓝量
      playerElf.value.mp = data.elfMp || (data.elf?.mp || data.userElf?.mp || 0)
      // 设置精灵最大蓝量
      playerElf.value.maxMp = data.userElf?.maxMp || (data.elf?.maxMp || 0)
      enemyName.value = data.monsterName || `训练人偶 ${data.mannequin?.type === 1 ? '火系' : data.mannequin?.type === 2 ? '水系' : '草系'}`
      enemyHp.value = data.monsterHp || (data.mannequinHp || 0)
      enemyMaxHp.value = data.monsterMaxHp || (data.mannequinHp || 0)
      enemyMp.value = data.monsterMp || (data.mannequinMp || data.mannequin?.mp || 0)
      enemyMaxMp.value = data.monsterMaxMp || (data.mannequin?.mp || 0)
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
    // 调用后端使用药品接口
    const response = await potionApi.usePotion(playerElf.value.id, potion.potionConfigId)
    if (response.code === 200) {
      const data = response.data
      // 更新精灵状态
      playerElf.value = data.elf
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
        
        // 立即更新MP值，确保UI及时反映MP消耗
        if (data.elfMp !== undefined) {
          playerElf.value.mp = data.elfMp
          // 更新battleStore中的状态
          if (battleStore.elves.length > 0) {
            battleStore.elves[0].current_mp = data.elfMp
          }
        } else if (data.userElf?.mp !== undefined) {
          playerElf.value.mp = data.userElf.mp
          // 更新battleStore中的状态
          if (battleStore.elves.length > 0) {
            battleStore.elves[0].current_mp = data.userElf.mp
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
          
          // 更新精灵状态
          if (data.playerElfHp !== undefined) {
            playerElf.value.hp = data.playerElfHp
            if (battleStore.elves.length > 0) {
              battleStore.elves[0].current_hp = data.playerElfHp
            }
          }
          if (data.playerElfMp !== undefined) {
            playerElf.value.mp = data.playerElfMp
            if (battleStore.elves.length > 0) {
              battleStore.elves[0].current_mp = data.playerElfMp
            }
          }
        }
        
        // 处理战斗结果
        setTimeout(async () => {
          if (data.status === 1 || data.trainResult === '胜利') {
            // 确保敌人HP显示为0
            enemyHp.value = 0
            if (battleStore.monsters.length > 0) {
              battleStore.monsters[0].current_hp = 0
            }
            // 停止BGM
            if (battleBgm.value) {
              battleBgm.value.pause()
              battleBgm.value = null
            }
            // 保存AI战报总结（训练模式）
            if (data.aiReport) {
              aiSummary.value = data.aiReport
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
            // 1.5秒后自动关闭弹窗，然后显示胜利信息
            setTimeout(() => {
              showBattleEndPopup.value = false
              
              battleResult.value = '战斗胜利！'
              showResult.value = true
              // 清除倒计时
              if (countdownTimer.value) {
                clearInterval(countdownTimer.value)
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
            }, 800) // 减少延迟到0.8秒，提升响应速度
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
              audio.play()
            } catch (audioError) {
              console.error('播放语音失败:', audioError)
            }
            // 1.5秒后自动关闭弹窗，然后显示战斗结果
            setTimeout(() => {
              showBattleEndPopup.value = false
              
              battleResult.value = data.trainResult === '逃跑' ? '你逃跑了' : '战斗失败！'
              showResult.value = true
              // 清除倒计时
              if (countdownTimer.value) {
                clearInterval(countdownTimer.value)
              }
              // 更新battleStore中的状态
              battleStore.status = 2 // 2=失败
              // 清空战斗状态
              battleStore.clearBattleState()
            }, 800) // 减少延迟到0.8秒，提升响应速度
          }
        }, 500) // 减少延迟到0.5秒，提升响应速度
    } else {
      console.error('攻击失败:', response.msg)
      // 特别处理MP不足的错误
      if (response.msg.includes('MP不足')) {
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
      } else {
        alert('攻击失败: ' + response.msg)
      }
    }
  } catch (error) {
    console.error('攻击失败:', error)
    alert('攻击失败，请重试')
  } finally {
    // 重置倒计时
    if (!showResult.value) {
      resetCountdown()
    }
  }
}

const useSkill = () => {
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
        
        // 立即更新MP值，确保UI及时反映MP消耗
        if (data.elfMp !== undefined) {
          playerElf.value.mp = data.elfMp
          // 更新battleStore中的状态
          if (battleStore.elves.length > 0) {
            battleStore.elves[0].current_mp = data.elfMp
          }
        } else if (data.userElf?.mp !== undefined) {
          playerElf.value.mp = data.userElf.mp
          // 更新battleStore中的状态
          if (battleStore.elves.length > 0) {
            battleStore.elves[0].current_mp = data.userElf.mp
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
        }
        
        // 处理战斗结果
        setTimeout(async () => {
          if (data.status === 1 || data.trainResult === '胜利') {
            // 停止BGM
            if (battleBgm.value) {
              battleBgm.value.pause()
              battleBgm.value = null
            }
            // 保存AI战报总结（训练模式）
            if (data.aiReport) {
              aiSummary.value = data.aiReport
            }
            // 准备音频
            const audio = new Audio('/src/assets/audio/battle/结束战斗.mp3')
            // 音频加载完成后同时显示弹窗和播放音频
            audio.oncanplaythrough = () => {
              // 显示胜负已分弹窗
              showBattleEndPopup.value = true
              // 播放结束战斗语音
              try {
                audio.play()
              } catch (audioError) {
                console.error('播放语音失败:', audioError)
              }
            }
            // 即使音频加载失败，也显示弹窗
            audio.onerror = () => {
              showBattleEndPopup.value = true
            }
            // 触发音频加载
            audio.load()
            // 1.5秒后自动关闭弹窗，然后显示胜利信息
            setTimeout(() => {
              showBattleEndPopup.value = false
              
              battleResult.value = '战斗胜利！'
              showResult.value = true
              // 清除倒计时
              if (countdownTimer.value) {
                clearInterval(countdownTimer.value)
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
              // 清空战斗状态
              battleStore.clearBattleState()
            }, 1500)
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
            // 准备音频
            const audio = new Audio('/src/assets/audio/battle/结束战斗.mp3')
            // 音频加载完成后同时显示弹窗和播放音频
            audio.oncanplaythrough = () => {
              // 显示胜负已分弹窗
              showBattleEndPopup.value = true
              // 播放结束战斗语音
              try {
                audio.play()
              } catch (audioError) {
                console.error('播放语音失败:', audioError)
              }
            }
            // 即使音频加载失败，也显示弹窗
            audio.onerror = () => {
              showBattleEndPopup.value = true
            }
            // 触发音频加载
            audio.load()
            // 1.5秒后自动关闭弹窗，然后显示战斗结果
            setTimeout(() => {
              showBattleEndPopup.value = false
              
              battleResult.value = data.trainResult === '逃跑' ? '你逃跑了' : '战斗失败！'
              showResult.value = true
              // 清除倒计时
              if (countdownTimer.value) {
                clearInterval(countdownTimer.value)
              }
              // 更新battleStore中的状态
              battleStore.status = 2 // 2=失败
              // 清空战斗状态
              battleStore.clearBattleState()
            }, 1500)
          }
        }, 4000) // 延迟4秒，确保所有攻击和结算动画执行完成
    } else {
      console.error('技能使用失败:', response.msg)
      // 特别处理MP不足的错误
      if (response.msg.includes('MP不足')) {
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
      } else {
        alert('技能使用失败: ' + response.msg)
      }
    }
    
    showSkills.value = false
  } catch (error) {
    console.error('使用技能失败:', error)
  } finally {
    // 重置倒计时
    if (!showResult.value) {
      resetCountdown()
    }
  }
}

// 加载出战精灵列表
const loadBattleElves = async () => {
  try {
    loadingBattleElves.value = true
    const response = await userElfApi.getBattleElves()
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
  await loadBattleElves()
  showSwitchElf.value = true
}

// 关闭精灵切换弹窗
const closeSwitchElf = () => {
  showSwitchElf.value = false
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
      playerElf.value = data.elf
      // 添加精灵名字
      playerElf.value.elfName = data.elfName
      // 添加精灵系别
      playerElf.value.elementType = data.elfElementType
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
      // 显示胜负已分弹窗
      showBattleEndPopup.value = true
      // 播放结束战斗语音
      try {
        const audio = new Audio('/src/assets/audio/battle/结束战斗.mp3')
        audio.play()
      } catch (audioError) {
        console.error('播放语音失败:', audioError)
      }
      // 1.5秒后自动关闭弹窗，然后显示战斗结果
      setTimeout(() => {
        showBattleEndPopup.value = false
        
        battleResult.value = '你逃跑了'
        showResult.value = true
        // 清除倒计时
        if (countdownTimer.value) {
          clearInterval(countdownTimer.value)
        }
      }, 1500)
    } else {
      console.error('逃跑失败:', response.msg)
      alert('逃跑失败: ' + response.msg)
    }
  } catch (error) {
    console.error('逃跑失败:', error)
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
    const battleType = route.query.type
    
    // 检查是否已经有AI战报总结
    if (aiSummary.value) {
      return
    }
    
    // 根据战斗类型调用相应的API
    let response
    if (battleType === 'train') {
      // 训练模式下，调用训练结算API获取AI总结
      response = await trainApi.trainSettlement()
      if (response.code === 200) {
        aiSummary.value = response.data.aiReport
      } else if (response.msg === '未进入训练或训练未结束') {
        alert('战斗还没有结束，无法获取AI战报总结')
      } else {
        alert('获取战报总结失败: ' + response.msg)
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
      playerElf.value = {
        id: elf.elf_id,
        elfName: elf.elfName,
        elementType: elf.elementType,
        hp: elf.current_hp,
        maxHp: elf.maxHp,
        mp: elf.current_mp,
        maxMp: elf.maxMp,
        level: elf.level
      }
    }
    
    // 设置敌人信息
    enemyName.value = battleData.monsterName || '敌人'
    enemyElementType.value = battleData.monsterElementType || 0
    if (battleData.monsters && battleData.monsters.length > 0) {
      const monster = battleData.monsters[0]
      enemyHp.value = monster.current_hp
      enemyMaxHp.value = monster.maxHp
      enemyMp.value = monster.current_mp
      enemyMaxMp.value = monster.maxMp
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
  
  // 通知后端保存战斗状态
  battleApi.playerOffline().catch(error => {
    console.error('通知后端保存战斗状态失败:', error)
  })
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

/* 战斗场景 */
.battle-field {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
  padding: 20px;
  background: rgba(245, 245, 245, 0.8);
  border-radius: 10px;
}

.enemy-section, .player-section {
  flex: 1;
  text-align: center;
}

.elf-card {
  background: white;
  border: 1px solid #ddd;
  border-radius: 10px;
  padding: 20px;
  margin: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.elf-image {
  margin-bottom: 15px;
}

.elf-image img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 50%;
  border: 3px solid #ff8c00;
}

.enemy-elf {
  border-color: #f44336;
  transition: all 0.3s ease;
}

.player-elf {
  border-color: #4caf50;
  transition: all 0.3s ease;
}

.elf-card.attacking {
  border-color: #ff9800 !important;
  box-shadow: 0 0 20px rgba(255, 152, 0, 0.6);
  animation: attackFlash 1s ease-in-out;
}

@keyframes attackFlash {
  0% { box-shadow: 0 0 0 rgba(255, 152, 0, 0); }
  50% { box-shadow: 0 0 30px rgba(255, 152, 0, 0.8); }
  100% { box-shadow: 0 0 0 rgba(255, 152, 0, 0); }
}

.elf-card.attacking .elf-image img {
  animation: impactEffect 1s ease-in-out;
}

@keyframes impactEffect {
  0% { transform: scale(1); }
  25% { transform: scale(1.2) rotate(5deg); }
  50% { transform: scale(1.1) rotate(-5deg); }
  75% { transform: scale(1.2) rotate(3deg); }
  100% { transform: scale(1) rotate(0); }
}

.elf-info h4 {
  margin-top: 0;
  color: #333;
}

.elf-info p {
  margin: 5px 0;
  font-size: 0.9rem;
  color: #666;
}

.skill-name {
  margin-top: 10px;
  padding: 5px 10px;
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
  border-radius: 15px;
  font-weight: bold;
  text-align: center;
  animation: skillNameFade 1s ease-in-out;
}

@keyframes skillNameFade {
  0% { opacity: 0; transform: translateY(10px); }
  50% { opacity: 1; transform: translateY(0); }
  100% { opacity: 0; transform: translateY(-10px); }
}

.hp-bar {
  width: 100%;
  height: 10px;
  background: #e0e0e0;
  border-radius: 5px;
  overflow: hidden;
  margin: 10px 0;
}

.hp-fill {
  height: 100%;
  background: #4caf50;
  transition: width 0.5s ease;
}

/* 战斗操作 */
.battle-actions {
  margin-bottom: 30px;
  padding: 20px;
  background: rgba(245, 245, 245, 0.8);
  border-radius: 10px;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin: 20px 0;
}

.action-btn {
  padding: 15px;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.attack-btn {
  background: #ff5722;
  color: white;
}

.skill-btn {
  background: #2196f3;
  color: white;
}

.switch-btn {
  background: #ffeb3b;
  color: #333;
}

.flee-btn {
  background: #9e9e9e;
  color: white;
}

.potion-btn {
  background: #4caf50;
  color: white;
  grid-column: span 2;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
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

.summary-content {
  line-height: 1.8;
  color: #333;
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
</style>