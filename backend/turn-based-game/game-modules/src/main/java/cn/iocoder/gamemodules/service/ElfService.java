package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.Elf;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ElfService extends IService<Elf> {
    Result<List<Elf>> getElfList();
    Result<List<Elf>> getStarterElves();
}