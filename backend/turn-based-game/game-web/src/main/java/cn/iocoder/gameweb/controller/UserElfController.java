package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.entity.UserElf;
import cn.iocoder.gamemodules.service.UserElfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import cn.iocoder.gameweb.dto.SetBattleElfDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user-elf")
@Tag(name = "精灵管理", description = "精灵相关接口")
@Loggable
public class UserElfController {

    @Autowired
    private UserElfService userElfService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "获取精灵列表", description = "获取用户的精灵列表")
    public Result<List<Map<String, Object>>> getElfList(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userElfService.getUserElfList(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @PostMapping("/set-battle")
    @Operation(summary = "设置出战精灵", description = "设置用户的出战精灵")
    public Result<UserElf> setBattleElf(HttpServletRequest request, 
            @Parameter(name = "setBattleElfDTO", description = "设置出战精灵参数", required = true) @RequestBody SetBattleElfDTO setBattleElfDTO) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userElfService.setBattleElf(userId, setBattleElfDTO.getElfId(), setBattleElfDTO.getFightOrder());
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }
    
    @GetMapping("/battle-elves")
    @Operation(summary = "获取出战精灵列表", description = "获取用户的出战精灵列表")
    public Result<List<Map<String, Object>>> getBattleElves(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userElfService.getBattleElves(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @PostMapping("/{elfId}/upgrade")
    @Operation(summary = "精灵升级", description = "精灵升级")
    public Result<UserElf> upgradeElf(
            @Parameter(name = "elfId", description = "精灵ID", required = true) @PathVariable Long elfId) {
        return userElfService.upgradeElf(elfId);
    }

    @GetMapping("/{elfId}")
    @Operation(summary = "查看精灵详情", description = "查看精灵的详细信息，包括等级、经验、属性和可解锁技能")
    public Result<Map<String, Object>> getElfDetail(
            @Parameter(name = "elfId", description = "精灵ID", required = true) @PathVariable Long elfId) {
        return userElfService.getElfDetail(elfId);
    }

    @PostMapping
    @Operation(summary = "创建精灵", description = "为用户创建新精灵，用于御三家选择")
    public Result<UserElf> createElf(HttpServletRequest request, 
            @Parameter(name = "elfId", description = "精灵模板ID", required = true) @RequestParam Integer elfId) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userElfService.createElf(userId, elfId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }
}