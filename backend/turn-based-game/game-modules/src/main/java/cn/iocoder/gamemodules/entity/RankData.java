package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("rank_data")
public class RankData {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String rankType;
    private Long userId;
    private Integer rankNum;
    private Integer score;
    private Date updateTime;

}