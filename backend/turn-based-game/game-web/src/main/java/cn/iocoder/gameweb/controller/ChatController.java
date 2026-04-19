package cn.iocoder.gameweb.controller;

import cn.iocoder.gamemodules.entity.ChatChannel;
import cn.iocoder.gamemodules.entity.ChatContent;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.service.ChatService;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamecommon.annotation.Loggable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 * 处理频道聊天、私聊、历史消息查询等功能
 */
@RestController
@RequestMapping("/chat")
@Tag(name = "聊天管理", description = "聊天频道、私聊、历史消息相关接口")
@Loggable
public class ChatController {

    @Autowired
    private ChatService chatService;

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
     * 获取频道列表
     */
    @GetMapping("/channels")
    @Operation(summary = "获取频道列表", description = "获取所有启用的聊天频道")
    public Result<List<ChatChannel>> getChannelList() {
        return chatService.getChannelList();
    }

    /**
     * 获取频道历史消息（ID游标分页）
     */
    @GetMapping("/channels/{channelId}/messages")
    @Operation(summary = "获取频道历史消息", description = "使用ID游标分页查询频道历史消息")
    public Result<Map<String, Object>> getChannelMessages(
            @PathVariable Long channelId,
            @Parameter(name = "lastId", description = "上一页最后一条消息ID（首次加载不传）") @RequestParam(required = false) Long lastId,
            @Parameter(name = "pageSize", description = "每页大小（默认20，最大100）") @RequestParam(required = false) Integer pageSize) {
        
        return chatService.getChannelMessages(channelId, lastId, pageSize);
    }

    /**
     * 获取私聊历史消息（ID游标分页）
     */
    @GetMapping("/private/{friendId}/messages")
    @Operation(summary = "获取私聊历史消息", description = "使用ID游标分页查询私聊历史消息")
    public Result<Map<String, Object>> getPrivateMessages(
            HttpServletRequest request,
            @PathVariable Long friendId,
            @Parameter(name = "lastId", description = "上一页最后一条消息ID（首次加载不传）") @RequestParam(required = false) Long lastId,
            @Parameter(name = "pageSize", description = "每页大小（默认20，最大100）") @RequestParam(required = false) Integer pageSize) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return chatService.getPrivateMessages(userId, friendId, lastId, pageSize);
    }

    /**
     * 撤回消息
     */
    @DeleteMapping("/messages/{msgId}")
    @Operation(summary = "撤回消息", description = "撤回自己2分钟内发送的消息")
    public Result<Void> recallMessage(
            HttpServletRequest request,
            @Parameter(name = "msgId", description = "消息ID", required = true) @PathVariable Long msgId) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return chatService.recallMessage(userId, msgId);
    }
}
