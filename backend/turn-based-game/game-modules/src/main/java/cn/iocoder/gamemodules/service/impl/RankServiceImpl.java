package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.RankConfig;
import cn.iocoder.gamemodules.entity.RankData;
import cn.iocoder.gamemodules.entity.User;
import cn.iocoder.gamemodules.mapper.RankConfigMapper;
import cn.iocoder.gamemodules.mapper.RankDataMapper;
import cn.iocoder.gamemodules.mapper.UserMapper;
import cn.iocoder.gamemodules.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RankServiceImpl implements RankService {

    @Autowired
    private RankConfigMapper rankConfigMapper;

    @Autowired
    private RankDataMapper rankDataMapper;

    @Override
    public Result<List<RankConfig>> getRankConfigs() {
        List<RankConfig> rankConfigs = rankConfigMapper.selectList(null);
        return Result.success(rankConfigs);
    }

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<List<Map<String, Object>>> getRankData(String rankType, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        // 如果是通关排行榜，直接查询用户表
        if ("stage".equals(rankType)) {
            List<User> users = userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .orderByDesc("current_level")
                .orderByAsc("update_time")
                .last("LIMIT " + limit)
            );
            
            List<Map<String, Object>> rankDataList = new ArrayList<>();
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                Map<String, Object> rankData = new HashMap<>();
                rankData.put("id", (long) (i + 1));
                rankData.put("rankType", rankType);
                rankData.put("userId", user.getId());
                rankData.put("nickname", user.getNickname());
                rankData.put("rankNum", i + 1);
                rankData.put("score", user.getCurrentLevel());
                rankData.put("updateTime", new Date());
                rankDataList.add(rankData);
            }
            return Result.success(rankDataList);
        } else {
            // 其他类型的排行榜使用原来的逻辑
            List<RankData> rankDataList = rankDataMapper.selectByRankType(rankType, limit);
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (RankData rankData : rankDataList) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rankData.getId());
                map.put("rankType", rankData.getRankType());
                map.put("userId", rankData.getUserId());
                // 查询用户信息获取nickname
                User user = userMapper.selectById(rankData.getUserId());
                map.put("nickname", user != null ? user.getNickname() : "未知用户");
                map.put("rankNum", rankData.getRankNum());
                map.put("score", rankData.getScore());
                map.put("updateTime", rankData.getUpdateTime());
                resultList.add(map);
            }
            return Result.success(resultList);
        }
    }

    @Override
    public Result<RankData> getUserRankData(String rankType, Long userId) {
        // 如果是通关排行榜，直接查询用户表
        if ("stage".equals(rankType)) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 计算用户排名
            long count = userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .gt("current_level", user.getCurrentLevel())
            );
            int rankNum = (int) count + 1;
            
            RankData rankData = new RankData();
            rankData.setRankType(rankType);
            rankData.setUserId(userId);
            rankData.setRankNum(rankNum);
            rankData.setScore(user.getCurrentLevel());
            rankData.setUpdateTime(new Date());
            return Result.success(rankData);
        } else {
            // 其他类型的排行榜使用原来的逻辑
            RankData rankData = rankDataMapper.selectByRankTypeAndUserId(rankType, userId);
            return Result.success(rankData);
        }
    }

    @Override
    @Transactional
    public Result<Void> updateUserRank(String rankType, Long userId, Integer score) {
        // 检查排行榜配置是否存在
        List<RankConfig> rankConfigs = rankConfigMapper.selectList(null);
        boolean rankExists = false;
        for (RankConfig config : rankConfigs) {
            if (config.getRankType().equals(rankType)) {
                rankExists = true;
                break;
            }
        }
        if (!rankExists) {
            return Result.error("排行榜配置不存在");
        }

        // 检查用户是否已经在排行榜中
        RankData existingRankData = rankDataMapper.selectByRankTypeAndUserId(rankType, userId);

        if (existingRankData != null) {
            // 更新分数
            existingRankData.setScore(score);
            existingRankData.setUpdateTime(new Date());
            rankDataMapper.updateById(existingRankData);
        } else {
            // 插入新数据
            RankData newRankData = new RankData();
            newRankData.setRankType(rankType);
            newRankData.setUserId(userId);
            newRankData.setScore(score);
            newRankData.setUpdateTime(new Date());
            rankDataMapper.insert(newRankData);
        }

        // 更新排行榜排名
        rankDataMapper.updateRankByRankType(rankType);

        return Result.success();
    }

}