package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.Elf;
import cn.iocoder.gamemodules.mapper.ElfMapper;
import cn.iocoder.gamemodules.service.ElfService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElfServiceImpl extends ServiceImpl<ElfMapper, Elf> implements ElfService {

    @Override
    public Result<List<Elf>> getElfList() {
        List<Elf> elfList = list();
        return Result.success(elfList);
    }

    @Override
    public Result<List<Elf>> getStarterElves() {
        List<Elf> elfList = list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Elf>()
                .in("id", 1, 2, 3)
                .orderByAsc("id"));
        return Result.success(elfList);
    }
}