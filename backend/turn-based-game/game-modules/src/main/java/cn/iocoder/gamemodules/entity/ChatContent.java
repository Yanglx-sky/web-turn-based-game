package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天内容表
 * 存储所有频道消息 + 私聊消息，支持历史记录、深分页查询
 */
@Data
@TableName("chat_content")
public class ChatContent {

    /**
     * 消息自增ID，用于深分页
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 全局唯一消息ID，防止重复发送
     */
    private String msgUuid;

    /**
     * 消息类型：1-频道消息 2-私聊消息
     */
    private Integer msgType;

    /**
     * 频道ID，频道消息必填
     */
    private Long channelId;

    /**
     * 发送者角色ID
     */
    private Long senderId;

    /**
     * 接收者角色ID，私聊消息必填
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 0-正常 1-已删除
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
