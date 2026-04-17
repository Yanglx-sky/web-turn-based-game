import api from './axios'

/**
 * 聊天相关API
 */
export const chatApi = {
  /**
   * 获取频道列表
   */
  getChannelList: () => api.get('/api/chat/channels'),

  /**
   * 发送频道消息
   * @param {number} channelId - 频道ID
   * @param {string} content - 消息内容
   * @param {string} msgUuid - 消息唯一标识（防重）
   */
  sendChannelMessage: (channelId, content, msgUuid) => 
    api.post('/api/chat/channel/send', null, {
      params: { channelId, content, msgUuid }
    }),

  /**
   * 发送私聊消息
   * @param {number} receiverId - 接收者ID
   * @param {string} content - 消息内容
   * @param {string} msgUuid - 消息唯一标识（防重）
   */
  sendPrivateMessage: (receiverId, content, msgUuid) => 
    api.post('/api/chat/private/send', null, {
      params: { receiverId, content, msgUuid }
    }),

  /**
   * 获取频道历史消息（ID游标分页）
   * @param {number} channelId - 频道ID
   * @param {number} lastId - 上一页最后一条消息ID（首次加载不传）
   * @param {number} pageSize - 每页大小（默认20，最大100）
   */
  getChannelMessages: (channelId, lastId, pageSize = 20) => 
    api.get(`/api/chat/channels/${channelId}/messages`, {
      params: { lastId, pageSize }
    }),

  /**
   * 获取私聊历史消息（ID游标分页）
   * @param {number} friendId - 好友ID
   * @param {number} lastId - 上一页最后一条消息ID（首次加载不传）
   * @param {number} pageSize - 每页大小（默认20，最大100）
   */
  getPrivateMessages: (friendId, lastId, pageSize = 20) => 
    api.get(`/api/chat/private/${friendId}/messages`, {
      params: { lastId, pageSize }
    }),

  /**
   * 撤回消息
   * @param {number} msgId - 消息ID
   */
  recallMessage: (msgId) => 
    api.post('/api/chat/recall', null, {
      params: { msgId }
    })
}

/**
 * 好友的相关API
 */
export const friendApi = {
  /**
   * 搜索用户（通过手机号）
   * @param {string} phone - 手机号
   */
  searchUserByPhone: (phone) => api.get('/api/users/search', {
    params: { phone }
  }),

  /**
   * 发送好友申请
   * @param {number} friendId - 被申请人ID
   * @param {string} remark - 备注名称
   */
  sendFriendRequest: (friendId, remark) => 
    api.post('/api/friends/requests', null, {
      params: { friendId, remark }
    }),

  /**
   * 同意好友申请
   * @param {number} friendId - 申请人ID
   */
  acceptFriendRequest: (friendId) => 
    api.put(`/api/friends/requests/${friendId}/accept`),

  /**
   * 拒绝好友申请
   * @param {number} friendId - 申请人ID
   */
  rejectFriendRequest: (friendId) => 
    api.put(`/api/friends/requests/${friendId}/reject`),

  /**
   * 删除好友
   * @param {number} friendId - 好友ID
   */
  deleteFriend: (friendId) => 
    api.delete(`/api/friends/${friendId}`),

  /**
   * 获取好友列表
   */
  getFriendList: () => api.get('/api/friends'),

  /**
   * 获取待确认的好友申请列表
   */
  getPendingRequests: () => api.get('/api/friends/requests/pending'),

  /**
   * 拉黑好友
   * @param {number} friendId - 好友ID
   */
  blacklistFriend: (friendId) => 
    api.put(`/api/friends/${friendId}/blacklist`),

  /**
   * 解除拉黑
   * @param {number} friendId - 好友ID
   */
  unblacklistFriend: (friendId) => 
    api.put(`/api/friends/${friendId}/unblacklist`),

  /**
   * 获取黑名单列表
   */
  getBlacklist: () => api.get('/api/friends/blacklists'),

  /**
   * 更新好友备注
   * @param {number} friendId - 好友ID
   * @param {string} remark - 备注名称
   */
  updateRemark: (friendId, remark) => 
    api.put(`/api/friends/${friendId}/remark`, null, {
      params: { remark }
    }),

  /**
   * 检查好友关系
   * @param {number} friendId - 好友ID
   */
  checkFriendship: (friendId) => 
    api.get(`/api/friends/${friendId}/check`)
}