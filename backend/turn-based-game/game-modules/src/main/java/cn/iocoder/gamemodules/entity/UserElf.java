package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_elf")
public class UserElf {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer elfId;
    private Integer level;
    private Long exp;
    @TableField("exp_need")
    private Long expNeed;
    @TableField("max_hp")
    private Integer maxHp;
    @TableField("max_mp")
    private Integer maxMp;
    private Integer hp;
    private Integer mp;
    private Integer attack;
    private Integer defense;
    @TableField("normal_damage")
    private Integer normalDamage;
    private Integer speed;
    @TableField("is_fight")
    private Integer isFight;
    @TableField("fight_order")
    private Integer fightOrder;
    
    private Long weaponId;
    private Long armorId;
    private Integer status; // 0为正常可切换上场，1为战斗中，2为已死亡本场战斗不可再上场
}