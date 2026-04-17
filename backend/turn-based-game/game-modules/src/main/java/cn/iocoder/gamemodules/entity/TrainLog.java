package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("train_log")
public class TrainLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String trainId;
    
    private Long userId;
    
    private Integer round;
    
    private String logText;
    
    private LocalDateTime createTime;
}
