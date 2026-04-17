package cn.iocoder.gameai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词实体类
 * 对应数据库表：sensitive_word
 */
@Data
@TableName("sensitive_word")
public class SensitiveWord {
    
    /**
     * 敏感词ID，主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 敏感词内容
     */
    private String word;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
