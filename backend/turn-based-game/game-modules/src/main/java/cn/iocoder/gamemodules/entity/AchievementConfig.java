package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("achievement_config")
public class AchievementConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String description;
    private String conditionType;
    private Integer conditionTarget;
    private String icon;
    private Integer isHide;
    private Integer isEnable;

}