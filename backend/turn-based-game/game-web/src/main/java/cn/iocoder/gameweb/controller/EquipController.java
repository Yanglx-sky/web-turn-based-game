package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.service.EquipService;
import cn.iocoder.gamemodules.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 装备控制器
 * 处理装备查询、购买、穿戴、卸下等功能
 */
@RestController
@RequestMapping("/equips")
@Tag(name = "装备管理", description = "装备查询、购买、穿戴、卸下相关接口")
@Loggable
@Slf4j
public class EquipController {

    @Autowired
    private EquipService equipService;

    @Autowired
    private ShopService shopService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取所有装备
     */
    @GetMapping
    @Operation(summary = "获取所有装备", description = "获取商店中所有可购买的装备列表")
    public Result<?> getAllEquips() {
        return shopService.getAllItems();
    }

    /**
     * 根据类型获取装备
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "根据类型获取装备", description = "获取指定类型的装备列表（1:武器 2:防具 3:药品）")
    public Result<?> getEquipsByType(
            @Parameter(name = "type", description = "装备类型", required = true) @PathVariable Integer type) {
        return shopService.getItemsByType(type);
    }

    /**
     * 获取装备详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取装备详情", description = "获取指定装备的详细信息")
    public Result<?> getEquipById(
            @Parameter(name = "id", description = "装备ID", required = true) @PathVariable Long id) {
        return equipService.getEquipById(id);
    }

    /**
     * 购买装备
     */
    @PostMapping
    @Operation(summary = "购买装备", description = "购买指定装备")
    public Result<?> buyEquip(HttpServletRequest request,
            @Parameter(name = "equipId", description = "装备ID", required = true) @RequestParam Long equipId) {
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
            return shopService.buyItem(userId, equipId);
        } catch (Exception e) {
            log.error("装备武器失败 - 获取用户信息异常: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 装备武器
     */
    @PutMapping("/weapon")
    @Operation(summary = "装备武器", description = "为指定精灵装备武器")
    public Result<?> equipWeapon(HttpServletRequest request,
            @Parameter(name = "elfId", description = "精灵ID", required = true) @RequestParam Long elfId,
            @Parameter(name = "weaponId", description = "武器ID", required = true) @RequestParam Long weaponId) {
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
            return equipService.equipWeapon(userId, elfId, weaponId);
        } catch (Exception e) {
            log.error("装备武器失败 - 获取用户信息异常: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 装备防具
     */
    @PutMapping("/armor")
    @Operation(summary = "装备防具", description = "为指定精灵装备防具")
    public Result<?> equipArmor(HttpServletRequest request,
            @Parameter(name = "elfId", description = "精灵ID", required = true) @RequestParam Long elfId,
            @Parameter(name = "armorId", description = "防具ID", required = true) @RequestParam Long armorId) {
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
            return equipService.equipArmor(userId, elfId, armorId);
        } catch (Exception e) {
            log.error("装备武器失败 - 获取用户信息异常: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 卸下武器
     */
    @DeleteMapping("/weapon")
    @Operation(summary = "卸下武器", description = "为指定精灵卸下武器")
    public Result<?> unequipWeapon(HttpServletRequest request,
            @Parameter(name = "elfId", description = "精灵ID", required = true) @RequestParam Long elfId) {
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
            return equipService.unequipWeapon(userId, elfId);
        } catch (Exception e) {
            log.error("装备武器失败 - 获取用户信息异常: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 卸下防具
     */
    @DeleteMapping("/armor")
    @Operation(summary = "卸下防具", description = "为指定精灵卸下防具")
    public Result<?> unequipArmor(HttpServletRequest request,
            @Parameter(name = "elfId", description = "精灵ID", required = true) @RequestParam Long elfId) {
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
            return equipService.unequipArmor(userId, elfId);
        } catch (Exception e) {
            log.error("装备武器失败 - 获取用户信息异常: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户拥有的指定类型装备
     */
    @GetMapping("/user/type")
    @Operation(summary = "获取用户装备", description = "获取用户拥有的指定类型装备")
    public Result<?> getUserEquipsByType(HttpServletRequest request,
            @Parameter(name = "type", description = "装备类型（1:武器 2:防具）", required = true) @RequestParam Integer type) {
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
            // 验证Token格式
            if (token.isEmpty() || !token.contains(".")) {
                log.warn("Token格式错误: token为空或缺少点号分隔符");
                return Result.error("登录已过期，请重新登录");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return shopService.getUserEquipsByType(userId, type);
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Token格式异常: {}", e.getMessage());
            return Result.error("登录已过期，请重新登录");
        } catch (Exception e) {
            log.error("装备武器失败 - 获取用户信息异常: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
}