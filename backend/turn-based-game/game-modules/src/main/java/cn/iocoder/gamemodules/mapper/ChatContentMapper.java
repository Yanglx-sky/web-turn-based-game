package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.ChatContent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天内容Mapper
 */
@Mapper
public interface ChatContentMapper extends BaseMapper<ChatContent> {

    /**
     * 根据消息UUID查询消息
     */
    @Select("SELECT * FROM chat_content WHERE msg_uuid = #{msgUuid} AND is_deleted = 0")
    ChatContent selectByMsgUuid(String msgUuid);

    /**
     * 查询频道历史消息（ID游标分页）
     * 使用lastId作为游标，实现深分页性能优化
     */
    @Select("SELECT * FROM chat_content " +
            "WHERE msg_type = 1 " +
            "AND channel_id = #{channelId} " +
            "AND is_deleted = 0 " +
            "AND id < #{lastId} " +
            "ORDER BY id DESC " +
            "LIMIT #{pageSize}")
    List<ChatContent> selectChannelMessagesByCursor(@Param("channelId") Long channelId,
                                                     @Param("lastId") Long lastId,
                                                     @Param("pageSize") Integer pageSize);

    /**
     * 查询私聊历史消息（ID游标分页）
     * 使用lastId作为游标，实现深分页性能优化
     */
    @Select("SELECT * FROM chat_content " +
            "WHERE msg_type = 2 " +
            "AND is_deleted = 0 " +
            "AND ((sender_id = #{userId} AND receiver_id = #{friendId}) " +
            "     OR (sender_id = #{friendId} AND receiver_id = #{userId})) " +
            "AND id < #{lastId} " +
            "ORDER BY id DESC " +
            "LIMIT #{pageSize}")
    List<ChatContent> selectPrivateMessagesByCursor(@Param("userId") Long userId,
                                                     @Param("friendId") Long friendId,
                                                     @Param("lastId") Long lastId,
                                                     @Param("pageSize") Integer pageSize);

    /**
     * 查询频道最新消息（首次加载）
     */
    @Select("SELECT * FROM chat_content " +
            "WHERE msg_type = 1 " +
            "AND channel_id = #{channelId} " +
            "AND is_deleted = 0 " +
            "ORDER BY id DESC " +
            "LIMIT #{pageSize}")
    List<ChatContent> selectLatestChannelMessages(@Param("channelId") Long channelId,
                                                   @Param("pageSize") Integer pageSize);

    /**
     * 查询私聊最新消息（首次加载）
     */
    @Select("SELECT * FROM chat_content " +
            "WHERE msg_type = 2 " +
            "AND is_deleted = 0 " +
            "AND ((sender_id = #{userId} AND receiver_id = #{friendId}) " +
            "     OR (sender_id = #{friendId} AND receiver_id = #{userId})) " +
            "ORDER BY id DESC " +
            "LIMIT #{pageSize}")
    List<ChatContent> selectLatestPrivateMessages(@Param("userId") Long userId,
                                                   @Param("friendId") Long friendId,
                                                   @Param("pageSize") Integer pageSize);
}
