<template>
  <div class="pve-container">
    <GameTopNav />

    <!-- 主内容区域 -->
    <div class="main-content">
      <div class="page-header">
        <p class="section-eyebrow">COMMUNICATION</p>
        <h1>聊天系统</h1>
      </div>
      
      <div class="chat-container">
        <!-- 侧边栏 -->
        <div class="chat-sidebar">
          <!-- 功能切换下拉框 -->
          <div class="section selector-section">
            <select v-model="activeSection" class="section-selector">
              <option value="channel">频道</option>
              <option value="friend">好友</option>
              <option value="request">好友申请</option>
              <option value="search">添加好友</option>
            </select>
          </div>
          
          <!-- 搜索用户 -->
          <div v-if="activeSection === 'search'" class="section search-section">
            <h3>添加好友</h3>
            <div class="search-box">
              <input 
                v-model="searchPhone" 
                placeholder="输入手机号搜索" 
                @keyup.enter="searchUser"
              />
              <button @click="searchUser" class="search-btn">搜索</button>
            </div>
            <div v-if="searchedUser" class="search-result">
              <div class="user-info">
                <span class="user-nickname">{{ searchedUser.nickname }}</span>
              </div>
              <button @click="sendFriendRequest" class="add-friend-btn">添加好友</button>
            </div>
          </div>
          
          <!-- 好友申请列表 -->
          <div v-if="activeSection === 'request'" class="section request-section">
            <h3>好友申请 <span v-if="pendingRequests.length > 0" class="badge">{{ pendingRequests.length }}</span></h3>
            <div class="request-list">
              <div v-if="pendingRequests.length === 0" class="empty-list">暂无申请</div>
              <div 
                v-for="request in pendingRequests" 
                :key="request.userId"
                class="request-item"
              >
                <div class="request-info">
                  <span class="request-nickname">{{ request.nickname || '未知用户' }}</span>
                </div>
                <div class="request-actions">
                  <button @click="acceptRequest(request.userId)" class="accept-btn">同意</button>
                  <button @click="rejectRequest(request.userId)" class="reject-btn">拒绝</button>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 频道列表 -->
          <div v-if="activeSection === 'channel'" class="section channel-section">
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
          <div v-if="activeSection === 'friend'" class="section friend-section">
            <h3>好友</h3>
            <div class="friend-list">
              <div v-if="friends.length === 0" class="empty-list">暂无好友</div>
              <div 
                v-for="friend in friends" 
                :key="friend.friendId"
                class="friend-item"
                :class="{ active: currentFriend?.friendId === friend.friendId }"
                @click="switchFriend(friend)"
              >
                <span class="friend-name">{{ friend.nickname || friend.remark || `用户${friend.friendId}` }}</span>
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
import GameTopNav from '../components/GameTopNav.vue'

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

// 功能切换
const activeSection = ref('channel') // 默认显示频道

// 好友申请相关
const searchPhone = ref('')
const searchedUser = ref(null)
const pendingRequests = ref([])

// WebSocket
let ws = null
let heartbeatTimer = null
let friendsRefreshTimer = null // 好友列表定时刷新定时器
let messagePollingTimer = null // 消息轮询定时器

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
    return currentFriend.value.nickname || currentFriend.value.remark || `用户${currentFriend.value.friendId}`
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
  await loadPendingRequests()
  initWebSocket()
  
  // 定时刷新好友列表（每10秒）
  friendsRefreshTimer = setInterval(async () => {
    await loadFriends()
  }, 10000)
  
  // 定时轮询消息（每3秒）
  messagePollingTimer = setInterval(async () => {
    if (currentChannel.value || currentFriend.value) {
      await pollNewMessages()
    }
  }, 3000)
})

onUnmounted(() => {
  closeWebSocket()
  // 清除好友列表刷新定时器
  if (friendsRefreshTimer) {
    clearInterval(friendsRefreshTimer)
    friendsRefreshTimer = null
  }
  // 清除消息轮询定时器
  if (messagePollingTimer) {
    clearInterval(messagePollingTimer)
    messagePollingTimer = null
  }
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

// 搜索用户
const searchUser = async () => {
  if (!searchPhone.value.trim()) {
    alert('请输入手机号')
    return
  }
  
  try {
    const response = await friendApi.searchUserByPhone(searchPhone.value.trim())
    if (response.code === 200) {
      searchedUser.value = response.data
    } else {
      searchedUser.value = null
      alert(response.msg || '未找到该用户')
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
    alert('搜索失败，请重试')
  }
}

// 发送好友申请
const sendFriendRequest = async () => {
  if (!searchedUser.value) return
  
  try {
    const response = await friendApi.sendFriendRequest(searchedUser.value.id, '')
    if (response.code === 200) {
      alert('好友申请已发送')
      searchedUser.value = null
      searchPhone.value = ''
    } else {
      alert(response.msg || '发送好友申请失败')
    }
  } catch (error) {
    console.error('发送好友申请失败:', error)
    alert('发送失败，请重试')
  }
}

// 加载待处理的好友申请
const loadPendingRequests = async () => {
  try {
    const response = await friendApi.getPendingRequests()
    if (response.code === 200) {
      pendingRequests.value = response.data || []
    }
  } catch (error) {
    console.error('加载好友申请失败:', error)
  }
}

// 同意好友申请
const acceptRequest = async (friendId) => {
  try {
    const response = await friendApi.acceptFriendRequest(friendId)
    if (response.code === 200) {
      alert('已同意好友申请')
      // 重新加载好友列表和申请列表
      await loadFriends()
      await loadPendingRequests()
    } else {
      alert(response.msg || '同意失败')
    }
  } catch (error) {
    console.error('同意好友申请失败:', error)
    alert('同意失败，请重试')
  }
}

// 拒绝好友申请
const rejectRequest = async (friendId) => {
  try {
    const response = await friendApi.rejectFriendRequest(friendId)
    if (response.code === 200) {
      alert('已拒绝好友申请')
      // 重新加载申请列表
      await loadPendingRequests()
    } else {
      alert(response.msg || '拒绝失败')
    }
  } catch (error) {
    console.error('拒绝好友申请失败:', error)
    alert('拒绝失败，请重试')
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

// 轮询新消息（通过axios）
const pollNewMessages = async () => {
  try {
    let response
    if (currentChannel.value) {
      // 获取频道最新消息
      response = await chatApi.getChannelMessages(
        currentChannel.value.id,
        null, // 不传lastId，获取最新消息
        10 // 获取最近10条
      )
    } else if (currentFriend.value) {
      // 获取私聊最新消息
      response = await chatApi.getPrivateMessages(
        currentFriend.value.friendId,
        null, // 不传lastId，获取最新消息
        10 // 获取最近10条
      )
    }
    
    if (response && response.code === 200) {
      const data = response.data
      const newMessages = (data.list || []).reverse() // 反转为正序
      
      if (newMessages.length > 0) {
        // 获取当前消息列表的最后一条消息ID
        const lastMessageId = messages.value.length > 0 
          ? messages.value[messages.value.length - 1].id 
          : 0
        
        // 过滤出比当前最新消息更新的消息
        const unseenMessages = newMessages.filter(msg => msg.id > lastMessageId)
        
        if (unseenMessages.length > 0) {
          console.log('[轮询] 发现', unseenMessages.length, '条新消息')
          // 追加新消息
          messages.value.push(...unseenMessages)
          // 滚动到底部
          scrollToBottom()
        }
      }
    }
  } catch (error) {
    // 静默失败，不显示错误（避免频繁弹窗）
    console.error('[轮询] 获取新消息失败:', error)
  }
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
  console.log('[WebSocket] 收到消息:', data.type, data)
  
  switch (data.type) {
    case 'channel_message':
      // 频道新消息
      if (currentChannel.value && 
          data.data.message.channelId === currentChannel.value.id) {
        console.log('[WebSocket] 添加频道消息到列表')
        messages.value.push(data.data.message)
        scrollToBottom()
      }
      break
    case 'private_message':
      // 私聊新消息
      if (currentFriend.value && 
          (data.data.message.senderId === currentFriend.value.friendId ||
           data.data.message.receiverId === currentFriend.value.friendId)) {
        console.log('[WebSocket] 添加私聊消息到列表')
        messages.value.push(data.data.message)
        scrollToBottom()
      } else {
        // 增加未读数
        const senderId = data.data.message.senderId
        unreadCounts.value[senderId] = (unreadCounts.value[senderId] || 0) + 1
        console.log('[WebSocket] 增加未读数, senderId:', senderId)
        
        // 刷新好友列表，确保显示所有好友
        loadFriends()
      }
      break
    case 'private_message_sent':
      // 自己发送的私聊消息确认
      console.log('[WebSocket] 自己发送的消息确认')
      messages.value.push(data.data.message)
      scrollToBottom()
      break
    case 'pong':
      // 心跳响应
      break
    case 'error':
      console.error('[WebSocket] 错误:', data.message)
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
    return currentFriend.value.nickname || currentFriend.value.remark || `用户${msg.senderId}`
  }
  // 频道消息使用senderNickname
  return msg.senderNickname || `用户${msg.senderId}`
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
.pve-container {
  min-height: 100vh;
  padding: 0 20px 28px;
  background:
    radial-gradient(circle at top, rgba(255, 165, 81, 0.16), transparent 24%),
    linear-gradient(180deg, #06080f 0%, #101827 52%, #111d2e 100%);
  color: #f8f1e4;
  overflow-x: hidden;
}

.main-content {
  max-width: 1200px;
  margin: 22px auto 0;
  padding: 0 40px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.page-header {
  text-align: center;
  margin-bottom: 0.5rem;
}

.page-header h1 {
  margin: 0;
  color: #fff4df;
  font-weight: 800;
  font-size: 2.8rem;
  letter-spacing: -0.02em;
  text-shadow: 0 4px 12px rgba(255, 140, 0, 0.4);
}

.section-eyebrow {
  margin: 0;
  color: rgba(255, 220, 162, 0.78);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  margin-bottom: 0.5rem;
}

.chat-container {
  display: flex;
  height: 75vh;
  background: linear-gradient(180deg, rgba(24, 15, 6, 0.96), rgba(16, 9, 3, 0.96));
  border: 1px solid rgba(255, 169, 79, 0.2);
  border-top: 3px solid rgba(255, 152, 0, 0.7);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 16px 28px rgba(255, 140, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  margin-top: 20px;
}

.chat-sidebar {
  width: 280px;
  background: rgba(15, 10, 5, 0.4);
  border-right: 1px solid rgba(255, 169, 79, 0.2);
  display: flex;
  flex-direction: column;
}

.selector-section {
  padding: 15px;
  border-bottom: 1px solid rgba(255, 169, 79, 0.2);
  background: transparent;
}

.section-selector {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(255, 169, 79, 0.4);
  border-radius: 8px;
  font-size: 15px;
  font-weight: bold;
  color: #fff4df;
  background: rgba(40, 20, 5, 0.8);
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
}
.section-selector option {
  background: #1a1005;
  color: #fff4df;
}


.section-selector:hover, .section-selector:focus {
  border-color: #ff9c3a;
  box-shadow: 0 0 8px rgba(255, 156, 58, 0.3);
}

.section {
  padding: 20px;
  border-bottom: 1px solid rgba(255, 169, 79, 0.1);
}

.section h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #ff9c3a;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(255, 140, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.badge {
  background: #ff4444;
  color: white;
  border-radius: 50%;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: bold;
  min-width: 20px;
  text-align: center;
}

.search-box {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.search-box input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid rgba(255, 169, 79, 0.3);
  border-radius: 6px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.05);
  color: #fff4df;
  transition: border-color 0.3s ease;
}

.search-box input:focus {
  outline: none;
  border-color: #ff9c3a;
}

.search-btn, .add-friend-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a 55%, #ff7a1a);
  color: #2d1a0a;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease;
  box-shadow: 0 4px 12px rgba(255, 132, 29, 0.2);
}

.search-btn:hover, .add-friend-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 132, 29, 0.35);
}

.search-result {
  background: rgba(255, 169, 79, 0.05);
  padding: 12px;
  border-radius: 8px;
  margin-top: 10px;
  border: 1px solid rgba(255, 195, 112, 0.1);
}

.user-info {
  margin-bottom: 10px;
}

.user-nickname {
  font-size: 16px;
  font-weight: bold;
  color: #fff4df;
}

.add-friend-btn {
  width: 100%;
}

.request-list {
  max-height: 200px;
  overflow-y: auto;
}

.empty-list {
  text-align: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}

.request-item {
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  margin-bottom: 8px;
  border: 1px solid rgba(255, 169, 79, 0.2);
}

.request-info {
  margin-bottom: 10px;
}

.request-nickname {
  font-size: 14px;
  font-weight: bold;
  color: #fff4df;
}

.request-actions {
  display: flex;
  gap: 8px;
}

.accept-btn, .reject-btn {
  flex: 1;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  transition: transform 180ms ease;
}

.accept-btn {
  background: linear-gradient(135deg, #a2ffc4, #3aff75 55%, #1aff53);
  color: #0a2d12;
}

.accept-btn:hover {
  transform: translateY(-2px);
}

.reject-btn {
  background: linear-gradient(135deg, #ff8c8c, #ff4c4c 55%, #ff1a1a);
  color: #fff;
}

.reject-btn:hover {
  transform: translateY(-2px);
}

.channel-list,
.friend-list,
.request-list {
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
  color: #e8eaed;
}

.channel-item:hover,
.friend-item:hover {
  background: rgba(255, 156, 58, 0.15);
  transform: translateX(5px);
}

.channel-item.active,
.friend-item.active {
  background: rgba(255, 156, 58, 0.25);
  border-left: 4px solid #ff9c3a;
  color: #fff4df;
  font-weight: bold;
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
  background: transparent;
}

.chat-header {
  padding: 20px;
  background: rgba(16, 9, 3, 0.6);
  border-bottom: 1px solid rgba(255, 169, 79, 0.2);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header h3 {
  margin: 0;
  font-size: 20px;
  color: #fff4df;
  font-weight: bold;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: rgba(0, 0, 0, 0.2);
}

.loading,
.empty {
  text-align: center;
  padding: 60px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
}

.load-more {
  text-align: center;
  padding: 15px;
  color: #ff9c3a;
  cursor: pointer;
  font-weight: bold;
  transition: color 0.3s ease;
}

.load-more:hover {
  color: #ffe1a2;
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
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-item.self .message-info {
  color: rgba(255, 156, 58, 0.8);
}

.message-content {
  display: inline-block;
  padding: 12px 18px;
  background: rgba(40, 25, 15, 0.8);
  border-radius: 12px;
  word-break: break-word;
  box-shadow: 0 4px 10px rgba(0,0,0,0.3);
  border: 1px solid rgba(255, 169, 79, 0.2);
  color: #fff4df;
  text-align: left;
}

.message-item.self .message-content {
  background: linear-gradient(135deg, rgba(255, 156, 58, 0.2), rgba(255, 122, 26, 0.1));
  border-color: rgba(255, 156, 58, 0.4);
}

.chat-input {
  padding: 20px;
  background: rgba(16, 9, 3, 0.8);
  border-top: 1px solid rgba(255, 169, 79, 0.2);
  display: flex;
  gap: 15px;
  align-items: flex-end;
}

.chat-input textarea {
  flex: 1;
  padding: 15px;
  border: 1px solid rgba(255, 169, 79, 0.3);
  border-radius: 8px;
  resize: none;
  height: 80px;
  font-size: 14px;
  background: rgba(0, 0, 0, 0.4);
  color: #fff4df;
  transition: border-color 0.3s ease;
}

.chat-input textarea:focus {
  outline: none;
  border-color: #ff9c3a;
  box-shadow: 0 0 8px rgba(255, 156, 58, 0.2);
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
  min-height: 48px;
}

.send-btn {
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a 55%, #ff7a1a);
  color: #2d1a0a;
  box-shadow: 0 12px 24px rgba(255, 132, 29, 0.2);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 16px 28px rgba(255, 132, 29, 0.35);
}

.send-btn:disabled {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.3);
  cursor: not-allowed;
  box-shadow: none;
}

::-webkit-scrollbar {
  width: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 156, 58, 0.4);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 156, 58, 0.6);
}
</style>
