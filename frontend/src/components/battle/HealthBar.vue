<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  level: { type: Number, default: 1 },
  elementType: { type: Number, default: 0 },
  currentHp: { type: Number, required: true },
  maxHp: { type: Number, required: true },
  currentMp: { type: Number, default: 0 },
  maxMp: { type: Number, default: 0 },
  isPlayer: { type: Boolean, default: false },
  isCritical: { type: Boolean, default: false }
})

const emit = defineEmits(['critical-alert'])

// HP百分比计算
const hpPercent = computed(() => {
  const percent = Math.max(0, Math.min(100, (props.currentHp / props.maxHp) * 100))
  return percent
})

const mpPercent = computed(() => {
  const percent = Math.max(0, Math.min(100, (props.currentMp / props.maxMp) * 100))
  return percent
})

// HP状态样式
const hpStatusClass = computed(() => {
  if (hpPercent.value <= 20) return 'critical'
  if (hpPercent.value <= 50) return 'warning'
  return 'healthy'
})

// 属性系别样式
const elementClass = computed(() => {
  const classes = ['', 'element-fire', 'element-water', 'element-grass', 'element-light']
  return classes[props.elementType] || ''
})

// 低血量警告
watch(hpPercent, (newVal) => {
  if (newVal <= 20 && !props.isCritical) {
    emit('critical-alert')
  }
})

// 动画状态
const isAnimating = ref(false)

const triggerAnimation = () => {
  isAnimating.value = true
  setTimeout(() => {
    isAnimating.value = false
  }, 500)
}

defineExpose({ triggerAnimation })
</script>

<template>
  <div
    class="health-bar-container"
    :class="[elementClass, { 'player-side': isPlayer, 'enemy-side': !isPlayer, 'critical-state': hpPercent <= 20 }]"
  >
    <!-- 名字和等级 -->
    <div class="sprite-header">
      <div class="sprite-name-wrapper">
        <span class="sprite-name">{{ name }}</span>
        <span class="level-badge">Lv.{{ level }}</span>
      </div>
      <div class="element-badge" v-if="elementType">
        <span class="element-icon">{{ ['', '🔥', '💧', '🌿', '✨'][elementType] }}</span>
      </div>
    </div>

    <!-- 血条组 -->
    <div class="bars-container">
      <!-- HP血条 -->
      <div class="bar-row hp-row">
        <div class="bar-label">
          <span class="label-text">HP</span>
        </div>
        <div class="bar-wrapper">
          <div class="bar-track hp-track">
            <div
              class="bar-fill hp-fill"
              :class="[hpStatusClass, { 'bar-changing': isAnimating }]"
              :style="{ width: `${hpPercent}%` }"
            >
              <div class="bar-shine"></div>
            </div>
          </div>
          <div class="bar-numbers">
            <span class="current-value">{{ Math.max(0, currentHp) }}</span>
            <span class="separator">/</span>
            <span class="max-value">{{ maxHp }}</span>
          </div>
        </div>
      </div>

      <!-- MP蓝条 -->
      <div class="bar-row mp-row" v-if="maxMp > 0">
        <div class="bar-label">
          <span class="label-text">MP</span>
        </div>
        <div class="bar-wrapper">
          <div class="bar-track mp-track">
            <div
              class="bar-fill mp-fill"
              :style="{ width: `${mpPercent}%` }"
            >
              <div class="bar-shine"></div>
            </div>
          </div>
          <div class="bar-numbers">
            <span class="current-value mp-text">{{ currentMp }}</span>
            <span class="separator">/</span>
            <span class="max-value">{{ maxMp }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.health-bar-container {
  background: linear-gradient(135deg, rgba(15, 52, 96, 0.9), rgba(22, 33, 62, 0.95));
  border: 2px solid var(--border-visible);
  border-radius: var(--radius-xl);
  padding: var(--space-md) var(--space-lg);
  min-width: 200px;
  max-width: 280px;
  position: relative;
  overflow: hidden;
}

.health-bar-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, var(--element-glow), transparent 70%);
  opacity: 0.3;
  pointer-events: none;
}

/* 属性系别样式 */
.health-bar-container.element-fire {
  border-color: var(--fire-primary);
}

.health-bar-container.element-water {
  border-color: var(--water-primary);
}

.health-bar-container.element-grass {
  border-color: var(--grass-primary);
}

.health-bar-container.element-light {
  border-color: var(--light-secondary);
}

/* 位置样式 */
.player-side {
  margin-right: auto;
}

.enemy-side {
  margin-left: auto;
}

.sprite-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-md);
}

.sprite-name-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.sprite-name {
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.level-badge {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  background: rgba(0, 0, 0, 0.3);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.element-badge {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.element-icon {
  filter: drop-shadow(0 0 4px var(--element-color));
}

/* 血条容器 */
.bars-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.bar-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.bar-label {
  width: 28px;
}

.label-text {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 1px;
}

.bar-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.bar-track {
  flex: 1;
  height: 16px;
  background: rgba(0, 0, 0, 0.4);
  border-radius: var(--radius-sm);
  overflow: hidden;
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.bar-fill {
  height: 100%;
  position: relative;
  transition: width 0.4s ease-out-quart;
  border-radius: var(--radius-sm);
}

.bar-shine {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.3), transparent);
}

/* HP状态颜色 */
.hp-fill.healthy {
  background: linear-gradient(90deg, #22c55e, #4ade80);
}

.hp-fill.warning {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.hp-fill.critical {
  background: linear-gradient(90deg, #dc2626, #ef4444);
  animation: critical-pulse 1s ease-in-out infinite;
}

.bar-changing {
  animation: bar-flash 0.3s ease-out;
}

@keyframes critical-pulse {
  0%, 100% {
    opacity: 1;
    box-shadow: 0 0 8px #ef4444;
  }
  50% {
    opacity: 0.8;
    box-shadow: 0 0 16px #ef4444, 0 0 24px #ef4444;
  }
}

@keyframes bar-flash {
  0% { filter: brightness(1); }
  50% { filter: brightness(1.3); }
  100% { filter: brightness(1); }
}

/* MP蓝条 */
.mp-fill {
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
}

.bar-numbers {
  font-size: var(--text-sm);
  font-weight: 600;
  min-width: 70px;
  text-align: right;
}

.current-value {
  color: var(--text-primary);
}

.mp-text {
  color: #60a5fa;
}

.separator {
  color: var(--text-muted);
  margin: 0 2px;
}

.max-value {
  color: var(--text-secondary);
}

/* 低血量整体警告 */
.critical-state {
  animation: container-shake 0.5s ease-in-out infinite;
  border-color: #ef4444 !important;
}

@keyframes container-shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-2px); }
  75% { transform: translateX(2px); }
}

/* 响应式 */
@media (max-width: 480px) {
  .health-bar-container {
    min-width: 160px;
    padding: var(--space-sm) var(--space-md);
  }

  .sprite-name {
    font-size: var(--text-base);
  }

  .bar-track {
    height: 12px;
  }

  .bar-numbers {
    font-size: var(--text-xs);
    min-width: 55px;
  }
}
</style>