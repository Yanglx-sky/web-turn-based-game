package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.PotionConfig;
import cn.iocoder.gamemodules.entity.UserElf;
import cn.iocoder.gamemodules.entity.UserPotion;
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

        // 检查药品是否存在
        PotionConfig potion = potionConfigMapper.selectById(potionId);
        if (potion == null) {
            return Result.error("药品不存在");
        }

        // 检查用户是否拥有该药品
        QueryWrapper<UserPotion> userPotionWrapper = new QueryWrapper<>();
        userPotionWrapper.eq("user_id", userId);
        userPotionWrapper.eq("potion_config_id", potionId);
        UserPotion userPotion = userPotionMapper.selectOne(userPotionWrapper);
        if (userPotion == null || userPotion.getCount() <= 0) {
            return Result.error("您没有该药品");
        }

        // 使用药品，根据药品类型恢复生命值或魔法值
        int healValue = potion.getHealValue();
        if (potion.getType() == 1) {
            // 血瓶，恢复生命值
            userElf.setHp(userElf.getHp() + healValue);
        } else if (potion.getType() == 2) {
            // 蓝瓶，恢复魔法值
            userElf.setMp(userElf.getMp() + healValue);
        }

        // 确保生命值和魔法值不超过最大值
        if (userElf.getHp() > userElf.getMaxHp()) {
            userElf.setHp(userElf.getMaxHp());
        }
        if (userElf.getMp() > userElf.getMaxMp()) {
            userElf.setMp(userElf.getMaxMp());
        }

        // 更新精灵信息
        userElfMapper.updateById(userElf);

        // 减少药品数量
        userPotion.setCount(userPotion.getCount() - 1);
        userPotion.setUpdateTime(LocalDateTime.now());
        if (userPotion.getCount() <= 0) {
            // 如果药品数量为0，删除该记录
            userPotionMapper.deleteById(userPotion.getId());
        } else {
            // 否则更新药品数量
            userPotionMapper.updateById(userPotion);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("elf", userElf);
        result.put("potion", potion);
        result.put("msg", "使用药品成功");
        return Result.success(result);
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