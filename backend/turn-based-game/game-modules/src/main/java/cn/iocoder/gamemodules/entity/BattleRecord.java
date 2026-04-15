package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("battle_record")
public class BattleRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer levelId;
    private Integer getExp;
    private LocalDateTime createTime;
    private String battleId;
    private Integer currentRound;
    private Integer status; // 0=战斗中 1=胜利 2=失败 3=断线暂停
    private LocalDateTime offlineTime;
}