package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("rank_config")
public class RankConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String rankType;
    private String rankName;
    private Integer refreshType;
    private String refreshCron;
    private Integer maxRank;
    private Integer sortType;
    private Integer isEnable;

}