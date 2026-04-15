package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.entity.Level;
import cn.iocoder.gamemodules.service.BattleService;
import cn.iocoder.gamemodules.service.LevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/levels")
@Loggable
@Tag(name = "关卡管理", description = "关卡相关接口")
public class LevelController {

    private static final Logger logger = LoggerFactory.getLogger(LevelController.class);

    @Autowired
    private LevelService levelService;

    @Autowired
    private BattleService battleService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "获取关卡列表", description = "获取所有关卡列表")
    public Result<List<Level>> getLevelList() {
        return levelService.getLevelList();
    }

    @GetMapping("/{levelId}")
    @Operation(summary = "获取关卡信息", description = "获取指定关卡的详细信息")
    public Result<Level> getLevelInfo(
            @Parameter(name = "levelId", description = "关卡ID", required = true) @PathVariable Integer levelId) {
        return levelService.getLevelById(levelId);
    }

    @PostMapping("/enter")
    @Operation(summary = "进入关卡", description = "进入指定关卡，精灵自动满血满蓝")
    public Result<Map<String, Object>> enterLevel(HttpServletRequest request, 
            @Parameter(name = "levelId", description = "关卡ID", required = true) Integer levelId,
            @Parameter(name = "userElfId", description = "用户精灵ID", required = true) Long userElfId) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return battleService.startBattle(userId, userElfId, levelId);
        } catch (Exception e) {
            logger.error("获取用户信息失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
}