package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.service.PotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 药品控制器
 * 处理药品查询、使用、数量管理等功能
 */
@RestController
@RequestMapping("/potions")
@Tag(name = "药品管理", description = "药品查询、使用、数量管理相关接口")
@Loggable
public class PotionController {

    @Autowired
    private PotionService potionService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取所有药品配置
     */
    @GetMapping
    @Operation(summary = "获取所有药品", description = "获取所有药品配置信息")
    public Result<?> getAllPotions() {
        return potionService.getAllPotions();
    }

    /**
     * 获取药品详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取药品详情", description = "获取指定药品的详细信息")
    public Result<?> getPotionById(
            @Parameter(name = "id", description = "药品ID", required = true) @PathVariable Long id) {
        return potionService.getPotionById(id);
    }

    /**
     * 获取用户拥有的药品
     */
    @GetMapping("/user")
    @Operation(summary = "获取用户药品", description = "获取当前用户拥有的所有药品")
    public Result<?> getUserPotions(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return potionService.getUserPotions(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    /**
     * 使用药品
     */
    @PostMapping("/use")
    @Operation(summary = "使用药品", description = "为指定精灵使用药品")
    public Result<?> usePotion(HttpServletRequest request,
            @Parameter(name = "elfId", description = "精灵ID", required = true) @RequestParam Long elfId,
            @Parameter(name = "potionId", description = "药品ID", required = true) @RequestParam Long potionId) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return potionService.usePotion(userId, elfId, potionId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    /**
     * 增加用户药品数量
     */
    @PostMapping("/add")
    @Operation(summary = "增加药品数量", description = "增加用户指定药品的数量")
    public Result<?> addUserPotion(HttpServletRequest request,
            @Parameter(name = "potionConfigId", description = "药品配置ID", required = true) @RequestParam Long potionConfigId,
            @Parameter(name = "count", description = "增加数量", required = true) @RequestParam Integer count) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return potionService.addUserPotion(userId, potionConfigId, count);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    /**
     * 减少用户药品数量
     */
    @PostMapping("/reduce")
    @Operation(summary = "减少药品数量", description = "减少用户指定药品的数量")
    public Result<?> reduceUserPotion(HttpServletRequest request,
            @Parameter(name = "potionConfigId", description = "药品配置ID", required = true) @RequestParam Long potionConfigId,
            @Parameter(name = "count", description = "减少数量", required = true) @RequestParam Integer count) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return potionService.reduceUserPotion(userId, potionConfigId, count);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }
}