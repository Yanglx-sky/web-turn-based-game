<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  type: { type: String, default: 'start' }, // 'start', 'victory', 'defeat'
  duration: { type: Number, default: 2000 },
  resultData: { type: Object, default: null }
})

const emit = defineEmits(['close'])

const showPopup = ref(false)
const animationPhase = ref('enter')

// 图片路径
const popupImages = {
  start: '/src/assets/photo/battle/开始战斗.jpeg',
  victory: '/src/assets/photo/battle/胜负已分.jpeg',
  defeat: '/src/assets/photo/battle/胜负已分.jpeg'
}

// 标题文本
const popupTitles = {
  start: '战斗开始！',
  victory: '胜利！',
  defeat: '失败...'
}

// 监听visible变化
watch(() => props.visible, (newVal) => {
  if (newVal) {
    showPopup.value = true
    animationPhase.value = 'enter'

    // 自动关闭
    setTimeout(() => {
      animationPhase.value = 'exit'
      setTimeout(() => {
        showPopup.value = false
        emit('close')
      }, 400)
    }, props.duration)
  }
})

// 手动关闭
const handleClose = () => {
  animationPhase.value = 'exit'
  setTimeout(() => {
    showPopup.value = false
    emit('close')
  }, 400)
}
</script>

<template>
  <div class="battle-popup-overlay" v-if="showPopup" @click="handleClose">
    <div
      class="battle-popup"
      :class="[`popup-${type}`, `phase-${animationPhase}`]"
      @click.stop
    >
      <!-- 背景光效 -->
      <div class="popup-glow" :class="`glow-${type}`"></div>

      <!-- 图片 -->
      <div class="popup-image-container">
        <img
          :src="popupImages[type]"
          :alt="popupTitles[type]"
          class="popup-image"
        />
      </div>

      <!-- 标题 -->
      <div class="popup-title">
        <span class="title-text" :class="`text-${type}`">{{ popupTitles[type] }}</span>
      </div>

      <!-- 结果数据 -->
      <div class="result-info" v-if="type === 'victory' && resultData">
        <div class="result-item">
          <span class="result-label">获得经验</span>
          <span class="result-value">+{{ resultData.expReward }}</span>
        </div>
        <div class="result-item">
          <span class="result-label">获得金币</span>
          <span class="result-value">+{{ resultData.goldReward }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.battle-popup-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: var(--z-overlay);
  display: flex;
  align-items: center;
  justify-content: center;
}

.battle-popup {
  position: relative;
  width: 320px;
  max-width: 90vw;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95), rgba(22, 33, 62, 0.98));
  border: 3px solid var(--border-visible);
  border-radius: var(--radius-2xl);
  padding: var(--space-xl);
  text-align: center;
  overflow: hidden;
}

/* 光效背景 */
.popup-glow {
  position: absolute;
  inset: -50px;
  z-index: 0;
  pointer-events: none;
}

.glow-start {
  background: radial-gradient(ellipse at center, rgba(59, 130, 246, 0.3), transparent 60%);
}

.glow-victory {
  background: radial-gradient(ellipse at center, rgba(34, 197, 94, 0.4), transparent 60%);
  animation: victory-glow 1s ease-out infinite;
}

.glow-defeat {
  background: radial-gradient(ellipse at center, rgba(239, 68, 68, 0.3), transparent 60%);
}

@keyframes victory-glow {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* 图片容器 */
.popup-image-container {
  position: relative;
  width: 200px;
  height: 150px;
  margin: 0 auto var(--space-lg);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 2px solid var(--border-visible);
  z-index: 1;
}

.popup-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 标题 */
.popup-title {
  position: relative;
  z-index: 1;
  margin-bottom: var(--space-md);
}

.title-text {
  font-size: var(--text-2xl);
  font-weight: 900;
  letter-spacing: 4px;
}

.text-start {
  color: var(--water-primary);
  text-shadow: 0 0 20px rgba(76, 201, 240, 0.5);
}

.text-victory {
  color: var(--grass-primary);
  text-shadow: 0 0 20px rgba(34, 197, 94, 0.5);
  animation: victory-text 0.5s ease-out infinite;
}

.text-defeat {
  color: var(--fire-primary);
  text-shadow: 0 0 20px rgba(239, 68, 68, 0.5);
}

@keyframes victory-text {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* 结果数据 */
.result-info {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  gap: var(--space-xl);
  padding: var(--space-md);
  background: rgba(0, 0, 0, 0.3);
  border-radius: var(--radius-lg);
}

.result-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.result-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.result-value {
  font-size: var(--text-lg);
  font-weight: 700;
  color: #fbbf24;
}

/* 动画阶段 */
.phase-enter {
  animation: popup-enter 0.8s ease-out forwards;
}

.phase-exit {
  animation: popup-exit 0.4s ease-in forwards;
}

@keyframes popup-enter {
  0% {
    opacity: 0;
    transform: scale(0.5) translateY(50px);
  }
  50% {
    opacity: 1;
    transform: scale(1.1) translateY(0);
  }
  70% {
    transform: scale(1.05);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes popup-exit {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  100% {
    opacity: 0;
    transform: scale(0.9);
  }
}

/* 响应式 */
@media (max-width: 480px) {
  .battle-popup {
    width: 280px;
    padding: var(--space-lg);
  }

  .popup-image-container {
    width: 160px;
    height: 120px;
  }

  .title-text {
    font-size: var(--text-xl);
  }
}
</style>