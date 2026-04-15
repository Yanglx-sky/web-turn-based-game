package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友关系表
 * 实现好友添加、私聊权限、拉黑功能
 */
@Data
@TableName("friend_relation")
public class FriendRelation {

    /**
     * 关系主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 本人角色ID
     */
    private Long userId;

    /**
     * 好友角色ID
     */
    private Long friendId;

    /**
     * 好友备注名称
     */
    private String remark;

    /**
     * 关系状态：0-待确认 1-已同意 2-已拉黑
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
