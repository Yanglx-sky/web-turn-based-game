package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.UserElf;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserElfService extends IService<UserElf> {
    Result<List<Map<String, Object>>> getUserElfList(Long userId);
    Result<UserElf> setBattleElf(Long userId, Long elfId, Integer fightOrder);
    Result<List<Map<String, Object>>> getBattleElves(Long userId);
    Result<UserElf> upgradeElf(Long elfId);
    Result<Map<String, Object>> getElfDetail(Long elfId);
    Result<UserElf> createElf(Long userId, Integer elfId);
}