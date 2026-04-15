package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.Monster;

public interface MonsterService {
    Result<Monster> getMonsterById(Integer monsterId);
}