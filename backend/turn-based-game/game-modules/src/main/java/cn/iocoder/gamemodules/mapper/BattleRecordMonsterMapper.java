package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.BattleRecordMonster;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BattleRecordMonsterMapper extends BaseMapper<BattleRecordMonster> {
    /**
     * 根据战斗ID查询怪物状态
     */
    List<BattleRecordMonster> selectByBattleId(@Param("battleId") String battleId);
}