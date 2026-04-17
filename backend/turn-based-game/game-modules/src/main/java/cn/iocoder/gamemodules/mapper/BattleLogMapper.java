package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.BattleLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BattleLogMapper extends BaseMapper<BattleLog> {
    
    /**
     * 根据战斗ID查询所有日志
     */
    List<BattleLog> selectByBattleId(@Param("battleId") String battleId);
}
