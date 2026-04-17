<template>
  <nav class="game-top-nav" aria-label="主导航">
    <div class="game-top-nav__inner">
      <button class="game-top-nav__brand" type="button" @click="navigate('/')">
        <span class="brand-mark">洛克王国</span>
        <span class="brand-subtitle">Fantasy Battle Lobby</span>
      </button>

      <div class="game-top-nav__scroll">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          class="game-top-nav__link"
          :class="{ 'is-active': isActive(item.path) }"
          @click="navigate(item.path)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="game-top-nav__utility">
        <slot name="right" />
        <button type="button" class="game-top-nav__logout" @click="logout">
          退出
        </button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({
  activePath: {
    type: String,
    default: ''
  }
})

const route = useRoute()
const router = useRouter()

const navItems = [
  { label: '首页', path: '/' },
  { label: '我的精灵', path: '/elves' },
  { label: '冒险', path: '/pve' },
  { label: '商店', path: '/shop' },
  { label: '背包', path: '/bag' },
  { label: '训练', path: '/train' },
  { label: '排行榜', path: '/rank' },
  { label: '成就', path: '/achievement' },
  { label: 'AI助手', path: '/ai' },
  { label: '聊天', path: '/chat' }
]

const resolvedActivePath = computed(() => props.activePath || route.path)

const isActive = (path) => resolvedActivePath.value === path

const navigate = (path) => {
  if (route.path === path) {
    return
  }
  router.push(path)
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/auth')
}
</script>
