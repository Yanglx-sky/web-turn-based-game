<template>
  <div class="home-container">
    <!-- 顶部导航栏 -->
    <nav class="nav-bar">
      <div class="nav-logo">洛克王国</div>
      <div class="nav-menu">
          <button class="nav-btn" @click="navigateTo('/')">首页</button>
          <button class="nav-btn" @click="navigateTo('/elves')">我的精灵</button>
          <button class="nav-btn" @click="navigateTo('/pve')">冒险</button>
          <button class="nav-btn" @click="navigateTo('/shop')">商店</button>
          <button class="nav-btn" @click="navigateTo('/bag')">背包</button>
          <button class="nav-btn" @click="navigateTo('/train')">训练</button>
          <button class="nav-btn" @click="navigateTo('/rank')">排行榜</button>
          <button class="nav-btn" @click="navigateTo('/achievement')">成就</button>
          <button class="nav-btn" @click="navigateTo('/ai')">AI助手</button>
          <button class="nav-btn" @click="navigateTo('/chat')">聊天</button>
          <button class="nav-btn" @click="logout">退出</button>
        </div>
    </nav>

    <!-- 主内容区域 -->
    <div class="main-content">
      <h1>聊天系统</h1>
      
      <div class="chat-container">
        <!-- 侧边栏 -->
        <div class="chat-sidebar">
          <!-- 频道列表 -->
          <div class="section channel-section">
            <h3>频道</h3>
            <div class="channel-list">
              <div 
                v-for="channel in channels" 
                :key="channel.id"
                class="channel-item"
                :class="{ active: currentChannel?.id === channel.id }"
                @click="switchChannel(channel)"
              >
                {{ channel.channelName }}
              </div>
            </div>
          </div>
          
          <!-- 好友列表 -->
          <div class="section friend-section">
            <h3>好友</h3>
            <div class="friend-list">
              <div 
                v-for="friend in friends" 
                :key="friend.friendId"
                class="friend-item"
                :class="{ active: currentFriend?.friendId === friend.friendId }"
                @click="switchFriend(friend)"
              >
                <span class="friend-name">{{ friend.remark || `用户${friend.friendId}` }}</span>
                <span v-if="unreadCounts[friend.friendId]" class="unread-badge">
                  {{ unreadCounts[friend.friendId] }}
                </span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 聊天主区域 -->
        <div class="chat-main">
          <!-- 聊天头部 -->
          <div class="chat-header">
            <h3>{{ chatTitle }}</h3>
          </div>
          
          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef">
            <div v-if="loading" class="loading">加载中...</div>
            <div v-else-if="messages.length === 0" class="empty">暂无消息</div>
            <template v-else>
              <div v-if="hasMore" class="load-more" @click="loadMoreMessages">
                加载更多
              </div>
              <div 
                v-for="msg in messages" 
                :key="msg.id"
                class="message-item"
                :class="{ self: isSelfMessage(msg) }"
              >
                <div class="message-info">
                  <span class="sender">{{ getSenderName(msg) }}</span>
                  <span class="time">{{ formatTime(msg.sendTime) }}</span>
                </div>
                <div class="message-content">{{ msg.content }}</div>
              </div>
            </template>
          </div>
          
          <!-- 输入框 -->
          <div class="chat-input">
            <textarea 
              v-model="inputMessage"
              placeholder="输入消息..."
              @keydown.enter.prevent="sendMessage"
            ></textarea>
            <button @click="sendMessage" :disabled="!canSend" class="action-btn send-btn">发送</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi, friendApi } from '../api/chat'

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/auth')
}

// 状态
const channels = ref([])
const friends = ref([])
const currentChannel = ref(null)
const currentFriend = ref(null)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const hasMore = ref(false)
const lastId = ref(null)
const isConnected = ref(false)
const unreadCounts = ref({})
const messageListRef = ref(null)

// WebSocket
let ws = null
let heartbeatTimer = null

// 当前用户ID
const currentUserId = computed(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.id
})

// 聊天标题
const chatTitle = computed(() => {
  if (currentChannel.value) {
    return currentChannel.value.channelName
  }
  if (currentFriend.value) {
    return currentFriend.value.remark || `用户${currentFriend.value.friendId}`
  }
  return '请选择频道或好友'
})

// 是否可以发送消息
const canSend = computed(() => {
  return (currentChannel.value || currentFriend.value) && 
         inputMessage.value.trim() && 
         isConnected.value
})

// 初始化
onMounted(async () => {
  await loadChannels()
  await loadFriends()
  initWebSocket()
})

onUnmounted(() => {
  closeWebSocket()
})

// 加载频道列表
const loadChannels = async () => {
  try {
    const response = await chatApi.getChannelList()
    if (response.code === 200) {
      channels.value = response.data
      // 默认选中第一个频道
      if (channels.value.length > 0 && !currentChannel.value) {
        switchChannel(channels.value[0])
      }
    }
  } catch (error) {
    console.error('加载频道失败:', error)
  }
}

// 加载好友列表
const loadFriends = async () => {
  try {
    const response = await friendApi.getFriendList()
    if (response.code === 200) {
      friends.value = response.data
    }
  } catch (error) {
    console.error('加载好友失败:', error)
  }
}

// 切换频道
const switchChannel = async (channel) => {
  currentChannel.value = channel
  currentFriend.value = null
  messages.value = []
  lastId.value = null
  hasMore.value = false
  await loadMessages()
  // 订阅频道
  if (ws && isConnected.value) {
    ws.send(JSON.stringify({
      type: 'subscribe_channel',
      channelId: channel.id
    }))
  }
}

// 切换好友
const switchFriend = async (friend) => {
  currentFriend.value = friend
  currentChannel.value = null
  messages.value = []
  lastId.value = null
  hasMore.value = false
  // 清除未读数
  unreadCounts.value[friend.friendId] = 0
  await loadMessages()
}

// 加载消息
const loadMessages = async () => {
  if (loading.value) return
  loading.value = true
  
  try {
    let response
    if (currentChannel.value) {
      response = await chatApi.getChannelMessages(
        currentChannel.value.id,
        lastId.value
      )
    } else if (currentFriend.value) {
      response = await chatApi.getPrivateMessages(
        currentFriend.value.friendId,
        lastId.value
      )
    }
    
    if (response && response.code === 200) {
      const data = response.data
      const newMessages = data.list || []
      
      if (lastId.value) {
        // 加载更多，追加到前面
        messages.value = [...newMessages.reverse(), ...messages.value]
      } else {
        // 首次加载
        messages.value = newMessages.reverse()
      }
      
      lastId.value = data.lastId
      hasMore.value = data.hasMore
      
      // 滚动到底部
      if (!lastId.value) {
        nextTick(() => scrollToBottom())
      }
    }
  } catch (error) {
    console.error('加载消息失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载更多消息
const loadMoreMessages = () => {
  loadMessages()
}

// 发送消息
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || !isConnected.value) return
  
  const msgUuid = generateUUID()
  
  try {
    if (currentChannel.value) {
      // 发送频道消息
      ws.send(JSON.stringify({
        type: 'send_channel_message',
        channelId: currentChannel.value.id,
        content,
        msgUuid
      }))
    } else if (currentFriend.value) {
      // 发送私聊消息
      ws.send(JSON.stringify({
        type: 'send_private_message',
        receiverId: currentFriend.value.friendId,
        content,
        msgUuid
      }))
    }
    
    inputMessage.value = ''
  } catch (error) {
    console.error('发送消息失败:', error)
    alert('发送失败，请重试')
  }
}

// 初始化WebSocket
const initWebSocket = () => {
  const token = localStorage.getItem('token')
  if (!token) return
  
  const wsUrl = `ws://localhost:8080/api/ws/chat?token=${token}`
  ws = new WebSocket(wsUrl)
  
  ws.onopen = () => {
    console.log('WebSocket连接成功')
    isConnected.value = true
    // 启动心跳
    startHeartbeat()
    // 如果当前有选中频道，订阅频道
    if (currentChannel.value) {
      ws.send(JSON.stringify({
        type: 'subscribe_channel',
        channelId: currentChannel.value.id
      }))
    }
  }
  
  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    handleWebSocketMessage(data)
  }
  
  ws.onclose = () => {
    console.log('WebSocket连接关闭')
    isConnected.value = false
    stopHeartbeat()
    // 3秒后重连
    setTimeout(initWebSocket, 3000)
  }
  
  ws.onerror = (error) => {
    console.error('WebSocket错误:', error)
  }
}

// 关闭WebSocket
const closeWebSocket = () => {
  stopHeartbeat()
  if (ws) {
    ws.close()
    ws = null
  }
}

// 启动心跳
const startHeartbeat = () => {
  heartbeatTimer = setInterval(() => {
    if (ws && isConnected.value) {
      ws.send(JSON.stringify({ type: 'ping' }))
    }
  }, 30000) // 30秒心跳
}

// 停止心跳
const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

// 处理WebSocket消息
const handleWebSocketMessage = (data) => {
  switch (data.type) {
    case 'channel_message':
      // 频道新消息
      if (currentChannel.value && 
          data.data.message.channelId === currentChannel.value.id) {
        messages.value.push(data.data.message)
        scrollToBottom()
      }
      break
    case 'private_message':
      // 私聊新消息
      if (currentFriend.value && 
          (data.data.message.senderId === currentFriend.value.friendId ||
           data.data.message.receiverId === currentFriend.value.friendId)) {
        messages.value.push(data.data.message)
        scrollToBottom()
      } else {
        // 增加未读数
        const senderId = data.data.message.senderId
        unreadCounts.value[senderId] = (unreadCounts.value[senderId] || 0) + 1
      }
      break
    case 'private_message_sent':
      // 自己发送的私聊消息确认
      messages.value.push(data.data.message)
      scrollToBottom()
      break
    case 'pong':
      // 心跳响应
      break
    case 'error':
      console.error('WebSocket错误:', data.message)
      break
  }
}

// 判断是否是自己发送的消息
const isSelfMessage = (msg) => {
  return msg.senderId === currentUserId.value
}

// 获取发送者名称
const getSenderName = (msg) => {
  if (isSelfMessage(msg)) {
    return '我'
  }
  if (currentFriend.value) {
    return currentFriend.value.remark || `用户${msg.senderId}`
  }
  return `用户${msg.senderId}`
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 生成UUID
const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #ff8c00 0%, #ff6f00 100%);
  padding: 20px;
}

.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 140, 0, 0.9);
  padding: 15px 30px;
  border-radius: 10px;
  margin-bottom: 30px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.nav-menu {
  display: flex;
  gap: 20px;
}

.nav-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid white;
  color: white;
  padding: 10px 20px;
  border-radius: 25px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.nav-btn:hover {
  background: white;
  color: #ff8c00;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(255, 140, 0, 0.4);
}

.nav-logo {
  font-size: 24px;
  font-weight: bold;
  color: white;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
}

.connection-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: bold;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 2px solid white;
}

.connection-status.connected {
  background: rgba(76, 175, 80, 0.8);
  color: white;
}

.main-content {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

h1 {
  text-align: center;
  margin-bottom: 2rem;
  color: #ff8c00;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.chat-container {
  display: flex;
  height: 70vh;
  background: rgba(245, 245, 245, 0.8);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.chat-sidebar {
  width: 280px;
  background: white;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.section {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.section h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #ff8c00;
  font-weight: bold;
  text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.1);
}

.channel-list,
.friend-list {
  max-height: 300px;
  overflow-y: auto;
}

.channel-item,
.friend-item {
  padding: 12px 15px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.channel-item:hover,
.friend-item:hover {
  background: rgba(255, 140, 0, 0.1);
  transform: translateX(5px);
}

.channel-item.active,
.friend-item.active {
  background: rgba(255, 140, 0, 0.2);
  border-left: 4px solid #ff8c00;
}

.unread-badge {
  background: #ff4444;
  color: white;
  border-radius: 50%;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: bold;
  min-width: 20px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(255, 68, 68, 0.3);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}

.chat-header {
  padding: 20px;
  background: rgba(255, 140, 0, 0.1);
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header h3 {
  margin: 0;
  font-size: 20px;
  color: #333;
  font-weight: bold;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f9f9f9;
}

.loading,
.empty {
  text-align: center;
  padding: 60px;
  color: #999;
  font-size: 16px;
}

.load-more {
  text-align: center;
  padding: 15px;
  color: #ff8c00;
  cursor: pointer;
  font-weight: bold;
  transition: color 0.3s ease;
}

.load-more:hover {
  color: #ff6f00;
  background: rgba(255, 140, 0, 0.1);
  border-radius: 8px;
}

.message-item {
  margin-bottom: 20px;
  max-width: 70%;
  animation: messageFadeIn 0.5s ease;
}

@keyframes messageFadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-item.self {
  margin-left: auto;
  text-align: right;
}

.message-info {
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-item.self .message-info {
  color: #ff8c00;
}

.message-content {
  display: inline-block;
  padding: 12px 18px;
  background: white;
  border-radius: 12px;
  word-break: break-word;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 1px solid #e0e0e0;
}

.message-item.self .message-content {
  background: rgba(255, 140, 0, 0.1);
  border-color: #ff8c00;
}

.chat-input {
  padding: 20px;
  background: white;
  border-top: 1px solid #e0e0e0;
  display: flex;
  gap: 15px;
  align-items: flex-end;
}

.chat-input textarea {
  flex: 1;
  padding: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  resize: none;
  height: 80px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.chat-input textarea:focus {
  outline: none;
  border-color: #ff8c00;
  box-shadow: 0 0 0 3px rgba(255, 140, 0, 0.1);
}

.action-btn {
  padding: 12px 30px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.send-btn {
  background: #ff8c00;
  color: white;
  box-shadow: 0 4px 8px rgba(255, 140, 0, 0.3);
}

.send-btn:hover {
  background: #ff6f00;
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(255, 140, 0, 0.4);
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #ff8c00;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #ff6f00;
}
</style>