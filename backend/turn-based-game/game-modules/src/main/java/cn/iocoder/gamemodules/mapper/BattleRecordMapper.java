package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.BattleRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BattleRecordMapper extends BaseMapper<BattleRecord> {
    /**
     * 根据用户ID查询当前战斗（状态为0战斗中或3断线暂停）
     */
    BattleRecord selectCurrentBattleByUserId(@Param("userId") Long userId);

    /**
     * 查询超时的断线战斗（状态为3且offline_time超过90秒）
     */
    List<BattleRecord> selectTimeoutBattles(@Param("thresholdTime") LocalDateTime thresholdTime);
}