package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("skill")
public class Skill {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String skillName;
    private Integer elementType;
    private Integer skillDamage;
    private Integer costMp;
    private Integer unlockLevel;
    private String des;
}