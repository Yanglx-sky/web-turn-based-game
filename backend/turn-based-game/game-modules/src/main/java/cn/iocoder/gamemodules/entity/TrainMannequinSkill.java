package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("train_mannequin_skill")
public class TrainMannequinSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long mannequinId;
    private Integer skillId;
}