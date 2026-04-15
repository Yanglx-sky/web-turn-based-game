package cn.iocoder.gamemodules.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.Level;

import java.util.List;

public interface LevelService extends IService<Level> {
    Result<List<Level>> getLevelList();
    Result<Level> getLevelById(Integer levelId);
}