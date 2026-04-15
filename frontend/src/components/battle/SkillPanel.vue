<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  skills: { type: Array, default: () => [] },
  availableSprites: { type: Array, default: () => [] },
  potions: { type: Array, default: () => [] },
  currentMp: { type: Number, default: 0 },
  currentSpriteId: { type: Number, default: 0 },
  isDisabled: { type: Boolean, default: false },
  countdown: { type: Number, default: 30 }
})

const emit = defineEmits(['attack', 'skill', 'switch', 'item', 'flee'])

// 选择面板状态
const showSkillSelector = ref(false)
const showSpriteSelector = ref(false)
const showPotionSelector = ref(false)

// 禁用状态计算
const isSkillDisabled = computed(() => {
  return props.isDisabled || props.skills.length === 0
})

const isSwitchDisabled = computed(() => {
  return props.isDisabled || props.availableSprites.length <= 1
})

const isItemDisabled = computed(() => {
  return props.isDisabled || props.potions.length === 0
})

// 处理攻击
const handleAttack = () => {
  if (props.isDisabled) return
  emit('attack')
}

// 处理技能选择
const handleSkillSelect = (skill) => {
  if (props.currentMp < skill.costMp) {
    return // MP不足
  }
  emit('skill', skill.id)
  showSkillSelector.value = false
}

// 处理精灵切换
const handleSpriteSelect = (sprite) => {
  if (sprite.id === props.currentSpriteId) return
  emit('switch', sprite.id)
  showSpriteSelector.value = false
}

// 处理药品使用
const handlePotionSelect = (potion) => {
  emit('item', potion.id)
  showPotionSelector.value = false
}

// 处理逃跑
const handleFlee = () => {
  if (props.isDisabled) return
  emit('flee')
}

// 关闭选择面板
const closeAllPanels = () => {
  showSkillSelector.value = false
  showSpriteSelector.value = false
  showPotionSelector.value = false
}
</script>

<template>
  <div class="skill-panel">
    <!-- 倒计时显示 -->
    <div class="countdown-bar" :class="{ 'countdown-warning': countdown <= 10, 'countdown-critical': countdown <= 5 }">
      <div class="countdown-track">
        <div class="countdown-fill" :style="{ width: `${(countdown / 30) * 100}%` }"></div>
      </div>
      <span class="countdown-text">决策时间</span>
      <span class="countdown-number">{{ countdown }}s</span>
    </div>

    <!-- 操作按钮组 -->
    <div class="action-buttons">
      <!-- 普攻按钮 -->
      <button
        class="action-btn attack-btn"
        :disabled="isDisabled"
        @click="handleAttack"
      >
        <div class="btn-icon">
          <span class="icon-emoji">⚔️</span>
        </div>
        <span class="btn-label">普通攻击</span>
      </button>

      <!-- 技能按钮 -->
      <button
        class="action-btn skill-btn"
        :disabled="isSkillDisabled"
        @click="showSkillSelector = true"
      >
        <div class="btn-icon">
          <span class="icon-emoji">✨</span>
        </div>
        <span class="btn-label">技能</span>
        <span class="btn-count" v-if="skills.length">{{ skills.length }}</span>
      </button>

      <!-- 切换精灵按钮 -->
      <button
        class="action-btn switch-btn"
        :disabled="isSwitchDisabled"
        @click="showSpriteSelector = true"
      >
        <div class="btn-icon">
          <span class="icon-emoji">🔄</span>
        </div>
        <span class="btn-label">切换精灵</span>
        <span class="btn-count" v-if="availableSprites.length">{{ availableSprites.length }}</span>
      </button>

      <!-- 使用药品按钮 -->
      <button
        class="action-btn item-btn"
        :disabled="isItemDisabled"
        @click="showPotionSelector = true"
      >
        <div class="btn-icon">
          <span class="icon-emoji">🧪</span>
        </div>
        <span class="btn-label">使用药品</span>
        <span class="btn-count" v-if="potions.length">{{ potions.length }}</span>
      </button>

      <!-- 逃跑按钮 -->
      <button
        class="action-btn flee-btn"
        :disabled="isDisabled"
        @click="handleFlee"
      >
        <div class="btn-icon">
          <span class="icon-emoji">🏃</span>
        </div>
        <span class="btn-label">逃跑</span>
      </button>
    </div>

    <!-- 技能选择面板 -->
    <div class="selector-overlay" v-if="showSkillSelector" @click="showSkillSelector = false">
      <div class="selector-panel skill-selector" @click.stop>
        <div class="selector-header">
          <span class="selector-title">选择技能</span>
          <button class="close-btn" @click="showSkillSelector = false">×</button>
        </div>
        <div class="selector-list">
          <div
            class="selector-item skill-item"
            v-for="skill in skills"
            :key="skill.id"
            :class="{ 'mp-insufficient': currentMp < skill.costMp }"
            @click="handleSkillSelect(skill)"
          >
            <div class="skill-info">
              <span class="skill-name">{{ skill.skillName }}</span>
              <span class="skill-element" :class="`element-${skill.elementType}`">
                {{ ['', '🔥', '💧', '🌿', '✨'][skill.elementType] }}
              </span>
            </div>
            <div class="skill-meta">
              <span class="skill-power">威力: {{ skill.skillDamage }}</span>
              <span class="skill-cost">MP: {{ skill.costMp }}</span>
            </div>
          </div>
          <div class="no-items" v-if="skills.length === 0">
            没有可用的技能
          </div>
        </div>
      </div>
    </div>

    <!-- 精灵选择面板 -->
    <div class="selector-overlay" v-if="showSpriteSelector" @click="showSpriteSelector = false">
      <div class="selector-panel sprite-selector" @click.stop>
        <div class="selector-header">
          <span class="selector-title">选择精灵</span>
          <button class="close-btn" @click="showSpriteSelector = false">×</button>
        </div>
        <div class="selector-list">
          <div
            class="selector-item sprite-item"
            v-for="sprite in availableSprites"
            :key="sprite.id"
            :class="{ 'current-sprite': sprite.id === currentSpriteId, 'is-dead': sprite.hp <= 0 }"
            @click="handleSpriteSelect(sprite)"
          >
            <div class="sprite-avatar">
              <img :src="sprite.imageUrl" :alt="sprite.name" class="sprite-thumb" />
              <span class="status-indicator" v-if="sprite.hp <= 0">已阵亡</span>
            </div>
            <div class="sprite-info">
              <span class="sprite-name">{{ sprite.name }}</span>
              <span class="sprite-level">Lv.{{ sprite.level }}</span>
              <div class="sprite-hp-bar">
                <div class="hp-fill" :style="{ width: `${(sprite.hp / sprite.maxHp) * 100}%` }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 药品选择面板 -->
    <div class="selector-overlay" v-if="showPotionSelector" @click="showPotionSelector = false">
      <div class="selector-panel potion-selector" @click.stop>
        <div class="selector-header">
          <span class="selector-title">选择药品</span>
          <button class="close-btn" @click="showPotionSelector = false">×</button>
        </div>
        <div class="selector-list">
          <div
            class="selector-item potion-item"
            v-for="potion in potions"
            :key="potion.id"
            @click="handlePotionSelect(potion)"
          >
            <div class="potion-icon">
              <img :src="potion.imageUrl" :alt="potion.name" class="potion-thumb" />
            </div>
            <div class="potion-info">
              <span class="potion-name">{{ potion.name }}</span>
              <span class="potion-effect">{{ potion.effect }}</span>
              <span class="potion-count">剩余: {{ potion.count }}</span>
            </div>
          </div>
          <div class="no-items" v-if="potions.length === 0">
            没有可用的药品
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.skill-panel {
  position: relative;
  background: linear-gradient(180deg, rgba(26, 26, 46, 0.95), rgba(22, 33, 62, 0.98));
  border-top: 2px solid var(--border-visible);
  padding: var(--space-lg) var(--space-xl) var(--space-xl);
}

/* 倒计时条 */
.countdown-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  padding: var(--space-sm) var(--space-md);
  background: rgba(0, 0, 0, 0.3);
  border-radius: var(--radius-lg);
}

.countdown-track {
  width: 120px;
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.countdown-fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #4ade80);
  transition: width 1s linear;
}

.countdown-warning .countdown-fill {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.countdown-critical .countdown-fill {
  background: linear-gradient(90deg, #dc2626, #ef4444);
  animation: pulse-warning 0.5s ease-in-out infinite;
}

@keyframes pulse-warning {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.countdown-text {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.countdown-number {
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--text-primary);
}

.countdown-warning .countdown-number {
  color: #fbbf24;
}

.countdown-critical .countdown-number {
  color: #ef4444;
  animation: number-pulse 0.5s ease-in-out infinite;
}

@keyframes number-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* 操作按钮组 */
.action-buttons {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: var(--space-md);
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-lg) var(--space-md);
  background: var(--bg-tertiary);
  border: 2px solid var(--border-visible);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all 0.2s ease-out;
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, var(--btn-glow-color), transparent 60%);
  opacity: 0;
  transition: opacity 0.3s;
}

.action-btn:hover:not(:disabled)::before {
  opacity: 0.3;
}

.action-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--btn-shadow-color);
}

.action-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
}

.action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  border-color: var(--border-subtle);
}

/* 按钮颜色 */
.attack-btn {
  --btn-glow-color: rgba(239, 68, 68, 0.3);
  --btn-shadow-color: rgba(239, 68, 68, 0.3);
  border-color: #ef4444;
}

.attack-btn:hover:not(:disabled) {
  border-color: #dc2626;
  background: rgba(239, 68, 68, 0.15);
}

.skill-btn {
  --btn-glow-color: rgba(59, 130, 246, 0.3);
  --btn-shadow-color: rgba(59, 130, 246, 0.3);
  border-color: #3b82f6;
}

.skill-btn:hover:not(:disabled) {
  border-color: #2563eb;
  background: rgba(59, 130, 246, 0.15);
}

.switch-btn {
  --btn-glow-color: rgba(34, 197, 94, 0.3);
  --btn-shadow-color: rgba(34, 197, 94, 0.3);
  border-color: #22c55e;
}

.switch-btn:hover:not(:disabled) {
  border-color: #16a34a;
  background: rgba(34, 197, 94, 0.15);
}

.item-btn {
  --btn-glow-color: rgba(245, 158, 11, 0.3);
  --btn-shadow-color: rgba(245, 158, 11, 0.3);
  border-color: #f59e0b;
}

.item-btn:hover:not(:disabled) {
  border-color: #d97706;
  background: rgba(245, 158, 11, 0.15);
}

.flee-btn {
  --btn-glow-color: rgba(100, 116, 139, 0.2);
  --btn-shadow-color: rgba(100, 116, 139, 0.2);
  border-color: #64748b;
}

.flee-btn:hover:not(:disabled) {
  border-color: #475569;
  background: rgba(100, 116, 139, 0.15);
}

.btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-bottom: var(--space-sm);
}

.icon-emoji {
  font-size: 24px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}

.btn-label {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.btn-count {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
  font-size: var(--text-xs);
  color: var(--text-secondary);
  background: rgba(0, 0, 0, 0.4);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

/* 选择面板 */
.selector-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: var(--z-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  animation: fade-in 0.2s ease-out;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.selector-panel {
  background: var(--bg-elevated);
  border: 2px solid var(--border-visible);
  border-radius: var(--radius-2xl) var(--radius-2xl) 0 0;
  width: 100%;
  max-width: 500px;
  max-height: 60vh;
  overflow: hidden;
  animation: slide-up 0.3s ease-out;
}

@keyframes slide-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-lg) var(--space-xl);
  border-bottom: 1px solid var(--border-subtle);
  background: rgba(0, 0, 0, 0.2);
}

.selector-title {
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--text-primary);
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: var(--text-primary);
}

.selector-list {
  padding: var(--space-md);
  overflow-y: auto;
  max-height: calc(60vh - 60px);
}

.selector-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-sm);
  cursor: pointer;
  transition: all 0.2s ease-out;
}

.selector-item:hover {
  border-color: var(--border-visible);
  transform: translateX(4px);
}

.selector-item.mp-insufficient {
  opacity: 0.5;
  cursor: not-allowed;
}

.selector-item.mp-insufficient:hover {
  transform: none;
}

/* 技能项 */
.skill-item .skill-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.skill-name {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.skill-element {
  font-size: 16px;
}

.skill-meta {
  display: flex;
  gap: var(--space-md);
}

.skill-power,
.skill-cost {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.mp-insufficient .skill-cost {
  color: #ef4444;
}

/* 精灵项 */
.sprite-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  overflow: hidden;
  position: relative;
}

.sprite-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sprite-item .sprite-info {
  flex: 1;
}

.sprite-info .sprite-name {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.sprite-info .sprite-level {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  margin-left: var(--space-sm);
}

.sprite-hp-bar {
  width: 100%;
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
  margin-top: var(--space-xs);
}

.sprite-hp-bar .hp-fill {
  height: 100%;
  background: #22c55e;
  border-radius: var(--radius-sm);
}

.current-sprite {
  border-color: #3b82f6;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.3);
}

.is-dead {
  opacity: 0.4;
  cursor: not-allowed;
}

.status-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  font-size: 10px;
  color: #ef4444;
  background: rgba(0, 0, 0, 0.7);
  text-align: center;
}

/* 药品项 */
.potion-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.potion-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.potion-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.potion-name {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.potion-effect,
.potion-count {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.no-items {
  text-align: center;
  padding: var(--space-xl);
  color: var(--text-muted);
  font-size: var(--text-sm);
}

/* 响应式 */
@media (max-width: 480px) {
  .action-buttons {
    grid-template-columns: repeat(5, 1fr);
    gap: var(--space-sm);
  }

  .action-btn {
    padding: var(--space-md) var(--space-sm);
  }

  .btn-icon {
    width: 32px;
    height: 32px;
  }

  .icon-emoji {
    font-size: 18px;
  }

  .btn-label {
    font-size: var(--text-xs);
  }

  .selector-panel {
    max-width: 100%;
  }
}
</style>