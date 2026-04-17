package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.service.TrainService;
import cn.iocoder.gamemodules.service.impl.TrainServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/train")
@Tag(name = "训练系统", description = "单人训练功能")
@Loggable
public class TrainController {

    @Autowired
    private TrainService trainService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 解析对象为Integer
     */
    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @PostMapping("/create")
    @Operation(summary = "创建训练人偶", description = "创建自定义训练人偶")
    public Result<Map<String, Object>> createMannequin(HttpServletRequest request,
            @RequestBody Map<String, Object> params) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            // 从请求体中获取参数
            Integer attack = parseInteger(params.get("attack"));
            Integer defense = parseInteger(params.get("defense"));
            Integer hp = parseInteger(params.get("hp"));
            Integer mp = parseInteger(params.get("mp"));
            Integer speed = parseInteger(params.get("speed"));
            Integer type = parseInteger(params.get("type"));
            Integer isAttack = parseInteger(params.get("isAttack"));
            return trainService.createMannequin(userId, attack, defense, hp, mp, speed, type, isAttack);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取用户信息失败：" + e.getMessage());
        }
    }

    @PostMapping("/start")
    @Operation(summary = "开始训练", description = "开始单人训练")
    public Result<Map<String, Object>> startTrain(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> params) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            // 检查是否是直接传递训练人偶属性
            Object attackObj = params != null ? params.get("attack") : null;
            Object defenseObj = params != null ? params.get("defense") : null;
            Object hpObj = params != null ? params.get("hp") : null;
            Object mpObj = params != null ? params.get("mp") : null;
            Object speedObj = params != null ? params.get("speed") : null;
            Object typeObj = params != null ? params.get("type") : null;
            Object isAttackObj = params != null ? params.get("isAttack") : null;
            
            if (attackObj != null && defenseObj != null && hpObj != null && mpObj != null && typeObj != null) {
                // 直接传递训练人偶属性
                Integer attack = parseInteger(attackObj);
                Integer defense = parseInteger(defenseObj);
                Integer hp = parseInteger(hpObj);
                Integer mp = parseInteger(mpObj);
                Integer speed = parseInteger(speedObj);
                Integer type = parseInteger(typeObj);
                Integer isAttack = parseInteger(isAttackObj != null ? isAttackObj : 1);
                
                return ((TrainServiceImpl) trainService).startTrainWithMannequinParams(userId, attack, defense, hp, mp, speed, type, isAttack);
            } else {
                // 从数据库读取训练人偶
                Object mannequinIdObj = params != null ? params.get("mannequinId") : null;
                if (mannequinIdObj == null) {
                    return Result.error("参数错误：mannequinId或训练人偶属性不能为空");
                }
                Long mannequinId;
                if (mannequinIdObj instanceof Number) {
                    mannequinId = ((Number) mannequinIdObj).longValue();
                } else if (mannequinIdObj instanceof String) {
                    try {
                        mannequinId = Long.parseLong((String) mannequinIdObj);
                    } catch (NumberFormatException e) {
                        return Result.error("参数错误：mannequinId格式不正确");
                    }
                } else {
                    return Result.error("参数错误：mannequinId类型不正确");
                }
                return trainService.startTrain(userId, mannequinId);
            }
        } catch (Exception e) {
            return Result.error("获取用户信息失败：" + e.getMessage());
        }
    }

    @PostMapping("/attack")
    @Operation(summary = "普通攻击", description = "训练中使用普通攻击")
    public Result<Map<String, Object>> normalAttack(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return trainService.normalAttack(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @PostMapping("/skill")
    @Operation(summary = "使用技能", description = "训练中使用技能")
    public Result<Map<String, Object>> useSkill(HttpServletRequest request,
            @RequestBody Map<String, Object> params) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            // 从请求体中获取参数
            Object skillIdObj = params.get("skillId");
            if (skillIdObj == null) {
                return Result.error("参数错误：skillId不能为空");
            }
            Integer skillId;
            if (skillIdObj instanceof Number) {
                skillId = ((Number) skillIdObj).intValue();
            } else if (skillIdObj instanceof String) {
                try {
                    skillId = Integer.parseInt((String) skillIdObj);
                } catch (NumberFormatException e) {
                    return Result.error("参数错误：skillId格式不正确");
                }
            } else {
                return Result.error("参数错误：skillId类型不正确");
            }
            return trainService.useSkill(userId, skillId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败：" + e.getMessage());
        }
    }

    @PostMapping("/monster_turn")
    @Operation(summary = "执行人偶行动", description = "训练中执行训练人偶行动")
    public Result<Map<String, Object>> executeMannequinTurn(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return trainService.executeMannequinTurn(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @PostMapping("/flee")
    @Operation(summary = "训练逃跑", description = "训练中逃跑")
    public Result<Map<String, Object>> flee(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return trainService.flee(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @PostMapping("/switch")
    @Operation(summary = "训练切换精灵", description = "训练中切换精灵")
    public Result<Map<String, Object>> switchElf(HttpServletRequest request,
            @RequestBody Map<String, Object> params) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            // 从请求体中获取参数
            Object elfIdObj = params.get("elfId");
            if (elfIdObj == null) {
                return Result.error("参数错误：elfId不能为空");
            }
            Long elfId;
            if (elfIdObj instanceof Number) {
                elfId = ((Number) elfIdObj).longValue();
            } else if (elfIdObj instanceof String) {
                try {
                    elfId = Long.parseLong((String) elfIdObj);
                } catch (NumberFormatException e) {
                    return Result.error("参数错误：elfId格式不正确");
                }
            } else {
                return Result.error("参数错误：elfId类型不正确");
            }
            return trainService.switchElf(userId, elfId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败：" + e.getMessage());
        }
    }

    @PostMapping("/settlement")
    @Operation(summary = "训练结算", description = "训练结束后进行结算")
    public Result<Map<String, Object>> trainSettlement(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return trainService.trainSettlement(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @GetMapping("/records")
    @Operation(summary = "获取训练记录", description = "获取用户的训练记录")
    public Result<Map<String, Object>> getTrainRecords(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return trainService.getTrainRecords(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @GetMapping("/battle_elves")
    @Operation(summary = "获取训练中的出战精灵", description = "获取训练模式下的出战精灵列表")
    public Result<List<Map<String, Object>>> getBattleElves(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return trainService.getBattleElves(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }
    
    @GetMapping("/logs")
    @Operation(summary = "获取训练日志", description = "获取指定训练的所有日志记录")
    public Result<List<Map<String, Object>>> getTrainLogs(
            HttpServletRequest request,
            @Parameter(description = "训练ID", required = true) @RequestParam String trainId) {
        try {
            return trainService.getTrainLogs(trainId);
        } catch (Exception e) {
            return Result.error("获取训练日志失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/use_potion")
    @Operation(summary = "训练使用药品", description = "训练中使用药品恢复精灵状态")
    public Result<Map<String, Object>> usePotion(HttpServletRequest request,
            @RequestParam Long elfId,
            @RequestParam Long potionId) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return trainService.usePotion(userId, elfId, potionId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @PutMapping("/offline")
    @Operation(summary = "训练玩家离线", description = "训练模式中玩家离线，暂停训练")
    public Result<Map<String, Object>> playerOffline(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return trainService.playerOffline(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }
}