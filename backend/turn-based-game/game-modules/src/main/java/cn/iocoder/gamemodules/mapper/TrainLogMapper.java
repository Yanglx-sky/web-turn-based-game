package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.TrainLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrainLogMapper extends BaseMapper<TrainLog> {
    
    /**
     * 根据训练ID查询所有日志
     */
    List<TrainLog> selectByTrainId(@Param("trainId") String trainId);
}
