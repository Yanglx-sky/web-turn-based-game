package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.service.RankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * 排行榜控制器
 * 处理排行榜配置、数据查询、用户排名等功能
 */
@RestController
@RequestMapping("/rank")
@Tag(name = "排行榜管理", description = "排行榜配置、数据查询、用户排名相关接口")
@Loggable
public class RankController {

    @Autowired
    private RankService rankService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取排行榜配置
     */
    @GetMapping("/configs")
    @Operation(summary = "获取排行榜配置", description = "获取所有排行榜配置信息")
    public Result getRankConfigs() {
        return rankService.getRankConfigs();
    }

    /**
     * 获取排行榜数据
     */
    @GetMapping("/data")
    @Operation(summary = "获取排行榜数据", description = "获取指定类型的排行榜数据")
    public Result<List<Map<String, Object>>> getRankData(
            @Parameter(name = "rankType", description = "排行榜类型", required = true) @RequestParam String rankType,
            @Parameter(name = "limit", description = "返回数量限制") @RequestParam(required = false) Integer limit) {
        return rankService.getRankData(rankType, limit);
    }

    /**
     * 获取用户排名
     */
    @GetMapping("/user")
    @Operation(summary = "获取用户排名", description = "获取当前用户在指定排行榜中的排名")
    public Result getUserRankData(HttpServletRequest request,
            @Parameter(name = "rankType", description = "排行榜类型", required = true) @RequestParam String rankType) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return rankService.getUserRankData(rankType, userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    /**
     * 更新用户排名
     */
    @PutMapping
    @Operation(summary = "更新用户排名", description = "更新用户在指定排行榜中的分数")
    public Result updateUserRank(HttpServletRequest request,
            @Parameter(name = "rankType", description = "排行榜类型", required = true) @RequestParam String rankType,
            @Parameter(name = "score", description = "分数", required = true) @RequestParam Integer score) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return rankService.updateUserRank(rankType, userId, score);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

}