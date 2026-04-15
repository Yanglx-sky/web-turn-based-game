package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("battle_record_elf")
public class BattleRecordElf {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String battleId;
    private Long elfId;
    private Integer currentHp;
    private Integer currentMp;
    private Integer elfState; // 0=战斗中 1=可上场 2=死亡
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}