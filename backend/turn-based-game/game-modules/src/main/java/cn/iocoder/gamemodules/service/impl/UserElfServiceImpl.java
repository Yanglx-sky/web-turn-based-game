package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.Elf;
import cn.iocoder.gamemodules.entity.ElfSkill;
import cn.iocoder.gamemodules.entity.Skill;
import cn.iocoder.gamemodules.entity.UserElf;
import cn.iocoder.gamemodules.mapper.ElfMapper;
import cn.iocoder.gamemodules.mapper.ElfSkillMapper;
import cn.iocoder.gamemodules.mapper.SkillMapper;
import cn.iocoder.gamemodules.mapper.UserElfMapper;
import cn.iocoder.gamemodules.service.UserElfService;
import cn.iocoder.gamemodules.service.AchievementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserElfServiceImpl extends ServiceImpl<UserElfMapper, UserElf> implements UserElfService {

    @Autowired
    private ElfSkillMapper elfSkillMapper;

    @Autowired
    private SkillMapper skillMapper;
    
    @Autowired
    private ElfMapper elfMapper;

    @Autowired
    private AchievementService achievementService;

    @Override
    public Result<List<Map<String, Object>>> getUserElfList(Long userId) {
        List<UserElf> elfList = lambdaQuery().eq(UserElf::getUserId, userId).list();
        
        // 转换为包含精灵名字的Map列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserElf userElf : elfList) {
            Map<String, Object> elfMap = new HashMap<>();
            // 复制UserElf的所有属性
            elfMap.put("id", userElf.getId());
            elfMap.put("userId", userElf.getUserId());
            elfMap.put("elfId", userElf.getElfId());
            elfMap.put("level", userElf.getLevel());
            elfMap.put("exp", userElf.getExp());
            elfMap.put("expNeed", userElf.getExpNeed());
            elfMap.put("maxHp", userElf.getMaxHp());
            elfMap.put("maxMp", userElf.getMaxMp());
            elfMap.put("hp", userElf.getHp());
            elfMap.put("mp", userElf.getMp());
            elfMap.put("attack", userElf.getAttack());
            elfMap.put("defense", userElf.getDefense());
            elfMap.put("normalDamage", userElf.getNormalDamage());
            elfMap.put("speed", userElf.getSpeed());
            elfMap.put("isFight", userElf.getIsFight());
            elfMap.put("fightOrder", userElf.getFightOrder());
            elfMap.put("status", userElf.getStatus());
            
            // 添加精灵名字和系别
            Elf elf = elfMapper.selectById(userElf.getElfId());
            if (elf != null && elf.getElfName() != null) {
                elfMap.put("elfName", elf.getElfName());
                elfMap.put("elementType", elf.getElementType());
            } else {
                // 添加默认精灵名字和系别
                switch (userElf.getElfId()) {
                    case 1:
                        elfMap.put("elfName", "宇智波佐助");
                        elfMap.put("elementType", 1); // 火系
                        break;
                    case 2:
                        elfMap.put("elfName", "照美冥");
                        elfMap.put("elementType", 2); // 水系
                        break;
                    case 3:
                        elfMap.put("elfName", "千手柱间");
                        elfMap.put("elementType", 3); // 草系
                        break;
                    default:
                        elfMap.put("elfName", "精灵 " + userElf.getElfId());
                        elfMap.put("elementType", null); // 未知系别
                        break;
                }
            }
            
            result.add(elfMap);
        }
        
        return Result.success(result);
    }

    @Override
    public Result<UserElf> setBattleElf(Long userId, Long elfId, Integer fightOrder) {
        // 处理出战的情况，需要校验出战顺序
        if (fightOrder > 0) {
            // 检查该出战顺序是否已被其他精灵占用
            UserElf existingElf = lambdaQuery()
                    .eq(UserElf::getUserId, userId)
                    .eq(UserElf::getFightOrder, fightOrder)
                    .ne(UserElf::getId, elfId)
                    .one();
            if (existingElf != null) {
                return Result.error("第" + fightOrder + "号位已经被其他精灵占用，请选择其他位置");
            }
        }
        
        // 处理不出战的情况
        if (fightOrder == 0) {
            boolean success = lambdaUpdate().eq(UserElf::getUserId, userId).eq(UserElf::getId, elfId)
                    .set(UserElf::getIsFight, 0)
                    .set(UserElf::getFightOrder, 0)
                    .update();
            if (!success) {
                return Result.error("设置出战精灵失败");
            }
        } else {
            // 将指定精灵设置为出战状态并设置出战顺序
            boolean success = lambdaUpdate().eq(UserElf::getUserId, userId).eq(UserElf::getId, elfId)
                    .set(UserElf::getIsFight, 1)
                    .set(UserElf::getFightOrder, fightOrder)
                    .update();
            if (!success) {
                return Result.error("设置出战精灵失败");
            }
        }
        UserElf battleElf = getById(elfId);
        return Result.success(battleElf);
    }

    @Override
    public Result<List<Map<String, Object>>> getBattleElves(Long userId) {
        List<UserElf> battleElves = lambdaQuery().eq(UserElf::getUserId, userId).gt(UserElf::getFightOrder, 0)
                .orderByAsc(UserElf::getFightOrder)
                .list();
        if (battleElves == null || battleElves.isEmpty()) {
            return Result.error("未设置出战精灵");
        }
        
        // 转换为包含精灵名字的Map列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserElf userElf : battleElves) {
            Map<String, Object> elfMap = new HashMap<>();
            // 复制UserElf的所有属性
            elfMap.put("id", userElf.getId());
            elfMap.put("userId", userElf.getUserId());
            elfMap.put("elfId", userElf.getElfId());
            elfMap.put("level", userElf.getLevel());
            elfMap.put("exp", userElf.getExp());
            elfMap.put("expNeed", userElf.getExpNeed());
            elfMap.put("maxHp", userElf.getMaxHp());
            elfMap.put("maxMp", userElf.getMaxMp());
            elfMap.put("hp", userElf.getHp());
            elfMap.put("mp", userElf.getMp());
            elfMap.put("attack", userElf.getAttack());
            elfMap.put("defense", userElf.getDefense());
            elfMap.put("normalDamage", userElf.getNormalDamage());
            elfMap.put("speed", userElf.getSpeed());
            elfMap.put("isFight", userElf.getIsFight());
            elfMap.put("fightOrder", userElf.getFightOrder());
            elfMap.put("status", userElf.getStatus());
            
            // 添加精灵名字和系别
            Elf elf = elfMapper.selectById(userElf.getElfId());
            if (elf != null && elf.getElfName() != null) {
                elfMap.put("elfName", elf.getElfName());
                elfMap.put("elementType", elf.getElementType());
            } else {
                // 添加默认精灵名字和系别
                switch (userElf.getElfId()) {
                    case 1:
                        elfMap.put("elfName", "宇智波佐助");
                        elfMap.put("elementType", 1); // 火系
                        break;
                    case 2:
                        elfMap.put("elfName", "照美冥");
                        elfMap.put("elementType", 2); // 水系
                        break;
                    case 3:
                        elfMap.put("elfName", "千手柱间");
                        elfMap.put("elementType", 3); // 草系
                        break;
                    default:
                        elfMap.put("elfName", "精灵 " + userElf.getElfId());
                        elfMap.put("elementType", null); // 未知系别
                        break;
                }
            }
            
            result.add(elfMap);
        }
        
        return Result.success(result);
    }

    @Override
    public Result<UserElf> upgradeElf(Long elfId) {
        UserElf userElf = getById(elfId);
        if (userElf == null) {
            return Result.error("精灵不存在");
        }
        if (userElf.getExp() < userElf.getExpNeed()) {
            return Result.error("经验不足，无法升级");
        }
        if (userElf.getLevel() >= 10) {
            return Result.error("精灵已达到等级上限，无法继续升级");
        }
        // 升级处理
        userElf.setLevel(userElf.getLevel() + 1);
        userElf.setExp(userElf.getExp() - userElf.getExpNeed());
        userElf.setExpNeed(100L); // 经验需求恒为100
        // 属性提升
        userElf.setMaxHp(userElf.getMaxHp() + 20);
        userElf.setMaxMp(userElf.getMaxMp() + 10);
        userElf.setAttack(userElf.getAttack() + 5);
        userElf.setDefense(userElf.getDefense() + 3);
        userElf.setNormalDamage(userElf.getNormalDamage() + 2);
        userElf.setSpeed(userElf.getSpeed() + 2);
        // 满血满蓝
        userElf.setHp(userElf.getMaxHp());
        userElf.setMp(userElf.getMaxMp());
        boolean success = updateById(userElf);
        if (!success) {
            return Result.error("升级失败");
        }
        return Result.success(userElf);
    }

    @Override
    public Result<Map<String, Object>> getElfDetail(Long elfId) {
        UserElf userElf = getById(elfId);
        if (userElf == null) {
            return Result.error("精灵不存在");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("elf", userElf);

        // 获取该精灵可以学习的所有技能
        List<ElfSkill> elfSkills = elfSkillMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ElfSkill>()
                .eq("elf_id", userElf.getElfId()));
        List<Integer> availableSkillIds = elfSkills.stream()
                .map(ElfSkill::getSkillId)
                .collect(Collectors.toList());
        
        // 获取已解锁的技能
        List<Skill> unlockedSkillList = new ArrayList<>();
        if (!availableSkillIds.isEmpty()) {
            // 这里简化处理，假设所有在elf_skill表中的技能都是已解锁的
            // 实际项目中应该有用户精灵技能关联表来记录哪些技能已解锁
            unlockedSkillList = skillMapper.selectBatchIds(availableSkillIds);
        }
        detail.put("unlockedSkills", unlockedSkillList);

        // 获取可解锁的技能（等级达到但未解锁）
        List<Skill> unlockableSkills = new ArrayList<>();
        Integer level = userElf.getLevel();
        if (level != null && !availableSkillIds.isEmpty()) {
            // 从可用技能中筛选等级符合要求且未解锁的技能
            List<Skill> allSkills = skillMapper.selectBatchIds(availableSkillIds);
            // 收集已解锁技能的ID
            List<Integer> unlockedSkillIds = unlockedSkillList.stream()
                    .map(Skill::getId)
                    .collect(Collectors.toList());
            // 筛选等级符合要求且未解锁的技能
            unlockableSkills = allSkills.stream()
                    .filter(skill -> skill.getUnlockLevel() <= level && !unlockedSkillIds.contains(skill.getId()))
                    .collect(Collectors.toList());
        }
        detail.put("unlockableSkills", unlockableSkills);

        return Result.success(detail);
    }

    @Override
    public Result<UserElf> createElf(Long userId, Integer elfId) {
        // 查询精灵模板信息
        Elf elf = elfMapper.selectById(elfId);
        if (elf == null) {
            return Result.error("精灵不存在");
        }
        
        // 创建新的用户精灵
        UserElf userElf = new UserElf();
        userElf.setUserId(userId);
        userElf.setElfId(elfId);
        userElf.setLevel(1);
        userElf.setExp(0L);
        userElf.setExpNeed(100L);
        userElf.setHp(elf.getBaseHp());
        userElf.setMaxHp(elf.getBaseHp());
        userElf.setMp(elf.getBaseMp());
        userElf.setMaxMp(elf.getBaseMp());
        userElf.setAttack(elf.getBaseAttack());
        userElf.setDefense(elf.getBaseDefense());
        userElf.setNormalDamage(elf.getBaseNormalDamage());
        userElf.setSpeed(elf.getBaseSpeed());
        userElf.setIsFight(0); // 默认设为不出战状态
        userElf.setFightOrder(0); // 默认出战顺序为0
        userElf.setStatus(0); // 0=正常可切换上场
        
        boolean success = save(userElf);
        if (!success) {
            return Result.error("创建精灵失败");
        }
        
        // 更新成就进度：收集精灵数量
        achievementService.updateAchievementProgress(userId, "collect_elf", 1);
        
        return Result.success(userElf);
    }
}