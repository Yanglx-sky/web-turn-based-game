<script setup>
import { ref, watch, computed, onUnmounted } from 'vue'

const props = defineProps({
  initialTime: { type: Number, default: 30 },
  isActive: { type: Boolean, default: true },
  warningThreshold: { type: Number, default: 10 },
  criticalThreshold: { type: Number, default: 5 }
})

const emit = defineEmits(['timeout', 'tick'])

const currentTime = ref(props.initialTime)
const isRunning = ref(false)
const timerId = ref(null)

// 状态计算
const percentRemaining = computed(() => {
  return (currentTime.value / props.initialTime) * 100
})

const isWarning = computed(() => {
  return currentTime.value <= props.warningThreshold && currentTime.value > props.criticalThreshold
})

const isCritical = computed(() => {
  return currentTime.value <= props.criticalThreshold
})

const timerClass = computed(() => {
  if (isCritical.value) return 'timer-critical'
  if (isWarning.value) return 'timer-warning'
  return ''
})

// 开始计时
const start = () => {
  if (isRunning.value || !props.isActive) return

  isRunning.value = true
  timerId.value = setInterval(() => {
    if (currentTime.value > 0) {
      currentTime.value--
      emit('tick', currentTime.value)

      // 触发数字跳动效果
      triggerTickAnimation()
    } else {
      stop()
      emit('timeout')
    }
  }, 1000)
}

// 停止计时
const stop = () => {
  isRunning.value = false
  if (timerId.value) {
    clearInterval(timerId.value)
    timerId.value = null
  }
}

// 重置计时
const reset = (newTime = props.initialTime) => {
  stop()
  currentTime.value = newTime
}

// 暂停
const pause = () => {
  stop()
}

// 继续
const resume = () => {
  if (currentTime.value > 0) {
    start()
  }
}

// 数字跳动效果
const tickAnimation = ref(false)
const triggerTickAnimation = () => {
  tickAnimation.value = true
  setTimeout(() => {
    tickAnimation.value = false
  }, 200)
}

// 监听isActive变化
watch(() => props.isActive, (newVal) => {
  if (newVal) {
    start()
  } else {
    stop()
  }
})

// 清理
onUnmounted(() => {
  stop()
})

// 暴露方法
defineExpose({
  start,
  stop,
  reset,
  pause,
  resume,
  currentTime: computed(() => currentTime.value)
})
</script>

<template>
  <div class="countdown-timer" :class="timerClass">
    <!-- 进度条 -->
    <div class="timer-track">
      <div
        class="timer-fill"
        :style="{ width: `${percentRemaining}%` }"
      ></div>
    </div>

    <!-- 数字显示 -->
    <div class="timer-display" :class="{ 'tick-pulse': tickAnimation }">
      <span class="timer-number">{{ currentTime }}</span>
      <span class="timer-unit">秒</span>
    </div>

    <!-- 状态文字 -->
    <div class="timer-status">
      <span class="status-text" v-if="isCritical">紧急!</span>
      <span class="status-text" v-else-if="isWarning">注意!</span>
      <span class="status-text" v-else>决策时间</span>
    </div>
  </div>
</template>

<style scoped>
.countdown-timer {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
}

/* 进度条 */
.timer-track {
  width: 80px;
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.timer-fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #4ade80);
  border-radius: var(--radius-sm);
  transition: width 1s linear;
}

.timer-warning .timer-fill {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.timer-critical .timer-fill {
  background: linear-gradient(90deg, #dc2626, #ef4444);
  animation: critical-pulse 0.5s ease-in-out infinite;
}

@keyframes critical-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* 数字显示 */
.timer-display {
  display: flex;
  align-items: baseline;
  gap: var(--space-xs);
}

.tick-pulse {
  animation: tick-jump 0.2s ease-out;
}

@keyframes tick-jump {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.timer-number {
  font-size: var(--text-xl);
  font-weight: 800;
  color: var(--text-primary);
  min-width: 24px;
  text-align: right;
}

.timer-warning .timer-number {
  color: #fbbf24;
}

.timer-critical .timer-number {
  color: #ef4444;
  animation: number-shake 0.3s ease-in-out infinite;
}

@keyframes number-shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-2px); }
  75% { transform: translateX(2px); }
}

.timer-unit {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

/* 状态文字 */
.timer-status {
  flex: 1;
}

.status-text {
  font-size: var(--text-sm);
  color: var(--text-muted);
}

.timer-warning .status-text {
  color: #fbbf24;
  font-weight: 600;
}

.timer-critical .status-text {
  color: #ef4444;
  font-weight: 700;
  animation: text-flash 0.5s ease-in-out infinite;
}

@keyframes text-flash {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 响应式 */
@media (max-width: 480px) {
  .countdown-timer {
    padding: var(--space-sm) var(--space-md);
  }

  .timer-track {
    width: 60px;
    height: 6px;
  }

  .timer-number {
    font-size: var(--text-lg);
  }
}
</style>