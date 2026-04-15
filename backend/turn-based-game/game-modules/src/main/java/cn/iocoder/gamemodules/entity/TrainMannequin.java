package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("train_mannequin")
public class TrainMannequin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer attack;
    private Integer defense;
    private Integer hp;
    private Integer mp;
    private Integer type;
    private Integer isAttack;
    private Date createTime;
    private Date updateTime;
}