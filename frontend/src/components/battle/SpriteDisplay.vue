<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  spriteId: { type: Number, required: false },
  spriteName: { type: String, default: '' },
  elementType: { type: Number, default: 0 },
  imageUrl: { type: String, default: '' },
  isPlayer: { type: Boolean, default: false },
  isActive: { type: Boolean, default: true }
})

const emit = defineEmits(['animation-end'])

// 动画状态
const isFlashing = ref(false)
const isKnockback = ref(false)
const isAttacking = ref(false)
const showSkillName = ref(false)
const skillNameText = ref('')

// 属性系别样式
const elementClass = computed(() => {
  const classes = ['', 'element-fire', 'element-water', 'element-grass', 'element-light']
  return classes[props.elementType] || ''
})

// 位置样式
const positionClass = computed(() => {
  return props.isPlayer ? 'player-sprite' : 'enemy-sprite'
})

// 触发闪白效果（被击中）
const triggerFlash = (duration = 300) => {
  isFlashing.value = true
  setTimeout(() => {
    isFlashing.value = false
  }, duration)
}

// 触发击退效果（被击中）
const triggerKnockback = (duration = 500) => {
  isKnockback.value = true
  setTimeout(() => {
    isKnockback.value = false
    emit('animation-end')
  }, duration)
}

// 触发攻击效果
const triggerAttack = (config = {}) => {
  const { skillName = '普通攻击', duration = 800 } = config

  isAttacking.value = true

  // 显示技能名称
  if (skillName) {
    skillNameText.value = skillName
    showSkillName.value = true
    setTimeout(() => {
      showSkillName.value = false
    }, 1500)
  }

  setTimeout(() => {
    isAttacking.value = false
  }, duration)
}

// 被击中组合效果
const triggerHit = () => {
  triggerFlash()
  triggerKnockback()
}

// 暴露方法供父组件调用
defineExpose({
  triggerFlash,
  triggerKnockback,
  triggerAttack,
  triggerHit
})

// 监听元素类型变化，更新光环颜色
watch(() => props.elementType, (newType) => {
  // 更新CSS变量用于光环效果
}, { immediate: true })
</script>

<template>
  <div
    class="sprite-display"
    :class="[elementClass, positionClass, {
      'is-flashing': isFlashing,
      'is-knockback': isKnockback,
      'is-attacking': isAttacking,
      'is-active': isActive,
      'is-dead': !isActive
    }]"
  >
    <!-- 精灵容器 -->
    <div class="sprite-visual">
      <!-- 属性光环（底部） -->
      <div class="element-aura" v-if="isActive"></div>

      <!-- 精灵图片容器 -->
      <div class="image-container">
        <img
          v-if="imageUrl"
          :src="imageUrl"
          :alt="spriteName"
          class="sprite-image"
          loading="eager"
        />
        <div v-else class="sprite-placeholder">
          <span class="placeholder-icon">⚡</span>
        </div>

        <!-- 闪白叠加层 -->
        <div class="flash-overlay" v-if="isFlashing"></div>

        <!-- 暴击特效层 -->
        <div class="critical-burst" v-if="isFlashing && elementType"></div>
      </div>

      <!-- 技能名称弹出 -->
      <div class="skill-name-popup" v-if="showSkillName">
        <span class="skill-name-text">{{ skillNameText }}</span>
      </div>

      <!-- 状态指示 -->
      <div class="status-indicator" v-if="!isActive">
        <span class="status-text">已阵亡</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sprite-display {
  position: relative;
  width: 160px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-sprite);
}

/* 位置区分 */
.player-sprite {
  transform-origin: center right;
}

.enemy-sprite {
  transform-origin: center left;
}

/* 精灵容器 */
.sprite-visual {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 属性光环 */
.element-aura {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 120px;
  height: 30px;
  background: radial-gradient(ellipse at center, var(--element-glow), transparent 60%);
  filter: blur(8px);
  animation: aura-breathe 3s ease-in-out infinite;
  pointer-events: none;
}

@keyframes aura-breathe {
  0%, 100% {
    opacity: 0.6;
    transform: translateX(-50%) scale(1);
  }
  50% {
    opacity: 0.9;
    transform: translateX(-50%) scale(1.15);
  }
}

/* 属性系别光环颜色 */
.sprite-display.element-fire {
  --element-color: var(--fire-primary);
  --element-secondary: var(--fire-secondary);
  --element-glow: var(--fire-glow);
}

.sprite-display.element-water {
  --element-color: var(--water-primary);
  --element-secondary: var(--water-secondary);
  --element-glow: var(--water-glow);
}

.sprite-display.element-grass {
  --element-color: var(--grass-primary);
  --element-secondary: var(--grass-secondary);
  --element-glow: var(--grass-glow);
}

.sprite-display.element-light {
  --element-color: var(--light-primary);
  --element-secondary: var(--light-secondary);
  --element-glow: var(--light-glow);
}

/* 图片容器 */
.image-container {
  position: relative;
  width: 140px;
  height: 140px;
  border-radius: var(--radius-full);
  overflow: hidden;
  border: 3px solid var(--element-color);
  background: rgba(0, 0, 0, 0.4);
  box-shadow:
    0 0 20px var(--element-glow),
    0 4px 16px rgba(0, 0, 0, 0.5);
  transition: all 0.3s ease-out;
}

.is-active .image-container {
  box-shadow:
    0 0 30px var(--element-glow),
    0 0 40px var(--element-glow),
    0 4px 20px rgba(0, 0, 0, 0.6);
}

.is-dead .image-container {
  opacity: 0.4;
  filter: grayscale(1);
  border-color: #666;
  box-shadow: none;
}

/* 精灵图片 */
.sprite-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s ease-out;
}

.sprite-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
}

.placeholder-icon {
  font-size: 48px;
  color: var(--text-muted);
}

/* === 动画效果 === */

/* 闪白效果 */
.flash-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.9);
  animation: flash-burst 0.3s ease-out forwards;
  pointer-events: none;
}

@keyframes flash-burst {
  0% {
    opacity: 0;
  }
  15% {
    opacity: 0.9;
  }
  30% {
    opacity: 0.5;
  }
  100% {
    opacity: 0;
  }
}

.is-flashing .sprite-image {
  filter: brightness(2.5) saturate(0.3);
}

/* 击退效果 */
.is-knockback {
  animation: knockback-shake 0.5s ease-out;
}

.player-sprite.is-knockback {
  animation: knockback-player 0.5s ease-out;
}

.enemy-sprite.is-knockback {
  animation: knockback-enemy 0.5s ease-out;
}

@keyframes knockback-player {
  0% { transform: translateX(0) rotate(0); }
  15% { transform: translateX(-25px) rotate(-5deg); }
  30% { transform: translateX(15px) rotate(3deg); }
  45% { transform: translateX(-10px) rotate(-2deg); }
  60% { transform: translateX(6px) rotate(1deg); }
  100% { transform: translateX(0) rotate(0); }
}

@keyframes knockback-enemy {
  0% { transform: translateX(0) rotate(0); }
  15% { transform: translateX(25px) rotate(5deg); }
  30% { transform: translateX(-15px) rotate(-3deg); }
  45% { transform: translateX(10px) rotate(2deg); }
  60% { transform: translateX(-6px) rotate(-1deg); }
  100% { transform: translateX(0) rotate(0); }
}

/* 攻击冲前效果 */
.is-attacking {
  animation: attack-rush 0.8s ease-out-quart;
}

.player-sprite.is-attacking {
  animation: attack-rush-player 0.8s ease-out-quart;
}

.enemy-sprite.is-attacking {
  animation: attack-rush-enemy 0.8s ease-out-quart;
}

@keyframes attack-rush-player {
  0% { transform: translateX(0) scale(1); }
  20% { transform: translateX(40px) scale(1.08); }
  40% { transform: translateX(70px) scale(1.12); }
  60% { transform: translateX(50px) scale(1.05); }
  100% { transform: translateX(0) scale(1); }
}

@keyframes attack-rush-enemy {
  0% { transform: translateX(0) scale(1); }
  20% { transform: translateX(-40px) scale(1.08); }
  40% { transform: translateX(-70px) scale(1.12); }
  60% { transform: translateX(-50px) scale(1.05); }
  100% { transform: translateX(0) scale(1); }
}

/* 暴击特效 */
.critical-burst {
  position: absolute;
  inset: -20px;
  background: radial-gradient(circle, var(--element-color), transparent 60%);
  opacity: 0.8;
  animation: critical-expand 0.3s ease-out forwards;
  pointer-events: none;
}

@keyframes critical-expand {
  0% {
    opacity: 0;
    transform: scale(0.5);
  }
  30% {
    opacity: 0.8;
    transform: scale(1.2);
  }
  100% {
    opacity: 0;
    transform: scale(1.5);
  }
}

/* 技能名称弹出 */
.skill-name-popup {
  position: absolute;
  bottom: -40px;
  left: 50%;
  transform: translateX(-50%);
  padding: var(--space-sm) var(--space-lg);
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.85), rgba(20, 30, 50, 0.9));
  border: 2px solid var(--element-color);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--element-color);
  white-space: nowrap;
  z-index: var(--z-effect);
  animation: skill-popup 1.5s ease-out forwards;
  box-shadow: 0 0 10px var(--element-glow);
}

@keyframes skill-popup {
  0% {
    opacity: 0;
    transform: translateX(-50%) translateY(15px) scale(0.8);
  }
  20% {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1.1);
  }
  30% {
    transform: translateX(-50%) scale(1);
  }
  80% {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
  100% {
    opacity: 0;
    transform: translateX(-50%) translateY(-10px);
  }
}

/* 状态指示 */
.status-indicator {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  border-radius: var(--radius-full);
}

.status-text {
  font-size: var(--text-sm);
  font-weight: 600;
  color: #ef4444;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

/* 响应式 */
@media (max-width: 480px) {
  .sprite-display {
    width: 120px;
    height: 150px;
  }

  .image-container {
    width: 100px;
    height: 100px;
  }

  .element-aura {
    width: 80px;
    height: 20px;
  }

  .skill-name-popup {
    font-size: var(--text-sm);
    padding: var(--space-xs) var(--space-md);
  }
}
</style>