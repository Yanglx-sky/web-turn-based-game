<template>
  <div class="chat-container">
    <div class="chat-sidebar">
      <!-- 频道列表 -->
      <div class="channel-section">
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
      <div class="friend-section">
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
    
    <div class="chat-main">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <h3>{{ chatTitle }}</h3>
        <div class="connection-status" :class="{ connected: isConnected }">
          {{ isConnected ? '已连接' : '未连接' }}
        </div>
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
        <button @click="sendMessage" :disabled="!canSend">发送</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { chatApi, friendApi } from '../api/chat'

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
  
  const wsUrl = `ws://localhost:8080/ws/chat?token=${token}`
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
.chat-container {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

.chat-sidebar {
  width: 250px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.channel-section,
.friend-section {
  padding: 15px;
  border-bottom: 1px solid #e0e0e0;
}

.channel-section h3,
.friend-section h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
}

.channel-list,
.friend-list {
  max-height: 200px;
  overflow-y: auto;
}

.channel-item,
.friend-item {
  padding: 10px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.channel-item:hover,
.friend-item:hover {
  background: #f0f0f0;
}

.channel-item.active,
.friend-item.active {
  background: #e3f2fd;
}

.unread-badge {
  background: #ff4444;
  color: white;
  border-radius: 50%;
  padding: 2px 6px;
  font-size: 12px;
  min-width: 18px;
  text-align: center;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 15px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header h3 {
  margin: 0;
  font-size: 18px;
}

.connection-status {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: #ffebee;
  color: #c62828;
}

.connection-status.connected {
  background: #e8f5e9;
  color: #2e7d32;
}

.message-list {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.loading,
.empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.load-more {
  text-align: center;
  padding: 10px;
  color: #1976d2;
  cursor: pointer;
}

.message-item {
  margin-bottom: 15px;
  max-width: 70%;
}

.message-item.self {
  margin-left: auto;
  text-align: right;
}

.message-info {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.message-item.self .message-info {
  color: #1976d2;
}

.message-content {
  display: inline-block;
  padding: 10px 15px;
  background: #fff;
  border-radius: 8px;
  word-break: break-word;
}

.message-item.self .message-content {
  background: #e3f2fd;
}

.chat-input {
  padding: 15px;
  background: #fff;
  border-top: 1px solid #e0e0e0;
  display: flex;
  gap: 10px;
}

.chat-input textarea {
  flex: 1;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  resize: none;
  height: 60px;
}

.chat-input button {
  padding: 10px 30px;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.chat-input button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
