package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 成就控制器
 * 处理成就配置查询、用户成就获取、成就进度更新等功能
 */
@RestController
@RequestMapping("/achievement")
@Tag(name = "成就管理", description = "成就配置、用户成就、进度更新相关接口")
@Loggable
public class AchievementController {

    @Autowired
    private AchievementService achievementService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取成就配置列表
     */
    @GetMapping("/configs")
    @Operation(summary = "获取成就配置", description = "获取所有成就配置信息")
    public Result getAchievementConfigs() {
        return achievementService.getAchievementConfigs();
    }

    /**
     * 获取用户成就列表
     */
    @GetMapping("/user")
    @Operation(summary = "获取用户成就", description = "获取当前用户的所有成就及进度")
    public Result getUserAchievements(HttpServletRequest request) {
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
            return achievementService.getUserAchievements(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    /**
     * 更新成就进度
     */
    @PutMapping
    @Operation(summary = "更新成就进度", description = "更新指定类型成就的进度")
    public Result updateAchievementProgress(HttpServletRequest request,
            @Parameter(name = "achievementType", description = "成就类型", required = true) @RequestParam String achievementType,
            @Parameter(name = "increment", description = "增量值", required = true) @RequestParam Integer increment) {
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
            return achievementService.updateAchievementProgress(userId, achievementType, increment);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    

}