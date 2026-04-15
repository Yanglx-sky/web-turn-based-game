package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.AchievementConfig;
import cn.iocoder.gamemodules.entity.UserAchievementProgress;

import java.util.List;

public interface AchievementService {

    Result<List<AchievementConfig>> getAchievementConfigs();

    Result<List<UserAchievementProgress>> getUserAchievements(Long userId);

    Result<Void> updateAchievementProgress(Long userId, String achievementType, Integer increment);

}