import { useBattleStore } from '../stores/battleStore'
import { battleApi } from '../api/battle'

// 网络状态管理
class NetworkManager {
  constructor() {
    this.battleStore = null
    this.isOnline = navigator.onLine
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 1000
    this.offlineTimer = null
  }
  
  // 初始化网络监听
  init() {
    // 获取battleStore实例
    this.battleStore = useBattleStore()
    
    // 监听在线状态
    window.addEventListener('online', this.handleOnline.bind(this))
    // 监听离线状态
    window.addEventListener('offline', this.handleOffline.bind(this))
    // 监听页面关闭/刷新
    window.addEventListener('beforeunload', this.handleBeforeUnload.bind(this))
  }
  
  // 处理在线状态
  handleOnline() {
    console.log('网络已连接')
    this.isOnline = true
    if (this.battleStore) {
      this.battleStore.setOnlineStatus(true)
      this.battleStore.setReconnecting(false)
    }
    
    // 尝试重连战斗
    this.attemptReconnect()
  }
  
  // 处理离线状态
  handleOffline() {
    console.log('网络已断开')
    this.isOnline = false
    if (this.battleStore) {
      this.battleStore.setOnlineStatus(false)
      this.battleStore.setReconnecting(true)
    }
    
    // 通知后端保存战斗状态
    this.notifyOffline()
  }
  
  // 处理页面关闭/刷新
  handleBeforeUnload() {
    // 通知后端保存战斗状态
    this.notifyOffline()
  }
  
  // 通知后端保存战斗状态
  notifyOffline() {
    // 清除之前的定时器
    if (this.offlineTimer) {
      clearTimeout(this.offlineTimer)
    }
    
    // 延迟发送，避免频繁调用
    this.offlineTimer = setTimeout(() => {
      battleApi.updateBattleStatus('offline')
        .then(() => {
          console.log('已通知后端保存战斗状态')
        })
        .catch(error => {
          console.error('通知后端保存战斗状态失败:', error)
        })
    }, 500)
  }
  
  // 尝试重连
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('重连次数达到上限')
      return
    }
    
    this.reconnectAttempts++
    
    // 检查是否有未结束的战斗
    battleApi.getCurrentBattle()
        .then(response => {
          if (response.code === 200 && response.data && this.battleStore) {
            console.log('检测到未结束的战斗，准备恢复')
            this.battleStore.setPendingBattle(response.data)
          }
        })
      .catch(error => {
        console.error('重连检测失败:', error)
        // 继续尝试重连
        setTimeout(() => {
          this.attemptReconnect()
        }, this.reconnectDelay * this.reconnectAttempts)
      })
  }
  
  // 重置重连状态
  resetReconnect() {
    this.reconnectAttempts = 0
  }
  
  // 销毁网络管理器
  destroy() {
    // 移除事件监听
    window.removeEventListener('online', this.handleOnline.bind(this))
    window.removeEventListener('offline', this.handleOffline.bind(this))
    window.removeEventListener('beforeunload', this.handleBeforeUnload.bind(this))
    
    // 清除定时器
    if (this.offlineTimer) {
      clearTimeout(this.offlineTimer)
      this.offlineTimer = null
    }
    
    // 清理引用
    this.battleStore = null
  }
}

// 导出类
export { NetworkManager }