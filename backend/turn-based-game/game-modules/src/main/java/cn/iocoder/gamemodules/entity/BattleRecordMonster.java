package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("battle_record_monster")
public class BattleRecordMonster {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String battleId;
    private Integer monsterId;
    private Integer currentHp;
    private Integer currentMp;
    private Integer isAlive; // 1=存活 0=死亡
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}