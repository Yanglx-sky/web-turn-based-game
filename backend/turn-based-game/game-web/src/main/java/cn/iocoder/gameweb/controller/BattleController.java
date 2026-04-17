package cn.iocoder.gameweb.controller;

import cn.iocoder.gameai.service.AIService;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.entity.*;
import cn.iocoder.gamemodules.mapper.ElfMapper;
import cn.iocoder.gamemodules.mapper.LevelMapper;
import cn.iocoder.gamemodules.mapper.MonsterMapper;
import cn.iocoder.gamemodules.service.BattleService;
import cn.iocoder.gamemodules.service.UserElfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/battle")
@Tag(name = "战斗管理", description = "战斗相关接口")
public class BattleController {

    @Autowired
    private BattleService battleService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private LevelMapper levelMapper;
    
    @Autowired
    private MonsterMapper monsterMapper;
    
    @Autowired
    private UserElfService userElfService;
    
    @Autowired
    private ElfMapper elfMapper;

    /**
     * 开始战斗
     * 自动使用玩家配置的出战精灵列表（按fight_order排序）
     */
    @PostMapping
    @Operation(summary = "开始战斗", description = "开始一场新的战斗，自动使用玩家配置的出战精灵列表")
    public Result<?> startBattle(
            HttpServletRequest request,
            @Parameter(description = "关卡ID", required = true) @RequestParam Integer levelId) {
        try {
            // 从token中获取userId
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);

            // 开始战斗
            return battleService.startBattle(userId, levelId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

/**
     * 获取当前战斗状态
     */
    @GetMapping
    @Operation(summary = "获取当前战斗状态", description = "获取当前用户的战斗状态")
    public Result<?> getCurrentBattle(HttpServletRequest request) {
        try {
            // 从token中获取userId
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);

            // 获取当前战斗状态
            return battleService.reconnect(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

/**
     * 更新战斗状态
     */
    @PutMapping("/status")
    @Operation(summary = "更新战斗状态", description = "更新战斗状态，如离线、在线、逃跑等")
    public Result<?> updateBattleStatus(
            HttpServletRequest request,
            @Parameter(description = "状态信息", required = true) @RequestBody Map<String, Object> params) {
        try {
            // 从token中获取userId
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            // 获取状态
            if (!params.containsKey("status")) {
                return Result.error("缺少status参数");
            }
            Object statusObj = params.get("status");
            if (statusObj == null) {
                return Result.error("status参数为null");
            }
            String status = statusObj.toString();
            
            if ("offline".equals(status)) {
                // 处理玩家离线
                return battleService.playerOffline(userId);
            } else if ("online".equals(status)) {
                // 处理玩家重连
                return battleService.reconnect(userId);
            } else if ("flee".equals(status)) {
                // 处理玩家逃跑
                return battleService.flee(userId);
            } else if ("abandon".equals(status)) {
                // 处理放弃战斗
                return battleService.abandonBattle(userId);
            } else {
                return Result.error("无效的状态值");
            }
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

/**
     * 执行战斗动作
     */
    @PostMapping("/action")
    @Operation(summary = "执行战斗动作", description = "执行战斗中的各种动作，如攻击、使用技能、切换精灵等")
    public Result<?> performAction(
            HttpServletRequest request,
            @Parameter(description = "动作信息", required = true) @RequestBody Map<String, Object> params) {
        try {
            // 从token中获取userId
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            // 获取动作类型
            if (!params.containsKey("type")) {
                return Result.error("缺少type参数");
            }
            String actionType = params.get("type").toString();
            
            switch (actionType) {
                case "attack":
                    // 处理普通攻击
                    return battleService.normalAttack(userId);
                case "skill":
                    // 获取技能ID
                    if (!params.containsKey("skillId")) {
                        return Result.error("缺少skillId参数");
                    }
                    Object skillIdObj = params.get("skillId");
                    if (skillIdObj == null) {
                        return Result.error("skillId参数为null");
                    }
                    Integer skillId;
                    try {
                        if (skillIdObj instanceof Number) {
                            skillId = ((Number) skillIdObj).intValue();
                        } else {
                            skillId = Integer.parseInt(skillIdObj.toString());
                        }
                    } catch (NumberFormatException e) {
                        return Result.error("skillId参数格式错误：" + e.getMessage());
                    }
                    // 处理使用技能
                    return battleService.useSkill(userId, skillId);
                case "monster_turn":
                    // 执行怪物行动
                    return battleService.executeMonsterTurn(userId);
                case "switch":
                    // 获取精灵ID
                    if (!params.containsKey("elfId")) {
                        return Result.error("缺少elfId参数");
                    }
                    Object elfIdObj = params.get("elfId");
                    Long elfId;
                    if (elfIdObj instanceof Number) {
                        elfId = ((Number) elfIdObj).longValue();
                    } else {
                        elfId = Long.parseLong(elfIdObj.toString());
                    }
                    // 处理切换精灵
                    return battleService.switchElf(userId, elfId);
                default:
                    return Result.error("无效的动作类型");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("执行动作失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取战斗策略推荐
     */
    @GetMapping("/ai/strategy")
    @Operation(summary = "获取战斗策略推荐", description = "获取针对指定关卡的战斗策略推荐")
    public Result<?> getStrategyRecommendation(
            HttpServletRequest request,
            @Parameter(description = "关卡ID", required = true) @RequestParam Integer levelId) {
        try {
            // 从token中获取userId
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);

            // 获取关卡信息
            Level level = levelMapper.selectById(levelId);
            if (level == null) {
                return Result.error("关卡不存在");
            }

            // 获取怪物信息
            Monster monster = monsterMapper.selectById(level.getMonsterId());
            if (monster == null) {
                return Result.error("怪物不存在");
            }

            // 获取用户出战精灵
            Result<List<Map<String, Object>>> elvesResult = userElfService.getBattleElves(userId);
            if (elvesResult.getCode() != 200 || elvesResult.getData() == null || elvesResult.getData().isEmpty()) {
                return Result.error("未设置出战精灵");
            }
            List<Map<String, Object>> elves = elvesResult.getData();

            // 构建精灵信息字符串
            StringBuilder elfInfoBuilder = new StringBuilder();
            for (int i = 0; i < elves.size(); i++) {
                Map<String, Object> elf = elves.get(i);
                String elfName = (String) elf.get("elfName");
                Integer elementType = (Integer) elf.get("elementType");
                
                String elementTypeName = getElementTypeName(elementType);
                elfInfoBuilder.append(elfName).append(",").append(elementTypeName);
                
                if (i < elves.size() - 1) {
                    elfInfoBuilder.append(";\n");
                }
            }

            // 构建怪物信息字符串
            String monsterElementTypeName = getElementTypeName(monster.getElementType());
            String monsterInfo = monster.getMonsterName() + "," + monsterElementTypeName;

            // 调用AI服务获取策略推荐
            String recommendation = aiService.getStrategyRecommendation(elfInfoBuilder.toString(), monsterInfo);

            Map<String, Object> result = new HashMap<>();
            result.put("recommendation", recommendation);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取战斗策略推荐失败: " + e.getMessage());
        }
    }

    /**
     * 获取战斗中的精灵列表（从battle_record_elf查询）
     */
    @GetMapping("/battle-elves")
    @Operation(summary = "获取战斗中的精灵列表", description = "从battle_record_elf查询战斗中的精灵状态，用于切换精灵")
    public Result<List<Map<String, Object>>> getBattleElves(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return battleService.getBattleElves(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取战斗精灵列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 将元素类型ID转换为名称
     */
    private String getElementTypeName(Integer elementType) {
        if (elementType == null) {
            return "未知";
        }
        switch (elementType) {
            case 1:
                return "火系";
            case 2:
                return "水系";
            case 3:
                return "草系";
            case 4:
                return "光系";
            default:
                return "未知";
        }
    }
    
    // ==================== 以下为战斗监控+一致性+每日收益上限功能接口 ====================
    
    /**
     * 提交战斗行动（带回合校验和幂等性）
     */
    @PostMapping("/submit")
    @Operation(summary = "提交战斗行动", description = "提交战斗行动，包含回合校验和幂等性保障")
    public Result<?> submitAction(
            HttpServletRequest request,
            @Parameter(description = "回合号", required = true) @RequestParam Integer round,
            @Parameter(description = "动作类型: attack/skill/switch", required = true) @RequestParam String type,
            @Parameter(description = "技能ID") @RequestParam(required = false) Integer skillId,
            @Parameter(description = "精灵ID") @RequestParam(required = false) Long elfId) {
        try {
            String token = getToken(request);
            Long userId = jwtUtil.getUserIdFromToken(token);
            return battleService.submitAction(userId, round, type, skillId, elfId);
        } catch (Exception e) {
            return Result.error("提交动作失败: " + e.getMessage());
        }
    }
    
    /**
     * 领取战斗奖励
     */
    @PostMapping("/reward")
    @Operation(summary = "领取战斗奖励", description = "战斗胜利后领取奖励，需验证战斗胜利标记")
    public Result<?> claimReward(
            HttpServletRequest request,
            @Parameter(description = "关卡ID", required = true) @RequestParam Integer levelId,
            @Parameter(description = "战斗ID", required = true) @RequestParam String battleId) {
        try {
            String token = getToken(request);
            Long userId = jwtUtil.getUserIdFromToken(token);
            return battleService.claimReward(userId, levelId, battleId);
        } catch (Exception e) {
            return Result.error("领取奖励失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取今日收益信息
     */
    @GetMapping("/daily-info")
    @Operation(summary = "获取今日收益信息", description = "获取今日经验、金币收益和剩余配额")
    public Result<?> getDailyRewardInfo(HttpServletRequest request) {
        try {
            String token = getToken(request);
            Long userId = jwtUtil.getUserIdFromToken(token);
            return battleService.getDailyRewardInfo(userId);
        } catch (Exception e) {
            return Result.error("获取收益信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取AI战报总结
     */
    @GetMapping("/ai/summary")
    @Operation(summary = "获取AI战报总结", description = "战斗结束后获取AI生成的战报总结")
    public Result<?> getBattleSummary(HttpServletRequest request) {
        try {
            String token = getToken(request);
            Long userId = jwtUtil.getUserIdFromToken(token);
            return battleService.getBattleSummary(userId);
        } catch (Exception e) {
            return Result.error("获取AI战报总结失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取战斗日志
     */
    @GetMapping("/logs")
    @Operation(summary = "获取战斗日志", description = "获取指定战斗的所有日志记录")
    public Result<?> getBattleLogs(
            HttpServletRequest request,
            @Parameter(description = "战斗ID", required = true) @RequestParam String battleId) {
        try {
            String token = getToken(request);
            Long userId = jwtUtil.getUserIdFromToken(token);
            return battleService.getBattleLogs(battleId);
        } catch (Exception e) {
            return Result.error("获取战斗日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 从请求头获取token
     */
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("未登录，无权限访问");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }
}