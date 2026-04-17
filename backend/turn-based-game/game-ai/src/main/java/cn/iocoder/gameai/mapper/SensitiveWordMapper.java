package cn.iocoder.gameai.mapper;

import cn.iocoder.gameai.entity.SensitiveWord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 敏感词Mapper接口
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
    
    /**
     * 查询所有敏感词
     * @return 敏感词列表
     */
    List<SensitiveWord> selectAllSensitiveWords();
}
