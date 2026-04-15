package cn.iocoder.gameai.controller;

import cn.iocoder.gameai.service.AIService;
import cn.iocoder.gamecommon.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AI助手控制器
 * 处理AI会话、对话、训练总结等功能
 */
@RestController
@RequestMapping("/ai")
public class AIController {
    
    private static final Logger logger = LoggerFactory.getLogger(AIController.class);
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 创建会话
     */
    @PostMapping("/session/create")
    public Object createSession(HttpServletRequest request,
                             @RequestParam(required = false) String title,
                             @RequestParam(required = false) String scene) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                logger.warn("创建会话失败：token为空");
                return cn.iocoder.gamecommon.result.Result.error("未登录，无权限访问");
            }
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            logger.info("创建会话 - userId: {}, title: {}, scene: {}", userId, title, scene);
            
            Long sessionId = aiService.createSession(userId, title, scene);
            logger.info("创建会话成功 - sessionId: {}", sessionId);
            
            return cn.iocoder.gamecommon.result.Result.success(sessionId);
        } catch (Exception e) {
            logger.error("创建会话异常", e);
            return cn.iocoder.gamecommon.result.Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 新增对话
     */
    @PostMapping("/conversation/add")
    public Object addConversation(HttpServletRequest request,
                                @RequestParam(required = false) String sessionId,
                                @RequestParam(required = false) String content) {
        if (sessionId == null || "undefined".equals(sessionId) || "".equals(sessionId) || content == null) {
            return cn.iocoder.gamecommon.result.Result.error("会话ID和内容不能为空");
        }
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return cn.iocoder.gamecommon.result.Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            Long sessionIdLong = Long.parseLong(sessionId);
            return aiService.addConversation(sessionIdLong, content);
        } catch (NumberFormatException e) {
            return cn.iocoder.gamecommon.result.Result.error("会话ID格式错误");
        } catch (Exception e) {
            return cn.iocoder.gamecommon.result.Result.error("获取用户信息失败");
        }
    }
    
    /**
     * 关闭会话
     */
    @PostMapping("/session/close")
    public Object closeSession(HttpServletRequest request,
                               @RequestParam(required = false) String sessionId) {
        if (sessionId == null || "undefined".equals(sessionId) || "".equals(sessionId)) {
            return cn.iocoder.gamecommon.result.Result.error("会话ID不能为空");
        }
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return cn.iocoder.gamecommon.result.Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            Long sessionIdLong = Long.parseLong(sessionId);
            return aiService.closeSession(sessionIdLong, userId);
        } catch (NumberFormatException e) {
            return cn.iocoder.gamecommon.result.Result.error("会话ID格式错误");
        } catch (Exception e) {
            return cn.iocoder.gamecommon.result.Result.error("获取用户信息失败");
        }
    }
    
    /**
     * 获取会话列表
     */
    @GetMapping("/session/list")
    public Object getSessionList(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return cn.iocoder.gamecommon.result.Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return aiService.getSessionList(userId);
        } catch (Exception e) {
            return cn.iocoder.gamecommon.result.Result.error("获取用户信息失败");
        }
    }
    
    /**
     * 更新会话标题
     */
    @PostMapping("/session/update-title")
    public Object updateSessionTitle(HttpServletRequest request,
                                     @RequestParam(required = false) String sessionId,
                                     @RequestParam(required = false) String title) {
        if (sessionId == null || "undefined".equals(sessionId) || "".equals(sessionId) || title == null) {
            return cn.iocoder.gamecommon.result.Result.error("会话ID和标题不能为空");
        }
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return cn.iocoder.gamecommon.result.Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            Long sessionIdLong = Long.parseLong(sessionId);
            return aiService.updateSessionTitle(sessionIdLong, userId, title);
        } catch (NumberFormatException e) {
            return cn.iocoder.gamecommon.result.Result.error("会话ID格式错误");
        } catch (Exception e) {
            return cn.iocoder.gamecommon.result.Result.error("获取用户信息失败");
        }
    }
    
    /**
     * 获取训练总结
     */
    @GetMapping("/training/summary")
    public Object getTrainingSummary(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return cn.iocoder.gamecommon.result.Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return aiService.getTrainingSummary(userId);
        } catch (Exception e) {
            return cn.iocoder.gamecommon.result.Result.error("获取用户信息失败");
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
        try {
            if (sessionId == null || "undefined".equals(sessionId) || "".equals(sessionId) || content == null) {
                response.setStatus(400);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"code\": 400, \"message\": \"会话ID和内容不能为空\", \"data\": null}");
                return;
            }
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                response.setStatus(401);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"code\": 401, \"message\": \"未登录，无权限访问\", \"data\": null}");
                return;
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            aiService.streamAIAnalysis(sessionId, content, response);
        } catch (Exception e) {
            try {
                response.setStatus(500);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"code\": 500, \"message\": \"服务器内部错误\", \"data\": null}");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * 检查AI调用次数限制
     */
    @GetMapping("/check/limit")
    public Object checkAICallLimit(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return cn.iocoder.gamecommon.result.Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return aiService.checkAICallLimit(userId);
        } catch (Exception e) {
            return cn.iocoder.gamecommon.result.Result.error("获取用户信息失败");
        }
    }
    
    /**
     * 获取用户今日AI调用次数
     */
    @GetMapping("/get/count")
    public Object getAITodayCallCount(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return cn.iocoder.gamecommon.result.Result.error("未登录，无权限访问");
            }
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return aiService.getAITodayCallCount(userId);
        } catch (Exception e) {
            return cn.iocoder.gamecommon.result.Result.error("获取用户信息失败");
        }
    }
}
