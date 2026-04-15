package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.UserAchievementProgress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserAchievementProgressMapper extends BaseMapper<UserAchievementProgress> {

    List<UserAchievementProgress> selectByUserId(@Param("userId") Long userId);

    UserAchievementProgress selectByUserIdAndAchievementId(@Param("userId") Long userId, @Param("achievementId") Integer achievementId);

}