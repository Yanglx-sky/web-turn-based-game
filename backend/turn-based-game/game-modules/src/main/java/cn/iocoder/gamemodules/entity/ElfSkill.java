package cn.iocoder.gamemodules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("elf_skill")
public class ElfSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer elfId;
    private Integer skillId;
    private Integer unlockLevel;
}