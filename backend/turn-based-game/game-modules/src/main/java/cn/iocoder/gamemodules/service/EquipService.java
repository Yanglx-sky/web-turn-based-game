package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.EquipConfig;

import java.util.Map;

public interface EquipService {
    // 获取装备详情
    Result<EquipConfig> getEquipById(Long id);
    
    // 装备武器
    Result<Map<String, Object>> equipWeapon(Long userId, Long elfId, Long weaponId);
    
    // 装备防具
    Result<Map<String, Object>> equipArmor(Long userId, Long elfId, Long armorId);
    
    // 卸下武器
    Result<Map<String, Object>> unequipWeapon(Long userId, Long elfId);
    
    // 卸下防具
    Result<Map<String, Object>> unequipArmor(Long userId, Long elfId);
}