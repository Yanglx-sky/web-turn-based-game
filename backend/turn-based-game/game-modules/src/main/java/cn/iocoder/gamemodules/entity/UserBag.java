package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_bag")
public class UserBag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    @TableField("equip_config_id")
    private Long equipConfigId;
    @TableField("is_worn")
    private Integer isWorn;
    private Long elfId;
    @TableField("create_time")
    private LocalDateTime createTime;
}