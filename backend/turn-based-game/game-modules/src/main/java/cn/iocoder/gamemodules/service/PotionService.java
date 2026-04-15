package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.PotionConfig;
import cn.iocoder.gamemodules.entity.UserPotion;

import java.util.List;
import java.util.Map;

public interface PotionService {
    // 获取所有药品配置
    Result<List<PotionConfig>> getAllPotions();
    
    // 获取药品详情
    Result<PotionConfig> getPotionById(Long id);
    
    // 获取用户拥有的药品
    Result<List<Map<String, Object>>> getUserPotions(Long userId);
    
    // 使用药品
    Result<Map<String, Object>> usePotion(Long userId, Long elfId, Long potionId);
    
    // 增加用户药品数量
    Result<Map<String, Object>> addUserPotion(Long userId, Long potionConfigId, int count);
    
    // 减少用户药品数量
    Result<Map<String, Object>> reduceUserPotion(Long userId, Long potionConfigId, int count);
}