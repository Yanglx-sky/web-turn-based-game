package cn.iocoder.gameweb.controller;

import cn.iocoder.gamemodules.entity.FriendRelation;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.service.FriendService;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamecommon.annotation.Loggable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友控制器
 * 处理好友申请、好友管理、拉黑等功能
 */
@RestController
@RequestMapping("/friends")
@Tag(name = "好友管理", description = "好友申请、好友列表、拉黑相关接口")
@Loggable
public class FriendController {

    @Autowired
    private FriendService friendService;

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
     * 发送好友申请
     */
    @PostMapping("/requests")
    @Operation(summary = "发送好友申请", description = "向指定用户发送好友申请")
    public Result<Void> sendFriendRequest(
            HttpServletRequest request,
            @Parameter(name = "friendId", description = "被申请人ID", required = true) @RequestParam Long friendId,
            @Parameter(name = "remark", description = "备注名称") @RequestParam(required = false) String remark) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.sendFriendRequest(userId, friendId, remark);
    }

    /**
     * 同意好友申请
     */
    @PutMapping("/requests/{friendId}/accept")
    @Operation(summary = "同意好友申请", description = "同意指定用户的好友申请")
    public Result<Void> acceptFriendRequest(
            HttpServletRequest request,
            @Parameter(name = "friendId", description = "申请人ID", required = true) @PathVariable Long friendId) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.acceptFriendRequest(userId, friendId);
    }

    /**
     * 拒绝好友申请
     */
    @PutMapping("/requests/{friendId}/reject")
    @Operation(summary = "拒绝好友申请", description = "拒绝指定用户的好友申请")
    public Result<Void> rejectFriendRequest(
            HttpServletRequest request,
            @Parameter(name = "friendId", description = "申请人ID", required = true) @PathVariable Long friendId) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.rejectFriendRequest(userId, friendId);
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    @Operation(summary = "删除好友", description = "删除指定好友")
    public Result<Void> deleteFriend(
            HttpServletRequest request,
            @Parameter(name = "friendId", description = "好友ID", required = true) @PathVariable Long friendId) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.deleteFriend(userId, friendId);
    }

    /**
     * 获取好友列表
     */
    @GetMapping
    @Operation(summary = "获取好友列表", description = "获取当前用户的所有好友")
    public Result<List<FriendRelation>> getFriendList(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.getFriendList(userId);
    }

    /**
     * 获取待确认的好友申请列表
     */
    @GetMapping("/requests/pending")
    @Operation(summary = "获取待确认申请", description = "获取待确认的好友申请列表")
    public Result<List<FriendRelation>> getPendingRequests(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.getPendingRequests(userId);
    }

    /**
     * 拉黑好友
     */
    @PutMapping("/{friendId}/blacklist")
    @Operation(summary = "拉黑好友", description = "将指定好友加入黑名单")
    public Result<Void> blacklistFriend(
            HttpServletRequest request,
            @Parameter(name = "friendId", description = "好友ID", required = true) @PathVariable Long friendId) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.blacklistFriend(userId, friendId);
    }

    /**
     * 解除拉黑
     */
    @PutMapping("/{friendId}/unblacklist")
    @Operation(summary = "解除拉黑", description = "将指定用户从黑名单移除")
    public Result<Void> unblacklistFriend(
            HttpServletRequest request,
            @Parameter(name = "friendId", description = "好友ID", required = true) @PathVariable Long friendId) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.unblacklistFriend(userId, friendId);
    }

    /**
     * 获取黑名单列表
     */
    @GetMapping("/blacklists")
    @Operation(summary = "获取黑名单", description = "获取当前用户的黑名单列表")
    public Result<List<FriendRelation>> getBlacklist(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.getBlacklist(userId);
    }

    /**
     * 更新好友备注
     */
    @PutMapping("/{friendId}/remark")
    @Operation(summary = "更新好友备注", description = "更新指定好友的备注名称")
    public Result<Void> updateRemark(
            HttpServletRequest request,
            @Parameter(name = "friendId", description = "好友ID", required = true) @PathVariable Long friendId,
            @Parameter(name = "remark", description = "备注名称", required = true) @RequestParam String remark) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        return friendService.updateRemark(userId, friendId, remark);
    }

    /**
     * 检查是否为好友
     */
    @GetMapping("/{friendId}/check")
    @Operation(summary = "检查好友关系", description = "检查与指定用户是否为好友关系")
    public Result<Boolean> checkFriendship(
            HttpServletRequest request,
            @Parameter(name = "friendId", description = "好友ID", required = true) @PathVariable Long friendId) {
        
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("未登录，无权限访问");
        }

        boolean isFriend = friendService.isFriend(userId, friendId);
        return Result.success(isFriend);
    }
}
