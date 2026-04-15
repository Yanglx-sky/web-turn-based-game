package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_level_star")
public class UserLevelStar {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long levelId;
    private Integer star;
    private Integer maxScore;
    private Integer isPassed;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}