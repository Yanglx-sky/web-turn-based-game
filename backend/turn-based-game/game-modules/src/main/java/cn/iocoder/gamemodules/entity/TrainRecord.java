package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("train_record")
public class TrainRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long mannequinId;
    private Integer aiScore;
    private String aiReport;
}