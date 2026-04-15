package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.AchievementConfig;
import cn.iocoder.gamemodules.entity.UserAchievementProgress;
import cn.iocoder.gamemodules.mapper.AchievementConfigMapper;
import cn.iocoder.gamemodules.mapper.UserAchievementProgressMapper;
import cn.iocoder.gamemodules.service.AchievementService;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Date;
import java.util.List;

@Service
public class AchievementServiceImpl implements AchievementService {

    @Autowired
    private AchievementConfigMapper achievementConfigMapper;

    @Autowired
    private UserAchievementProgressMapper userAchievementProgressMapper;

    @Override
    public Result<List<AchievementConfig>> getAchievementConfigs() {
        List<AchievementConfig> achievementConfigs = achievementConfigMapper.selectList(null);
        return Result.success(achievementConfigs);
    }

    @Override
    public Result<List<UserAchievementProgress>> getUserAchievements(Long userId) {
        List<UserAchievementProgress> userAchievements = userAchievementProgressMapper.selectByUserId(userId);
        return Result.success(userAchievements);
    }

    @Override
    public Result<Void> updateAchievementProgress(Long userId, String achievementType, Integer increment) {
        System.out.println("=== 开始更新成就进度 ===");
        System.out.println("用户ID: " + userId);
        System.out.println("成就类型: " + achievementType);
        System.out.println("增量: " + increment);
        
        try {
            // 参数校验
            if (userId == null || achievementType == null || increment == null || increment < 0) {
                System.out.println("参数错误");
                return Result.error("参数错误");
            }
            
            // 获取与成就类型相关的成就配置
            List<AchievementConfig> achievementConfigs = achievementConfigMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AchievementConfig>()
                    .eq("condition_type", achievementType)
            );
            
            System.out.println("找到的成就配置数量: " + achievementConfigs.size());
            for (AchievementConfig config : achievementConfigs) {
                System.out.println("成就ID: " + config.getId() + ", 名称: " + config.getName() + ", 目标值: " + config.getConditionTarget());
                
                // 检查用户是否已有该成就的进度
                UserAchievementProgress progress = userAchievementProgressMapper.selectByUserIdAndAchievementId(userId, config.getId());
                
                if (progress == null) {
                    System.out.println("用户没有该成就的进度，创建新记录");
                    // 创建新的进度记录
                    progress = new UserAchievementProgress();
                    progress.setUserId(userId);
                    progress.setAchievementId(config.getId());
                    progress.setCurrentValue(increment);
                    progress.setStatus(0);
                    progress.setCreateTime(new Date());
                    progress.setUpdateTime(new Date());
                    
                    int insertResult = userAchievementProgressMapper.insert(progress);
                    System.out.println("插入结果: " + insertResult);
                    
                    // 检查是否完成成就
                    if (increment >= config.getConditionTarget()) {
                        System.out.println("成就完成，更新状态为1");
                        progress.setStatus(1);
                        userAchievementProgressMapper.updateById(progress);
                    }
                } else {
                    System.out.println("用户已有该成就的进度，当前值: " + progress.getCurrentValue());
                    // 更新进度
                    int oldValue = progress.getCurrentValue();
                    progress.setCurrentValue(oldValue + increment);
                    progress.setUpdateTime(new Date());

                    // 检查是否完成成就
                    if (progress.getCurrentValue() >= config.getConditionTarget() && progress.getStatus() == 0) {
                        System.out.println("成就完成，更新状态为1");
                        progress.setStatus(1);
                    }

                    int updateResult = userAchievementProgressMapper.updateById(progress);
                    System.out.println("更新结果: " + updateResult);
                }
            }

            System.out.println("=== 成就进度更新完成 ===");
            return Result.success();
        } catch (Exception e) {
            System.out.println("更新成就进度失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("更新成就进度失败: " + e.getMessage());
        }
    }

}