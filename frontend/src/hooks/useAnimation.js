import { ref, computed } from 'vue'

/**
 * 动画控制Hook
 * 处理战斗中的动画触发逻辑
 */
export function useAnimation() {
  // 动画状态
  const isFlashing = ref(false)
  const isKnockback = ref(false)
  const isAttacking = ref(false)
  const isSkillCasting = ref(false)
  const activeEffect = ref(null)
  const damageNumber = ref(null)
  const skillNamePopup = ref(null)

  // 动画队列
  const animationQueue = ref([])
  const isPlaying = ref(false)

  // 当前动画配置
  const currentAnimation = ref({
    type: '', // 'attack', 'skill', 'hit', 'switch'
    attacker: '', // 'player', 'enemy'
    skillName: '',
    damage: 0,
    duration: 800
  })

  // 触发闪白效果
  const triggerFlash = (duration = 300) => {
    isFlashing.value = true
    return new Promise(resolve => {
      setTimeout(() => {
        isFlashing.value = false
        resolve()
      }, duration)
    })
  }

  // 触发击退效果
  const triggerKnockback = (duration = 500) => {
    isKnockback.value = true
    return new Promise(resolve => {
      setTimeout(() => {
        isKnockback.value = false
        resolve()
      }, duration)
    })
  }

  // 触发攻击效果
  const triggerAttack = (config = {}) => {
    const {
      attacker = 'player',
      skillName = '普通攻击',
      duration = 800,
      onHit = null
    } = config

    isAttacking.value = true
    skillNamePopup.value = skillName

    // 技能施放效果
    if (skillName !== '普通攻击') {
      isSkillCasting.value = true
    }

    // 攻击动画持续时间
    const hitTime = duration * 0.3

    return new Promise(resolve => {
      setTimeout(() => {
        // 触发受击回调
        if (onHit) {
          onHit()
        }

        setTimeout(() => {
          isAttacking.value = false
          isSkillCasting.value = false
          skillNamePopup.value = null
          resolve()
        }, duration - hitTime)
      }, hitTime)
    })
  }

  // 触发受击效果（组合闪白+击退）
  const triggerHit = (config = {}) => {
    const {
      damage = 0,
      isCritical = false,
      duration = 500
    } = config

    // 显示伤害数字
    if (damage > 0) {
      damageNumber.value = {
        value: damage,
        isCritical,
        visible: true
      }
    }

    // 并行执行闪白和击退
    return Promise.all([
      triggerFlash(300),
      triggerKnockback(duration)
    ]).then(() => {
      // 隐藏伤害数字
      setTimeout(() => {
        damageNumber.value = null
      }, 500)
    })
  }

  // 触发技能特效
  const triggerSkillEffect = (elementType, fromPos, toPos) => {
    activeEffect.value = {
      elementType,
      fromPosition: fromPos,
      toPosition: toPos,
      active: true
    }

    return new Promise(resolve => {
      setTimeout(() => {
        activeEffect.value = null
        resolve()
      }, 1000)
    })
  }

  // 触发精灵切换效果
  const triggerSwitch = (duration = 600) => {
    return new Promise(resolve => {
      // 消失动画
      setTimeout(() => {
        // 出现动画
        setTimeout(resolve, duration)
      }, duration * 0.4)
    })
  }

  // 执行完整攻击序列
  const executeAttackSequence = async (config) => {
    if (isPlaying.value) return

    isPlaying.value = true

    const {
      attacker,
      skillName,
      elementType,
      damage,
      isCritical,
      fromPosition,
      toPosition
    } = config

    try {
      // 1. 攻击方动画
      await triggerAttack({
        attacker,
        skillName,
        onHit: async () => {
          // 2. 技能粒子特效（如果是技能）
          if (elementType && skillName !== '普通攻击') {
            await triggerSkillEffect(elementType, fromPosition, toPosition)
          }

          // 3. 受击方效果
          await triggerHit({ damage, isCritical })
        }
      })
    } finally {
      isPlaying.value = false
    }
  }

  // 添加动画到队列
  const queueAnimation = (config) => {
    animationQueue.value.push(config)
    if (!isPlaying.value) {
      playNextAnimation()
    }
  }

  // 播放下一个动画
  const playNextAnimation = async () => {
    if (animationQueue.value.length === 0) {
      isPlaying.value = false
      return
    }

    isPlaying.value = true
    const config = animationQueue.value.shift()
    await executeAttackSequence(config)
    playNextAnimation()
  }

  // 清空动画队列
  const clearQueue = () => {
    animationQueue.value = []
    isPlaying.value = false
    isFlashing.value = false
    isKnockback.value = false
    isAttacking.value = false
    isSkillCasting.value = false
    activeEffect.value = null
    damageNumber.value = null
    skillNamePopup.value = null
  }

  return {
    // 状态
    isFlashing,
    isKnockback,
    isAttacking,
    isSkillCasting,
    activeEffect,
    damageNumber,
    skillNamePopup,
    isPlaying,
    animationQueue,
    currentAnimation,

    // 方法
    triggerFlash,
    triggerKnockback,
    triggerAttack,
    triggerHit,
    triggerSkillEffect,
    triggerSwitch,
    executeAttackSequence,
    queueAnimation,
    playNextAnimation,
    clearQueue
  }
}