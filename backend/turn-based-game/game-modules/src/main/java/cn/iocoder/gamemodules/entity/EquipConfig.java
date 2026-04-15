package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("equip_config")
public class EquipConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer type; // 1: 武器, 2: 防具
    private Integer atk;
    private Integer def;
    private Integer hp;
    private Integer mp;
    private Integer speed;
    private Integer price;
}