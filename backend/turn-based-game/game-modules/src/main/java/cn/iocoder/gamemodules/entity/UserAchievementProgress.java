package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_achievement_progress")
public class UserAchievementProgress {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer achievementId;
    private Integer currentValue;
    private Integer status;
    private Date createTime;
    private Date updateTime;

}