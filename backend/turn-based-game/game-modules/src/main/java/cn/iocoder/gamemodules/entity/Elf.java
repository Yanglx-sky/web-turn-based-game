package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("elf")
public class Elf {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String elfName;
    private Integer elementType;
    private Integer baseHp;
    private Integer baseMp;
    private Integer baseAttack;
    private Integer baseDefense;
    private Integer baseNormalDamage;
    private Integer baseSpeed;
}