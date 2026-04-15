package cn.iocoder.gameai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI对话会话实体类
 * 对应数据库表：chat_session
 */
@Data
@TableName("chat_session")
public class ChatSession {
    
    /**
     * 会话ID，主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID，用于区分不同用户的会话
     */
    private Long userId;
    
    /**
     * 会话标题
     */
    private String title;
    
    /**
     * 使用场景：common=普通对话、train=训练人偶、battle=战斗
     */
    private String scene;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 删除标记：0=未删除，1=已删除
     */
    private Integer isDeleted;
}