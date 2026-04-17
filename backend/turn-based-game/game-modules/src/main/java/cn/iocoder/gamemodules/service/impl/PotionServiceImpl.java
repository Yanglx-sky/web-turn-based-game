package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.BattleRecord;
import cn.iocoder.gamemodules.entity.BattleRecordElf;
import cn.iocoder.gamemodules.entity.PotionConfig;
import cn.iocoder.gamemodules.entity.UserElf;
import cn.iocoder.gamemodules.entity.UserPotion;
import cn.iocoder.gamemodules.mapper.BattleRecordElfMapper;
import cn.iocoder.gamemodules.mapper.BattleRecordMapper;
import cn.iocoder.gamemodules.mapper.PotionConfigMapper;
import cn.iocoder.gamemodules.mapper.UserElfMapper;
import cn.iocoder.gamemodules.mapper.UserPotionMapper;
import cn.iocoder.gamemodules.service.PotionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PotionServiceImpl implements PotionService {

    @Autowired
    private PotionConfigMapper potionConfigMapper;

    @Autowired
    private UserPotionMapper userPotionMapper;

    @Autowired
    private UserElfMapper userElfMapper;
    
    @Autowired
    private BattleRecordMapper battleRecordMapper;
    
    @Autowired
    private BattleRecordElfMapper battleRecordElfMapper;

    @Override
    public Result<List<PotionConfig>> getAllPotions() {
        List<PotionConfig> potions = potionConfigMapper.selectList(null);
        return Result.success(potions);
    }

    @Override
    public Result<PotionConfig> getPotionById(Long id) {
        PotionConfig potion = potionConfigMapper.selectById(id);
        if (potion == null) {
            return Result.error("药品不存在");
        }
        return Result.success(potion);
    }

    @Override
    public Result<List<Map<String, Object>>> getUserPotions(Long userId) {
        QueryWrapper<UserPotion> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<UserPotion> userPotions = userPotionMapper.selectList(wrapper);
        
        // 为药品添加详细信息，使用Map包装返回数据
        List<Map<String, Object>> resultItems = new ArrayList<>();
        for (UserPotion userPotion : userPotions) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", userPotion.getId());
            itemMap.put("userId", userPotion.getUserId());
            itemMap.put("potionConfigId", userPotion.getPotionConfigId());
            itemMap.put("count", userPotion.getCount());
            itemMap.put("updateTime", userPotion.getUpdateTime());
            
            // 从potion_config表获取药品详情
            PotionConfig potion = potionConfigMapper.selectById(userPotion.getPotionConfigId());
            if (potion != null) {
                itemMap.put("name", potion.getName());
                itemMap.put("description", potion.getDescription());
                itemMap.put("healValue", potion.getHealValue());
                itemMap.put("type", potion.getType());
            }
            
            resultItems.add(itemMap);
        }
        
        return Result.success(resultItems);
    }

    @Override
    public Result<Map<String, Object>> usePotion(Long userId, Long elfId, Long potionId) {
        // 检查精灵是否存在且属于该用户
        QueryWrapper<UserElf> elfWrapper = new QueryWrapper<>();
        elfWrapper.eq("id", elfId).eq("user_id", userId);
        UserElf userElf = userElfMapper.selectOne(elfWrapper);
        if (userElf == null) {
            return Result.error("精灵不存在或不属于该用户");
        }

        System.out.println("[DEBUG] 使用药品前 - HP: " + userElf.getHp() + "/" + userElf.getMaxHp() + ", MP: " + userElf.getMp() + "/" + userElf.getMaxMp());

        // 检查药品是否存在
        PotionConfig potion = potionConfigMapper.selectById(potionId);
        if (potion == null) {
            return Result.error("药品不存在");
        }

        System.out.println("[DEBUG] 药品信息 - 名称: " + potion.getName() + ", 类型: " + potion.getType() + ", 恢复值: " + potion.getHealValue());

        // 检查用户是否拥有该药品
        QueryWrapper<UserPotion> userPotionWrapper = new QueryWrapper<>();
        userPotionWrapper.eq("user_id", userId);
        userPotionWrapper.eq("potion_config_id", potionId);
        UserPotion userPotion = userPotionMapper.selectOne(userPotionWrapper);
        if (userPotion == null || userPotion.getCount() <= 0) {
            return Result.error("您没有该药品");
        }

        // 检查是否有进行中的战斗
        BattleRecord currentBattle = battleRecordMapper.selectCurrentBattleByUserId(userId);
        
        if (currentBattle != null && currentBattle.getStatus() == 0) {
            // 战斗中：以 battle_record_elf 为准
            QueryWrapper<BattleRecordElf> battleElfWrapper = new QueryWrapper<>();
            battleElfWrapper.eq("battle_id", currentBattle.getBattleId());
            List<BattleRecordElf> battleElves = battleRecordElfMapper.selectList(battleElfWrapper);
            
            BattleRecordElf battleElf = null;
            for (BattleRecordElf be : battleElves) {
                if (be.getElfId().equals(elfId)) {
                    battleElf = be;
                    break;
                }
            }
            
            if (battleElf == null) {
                return Result.error("战斗记录中未找到该精灵");
            }
            
            System.out.println("[DEBUG] 战斗记录中 - HP: " + battleElf.getCurrentHp() + ", MP: " + battleElf.getCurrentMp());
            
            // 根据药品类型恢复对应的属性
            int healValue = potion.getHealValue();
            if (potion.getType() == 1) {
                // 血瓶，恢复生命值
                int oldHp = battleElf.getCurrentHp();
                battleElf.setCurrentHp(Math.min(battleElf.getCurrentHp() + healValue, userElf.getMaxHp()));
                System.out.println("[DEBUG] 血瓶 - HP从 " + oldHp + " 增加到 " + battleElf.getCurrentHp());
            } else if (potion.getType() == 2) {
                // 蓝瓶，恢复魔法值
                int oldMp = battleElf.getCurrentMp();
                battleElf.setCurrentMp(Math.min(battleElf.getCurrentMp() + healValue, userElf.getMaxMp()));
                System.out.println("[DEBUG] 蓝瓶 - MP从 " + oldMp + " 增加到 " + battleElf.getCurrentMp());
            }
            
            battleElf.setUpdateTime(LocalDateTime.now());
            battleRecordElfMapper.updateById(battleElf);
            
            System.out.println("[DEBUG] 战斗记录更新后 - HP: " + battleElf.getCurrentHp() + ", MP: " + battleElf.getCurrentMp());
            
            // 同步更新 user_elf 表（保持数据一致性）
            if (potion.getType() == 1) {
                userElf.setHp(battleElf.getCurrentHp());
            } else if (potion.getType() == 2) {
                userElf.setMp(battleElf.getCurrentMp());
            }
            userElfMapper.updateById(userElf);
            
            // 构建返回结果，使用战斗记录中的数据
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> elfData = new HashMap<>();
            elfData.put("id", userElf.getId());
            elfData.put("elfId", userElf.getElfId());
            elfData.put("userId", userElf.getUserId());
            elfData.put("level", userElf.getLevel());
            elfData.put("exp", userElf.getExp());
            elfData.put("hp", battleElf.getCurrentHp());
            elfData.put("maxHp", userElf.getMaxHp());
            elfData.put("mp", battleElf.getCurrentMp());
            elfData.put("maxMp", userElf.getMaxMp());
            elfData.put("attack", userElf.getAttack());
            elfData.put("defense", userElf.getDefense());
            elfData.put("speed", userElf.getSpeed());
            elfData.put("isFight", userElf.getIsFight());
            elfData.put("fightOrder", userElf.getFightOrder());
            elfData.put("status", userElf.getStatus());
            
            result.put("elf", elfData);
            result.put("potion", potion);
            result.put("msg", "使用药品成功");
            
            // 减少药品数量
            userPotion.setCount(userPotion.getCount() - 1);
            userPotion.setUpdateTime(LocalDateTime.now());
            if (userPotion.getCount() <= 0) {
                userPotionMapper.deleteById(userPotion.getId());
            } else {
                userPotionMapper.updateById(userPotion);
            }
            
            return Result.success(result);
        } else {
            // 非战斗状态：直接更新 user_elf
            int healValue = potion.getHealValue();
            if (potion.getType() == 1) {
                // 血瓶，恢复生命值
                int oldHp = userElf.getHp();
                userElf.setHp(Math.min(userElf.getHp() + healValue, userElf.getMaxHp()));
                System.out.println("[DEBUG] 血瓶 - HP从 " + oldHp + " 增加到 " + userElf.getHp());
            } else if (potion.getType() == 2) {
                // 蓝瓶，恢复魔法值
                int oldMp = userElf.getMp();
                userElf.setMp(Math.min(userElf.getMp() + healValue, userElf.getMaxMp()));
                System.out.println("[DEBUG] 蓝瓶 - MP从 " + oldMp + " 增加到 " + userElf.getMp());
            }
            
            userElfMapper.updateById(userElf);
            
            System.out.println("[DEBUG] 使用药品后 - HP: " + userElf.getHp() + "/" + userElf.getMaxHp() + ", MP: " + userElf.getMp() + "/" + userElf.getMaxMp());
            
            // 减少药品数量
            userPotion.setCount(userPotion.getCount() - 1);
            userPotion.setUpdateTime(LocalDateTime.now());
            if (userPotion.getCount() <= 0) {
                userPotionMapper.deleteById(userPotion.getId());
            } else {
                userPotionMapper.updateById(userPotion);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("elf", userElf);
            result.put("potion", potion);
            result.put("msg", "使用药品成功");
            return Result.success(result);
        }
    }

    @Override
    public Result<Map<String, Object>> addUserPotion(Long userId, Long potionConfigId, int count) {
        // 检查药品是否存在
        PotionConfig potion = potionConfigMapper.selectById(potionConfigId);
        if (potion == null) {
            return Result.error("药品不存在");
        }

        // 检查用户是否已有该药品
        QueryWrapper<UserPotion> userPotionWrapper = new QueryWrapper<>();
        userPotionWrapper.eq("user_id", userId);
        userPotionWrapper.eq("potion_config_id", potionConfigId);
        UserPotion userPotion = userPotionMapper.selectOne(userPotionWrapper);

        if (userPotion != null) {
            // 如果已有该药品，增加数量
            userPotion.setCount(userPotion.getCount() + count);
            userPotion.setUpdateTime(LocalDateTime.now());
            userPotionMapper.updateById(userPotion);
        } else {
            // 如果没有该药品，创建新记录
            userPotion = new UserPotion();
            userPotion.setUserId(userId);
            userPotion.setPotionConfigId(potionConfigId);
            userPotion.setCount(count);
            userPotion.setUpdateTime(LocalDateTime.now());
            userPotionMapper.insert(userPotion);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userPotion", userPotion);
        result.put("msg", "增加药品成功");
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> reduceUserPotion(Long userId, Long potionConfigId, int count) {
        // 检查用户是否拥有该药品
        QueryWrapper<UserPotion> userPotionWrapper = new QueryWrapper<>();
        userPotionWrapper.eq("user_id", userId);
        userPotionWrapper.eq("potion_config_id", potionConfigId);
        UserPotion userPotion = userPotionMapper.selectOne(userPotionWrapper);

        if (userPotion == null || userPotion.getCount() < count) {
            return Result.error("药品数量不足");
        }

        // 减少药品数量
        userPotion.setCount(userPotion.getCount() - count);
        userPotion.setUpdateTime(LocalDateTime.now());

        if (userPotion.getCount() <= 0) {
            // 如果药品数量为0，删除该记录
            userPotionMapper.deleteById(userPotion.getId());
        } else {
            // 否则更新药品数量
            userPotionMapper.updateById(userPotion);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("msg", "减少药品成功");
        return Result.success(result);
    }
}