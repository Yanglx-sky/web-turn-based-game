package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.*;
import cn.iocoder.gamemodules.mapper.ShopItemMapper;
import cn.iocoder.gamemodules.mapper.UserAssetMapper;
import cn.iocoder.gamemodules.mapper.UserBagMapper;
import cn.iocoder.gamemodules.mapper.UserPotionMapper;
import cn.iocoder.gamemodules.mapper.EquipConfigMapper;
import cn.iocoder.gamemodules.mapper.PotionConfigMapper;
import cn.iocoder.gamemodules.service.ShopService;
import cn.iocoder.gamemodules.service.PotionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private ShopItemMapper shopItemMapper;

    @Autowired
    private UserAssetMapper userAssetMapper;

    @Autowired
    private UserBagMapper userBagMapper;

    @Autowired
    private UserPotionMapper userPotionMapper;

    @Autowired
    private EquipConfigMapper equipConfigMapper;

    @Autowired
    private PotionConfigMapper potionConfigMapper;

    @Autowired
    private PotionService potionService;

    @Override
    public Result<List<Map<String, Object>>> getAllItems() {
        QueryWrapper<ShopItem> wrapper = new QueryWrapper<>();
        wrapper.eq("is_on_sale", 1);
        List<ShopItem> items = shopItemMapper.selectList(wrapper);
        
        // 为商品添加详细信息，使用Map包装返回数据
        List<Map<String, Object>> resultItems = new ArrayList<>();
        for (ShopItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("itemType", item.getItemType());
            itemMap.put("itemId", item.getItemId());
            itemMap.put("price", item.getPrice());
            itemMap.put("isOnSale", item.getIsOnSale());
            itemMap.put("name", item.getName());
            
            if (item.getItemType() == 2) {
                // 药品，从potion_config表获取描述
                PotionConfig potion = potionConfigMapper.selectById(item.getItemId());
                if (potion != null) {
                    itemMap.put("name", potion.getName());
                    itemMap.put("description", potion.getDescription());
                    itemMap.put("healValue", potion.getHealValue());
                }
            } else if (item.getItemType() == 1) {
                // 装备，从equip_config表获取详细信息
                EquipConfig equip = equipConfigMapper.selectById(item.getItemId());
                if (equip != null) {
                    itemMap.put("name", equip.getName());
                    itemMap.put("type", equip.getType());
                    itemMap.put("atk", equip.getAtk());
                    itemMap.put("def", equip.getDef());
                    itemMap.put("hp", equip.getHp());
                    itemMap.put("mp", equip.getMp());
                    itemMap.put("speed", equip.getSpeed());
                    // 确保价格信息不被覆盖
                    itemMap.put("price", item.getPrice());
                } else {
                    // 装备不存在，跳过该商品
                    continue;
                }
            }
            
            resultItems.add(itemMap);
        }
        
        return Result.success(resultItems);
    }

    @Override
    public Result<List<Map<String, Object>>> getItemsByType(Integer type) {
        QueryWrapper<ShopItem> wrapper = new QueryWrapper<>();
        wrapper.eq("is_on_sale", 1);
        if (type == 1 || type == 2) {
            // 1: 装备, 2: 药品
            wrapper.eq("item_type", type);
        } else if (type == 3) {
            // 3: 武器 (装备的一种)
            wrapper.eq("item_type", 1);
            // 关联查询equip_config表，筛选type=1的装备（武器）
            wrapper.inSql("item_id", "SELECT id FROM equip_config WHERE type = 1");
        } else if (type == 4) {
            // 4: 防具 (装备的一种)
            wrapper.eq("item_type", 1);
            // 关联查询equip_config表，筛选type=2的装备（防具）
            wrapper.inSql("item_id", "SELECT id FROM equip_config WHERE type = 2");
        }
        List<ShopItem> items = shopItemMapper.selectList(wrapper);
        
        // 为商品添加详细信息，使用Map包装返回数据
        List<Map<String, Object>> resultItems = new ArrayList<>();
        for (ShopItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("itemType", item.getItemType());
            itemMap.put("itemId", item.getItemId());
            itemMap.put("price", item.getPrice());
            itemMap.put("isOnSale", item.getIsOnSale());
            itemMap.put("name", item.getName());
            
            if (item.getItemType() == 2) {
                // 药品，从potion_config表获取描述
                PotionConfig potion = potionConfigMapper.selectById(item.getItemId());
                if (potion != null) {
                    itemMap.put("name", potion.getName());
                    itemMap.put("description", potion.getDescription());
                    itemMap.put("healValue", potion.getHealValue());
                }
            } else if (item.getItemType() == 1) {
                // 装备，从equip_config表获取详细信息
                EquipConfig equip = equipConfigMapper.selectById(item.getItemId());
                if (equip != null) {
                    itemMap.put("name", equip.getName());
                    itemMap.put("type", equip.getType());
                    itemMap.put("atk", equip.getAtk());
                    itemMap.put("def", equip.getDef());
                    itemMap.put("hp", equip.getHp());
                    itemMap.put("mp", equip.getMp());
                    itemMap.put("speed", equip.getSpeed());
                } else {
                    // 装备不存在，跳过该商品
                    continue;
                }
            }
            
            resultItems.add(itemMap);
        }
        
        return Result.success(resultItems);
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> buyItem(Long userId, Long shopItemId) {
        // 1. 检查商品是否存在且在售
        ShopItem shopItem = shopItemMapper.selectById(shopItemId);
        if (shopItem == null || shopItem.getIsOnSale() != 1) {
            return Result.error("商品不存在或已下架");
        }
        
        // 2. 检查用户金币是否足够
        QueryWrapper<UserAsset> assetWrapper = new QueryWrapper<>();
        assetWrapper.eq("user_id", userId);
        UserAsset userAsset = userAssetMapper.selectOne(assetWrapper);
        
        if (userAsset == null || userAsset.getGold() < shopItem.getPrice()) {
            return Result.error("金币不足");
        }
        
        // 3. 扣除金币
        userAsset.setGold(userAsset.getGold() - shopItem.getPrice());
        userAsset.setUpdateTime(LocalDateTime.now());
        userAssetMapper.updateById(userAsset);
        
        // 4. 根据商品类型进行不同处理
        if (shopItem.getItemType() == 1) {
            // 装备
            EquipConfig equip = equipConfigMapper.selectById(shopItem.getItemId());
            if (equip == null) {
                return Result.error("装备不存在");
            }
            
            // 添加装备到用户背包
            UserBag userBag = new UserBag();
            userBag.setUserId(userId);
            userBag.setEquipConfigId(shopItem.getItemId());
            userBag.setIsWorn(0); // 0表示未装备
            userBag.setElfId(null); // 未装备到任何精灵
            userBag.setCreateTime(LocalDateTime.now());
            userBagMapper.insert(userBag);
            
            Map<String, Object> result = new HashMap<>();
            result.put("equip", equip);
            result.put("remainingGold", userAsset.getGold());
            result.put("msg", "购买成功");
            
            return Result.success(result);
        } else if (shopItem.getItemType() == 2) {
            // 药品
            // 调用PotionService添加药品
            Result<Map<String, Object>> addPotionResult = potionService.addUserPotion(userId, shopItem.getItemId(), 1);
            if (addPotionResult.getCode() != 200) {
                return addPotionResult;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("item", shopItem);
            result.put("remainingGold", userAsset.getGold());
            result.put("msg", "购买成功");
            
            return Result.success(result);
        } else {
            return Result.error("未知商品类型");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getUserEquipsByType(Long userId, Integer type) {
        List<Map<String, Object>> resultItems = new ArrayList<>();
        
        if (type == 2) {
            // 药品，从UserPotion表获取
            QueryWrapper<UserPotion> userPotionWrapper = new QueryWrapper<>();
            userPotionWrapper.eq("user_id", userId);
            userPotionWrapper.gt("count", 0);
            List<UserPotion> userPotions = userPotionMapper.selectList(userPotionWrapper);
            
            for (UserPotion userPotion : userPotions) {
                PotionConfig potion = potionConfigMapper.selectById(userPotion.getPotionConfigId());
                if (potion != null) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", userPotion.getId());
                    itemMap.put("itemType", 2);
                    itemMap.put("itemId", userPotion.getPotionConfigId());
                    itemMap.put("name", potion.getName());
                    itemMap.put("description", potion.getDescription());
                    itemMap.put("healValue", potion.getHealValue());
                    itemMap.put("count", userPotion.getCount());
                    resultItems.add(itemMap);
                }
            }
        } else {
            // 装备，从UserBag表获取
            QueryWrapper<UserBag> userBagWrapper = new QueryWrapper<>();
            userBagWrapper.eq("user_id", userId);
            // 移除is_worn=0的限制，获取所有物品，包括已装备的
            List<UserBag> userBags = userBagMapper.selectList(userBagWrapper);
            
            if (!userBags.isEmpty()) {
                // 为每个装备实例添加详细信息，使用Map包装返回数据
                for (UserBag bag : userBags) {
                    Long equipConfigId = bag.getEquipConfigId();
                    
                    // 装备，从equip_config表获取详细信息
                    EquipConfig equip = equipConfigMapper.selectById(equipConfigId);
                    if (equip != null) {
                        // 根据type筛选武器或防具
                        if (type == 3 && equip.getType() != 1) {
                            // 只显示武器（type=1）
                            continue;
                        } else if (type == 4 && equip.getType() != 2) {
                            // 只显示防具（type=2）
                            continue;
                        }
                        
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("id", bag.getId()); // 使用UserBag的ID作为唯一标识
                        itemMap.put("itemType", 1); // 装备类型
                        itemMap.put("itemId", equipConfigId);
                        itemMap.put("name", equip.getName());
                        itemMap.put("type", equip.getType());
                        itemMap.put("atk", equip.getAtk());
                        itemMap.put("def", equip.getDef());
                        itemMap.put("hp", equip.getHp());
                        itemMap.put("mp", equip.getMp());
                        itemMap.put("speed", equip.getSpeed());
                        itemMap.put("isWorn", bag.getIsWorn() == 1);
                        itemMap.put("elfId", bag.getElfId());
                        resultItems.add(itemMap);
                    }
                }
            }
        }
        
        return Result.success(resultItems);
    }
}