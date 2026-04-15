package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.ChatChannel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天频道Mapper
 */
@Mapper
public interface ChatChannelMapper extends BaseMapper<ChatChannel> {

    /**
     * 根据频道编码查询频道
     */
    @Select("SELECT * FROM chat_channel WHERE channel_code = #{channelCode} AND status = 1")
    ChatChannel selectByChannelCode(String channelCode);

    /**
     * 查询所有启用的频道
     */
    @Select("SELECT * FROM chat_channel WHERE status = 1 ORDER BY id ASC")
    List<ChatChannel> selectAllActiveChannels();
}
