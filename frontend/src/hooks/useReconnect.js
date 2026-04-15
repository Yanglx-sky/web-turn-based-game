import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { battleApi } from '@/api/battle'
import { useBattleStore } from '@/stores/battleStore'

/**
 * 断线重连Hook
 * 处理网络状态监控和战斗恢复
 */
export function useReconnect() {
  const store = useBattleStore()

  // 网络状态
  const isOnline = ref(navigator.onLine)
  const isReconnecting = ref(false)
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 5
  const reconnectDelay = 3000

  // 待恢复战斗
  const hasPendingBattle = ref(false)
  const pendingBattleData = ref(null)

  // 重连状态
  const reconnectStatus = ref('') // '', 'checking', 'found', 'failed', 'success'
  const reconnectMessage = ref('')

  // 检查是否有待恢复的战斗
  const checkPendingBattle = async () => {
    try {
      reconnectStatus.value = 'checking'
      reconnectMessage.value = '检查战斗状态...'

      const response = await battleApi.getCurrentBattle()

      if (response?.code === 200 && response?.data?.battleId) {
        // 存在未完成的战斗
        hasPendingBattle.value = true
        pendingBattleData.value = response.data
        reconnectStatus.value = 'found'
        reconnectMessage.value = '发现未完成的战斗'

        // 保存到store
        store.setPendingBattle(response.data)
        return true
      } else {
        hasPendingBattle.value = false
        pendingBattleData.value = null
        reconnectStatus.value = ''
        store.setPendingBattle(null)
        return false
      }
    } catch (err) {
      console.error('检查战斗状态失败:', err)
      reconnectStatus.value = 'failed'
      reconnectMessage.value = '检查失败'
      return false
    }
  }

  // 恢复战斗
  const resumeBattle = async () => {
    if (!hasPendingBattle.value) return false

    try {
      isReconnecting.value = true
      reconnectStatus.value = 'checking'
      reconnectMessage.value = '正在恢复战斗...'

      // 更新在线状态
      await battleApi.playerOffline()

      // 获取战斗状态
      const response = await battleApi.getCurrentBattle()

      if (response?.code === 200 && response?.data) {
        reconnectStatus.value = 'success'
        reconnectMessage.value = '战斗恢复成功'

        // 初始化战斗状态
        store.initBattle(response.data)

        // 清除待恢复标记
        hasPendingBattle.value = false
        pendingBattleData.value = null
        store.setPendingBattle(null)

        return true
      } else {
        reconnectStatus.value = 'failed'
        reconnectMessage.value = '战斗已结束或不存在'
        return false
      }
    } catch (err) {
      console.error('恢复战斗失败:', err)
      reconnectStatus.value = 'failed'
      reconnectMessage.value = '恢复失败: ' + (err.message || '网络错误')
      return false
    } finally {
      isReconnecting.value = false
    }
  }

  // 放弃战斗
  const abandonBattle = async () => {
    try {
      await battleApi.abandonBattle()
      hasPendingBattle.value = false
      pendingBattleData.value = null
      store.clearBattleState()
      reconnectStatus.value = ''
      return true
    } catch (err) {
      console.error('放弃战斗失败:', err)
      return false
    }
  }

  // 网络状态变化处理
  const handleOnlineChange = () => {
    isOnline.value = navigator.onLine

    if (navigator.onLine) {
      // 网络恢复
      onNetworkRecover()
    } else {
      // 网络断开
      onNetworkLost()
    }
  }

  // 网络断开处理
  const onNetworkLost = async () => {
    console.log('网络断开')
    store.setOnlineStatus(false)

    // 如果正在战斗，记录离线状态
    if (store.battleId) {
      try {
        await battleApi.playerOffline()
      } catch (err) {
        console.warn('记录离线状态失败:', err)
      }
    }
  }

  // 网络恢复处理
  const onNetworkRecover = async () => {
    console.log('网络恢复')
    store.setOnlineStatus(true)

    // 如果有待恢复的战斗，尝试重连
    if (store.battleId && store.status === 3) {
      await attemptReconnect()
    }
  }

  // 尝试重连
  const attemptReconnect = async () => {
    if (reconnectAttempts.value >= maxReconnectAttempts) {
      reconnectStatus.value = 'failed'
      reconnectMessage.value = '重连次数已达上限'
      return false
    }

    isReconnecting.value = true
    reconnectAttempts.value++
    reconnectStatus.value = 'checking'
    reconnectMessage.value = `正在重连 (${reconnectAttempts.value}/${maxReconnectAttempts})...`

    try {
      const success = await resumeBattle()

      if (success) {
        reconnectAttempts.value = 0
        return true
      } else {
        // 失败后延迟重试
        if (reconnectAttempts.value < maxReconnectAttempts) {
          await new Promise(resolve => setTimeout(resolve, reconnectDelay))
          return attemptReconnect()
        }
        return false
      }
    } catch (err) {
      console.error('重连失败:', err)
      if (reconnectAttempts.value < maxReconnectAttempts) {
        await new Promise(resolve => setTimeout(resolve, reconnectDelay))
        return attemptReconnect()
      }
      return false
    } finally {
      isReconnecting.value = false
    }
  }

  // 页面关闭前保存战斗状态
  const handleBeforeUnload = (e) => {
    if (store.battleId && store.status === 0) {
      // 有未完成的战斗
      battleApi.playerOffline().catch(() => {})

      // 提示用户
      e.preventDefault()
      e.returnValue = '战斗尚未结束，确定要离开吗？'
      return e.returnValue
    }
  }

  // 监听网络状态
  onMounted(() => {
    window.addEventListener('online', handleOnlineChange)
    window.addEventListener('offline', handleOnlineChange)
    window.addEventListener('beforeunload', handleBeforeUnload)

    // 初始检查
    checkPendingBattle()
  })

  // 清理
  onUnmounted(() => {
    window.removeEventListener('online', handleOnlineChange)
    window.removeEventListener('offline', handleOnlineChange)
    window.removeEventListener('beforeunload', handleBeforeUnload)
  })

  // 计算属性
  const showReconnectDialog = computed(() => {
    return hasPendingBattle.value && !isReconnecting.value
  })

  return {
    // 状态
    isOnline,
    isReconnecting,
    reconnectAttempts,
    hasPendingBattle,
    pendingBattleData,
    reconnectStatus,
    reconnectMessage,
    showReconnectDialog,

    // 方法
    checkPendingBattle,
    resumeBattle,
    abandonBattle,
    attemptReconnect,
    handleOnlineChange
  }
}