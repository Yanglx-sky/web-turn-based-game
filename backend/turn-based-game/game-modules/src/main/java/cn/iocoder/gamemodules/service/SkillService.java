package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.Skill;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SkillService extends IService<Skill> {
    Result<List<Skill>> getSkillList();
    Result<List<Skill>> getElfSkills(Integer elfId);
    Result<Skill> unlockSkill(Long userId, Integer elfId, Integer skillId);
    Result<List<Skill>> getUnlockedSkills(Long userId, Integer elfId);
}