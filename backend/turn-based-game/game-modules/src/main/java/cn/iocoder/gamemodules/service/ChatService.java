package cn.iocoder.gamemodules.service;

import cn.iocoder.gamemodules.entity.ChatChannel;
import cn.iocoder.gamemodules.entity.ChatContent;
import cn.iocoder.gamecommon.result.Result;

import java.util.List;
import java.util.Map;

/**
 * 聊天服务接口
 */
public interface ChatService {

    /**
     * 获取所有启用的频道列表
     */
    Result<List<ChatChannel>> getChannelList();

    /**
     * 发送频道消息
     * @param userId 发送者ID
     * @param channelId 频道ID
     * @param content 消息内容
     * @param msgUuid 消息唯一标识（防重）
     */
    Result<ChatContent> sendChannelMessage(Long userId, Long channelId, String content, String msgUuid);

    /**
     * 发送私聊消息
     * @param userId 发送者ID
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @param msgUuid 消息唯一标识（防重）
     */
    Result<ChatContent> sendPrivateMessage(Long userId, Long receiverId, String content, String msgUuid);

    /**
     * 获取频道历史消息（ID游标分页）
     * @param channelId 频道ID
     * @param lastId 上一页最后一条消息ID（首次加载传null）
     * @param pageSize 每页大小
     */
    Result<Map<String, Object>> getChannelMessages(Long channelId, Long lastId, Integer pageSize);

    /**
     * 获取私聊历史消息（ID游标分页）
     * @param userId 当前用户ID
     * @param friendId 好友ID
     * @param lastId 上一页最后一条消息ID（首次加载传null）
     * @param pageSize 每页大小
     */
    Result<Map<String, Object>> getPrivateMessages(Long userId, Long friendId, Long lastId, Integer pageSize);

    /**
     * 撤回消息
     * @param userId 操作用户ID
     * @param msgId 消息ID
     */
    Result<Void> recallMessage(Long userId, Long msgId);
}
