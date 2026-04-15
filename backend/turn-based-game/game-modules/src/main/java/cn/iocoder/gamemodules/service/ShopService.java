package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.ShopItem;

import java.util.List;
import java.util.Map;

public interface ShopService {
    // 获取所有商品
    Result<List<Map<String, Object>>> getAllItems();
    
    // 根据类型获取商品
    Result<List<Map<String, Object>>> getItemsByType(Integer type);
    
    // 购买商品
    Result<Map<String, Object>> buyItem(Long userId, Long shopItemId);
    
    // 获取用户拥有的指定类型装备
    Result<List<Map<String, Object>>> getUserEquipsByType(Long userId, Integer type);
}