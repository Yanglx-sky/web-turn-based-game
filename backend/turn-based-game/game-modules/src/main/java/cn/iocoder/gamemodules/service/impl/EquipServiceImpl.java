package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.EquipConfig;
import cn.iocoder.gamemodules.entity.UserBag;
import cn.iocoder.gamemodules.entity.UserElf;
import cn.iocoder.gamemodules.mapper.EquipConfigMapper;
import cn.iocoder.gamemodules.mapper.UserBagMapper;
import cn.iocoder.gamemodules.mapper.UserElfMapper;
import cn.iocoder.gamemodules.service.EquipService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EquipServiceImpl implements EquipService {

    @Autowired
    private EquipConfigMapper equipConfigMapper;

    @Autowired
    private UserElfMapper userElfMapper;

    @Autowired
    private UserBagMapper userBagMapper;

    @Override
    public Result<EquipConfig> getEquipById(Long id) {
        EquipConfig equip = equipConfigMapper.selectById(id);
        if (equip == null) {
            return Result.error("装备不存在");
        }
        return Result.success(equip);
    }

    @Override
    public Result<Map<String, Object>> equipWeapon(Long userId, Long elfId, Long userBagId) {
        // 检查精灵是否存在且属于该用户
        QueryWrapper<UserElf> wrapper = new QueryWrapper<>();
        wrapper.eq("id", elfId).eq("user_id", userId);
        UserElf userElf = userElfMapper.selectOne(wrapper);
        if (userElf == null) {
            return Result.error("精灵不存在或不属于该用户");
        }

        // 检查UserBag是否存在且属于该用户
        QueryWrapper<UserBag> userBagWrapper = new QueryWrapper<>();
        userBagWrapper.eq("id", userBagId).eq("user_id", userId);
        userBagWrapper.eq("is_worn", 0);
        UserBag userBag = userBagMapper.selectOne(userBagWrapper);
        if (userBag == null) {
            return Result.error("您没有该装备或该装备已被使用");
        }

        // 检查装备是否存在且是武器
        Long equipConfigId = userBag.getEquipConfigId();
        EquipConfig weapon = equipConfigMapper.selectById(equipConfigId);
        if (weapon == null) {
            return Result.error("武器不存在");
        }
        if (weapon.getType() != 1) {
            return Result.error("该装备不是武器");
        }

        // 标记装备为已装备
        userBag.setIsWorn(1);
        userBag.setElfId(elfId);
        userBagMapper.updateById(userBag);

        // 装备武器
        userElf.setWeaponId(equipConfigId);

        // 直接计算并更新精灵属性
        // 1. 获取当前装备的属性加成
        int attackBonus = weapon.getAtk();
        int hpBonus = weapon.getHp();
        int mpBonus = weapon.getMp();
        int speedBonus = weapon.getSpeed() != null ? weapon.getSpeed() : 0;
        
        // 2. 更新精灵属性
        userElf.setAttack(userElf.getAttack() + attackBonus);
        userElf.setMaxHp(userElf.getMaxHp() + hpBonus);
        userElf.setMaxMp(userElf.getMaxMp() + mpBonus);
        userElf.setSpeed(userElf.getSpeed() + speedBonus);
        
        // 3. 确保当前HP和MP不超过最大值
        if (userElf.getHp() > userElf.getMaxHp()) {
            userElf.setHp(userElf.getMaxHp());
        }
        if (userElf.getMp() > userElf.getMaxMp()) {
            userElf.setMp(userElf.getMaxMp());
        }
        
        // 4. 更新数据库
        userElfMapper.updateById(userElf);

        Map<String, Object> result = new HashMap<>();
        result.put("elf", userElf);
        result.put("weapon", weapon);
        result.put("msg", "武器装备成功");
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> equipArmor(Long userId, Long elfId, Long userBagId) {
        // 检查精灵是否存在且属于该用户
        QueryWrapper<UserElf> wrapper = new QueryWrapper<>();
        wrapper.eq("id", elfId).eq("user_id", userId);
        UserElf userElf = userElfMapper.selectOne(wrapper);
        if (userElf == null) {
            return Result.error("精灵不存在或不属于该用户");
        }

        // 检查UserBag是否存在且属于该用户
        QueryWrapper<UserBag> userBagWrapper = new QueryWrapper<>();
        userBagWrapper.eq("id", userBagId).eq("user_id", userId);
        userBagWrapper.eq("is_worn", 0);
        UserBag userBag = userBagMapper.selectOne(userBagWrapper);
        if (userBag == null) {
            return Result.error("您没有该装备或该装备已被使用");
        }

        // 检查装备是否存在且是防具
        Long equipConfigId = userBag.getEquipConfigId();
        EquipConfig armor = equipConfigMapper.selectById(equipConfigId);
        if (armor == null) {
            return Result.error("防具不存在");
        }
        if (armor.getType() != 2) {
            return Result.error("该装备不是防具");
        }

        // 标记装备为已装备
        userBag.setIsWorn(1);
        userBag.setElfId(elfId);
        userBagMapper.updateById(userBag);

        // 装备防具
        userElf.setArmorId(equipConfigId);

        // 直接计算并更新精灵属性
        // 1. 获取当前装备的属性加成
        int defenseBonus = armor.getDef();
        int hpBonus = armor.getHp();
        int mpBonus = armor.getMp();
        int speedBonus = armor.getSpeed() != null ? armor.getSpeed() : 0;
        
        // 2. 更新精灵属性
        userElf.setDefense(userElf.getDefense() + defenseBonus);
        userElf.setMaxHp(userElf.getMaxHp() + hpBonus);
        userElf.setMaxMp(userElf.getMaxMp() + mpBonus);
        userElf.setSpeed(userElf.getSpeed() + speedBonus);
        
        // 3. 确保当前HP和MP不超过最大值
        if (userElf.getHp() > userElf.getMaxHp()) {
            userElf.setHp(userElf.getMaxHp());
        }
        if (userElf.getMp() > userElf.getMaxMp()) {
            userElf.setMp(userElf.getMaxMp());
        }
        
        // 4. 更新数据库
        userElfMapper.updateById(userElf);

        Map<String, Object> result = new HashMap<>();
        result.put("elf", userElf);
        result.put("armor", armor);
        result.put("msg", "防具装备成功");
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> unequipWeapon(Long userId, Long elfId) {
        // 检查精灵是否存在且属于该用户
        QueryWrapper<UserElf> wrapper = new QueryWrapper<>();
        wrapper.eq("id", elfId).eq("user_id", userId);
        UserElf userElf = userElfMapper.selectOne(wrapper);
        if (userElf == null) {
            return Result.error("精灵不存在或不属于该用户");
        }

        // 获取武器ID
        Long weaponId = userElf.getWeaponId();
        if (weaponId == null) {
            return Result.error("该精灵未装备武器");
        }

        // 获取武器信息，用于计算属性加成
        EquipConfig weapon = equipConfigMapper.selectById(weaponId);
        
        // 标记装备为未装备
        QueryWrapper<UserBag> userBagWrapper = new QueryWrapper<>();
        userBagWrapper.eq("user_id", userId);
        userBagWrapper.eq("equip_config_id", weaponId);
        userBagWrapper.eq("is_worn", 1);
        userBagWrapper.eq("elf_id", elfId);
        List<UserBag> userBags = userBagMapper.selectList(userBagWrapper);
        if (userBags != null && !userBags.isEmpty()) {
            // 取第一条记录
            UserBag userBag = userBags.get(0);
            userBag.setIsWorn(0);
            userBag.setElfId(null);
            userBagMapper.updateById(userBag);
        }

        // 卸下武器，使用UpdateWrapper确保null值被更新
        UpdateWrapper<UserElf> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userElf.getId());
        updateWrapper.set("weapon_id", null);
        userElfMapper.update(null, updateWrapper);
        
        // 更新内存中的对象
        userElf.setWeaponId(null);

        // 直接计算并更新精灵属性
        if (weapon != null) {
            // 1. 获取装备的属性加成
            int attackBonus = weapon.getAtk();
            int hpBonus = weapon.getHp();
            int mpBonus = weapon.getMp();
            int speedBonus = weapon.getSpeed() != null ? weapon.getSpeed() : 0;
            
            // 2. 从精灵属性中减去装备加成
            userElf.setAttack(userElf.getAttack() - attackBonus);
            userElf.setMaxHp(userElf.getMaxHp() - hpBonus);
            userElf.setMaxMp(userElf.getMaxMp() - mpBonus);
            userElf.setSpeed(userElf.getSpeed() - speedBonus);
            
            // 3. 确保属性值不为负数
            if (userElf.getAttack() < 0) {
                userElf.setAttack(0);
            }
            if (userElf.getMaxHp() < 0) {
                userElf.setMaxHp(0);
            }
            if (userElf.getMaxMp() < 0) {
                userElf.setMaxMp(0);
            }
            if (userElf.getSpeed() < 0) {
                userElf.setSpeed(0);
            }
            
            // 4. 确保当前HP和MP不超过最大值
            if (userElf.getHp() > userElf.getMaxHp()) {
                userElf.setHp(userElf.getMaxHp());
            }
            if (userElf.getMp() > userElf.getMaxMp()) {
                userElf.setMp(userElf.getMaxMp());
            }
            
            // 5. 更新数据库
            userElfMapper.updateById(userElf);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("elf", userElf);
        result.put("msg", "武器卸下成功");
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> unequipArmor(Long userId, Long elfId) {
        // 检查精灵是否存在且属于该用户
        QueryWrapper<UserElf> wrapper = new QueryWrapper<>();
        wrapper.eq("id", elfId).eq("user_id", userId);
        UserElf userElf = userElfMapper.selectOne(wrapper);
        if (userElf == null) {
            return Result.error("精灵不存在或不属于该用户");
        }

        // 获取防具ID
        Long armorId = userElf.getArmorId();
        if (armorId == null) {
            return Result.error("该精灵未装备防具");
        }

        // 获取防具信息，用于计算属性加成
        EquipConfig armor = equipConfigMapper.selectById(armorId);
        
        // 标记装备为未装备
        QueryWrapper<UserBag> userBagWrapper = new QueryWrapper<>();
        userBagWrapper.eq("user_id", userId);
        userBagWrapper.eq("equip_config_id", armorId);
        userBagWrapper.eq("is_worn", 1);
        userBagWrapper.eq("elf_id", elfId);
        List<UserBag> userBags = userBagMapper.selectList(userBagWrapper);
        if (userBags != null && !userBags.isEmpty()) {
            // 取第一条记录
            UserBag userBag = userBags.get(0);
            userBag.setIsWorn(0);
            userBag.setElfId(null);
            userBagMapper.updateById(userBag);
        }

        // 卸下防具，使用UpdateWrapper确保null值被更新
        UpdateWrapper<UserElf> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userElf.getId());
        updateWrapper.set("armor_id", null);
        userElfMapper.update(null, updateWrapper);
        
        // 更新内存中的对象
        userElf.setArmorId(null);

        // 直接计算并更新精灵属性
        if (armor != null) {
            // 1. 获取装备的属性加成
            int defenseBonus = armor.getDef();
            int hpBonus = armor.getHp();
            int mpBonus = armor.getMp();
            int speedBonus = armor.getSpeed() != null ? armor.getSpeed() : 0;
            
            // 2. 从精灵属性中减去装备加成
            userElf.setDefense(userElf.getDefense() - defenseBonus);
            userElf.setMaxHp(userElf.getMaxHp() - hpBonus);
            userElf.setMaxMp(userElf.getMaxMp() - mpBonus);
            userElf.setSpeed(userElf.getSpeed() - speedBonus);
            
            // 3. 确保属性值不为负数
            if (userElf.getDefense() < 0) {
                userElf.setDefense(0);
            }
            if (userElf.getMaxHp() < 0) {
                userElf.setMaxHp(0);
            }
            if (userElf.getMaxMp() < 0) {
                userElf.setMaxMp(0);
            }
            if (userElf.getSpeed() < 0) {
                userElf.setSpeed(0);
            }
            
            // 4. 确保当前HP和MP不超过最大值
            if (userElf.getHp() > userElf.getMaxHp()) {
                userElf.setHp(userElf.getMaxHp());
            }
            if (userElf.getMp() > userElf.getMaxMp()) {
                userElf.setMp(userElf.getMaxMp());
            }
            
            // 5. 更新数据库
            userElfMapper.updateById(userElf);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("elf", userElf);
        result.put("msg", "防具卸下成功");
        return Result.success(result);
    }
}