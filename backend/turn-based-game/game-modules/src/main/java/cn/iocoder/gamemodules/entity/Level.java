package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("level")
public class Level {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("level_name")
    private String levelName;
    @TableField("monster_id")
    private Integer monsterId;
    @TableField("reward_exp")
    private Integer rewardExp;
    @TableField("reward_gold")
    private Integer rewardGold;
}