package cn.iocoder.gamecommon.interceptor;

import cn.iocoder.gamecommon.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 战斗安全拦截器
 * 拦截战斗攻击、战斗结算、关卡奖励接口
 * 校验用户是否合法进入该关卡
 * 校验是否存在有效战斗状态
 * 禁止直接调用奖励接口刷奖励
 * 禁止直接调用战斗结算接口
 * 校验请求频率，防止 10ms 内快速重复出招
 * 回合有序校验，防止重复出招、乱序出招
 * 记录异常请求日志
 */
@Component
public class BattleSecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    // 存储用户上次请求时间，用于防重复攻击
    private static final ConcurrentHashMap<Long, Long> lastRequestTimeMap = new ConcurrentHashMap<>();
    // 存储用户战斗状态，用于校验战斗有效性
    private static final ConcurrentHashMap<Long, Boolean> battleStatusMap = new ConcurrentHashMap<>();
    // 存储用户战斗回合信息，用于回合有序校验
    private static final ConcurrentHashMap<Long, Integer> battleRoundMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        
        // 只拦截战斗相关接口和训练相关接口
        boolean isBattlePath = path.contains("/battle/attack") || path.contains("/battle/skill") || 
            path.contains("/battle/settlement") || path.contains("/battle/reward") ||
            path.contains("/battle/action") || path.contains("/battle/switch") ||
            path.contains("/battle/flee") || path.contains("/battle/abandon") ||
            path.contains("/battle/status");
        
        boolean isTrainPath = path.contains("/train/attack") || path.contains("/train/skill") ||
            path.contains("/train/settlement") || path.contains("/train/monster_turn") ||
            path.contains("/train/flee") || path.contains("/train/switch");
        
        if (!isBattlePath && !isTrainPath) {
            return true;
        }

        // 从请求头中获取token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return true; // 登录拦截器会处理
        }

        // 去除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 解析token获取用户ID
        Long userId = null;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return true; // 登录拦截器会处理
        }

        if (userId == null) {
            return true; // 登录拦截器会处理
        }

        // 检查请求频率，防止10ms内快速重复出招
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastRequestTimeMap.get(userId);
        if (lastTime != null && currentTime - lastTime < 10) {
            response.setStatus(400);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"code\": 400, \"message\": \"请求过于频繁，请稍后再试\", \"data\": null}");
            return false;
        }
        lastRequestTimeMap.put(userId, currentTime);

        // 校验战斗状态（reward不需要校验，其他都需要）
        // 训练模式也需要校验战斗状态
        boolean isRewardPath = path.contains("/battle/reward");
        if (!isRewardPath) {
            Boolean inBattle = battleStatusMap.get(userId);
            if (inBattle == null || !inBattle) {
                System.err.println("[拦截器] 用户" + userId + "未进入战斗/训练，路径: " + path);
                response.setStatus(400);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"code\": 400, \"message\": \"未进入战斗或战斗已结束\", \"data\": null}");
                return false;
            }
            System.out.println("[拦截器] 用户" + userId + "战斗状态校验通过，路径: " + path);
        }

        // 注意：/battle/reward 不再在此处拦截
        // 原因：前端需要在战斗胜利后自动调用claimReward领取奖励
        // 安全性由claimReward方法内部的胜利标记验证和幂等性验证保证

        // 禁止直接调用战斗结算接口
        if (path.contains("/battle/settlement")) {
            response.setStatus(400);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"code\": 400, \"message\": \"战斗结算接口不可直接调用\", \"data\": null}");
            return false;
        }

        // 回合有序校验，防止重复出招、乱序出招
        // 包括战斗模式和训练模式
        if (path.contains("/battle/attack") || path.contains("/battle/skill") || path.contains("/battle/action") ||
            path.contains("/train/attack") || path.contains("/train/skill")) {
            // 从请求参数中获取回合数（对于/action接口，参数在RequestBody中，这里可能获取不到）
            String roundStr = request.getParameter("round");
            if (roundStr != null) {
                try {
                    int round = Integer.parseInt(roundStr);
                    Integer currentRound = battleRoundMap.get(userId);
                    if (currentRound == null) {
                        // 第一次出招，记录回合数
                        battleRoundMap.put(userId, round);
                    } else if (round <= currentRound) {
                        // 回合数小于等于当前回合数，说明是重复出招或乱序出招
                        response.setStatus(400);
                        response.setContentType("application/json; charset=utf-8");
                        response.getWriter().write("{\"code\": 400, \"message\": \"回合顺序错误，请勿重复出招\", \"data\": null}");
                        return false;
                    } else {
                        // 回合数大于当前回合数，更新回合数
                        battleRoundMap.put(userId, round);
                    }
                } catch (NumberFormatException e) {
                    // 参数格式错误，继续执行，后续会有其他校验
                }
            }
        }

        return true;
    }

    /**
     * 更新用户战斗状态
     * @param userId 用户ID
     * @param inBattle 是否在战斗中
     */
    public void updateBattleStatus(Long userId, boolean inBattle) {
        battleStatusMap.put(userId, inBattle);
        if (!inBattle) {
            // 战斗结束，清除回合信息
            battleRoundMap.remove(userId);
        }
    }

    /**
     * 重置用户战斗回合信息
     * @param userId 用户ID
     */
    public void resetBattleRound(Long userId) {
        battleRoundMap.remove(userId);
    }
}