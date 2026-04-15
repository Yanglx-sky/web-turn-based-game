<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import HealthBar from './HealthBar.vue'
import SpriteDisplay from './SpriteDisplay.vue'
import SkillEffect from './SkillEffect.vue'

const props = defineProps({
  playerSprite: { type: Object, required: true },
  enemySprite: { type: Object, required: true },
  battleLogs: { type: Array, default: () => [] },
  isPlayerAttacking: { type: Boolean, default: false },
  isEnemyAttacking: { type: Boolean, default: false },
  isPlayerHurt: { type: Boolean, default: false },
  isEnemyHurt: { type: Boolean, default: false },
  activeSkill: { type: Object, default: null },
  currentRound: { type: Number, default: 1 },
  enemyDialog: { type: String, default: '' }
})

const emit = defineEmits(['animation-complete'])

// 组件引用
const playerSpriteRef = ref(null)
const enemySpriteRef = ref(null)
const skillEffectRef = ref(null)

// 粒子特效状态
const showSkillEffect = ref(false)
const skillEffectConfig = ref({
  elementType: 1,
  fromPosition: { x: 0, y: 0 },
  toPosition: { x: 0, y: 0 }
})

// 精灵图片URL
const playerImage = computed(() => props.playerSprite?.imageUrl || '/src/assets/hero.png')
const enemyImage = computed(() => props.enemySprite?.imageUrl || '/src/assets/hero.png')

// 触发攻击动画
const triggerAttackAnimation = (attacker, skillName) => {
  const isPlayerAttack = attacker === 'player'
  const attackerRef = isPlayerAttack ? playerSpriteRef.value : enemySpriteRef.value
  const targetRef = isPlayerAttack ? enemySpriteRef.value : playerSpriteRef.value

  // 攻击方动画
  if (attackerRef) {
    attackerRef.triggerAttack({ skillName })
  }

  // 粒子特效
  if (props.activeSkill) {
    showSkillEffect.value = true
    skillEffectConfig.value = {
      elementType: props.activeSkill.elementType,
      fromPosition: { x: isPlayerAttack ? 150 : 650, y: 250 },
      toPosition: { x: isPlayerAttack ? 650 : 150, y: 250 }
    }
  }
}

// 触发受击动画
const triggerHitAnimation = (target) => {
  const targetRef = target === 'player' ? playerSpriteRef.value : enemySpriteRef.value
  if (targetRef) {
    targetRef.triggerHit()
  }
}

// 监听攻击状态变化
watch(() => props.isPlayerAttacking, (newVal) => {
  if (newVal) {
    triggerAttackAnimation('player', props.activeSkill?.skillName || '普通攻击')
  }
})

watch(() => props.isEnemyAttacking, (newVal) => {
  if (newVal) {
    triggerAttackAnimation('enemy', props.activeSkill?.skillName || '普通攻击')
  }
})

// 监听受击状态变化
watch(() => props.isPlayerHurt, (newVal) => {
  if (newVal) {
    triggerHitAnimation('player')
    showSkillEffect.value = false
  }
})

watch(() => props.isEnemyHurt, (newVal) => {
  if (newVal) {
    triggerHitAnimation('enemy')
    showSkillEffect.value = false
  }
})

// 粒子特效结束
const handleEffectEnd = () => {
  emit('animation-complete')
}

// 获取精灵图片URL
const getSpriteImage = (spriteId, elementType) => {
  const images = {
    1: '/src/assets/photo/sasuke/佐助.jpg', // 火系 - 佐助
    2: '/src/assets/photo/zhaomeiming/照美冥.webp', // 水系 - 照美冥
    3: '/src/assets/photo/qianshouzhujian/千手柱间.jpg', // 草系 - 千手柱间
  }
  return images[elementType] || '/src/assets/hero.png'
}

const getMonsterImage = (name) => {
  const monsterImages = {
    '迪莫': '/src/assets/photo/t01ad4c8a8c1b78d5e9.jpg',
    '焰阳火灵': '/src/assets/photo/78714861ed8311a9d5af0982af3bf12196230622.jpg',
    '惊涛水灵': '/src/assets/photo/7acb0a46f21fbe096b63310c0e2a1b338744ebf807da.png',
    '魔草巫灵': '/src/assets/photo/ac6eddc451da81cb39db2c96742dc7160924ab189b1f.png'
  }
  return monsterImages[name] || '/src/assets/hero.png'
}

// 暴露方法
defineExpose({
  triggerAttackAnimation,
  triggerHitAnimation
})
</script>

<template>
  <div class="battle-scene">
    <!-- 背景层 -->
    <div class="battle-background">
      <div class="bg-gradient"></div>
      <div class="bg-pattern"></div>
    </div>

    <!-- 回合显示 -->
    <div class="round-display">
      <span class="round-label">回合</span>
      <span class="round-number">{{ currentRound }}</span>
    </div>

    <!-- 敌人对话 -->
    <div class="enemy-dialog" v-if="enemyDialog">
      <div class="dialog-box">
        <span class="dialog-name">{{ enemySprite?.name }}:</span>
        <span class="dialog-text">{{ enemyDialog }}</span>
      </div>
    </div>

    <!-- 顶部血条区域 -->
    <div class="health-bars-area">
      <!-- 玩家血条 -->
      <HealthBar
        :name="playerSprite?.name || '我的精灵'"
        :level="playerSprite?.level || 1"
        :elementType="playerSprite?.elementType || 1"
        :currentHp="playerSprite?.hp || 100"
        :maxHp="playerSprite?.maxHp || 100"
        :currentMp="playerSprite?.mp || 50"
        :maxMp="playerSprite?.maxMp || 50"
        :isPlayer="true"
        :isCritical="playerSprite?.hp <= playerSprite?.maxHp * 0.2"
      />

      <!-- 敌人血条 -->
      <HealthBar
        :name="enemySprite?.name || '敌人'"
        :level="enemySprite?.level || 1"
        :elementType="enemySprite?.elementType || 1"
        :currentHp="enemySprite?.hp || 100"
        :maxHp="enemySprite?.maxHp || 100"
        :currentMp="enemySprite?.mp || 0"
        :maxMp="enemySprite?.maxMp || 0"
        :isPlayer="false"
        :isCritical="enemySprite?.hp <= enemySprite?.maxHp * 0.2"
      />
    </div>

    <!-- 战斗舞台 -->
    <div class="battle-stage">
      <!-- 玩家精灵 -->
      <SpriteDisplay
        ref="playerSpriteRef"
        :spriteId="playerSprite?.id"
        :spriteName="playerSprite?.name"
        :elementType="playerSprite?.elementType"
        :imageUrl="playerSprite?.imageUrl || getSpriteImage(playerSprite?.id, playerSprite?.elementType)"
        :isPlayer="true"
        :isActive="playerSprite?.hp > 0"
        @animation-end="emit('animation-complete')"
      />

      <!-- 战斗特效层 -->
      <SkillEffect
        ref="skillEffectRef"
        :active="showSkillEffect"
        :elementType="skillEffectConfig.elementType"
        :fromPosition="skillEffectConfig.fromPosition"
        :toPosition="skillEffectConfig.toPosition"
        @effect-end="handleEffectEnd"
      />

      <!-- VS分隔 -->
      <div class="battle-divider">
        <span class="vs-text">VS</span>
      </div>

      <!-- 敌人精灵 -->
      <SpriteDisplay
        ref="enemySpriteRef"
        :spriteId="enemySprite?.id"
        :spriteName="enemySprite?.name"
        :elementType="enemySprite?.elementType"
        :imageUrl="enemySprite?.imageUrl || getMonsterImage(enemySprite?.name)"
        :isPlayer="false"
        :isActive="enemySprite?.hp > 0"
        @animation-end="emit('animation-complete')"
      />
    </div>

    <!-- 战斗日志 -->
    <div class="battle-log-area" v-if="battleLogs.length > 0">
      <div class="log-container">
        <div class="log-item" v-for="(log, index) in battleLogs.slice(-3)" :key="index">
          {{ log }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.battle-scene {
  position: relative;
  width: 100%;
  min-height: 55vh;
  max-height: 60vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 背景层 */
.battle-background {
  position: absolute;
  inset: 0;
  z-index: var(--z-bg);
}

.bg-gradient {
  background: linear-gradient(
    135deg,
    #0d1b2a 0%,
    #1b263b 30%,
    #415a77 60%,
    #1b263b 80%,
    #0d1b2a 100%
  );
  height: 100%;
}

.bg-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 80%, rgba(76, 201, 240, 0.05) 0%, transparent 40%),
    radial-gradient(circle at 80% 20%, rgba(255, 107, 53, 0.05) 0%, transparent 40%);
  pointer-events: none;
}

/* 回合显示 */
.round-display {
  position: absolute;
  top: var(--space-lg);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid var(--border-visible);
  border-radius: var(--radius-lg);
  z-index: var(--z-ui);
}

.round-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.round-number {
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--text-accent);
}

/* 敌人对话 */
.enemy-dialog {
  position: absolute;
  top: var(--space-3xl);
  left: 50%;
  transform: translateX(-50%);
  max-width: 80%;
  z-index: var(--z-ui);
}

.dialog-box {
  padding: var(--space-md) var(--space-xl);
  background: rgba(0, 0, 0, 0.7);
  border: 2px solid var(--border-visible);
  border-radius: var(--radius-xl);
  animation: dialog-appear 0.3s ease-out;
}

@keyframes dialog-appear {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dialog-name {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--water-primary);
}

.dialog-text {
  font-size: var(--text-base);
  color: var(--text-primary);
}

/* 血条区域 */
.health-bars-area {
  position: relative;
  display: flex;
  justify-content: space-between;
  padding: var(--space-xl) var(--space-xl) var(--space-md);
  z-index: var(--z-ui);
}

/* 战斗舞台 */
.battle-stage {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2xl) var(--space-3xl);
  flex: 1;
  z-index: var(--z-sprite);
}

/* VS分隔 */
.battle-divider {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: var(--z-bg);
}

.vs-text {
  font-size: var(--text-3xl);
  font-weight: 900;
  color: var(--text-muted);
  opacity: 0.3;
  letter-spacing: 8px;
}

/* 战斗日志 */
.battle-log-area {
  position: absolute;
  bottom: var(--space-lg);
  left: var(--space-xl);
  right: var(--space-xl);
  z-index: var(--z-ui);
}

.log-container {
  padding: var(--space-md);
  background: rgba(0, 0, 0, 0.4);
  border-radius: var(--radius-lg);
  max-height: 60px;
  overflow: hidden;
}

.log-item {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  padding: var(--space-xs) 0;
  animation: log-fade 0.3s ease-out;
}

@keyframes log-fade {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .battle-stage {
    padding: var(--space-xl);
  }

  .health-bars-area {
    padding: var(--space-md);
  }

  .vs-text {
    font-size: var(--text-2xl);
  }

  .battle-log-area {
    left: var(--space-md);
    right: var(--space-md);
  }
}

@media (max-width: 480px) {
  .round-display {
    padding: var(--space-xs) var(--space-md);
  }

  .round-number {
    font-size: var(--text-lg);
  }

  .dialog-box {
    padding: var(--space-sm) var(--space-md);
    font-size: var(--text-sm);
  }
}
</style>