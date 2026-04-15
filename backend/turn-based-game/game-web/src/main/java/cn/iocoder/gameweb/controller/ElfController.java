package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamemodules.entity.Elf;
import cn.iocoder.gamemodules.service.ElfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 精灵模板控制器
 * 处理精灵模板查询、御三家获取等功能
 */
@RestController
@RequestMapping("/elves")
@Tag(name = "精灵模板", description = "精灵模板查询、御三家获取相关接口")
@Loggable
public class ElfController {

    @Autowired
    private ElfService elfService;

    @GetMapping
    @Operation(summary = "获取精灵列表", description = "获取所有精灵列表")
    public Result<List<Elf>> getElfList() {
        return elfService.getElfList();
    }

    @GetMapping("/starter")
    @Operation(summary = "获取御三家精灵", description = "获取id为1、2、3的精灵")
    public Result<List<Elf>> getStarterElves() {
        return elfService.getStarterElves();
    }
}