import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Auth from '../views/Auth.vue'
import ElfDetail from '../views/ElfDetail.vue'
import Battle from '../views/Battle.vue'
import Elves from '../views/Elves.vue'
import PVE from '../views/PVE.vue'
import Shop from '../views/Shop.vue'
import Bag from '../views/Bag.vue'
import Train from '../views/Train.vue'
import Rank from '../views/Rank.vue'
import Achievement from '../views/Achievement.vue'
import AI from '../views/AI.vue'
import Chat from '../views/Chat.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/auth',
    name: 'Auth',
    component: Auth
  },
  {
    path: '/elf/:id',
    name: 'ElfDetail',
    component: ElfDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/battle/:id',
    name: 'Battle',
    component: Battle,
    meta: { requiresAuth: true }
  },
  {
    path: '/battle',
    name: 'BattleTrain',
    component: Battle,
    meta: { requiresAuth: true }
  },
  {
    path: '/elves',
    name: 'Elves',
    component: Elves,
    meta: { requiresAuth: true }
  },
  {
    path: '/pve',
    name: 'PVE',
    component: PVE,
    meta: { requiresAuth: true }
  },
  {
    path: '/shop',
    name: 'Shop',
    component: Shop,
    meta: { requiresAuth: true }
  },
  {
    path: '/bag',
    name: 'Bag',
    component: Bag,
    meta: { requiresAuth: true }
  },
  {
    path: '/train',
    name: 'Train',
    component: Train,
    meta: { requiresAuth: true }
  },
  {
    path: '/rank',
    name: 'Rank',
    component: Rank,
    meta: { requiresAuth: true }
  },
  {
    path: '/achievement',
    name: 'Achievement',
    component: Achievement,
    meta: { requiresAuth: true }
  },
  {
    path: '/ai',
    name: 'AI',
    component: AI,
    meta: { requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: Chat,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from) => {
  const requiresAuth = to.meta.requiresAuth
  const user = localStorage.getItem('user')
  
  if (requiresAuth && !user) {
    return '/auth'
  }
  return true
})

export default router