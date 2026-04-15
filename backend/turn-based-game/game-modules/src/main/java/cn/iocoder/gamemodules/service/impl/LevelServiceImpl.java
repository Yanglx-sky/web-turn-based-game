package cn.iocoder.gamemodules.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.Level;
import cn.iocoder.gamemodules.mapper.LevelMapper;
import cn.iocoder.gamemodules.service.LevelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LevelServiceImpl extends ServiceImpl<LevelMapper, Level> implements LevelService {

    @Override
    public Result<List<Level>> getLevelList() {
        List<Level> levelList = list();
        return Result.success(levelList);
    }

    @Override
    public Result<Level> getLevelById(Integer levelId) {
        Level level = getById(levelId);
        if (level == null) {
            return Result.error("关卡不存在");
        }
        return Result.success(level);
    }
}