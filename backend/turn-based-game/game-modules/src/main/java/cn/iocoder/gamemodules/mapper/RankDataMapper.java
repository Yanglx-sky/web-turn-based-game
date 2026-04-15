package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.RankData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RankDataMapper extends BaseMapper<RankData> {

    List<RankData> selectByRankType(@Param("rankType") String rankType, @Param("limit") Integer limit);

    RankData selectByRankTypeAndUserId(@Param("rankType") String rankType, @Param("userId") Long userId);

    void updateRankByRankType(@Param("rankType") String rankType);

}