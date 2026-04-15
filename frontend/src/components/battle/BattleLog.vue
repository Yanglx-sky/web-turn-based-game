<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  logs: { type: Array, default: () => [] },
  maxVisible: { type: Number, default: 5 },
  autoScroll: { type: Boolean, default: true }
})

const emit = defineEmits(['log-click'])

const logContainerRef = ref(null)
const visibleLogs = computed(() => props.logs.slice(-props.maxVisible))

// 监听logs变化，自动滚动到底部
watch(() => props.logs.length, () => {
  if (props.autoScroll && logContainerRef.value) {
    setTimeout(() => {
      logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
    }, 50)
  }
})

// 解析日志中的伤害数字
const parseDamageNumber = (text) => {
  const damageMatch = text.match(/造成了(\d+)点伤害/)
  if (damageMatch) {
    return { hasDamage: true, damage: damageMatch[1] }
  }
  return { hasDamage: false }
}

// 点击日志项
const handleLogClick = (log, index) => {
  emit('log-click', { log, index })
}
</script>

<template>
  <div class="battle-log">
    <div class="log-header">
      <span class="log-title">战斗记录</span>
      <span class="log-count" v-if="logs.length">{{ logs.length }}条</span>
    </div>

    <div class="log-container" ref="logContainerRef">
      <div
        class="log-item"
        v-for="(log, index) in visibleLogs"
        :key="`${index}-${log}`"
        :class="getLogClass(log)"
        @click="handleLogClick(log, index)"
      >
        <!-- 日志图标 -->
        <div class="log-icon" :class="getLogIconClass(log)">
          <span class="icon-emoji">{{ getLogIcon(log) }}</span>
        </div>

        <!-- 日志文本 -->
        <div class="log-text">
          <span class="text-main">{{ log }}</span>
          <!-- 伤害数字高亮 -->
          <span
            class="damage-highlight"
            v-if="parseDamageNumber(log).hasDamage"
          >
            -{{ parseDamageNumber(log).damage }}
          </span>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="log-empty" v-if="logs.length === 0">
        战斗即将开始...
      </div>
    </div>
  </div>
</template>

<script>
// 辅助函数在script setup外部定义
function getLogClass(log) {
  if (log.includes('胜利')) return 'log-victory'
  if (log.includes('失败') || log.includes('逃跑')) return 'log-defeat'
  if (log.includes('造成了')) return 'log-damage'
  if (log.includes('使用') && log.includes('技能')) return 'log-skill'
  return ''
}

function getLogIconClass(log) {
  if (log.includes('胜利')) return 'icon-victory'
  if (log.includes('失败')) return 'icon-defeat'
  if (log.includes('造成了')) return 'icon-attack'
  if (log.includes('技能')) return 'icon-skill'
  return ''
}

function getLogIcon(log) {
  if (log.includes('胜利')) return '🏆'
  if (log.includes('失败') || log.includes('逃跑')) return '💔'
  if (log.includes('造成了')) return '💥'
  if (log.includes('技能')) return '✨'
  if (log.includes('切换')) return '🔄'
  if (log.includes('使用') && log.includes('药品')) return '🧪'
  return '📝'
}
</script>

<style scoped>
.battle-log {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) var(--space-md);
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid var(--border-subtle);
}

.log-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-secondary);
}

.log-count {
  font-size: var(--text-xs);
  color: var(--text-muted);
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.log-container {
  max-height: 120px;
  overflow-y: auto;
  padding: var(--space-sm);
}

.log-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xs) var(--space-sm);
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-xs);
  animation: log-slide-in 0.3s ease-out;
  cursor: default;
  transition: background 0.2s;
}

.log-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

@keyframes log-slide-in {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.log-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-emoji {
  font-size: 14px;
}

.log-victory {
  background: rgba(34, 197, 94, 0.1);
  border-left: 2px solid #22c55e;
}

.log-defeat {
  background: rgba(239, 68, 68, 0.1);
  border-left: 2px solid #ef4444;
}

.log-damage {
  border-left: 2px solid #fbbf24;
}

.log-skill {
  border-left: 2px solid #3b82f6;
}

.icon-victory .icon-emoji {
  color: #22c55e;
}

.icon-defeat .icon-emoji {
  color: #ef4444;
}

.log-text {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.text-main {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.4;
}

.damage-highlight {
  font-size: var(--text-sm);
  font-weight: 700;
  color: #ef4444;
  padding: 2px 4px;
  background: rgba(239, 68, 68, 0.2);
  border-radius: var(--radius-sm);
}

.log-empty {
  text-align: center;
  padding: var(--space-md);
  color: var(--text-muted);
  font-size: var(--text-sm);
}

/* 滚动条样式 */
.log-container::-webkit-scrollbar {
  width: 4px;
}

.log-container::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.log-container::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}
</style>