package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.BattleRecordElf;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BattleRecordElfMapper extends BaseMapper<BattleRecordElf> {
    /**
     * 根据战斗ID查询精灵状态
     */
    List<BattleRecordElf> selectByBattleId(@Param("battleId") String battleId);
}