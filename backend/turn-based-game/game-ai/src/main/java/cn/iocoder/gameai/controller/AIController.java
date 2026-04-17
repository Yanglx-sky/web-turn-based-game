package cn.iocoder.gameai.controller;

import cn.iocoder.gameai.service.AIService;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI助手控制器
 * 处理AI会话、对话、训练总结等功能
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AIController {
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return null;
        }
        // 去除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 创建会话
     */
    @PostMapping("/session/create")
    public Result<Long> createSession(HttpServletRequest request,
                             @RequestParam(required = false) String title,
                             @RequestParam(required = false) String scene) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error("未登录，无权限访问");
            }
            log.info("创建会话 - userId: {}, title: {}, scene: {}", userId, title, scene);
            
            Long sessionId = aiService.createSession(userId, title, scene);
            log.info("创建会话成功 - sessionId: {}", sessionId);
            
            return Result.success(sessionId);
        } catch (Exception e) {
            log.error("创建会话失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 新增对话
     */
    @PostMapping("/conversation/add")
    public Result<String> addConversation(HttpServletRequest request,
                                @RequestParam(required = false) String sessionId,
                                @RequestParam(required = false) String content) {
        if (sessionId == null || "undefined".equals(sessionId) || "".equals(sessionId) || content == null) {
            return Result.error("会话ID和内容不能为空");
        }
        
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error("未登录，无权限访问");
            }
            Long sessionIdLong = Long.parseLong(sessionId);
            return Result.success(aiService.addConversation(sessionIdLong, content));
        } catch (Exception e) {
            log.error("新增对话失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 关闭会话
     */
    @PostMapping("/session/close")
    public Result<Boolean> closeSession(HttpServletRequest request,
                               @RequestParam(required = false) String sessionId) {
        if (sessionId == null || "undefined".equals(sessionId) || "".equals(sessionId)) {
            return Result.error("会话ID不能为空");
        }
        
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error("未登录，无权限访问");
            }
            Long sessionIdLong = Long.parseLong(sessionId);
            return Result.success(aiService.closeSession(sessionIdLong, userId));
        } catch (Exception e) {
            log.error("关闭会话失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取会话列表
     */
    @GetMapping("/session/list")
    public Result<List<Map<String, Object>>> getSessionList(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error("未登录，无权限访问");
            }
            return Result.success(aiService.getSessionList(userId));
        } catch (Exception e) {
            log.error("获取会话列表失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新会话标题
     */
    @PostMapping("/session/update-title")
    public Result<Boolean> updateSessionTitle(HttpServletRequest request,
                                     @RequestParam(required = false) String sessionId,
                                     @RequestParam(required = false) String title) {
        if (sessionId == null || "undefined".equals(sessionId) || "".equals(sessionId) || title == null) {
            return Result.error("会话ID和标题不能为空");
        }
        
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error("未登录，无权限访问");
            }
            Long sessionIdLong = Long.parseLong(sessionId);
            return Result.success(aiService.updateSessionTitle(sessionIdLong, userId, title));
        } catch (Exception e) {
            log.error("更新会话标题失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取训练总结
     */
    @GetMapping("/training/summary")
    public Result<String> getTrainingSummary(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error("未登录，无权限访问");
            }
            return Result.success(aiService.getTrainingSummary(userId));
        } catch (Exception e) {
            log.error("获取训练总结失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * SSE流式输出
     */
    @GetMapping("/stream/analysis")
    public void streamAIAnalysis(HttpServletRequest request,
                                 @RequestParam(required = false) String sessionId,
                                 @RequestParam(required = false) String content,
                                 HttpServletResponse response) {
        if (sessionId == null || "undefined".equals(sessionId) || "".equals(sessionId) || content == null) {
            response.setStatus(400);
            response.setContentType("application/json; charset=utf-8");
            try {
                response.getWriter().write("{\"code\": 400, \"message\": \"会话ID和内容不能为空\", \"data\": null}");
            } catch (Exception e) {
                log.error("写入响应失败", e);
            }
            return;
        }
        
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                response.setStatus(401);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"code\": 401, \"message\": \"未登录，无权限访问\", \"data\": null}");
                return;
            }
            aiService.streamAIAnalysis(sessionId, content, response, userId);
        } catch (Exception e) {
            log.error("流式输出失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查AI调用次数限制
     */
    @GetMapping("/check/limit")
    public Result<Boolean> checkAICallLimit(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error("未登录，无权限访问");
            }
            return Result.success(aiService.checkAICallLimit(userId));
        } catch (Exception e) {
            log.error("检查AI调用次数限制失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户今日AI调用次数
     */
    @GetMapping("/get/count")
    public Result<Integer> getAITodayCallCount(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error("未登录，无权限访问");
            }
            return Result.success(aiService.getAITodayCallCount(userId));
        } catch (Exception e) {
            log.error("获取AI调用次数失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
}
