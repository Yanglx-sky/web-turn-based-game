package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("monster")
public class Monster {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("monster_name")
    private String monsterName;
    @TableField("element_type")
    private Integer elementType;
    private Integer hp;
    private Integer mp;
    private Integer attack;
    private Integer defense;
    @TableField("normal_damage")
    private Integer normalDamage;
    private Integer speed;
}