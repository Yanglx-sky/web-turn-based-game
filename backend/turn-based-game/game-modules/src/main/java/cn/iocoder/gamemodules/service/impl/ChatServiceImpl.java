package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamemodules.entity.ChatChannel;
import cn.iocoder.gamemodules.entity.ChatContent;
import cn.iocoder.gamemodules.mapper.ChatChannelMapper;
import cn.iocoder.gamemodules.mapper.ChatContentMapper;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.service.ChatService;
import cn.iocoder.gamemodules.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 聊天服务实现类
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatChannelMapper chatChannelMapper;

    @Autowired
    private ChatContentMapper chatContentMapper;

    @Autowired
    private FriendService friendService;

    @Override
    public Result<List<ChatChannel>> getChannelList() {
        List<ChatChannel> channels = chatChannelMapper.selectAllActiveChannels();
        return Result.success(channels);
    }

    @Override
    @Transactional
    public Result<ChatContent> sendChannelMessage(Long userId, Long channelId, String content, String msgUuid) {
        // 参数校验
        if (userId == null || channelId == null || content == null || content.trim().isEmpty()) {
            return Result.error("参数错误");
        }

        // 检查频道是否存在且启用
        ChatChannel channel = chatChannelMapper.selectById(channelId);
        if (channel == null || channel.getStatus() != 1) {
            return Result.error("频道不存在或已禁用");
        }

        // 检查消息是否重复发送
        if (msgUuid != null && !msgUuid.isEmpty()) {
            ChatContent existingMsg = chatContentMapper.selectByMsgUuid(msgUuid);
            if (existingMsg != null) {
                return Result.success(existingMsg); // 返回已存在的消息
            }
        }

        // 生成消息UUID（如果未提供）
        if (msgUuid == null || msgUuid.isEmpty()) {
            msgUuid = UUID.randomUUID().toString();
        }

        // 创建消息
        ChatContent message = new ChatContent();
        message.setMsgUuid(msgUuid);
        message.setMsgType(1); // 频道消息
        message.setChannelId(channelId);
        message.setSenderId(userId);
        message.setReceiverId(null); // 频道消息接收者为空
        message.setContent(content.trim());
        message.setSendTime(LocalDateTime.now());
        message.setIsDeleted(0);
        message.setCreateTime(LocalDateTime.now());

        chatContentMapper.insert(message);

        return Result.success(message);
    }

    @Override
    @Transactional
    public Result<ChatContent> sendPrivateMessage(Long userId, Long receiverId, String content, String msgUuid) {
        // 参数校验
        if (userId == null || receiverId == null || content == null || content.trim().isEmpty()) {
            return Result.error("参数错误");
        }

        // 不能给自己发消息
        if (userId.equals(receiverId)) {
            return Result.error("不能给自己发送私聊消息");
        }

        // 检查是否为好友关系
        if (!friendService.isFriend(userId, receiverId)) {
            return Result.error("对方不是您的好友，无法发送私聊消息");
        }

        // 检查是否被拉黑
        if (friendService.isBlacklisted(receiverId, userId)) {
            return Result.error("您已被对方拉黑，无法发送消息");
        }

        // 检查消息是否重复发送
        if (msgUuid != null && !msgUuid.isEmpty()) {
            ChatContent existingMsg = chatContentMapper.selectByMsgUuid(msgUuid);
            if (existingMsg != null) {
                return Result.success(existingMsg); // 返回已存在的消息
            }
        }

        // 生成消息UUID（如果未提供）
        if (msgUuid == null || msgUuid.isEmpty()) {
            msgUuid = UUID.randomUUID().toString();
        }

        // 创建消息
        ChatContent message = new ChatContent();
        message.setMsgUuid(msgUuid);
        message.setMsgType(2); // 私聊消息
        message.setChannelId(null); // 私聊消息频道ID为空
        message.setSenderId(userId);
        message.setReceiverId(receiverId);
        message.setContent(content.trim());
        message.setSendTime(LocalDateTime.now());
        message.setIsDeleted(0);
        message.setCreateTime(LocalDateTime.now());

        chatContentMapper.insert(message);

        return Result.success(message);
    }

    @Override
    public Result<Map<String, Object>> getChannelMessages(Long channelId, Long lastId, Integer pageSize) {
        // 参数校验
        if (channelId == null) {
            return Result.error("频道ID不能为空");
        }

        // 默认分页大小
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 20;
        }

        List<ChatContent> messages;
        if (lastId == null || lastId == 0) {
            // 首次加载，查询最新消息
            messages = chatContentMapper.selectLatestChannelMessages(channelId, pageSize);
        } else {
            // 使用ID游标分页查询历史消息
            messages = chatContentMapper.selectChannelMessagesByCursor(channelId, lastId, pageSize);
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", messages);
        result.put("pageSize", pageSize);
        if (!messages.isEmpty()) {
            result.put("lastId", messages.get(messages.size() - 1).getId());
            result.put("hasMore", messages.size() >= pageSize);
        } else {
            result.put("lastId", lastId);
            result.put("hasMore", false);
        }

        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> getPrivateMessages(Long userId, Long friendId, Long lastId, Integer pageSize) {
        // 参数校验
        if (userId == null || friendId == null) {
            return Result.error("用户ID和好友ID不能为空");
        }

        // 默认分页大小
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 20;
        }

        List<ChatContent> messages;
        if (lastId == null || lastId == 0) {
            // 首次加载，查询最新消息
            messages = chatContentMapper.selectLatestPrivateMessages(userId, friendId, pageSize);
        } else {
            // 使用ID游标分页查询历史消息
            messages = chatContentMapper.selectPrivateMessagesByCursor(userId, friendId, lastId, pageSize);
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", messages);
        result.put("pageSize", pageSize);
        if (!messages.isEmpty()) {
            result.put("lastId", messages.get(messages.size() - 1).getId());
            result.put("hasMore", messages.size() >= pageSize);
        } else {
            result.put("lastId", lastId);
            result.put("hasMore", false);
        }

        return Result.success(result);
    }

    @Override
    @Transactional
    public Result<Void> recallMessage(Long userId, Long msgId) {
        // 参数校验
        if (userId == null || msgId == null) {
            return Result.error("参数错误");
        }

        // 查询消息
        ChatContent message = chatContentMapper.selectById(msgId);
        if (message == null || message.getIsDeleted() == 1) {
            return Result.error("消息不存在或已删除");
        }

        // 只能撤回自己发送的消息
        if (!userId.equals(message.getSenderId())) {
            return Result.error("只能撤回自己发送的消息");
        }

        // 检查是否在2分钟内
        LocalDateTime sendTime = message.getSendTime();
        if (sendTime == null || sendTime.plusMinutes(2).isBefore(LocalDateTime.now())) {
            return Result.error("消息发送超过2分钟，无法撤回");
        }

        // 逻辑删除消息
        message.setIsDeleted(1);
        chatContentMapper.updateById(message);

        return Result.success();
    }
}
