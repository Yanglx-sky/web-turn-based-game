package cn.iocoder.gameai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI对话消息实体类
 * 对应数据库表：chat_message
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    
    /**
     * 消息ID，主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 所属会话ID，关联chat_session表
     */
    private Long sessionId;
    
    /**
     * 消息角色：user=用户、assistant=AI、system=系统提示
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型：text=普通文本、summary=成长总结、strategy=策略推荐
     */
    private String contentType;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}