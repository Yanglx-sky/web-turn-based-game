<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'

const props = defineProps({
  active: { type: Boolean, default: false },
  elementType: { type: Number, default: 1 },
  fromPosition: { type: Object, default: () => ({ x: 0, y: 0 }) },
  toPosition: { type: Object, default: () => ({ x: 0, y: 0 }) },
  skillName: { type: String, default: '' }
})

const emit = defineEmits(['effect-end'])

// Canvas引用
const canvasRef = ref(null)
const animationId = ref(null)
const particles = ref([])
const isPlaying = ref(false)

// 属性颜色配置
const elementColors = computed(() => {
  const configs = {
    1: { // 火系 - 炽热橙红
      primary: '#ff6b35',
      secondary: '#f7931e',
      glow: '#ff4500',
      particles: ['#ff6b35', '#f7931e', '#ff4500', '#ffa500', '#ff8c00']
    },
    2: { // 水系 - 深蓝
      primary: '#4cc9f0',
      secondary: '#4895ef',
      glow: '#00bfff',
      particles: ['#4cc9f0', '#4895ef', '#00bfff', '#87ceeb', '#1e90ff']
    },
    3: { // 草系 - 自然绿
      primary: '#7cb518',
      secondary: '#5fa8d3',
      glow: '#32cd32',
      particles: ['#7cb518', '#32cd32', '#228b22', '#90ee90', '#00fa9a']
    },
    4: { // 光系 - 神圣白金
      primary: '#f0e6d3',
      secondary: '#d4af37',
      glow: '#ffd700',
      particles: ['#f0e6d3', '#d4af37', '#ffd700', '#fff8dc', '#fffacd']
    }
  }
  return configs[props.elementType] || configs[1]
})

// 粒子类
class Particle {
  constructor(x, y, targetX, targetY, color, type) {
    this.x = x
    this.y = y
    this.targetX = targetX
    this.targetY = targetY
    this.color = color
    this.type = type

    // 根据类型设置不同参数
    this.setupByType()
  }

  setupByType() {
    const dx = this.targetX - this.x
    const dy = this.targetY - this.y
    const distance = Math.sqrt(dx * dx + dy * dy)

    switch (this.type) {
      case 'fire': // 火球 - 随机抖动
        this.vx = (dx / distance) * 8 + (Math.random() - 0.5) * 4
        this.vy = (dy / distance) * 8 + (Math.random() - 0.5) * 4
        this.size = Math.random() * 12 + 6
        this.life = 60
        this.decay = 1.5
        break

      case 'water': // 水龙 - 螺旋轨迹
        this.angle = Math.random() * Math.PI * 2
        this.speed = Math.random() * 3 + 2
        this.radius = Math.random() * 30 + 10
        this.progress = 0
        this.vx = dx / distance * 6
        this.vy = dy / distance * 6
        this.size = Math.random() * 8 + 4
        this.life = 80
        this.decay = 1
        break

      case 'grass': // 草系 - 跟踪曲线
        this.vx = (dx / distance) * 5
        this.vy = (dy / distance) * 5
        this.curve = (Math.random() - 0.5) * 0.3
        this.size = Math.random() * 10 + 5
        this.life = 70
        this.decay = 1.2
        break

      case 'light': // 光系 - 直线光束
        this.vx = (dx / distance) * 12
        this.vy = (dy / distance) * 12
        this.size = Math.random() * 6 + 3
        this.life = 40
        this.decay = 2
        this.trail = []
        break

      default:
        this.vx = (dx / distance) * 6
        this.vy = (dy / distance) * 6
        this.size = Math.random() * 8 + 4
        this.life = 50
        this.decay = 1
    }
  }

  update(type) {
    this.life -= this.decay

    switch (type) {
      case 'fire':
        this.x += this.vx
        this.y += this.vy
        this.vx *= 0.98
        this.vy *= 0.98
        this.vx += (Math.random() - 0.5) * 0.5
        this.vy += (Math.random() - 0.5) * 0.5
        this.size *= 0.97
        break

      case 'water':
        this.progress += 0.02
        this.angle += this.speed * 0.1
        const spiralX = this.x + Math.cos(this.angle) * this.radius * Math.sin(this.progress)
        const spiralY = this.y + Math.sin(this.angle) * this.radius * Math.sin(this.progress)
        this.x = spiralX + this.vx
        this.y = spiralY + this.vy
        break

      case 'grass':
        this.x += this.vx
        this.y += this.vy
        this.vy += this.curve * Math.sin(this.life * 0.1)
        break

      case 'light':
        if (this.trail.length < 5) {
          this.trail.push({ x: this.x, y: this.y, size: this.size })
        }
        this.x += this.vx
        this.y += this.vy
        break

      default:
        this.x += this.vx
        this.y += this.vy
    }
  }

  draw(ctx, type) {
    ctx.beginPath()

    switch (type) {
      case 'fire':
        // 火焰渐变
        const gradient = ctx.createRadialGradient(this.x, this.y, 0, this.x, this.y, this.size)
        gradient.addColorStop(0, this.color)
        gradient.addColorStop(0.5, this.color + '80')
        gradient.addColorStop(1, 'transparent')
        ctx.fillStyle = gradient
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
        ctx.fill()
        break

      case 'water':
        // 水滴形状
        ctx.fillStyle = this.color + Math.floor(this.life * 2).toString(16)
        ctx.ellipse(this.x, this.y, this.size, this.size * 1.5, Math.PI / 4, 0, Math.PI * 2)
        ctx.fill()
        break

      case 'grass':
        // 叶片形状
        ctx.fillStyle = this.color
        ctx.moveTo(this.x, this.y)
        ctx.lineTo(this.x + this.size, this.y - this.size / 2)
        ctx.lineTo(this.x + this.size / 2, this.y - this.size)
        ctx.lineTo(this.x - this.size / 2, this.y - this.size / 2)
        ctx.closePath()
        ctx.fill()
        break

      case 'light':
        // 光束+尾迹
        ctx.fillStyle = this.color
        ctx.shadowColor = this.color
        ctx.shadowBlur = 10
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
        ctx.fill()
        ctx.shadowBlur = 0

        // 尾迹
        this.trail.forEach((t, i) => {
          ctx.fillStyle = this.color + (Math.floor(255 * (i / this.trail.length) * 0.5)).toString(16).padStart(2, '0')
          ctx.arc(t.x, t.y, t.size * (i / this.trail.length), 0, Math.PI * 2)
          ctx.fill()
        })
        break

      default:
        ctx.fillStyle = this.color
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
        ctx.fill()
    }
  }

  isAlive() {
    return this.life > 0 && this.size > 0.5
  }
}

// 获取技能类型
const getSkillType = () => {
  const types = { 1: 'fire', 2: 'water', 3: 'grass', 4: 'light' }
  return types[props.elementType] || 'fire'
}

// 创建粒子
const createParticles = () => {
  const colors = elementColors.value.particles
  const type = getSkillType()
  const count = type === 'light' ? 20 : 35

  for (let i = 0; i < count; i++) {
    const particle = new Particle(
      props.fromPosition.x,
      props.fromPosition.y,
      props.toPosition.x,
      props.toPosition.y,
      colors[Math.floor(Math.random() * colors.length)],
      type
    )
    particles.value.push(particle)
  }
}

// 创建爆炸效果
const createExplosion = () => {
  const colors = elementColors.value.particles
  const type = getSkillType()

  for (let i = 0; i < 20; i++) {
    const angle = (Math.PI * 2 / 20) * i
    const speed = Math.random() * 8 + 4
    const particle = {
      x: props.toPosition.x,
      y: props.toPosition.y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      color: colors[Math.floor(Math.random() * colors.length)],
      size: Math.random() * 8 + 4,
      life: 30,
      type: type
    }
    particles.value.push(particle)
  }
}

// 动画循环
const animate = () => {
  if (!canvasRef.value) return

  const ctx = canvasRef.value.getContext('2d')
  const type = getSkillType()

  // 清空画布
  ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)

  // 更新和绘制粒子
  particles.value = particles.value.filter(p => {
    if (p instanceof Particle) {
      p.update(type)
      p.draw(ctx, type)
      return p.isAlive()
    } else {
      // 爆炸粒子
      p.x += p.vx
      p.y += p.vy
      p.vx *= 0.95
      p.vy *= 0.95
      p.life -= 2
      p.size *= 0.95

      if (p.life > 0) {
        ctx.beginPath()
        ctx.fillStyle = p.color + Math.floor(p.life * 4).toString(16).padStart(2, '0')
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
        ctx.fill()
        return true
      }
      return false
    }
  })

  // 检查是否需要爆炸
  if (isPlaying.value && particles.value.length > 0) {
    const hasReachedTarget = particles.value.some(p => {
      if (p instanceof Particle) {
        const dx = Math.abs(p.x - props.toPosition.x)
        const dy = Math.abs(p.y - props.toPosition.y)
        return dx < 50 && dy < 50 && p.life < 30
      }
      return false
    })

    if (hasReachedTarget && particles.value.filter(p => p.type !== 'explosion').length > 0) {
      createExplosion()
      setTimeout(() => {
        emit('effect-end')
      }, 300)
    }
  }

  if (particles.value.length > 0) {
    animationId.value = requestAnimationFrame(animate)
  } else {
    isPlaying.value = false
    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  }
}

// 播放效果
const playEffect = () => {
  if (isPlaying.value) return

  isPlaying.value = true
  particles.value = []
  createParticles()
  animate()
}

// 停止效果
const stopEffect = () => {
  isPlaying.value = false
  particles.value = []
  if (animationId.value) {
    cancelAnimationFrame(animationId.value)
    animationId.value = null
  }
  if (canvasRef.value) {
    const ctx = canvasRef.value.getContext('2d')
    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  }
}

// 监听active变化
watch(() => props.active, (newVal) => {
  if (newVal) {
    playEffect()
  } else {
    stopEffect()
  }
})

// 生命周期
onMounted(() => {
  if (canvasRef.value) {
    // 设置canvas尺寸
    const parent = canvasRef.value.parentElement
    if (parent) {
      canvasRef.value.width = parent.offsetWidth || 800
      canvasRef.value.height = parent.offsetHeight || 400
    }
  }
})

onUnmounted(() => {
  stopEffect()
})

// 暴露方法
defineExpose({
  playEffect,
  stopEffect
})
</script>

<template>
  <canvas
    ref="canvasRef"
    class="skill-effect-canvas"
    :class="`effect-type-${elementType}`"
  ></canvas>
</template>

<style scoped>
.skill-effect-canvas {
  position: absolute;
  inset: 0;
  z-index: var(--z-effect);
  pointer-events: none;
}

.effect-type-1 {
  /* 火系特效 - 暖色调 */
}

.effect-type-2 {
  /* 水系特效 - 冷色调 */
}

.effect-type-3 {
  /* 草系特效 - 绿色调 */
}

.effect-type-4 {
  /* 光系特效 - 金色调 */
}
</style>