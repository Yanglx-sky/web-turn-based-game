package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天频道表
 * 管理世界频道、深夜修仙频道等所有聊天频道
 */
@Data
@TableName("chat_channel")
public class ChatChannel {

    /**
     * 频道ID，主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 频道唯一标识：world-世界频道，midnight-深夜修仙频道
     */
    private String channelCode;

    /**
     * 频道显示名称
     */
    private String channelName;

    /**
     * 频道描述
     */
    private String description;

    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
