package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("monster_skill")
public class MonsterSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer monsterId;
    private Integer skillId;
}