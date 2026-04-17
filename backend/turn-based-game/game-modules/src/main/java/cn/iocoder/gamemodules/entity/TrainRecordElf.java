package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("train_record_elf")
public class TrainRecordElf {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 精灵ID（user_elf表的主键ID）
    private Long elfId;
    
    // 训练人偶ID
    private Long mannequinId;
    
    // 当前HP
    private Integer currentHp;
    
    // 当前MP
    private Integer currentMp;
    
    // 精灵状态：0=战斗中，1=可上场，2=死亡
    private Integer elfState;
    
    // 创建时间
    private LocalDateTime createTime;
    
    // 更新时间
    private LocalDateTime updateTime;
}
