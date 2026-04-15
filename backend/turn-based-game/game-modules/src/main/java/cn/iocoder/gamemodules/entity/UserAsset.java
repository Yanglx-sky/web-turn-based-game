package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_asset")
public class UserAsset {
    @TableId
    private Long userId;
    private Long gold;
    private LocalDateTime updateTime;
}