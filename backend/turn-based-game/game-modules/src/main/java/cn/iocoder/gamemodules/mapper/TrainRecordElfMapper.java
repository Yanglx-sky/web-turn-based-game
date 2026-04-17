package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.TrainRecordElf;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TrainRecordElfMapper extends BaseMapper<TrainRecordElf> {
    
    /**
     * 根据训练人偶ID查询所有精灵记录
     */
    @Select("SELECT * FROM train_record_elf WHERE mannequin_id = #{mannequinId} ORDER BY id ASC")
    List<TrainRecordElf> selectByMannequinId(@Param("mannequinId") Long mannequinId);
    
    /**
     * 根据精灵ID查询记录
     */
    @Select("SELECT * FROM train_record_elf WHERE elf_id = #{elfId} AND mannequin_id = #{mannequinId}")
    TrainRecordElf selectByElfIdAndMannequinId(@Param("elfId") Long elfId, @Param("mannequinId") Long mannequinId);
}
