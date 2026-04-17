package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("battle_log")
public class BattleLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String battleId;
    
    private Integer round;
    
    private String logText;
    
    private LocalDateTime createTime;
}
