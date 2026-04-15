package cn.iocoder.gamemodules.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.Monster;
import cn.iocoder.gamemodules.mapper.MonsterMapper;
import cn.iocoder.gamemodules.service.MonsterService;
import org.springframework.stereotype.Service;

@Service
public class MonsterServiceImpl extends ServiceImpl<MonsterMapper, Monster> implements MonsterService {

    @Override
    public Result<Monster> getMonsterById(Integer monsterId) {
        Monster monster = getById(monsterId);
        if (monster == null) {
            return Result.error("怪物不存在");
        }
        return Result.success(monster);
    }
}