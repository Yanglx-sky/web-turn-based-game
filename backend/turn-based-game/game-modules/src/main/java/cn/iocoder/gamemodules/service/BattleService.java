package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.BattleRecord;
import cn.iocoder.gamemodules.entity.BattleRecordElf;
import cn.iocoder.gamemodules.entity.BattleRecordMonster;

import java.util.List;
import java.util.Map;

public interface BattleService {
    /**
     * 开始战斗，生成战斗记录
     */
    Result<Map<String, Object>> startBattle(Long userId, Long userElfId, Integer levelId);

    /**
     * 玩家退出/断线，更新战斗状态
     */
    Result<?> playerOffline(Long userId);

    /**
     * 玩家重连恢复战斗
     */
    Result<Map<String, Object>> reconnect(Long userId);

    /**
     * 处理超时未重连的战斗，自动判负
     */
    void handleTimeoutBattles();

    /**
     * 保存战斗状态
     */
    void saveBattleState(String battleId, List<BattleRecordElf> elves, List<BattleRecordMonster> monsters, Integer currentRound);
    
    /**
     * 玩家逃跑
     */
    Result<?> flee(Long userId);
    
    /**
     * 放弃战斗
     */
    Result<?> abandonBattle(Long userId);
    
    /**
     * 普通攻击
     */
    Result<Map<String, Object>> normalAttack(Long userId);
    
    /**
     * 使用技能
     */
    Result<Map<String, Object>> useSkill(Long userId, Integer skillId);
    
    /**
     * 切换精灵
     */
    Result<Map<String, Object>> switchElf(Long userId, Long elfId);
}