package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.RankConfig;
import cn.iocoder.gamemodules.entity.RankData;

import java.util.List;
import java.util.Map;

public interface RankService {

    Result<List<RankConfig>> getRankConfigs();

    Result<List<Map<String, Object>>> getRankData(String rankType, Integer limit);

    Result<RankData> getUserRankData(String rankType, Long userId);

    Result<Void> updateUserRank(String rankType, Long userId, Integer score);

}