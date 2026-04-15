package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.ElfSkill;
import cn.iocoder.gamemodules.entity.Skill;
import cn.iocoder.gamemodules.entity.UserElf;
import cn.iocoder.gamemodules.mapper.ElfSkillMapper;
import cn.iocoder.gamemodules.mapper.SkillMapper;
import cn.iocoder.gamemodules.mapper.UserElfMapper;
import cn.iocoder.gamemodules.service.SkillService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, Skill> implements SkillService {

    @Autowired
    private ElfSkillMapper elfSkillMapper;

    @Override
    public Result<List<Skill>> getSkillList() {
        List<Skill> skillList = list();
        return Result.success(skillList);
    }

    @Override
    public Result<List<Skill>> getElfSkills(Integer elfId) {
        List<ElfSkill> elfSkills = elfSkillMapper.selectList(null);
        List<Integer> skillIds = elfSkills.stream()
                .filter(es -> es.getElfId().equals(elfId))
                .map(ElfSkill::getSkillId)
                .collect(Collectors.toList());
        List<Skill> skills = listByIds(skillIds);
        return Result.success(skills);
    }

    @Autowired
    private UserElfMapper userElfMapper;

    @Override
    public Result<Skill> unlockSkill(Long userId, Integer elfId, Integer skillId) {
        // 获取精灵信息
        UserElf userElf = userElfMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserElf>()
                .eq("user_id", userId)
                .eq("elf_id", elfId));
        if (userElf == null) {
            return Result.error("精灵不存在");
        }
        
        // 获取技能信息
        Skill skill = getById(skillId);
        if (skill == null) {
            return Result.error("技能不存在");
        }
        
        // 检查精灵等级是否达到解锁要求
        if (userElf.getLevel() < skill.getUnlockLevel()) {
            return Result.error("精灵等级不足，无法解锁该技能");
        }
        
        // 检查该精灵是否可以学习该技能（数据库中是否有对应关系）
        ElfSkill availableSkill = elfSkillMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ElfSkill>()
                .eq("elf_id", elfId)
                .eq("skill_id", skillId));
        if (availableSkill == null) {
            return Result.error("该精灵无法学习此技能");
        }
        
        // 检查技能是否已经解锁（这里需要根据实际的用户精灵技能关联表来检查）
        // 由于当前没有用户精灵技能关联表，暂时跳过此检查
        
        // 解锁技能（实际是记录到用户精灵的技能列表中）
        // 这里可以根据需要添加用户精灵技能关联表的操作
        
        return Result.success(skill);
    }

    @Override
    public Result<List<Skill>> getUnlockedSkills(Long userId, Integer elfId) {
        // 检查精灵是否属于该用户
        UserElf userElf = userElfMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserElf>()
                .eq("user_id", userId)
                .eq("elf_id", elfId));
        if (userElf == null) {
            return Result.error("精灵不存在");
        }
        
        // 获取精灵已解锁的技能
        List<ElfSkill> elfSkills = elfSkillMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ElfSkill>()
                .eq("elf_id", elfId));
        List<Integer> skillIds = elfSkills.stream()
                .map(ElfSkill::getSkillId)
                .collect(Collectors.toList());
        List<Skill> skills = listByIds(skillIds);
        return Result.success(skills);
    }
}