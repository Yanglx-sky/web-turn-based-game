package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import java.util.List;
import java.util.Map;

public interface TrainService {
    Result<Map<String, Object>> createMannequin(Long userId, Integer attack, Integer defense, Integer hp, Integer mp, Integer speed, Integer type, Integer isAttack);
    Result<Map<String, Object>> startTrain(Long userId, Long mannequinId);
    Result<Map<String, Object>> startTrainWithMannequinParams(Long userId, Integer attack, Integer defense, Integer hp, Integer mp, Integer speed, Integer type, Integer isAttack);
    Result<Map<String, Object>> normalAttack(Long userId);
    Result<Map<String, Object>> useSkill(Long userId, Integer skillId);
    Result<Map<String, Object>> executeMannequinTurn(Long userId);
    Result<Map<String, Object>> flee(Long userId);
    Result<Map<String, Object>> switchElf(Long userId, Long elfId);
    Result<Map<String, Object>> trainSettlement(Long userId);
    Result<Map<String, Object>> getTrainRecords(Long userId);
    Result<List<Map<String, Object>>> getBattleElves(Long userId);
}