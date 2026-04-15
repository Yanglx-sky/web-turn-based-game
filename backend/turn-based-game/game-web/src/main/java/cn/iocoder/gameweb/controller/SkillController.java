package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.entity.Skill;
import cn.iocoder.gamemodules.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/skill")
@Loggable
@Tag(name = "技能管理", description = "技能相关接口")
public class SkillController {

    @Autowired
    private SkillService skillService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "获取技能列表", description = "获取所有技能列表")
    public Result<List<Skill>> getSkillList() {
        return skillService.getSkillList();
    }

    @GetMapping("/elves/{elfId}")
    @Operation(summary = "获取精灵技能", description = "获取指定精灵的技能列表")
    public Result<List<Skill>> getElfSkills(
            @Parameter(name = "elfId", description = "精灵ID", required = true) @PathVariable Integer elfId) {
        return skillService.getElfSkills(elfId);
    }

    @PostMapping("/unlock")
    @Operation(summary = "解锁技能", description = "为精灵解锁技能")
    public Result<Skill> unlockSkill(HttpServletRequest request,
            @RequestParam(name = "elfId", required = true) @Parameter(name = "elfId", description = "精灵ID", required = true) Integer elfId,
            @RequestParam(name = "skillId", required = true) @Parameter(name = "skillId", description = "技能ID", required = true) Integer skillId) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return skillService.unlockSkill(userId, elfId, skillId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @GetMapping("/elves/{elfId}/unlocked")
    @Operation(summary = "获取已解锁技能", description = "获取精灵已解锁的技能列表")
    public Result<List<Skill>> getUnlockedSkills(HttpServletRequest request,
            @PathVariable Integer elfId) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return skillService.getUnlockedSkills(userId, elfId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }
}