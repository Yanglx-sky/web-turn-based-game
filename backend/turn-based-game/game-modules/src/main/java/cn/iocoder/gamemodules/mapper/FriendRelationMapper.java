package cn.iocoder.gamemodules.mapper;

import cn.iocoder.gamemodules.entity.FriendRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 好友关系Mapper
 */
@Mapper
public interface FriendRelationMapper extends BaseMapper<FriendRelation> {

    /**
     * 查询双向好友关系
     * 用于校验双方是否为好友且未拉黑
     */
    @Select("SELECT * FROM friend_relation " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 1")
    FriendRelation selectFriendRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 查询用户的所有好友
     */
    @Select("SELECT * FROM friend_relation " +
            "WHERE user_id = #{userId} AND status = 1 " +
            "ORDER BY create_time DESC")
    List<FriendRelation> selectUserFriends(Long userId);

    /**
     * 查询待确认的好友申请
     */
    @Select("SELECT * FROM friend_relation " +
            "WHERE friend_id = #{userId} AND status = 0 " +
            "ORDER BY create_time DESC")
    List<FriendRelation> selectPendingRequests(Long userId);

    /**
     * 查询拉黑列表
     */
    @Select("SELECT * FROM friend_relation " +
            "WHERE user_id = #{userId} AND status = 2 " +
            "ORDER BY create_time DESC")
    List<FriendRelation> selectBlackList(Long userId);

    /**
     * 检查是否为好友关系（双向）
     */
    @Select("SELECT COUNT(*) FROM friend_relation " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 1")
    int checkFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 检查是否被拉黑
     */
    @Select("SELECT COUNT(*) FROM friend_relation " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 2")
    int checkBlacklisted(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 更新好友备注
     */
    @Update("UPDATE friend_relation SET remark = #{remark} " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int updateRemark(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("remark") String remark);

    /**
     * 同意好友申请
     */
    @Update("UPDATE friend_relation SET status = 1 " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 0")
    int acceptFriendRequest(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 拉黑好友
     */
    @Update("UPDATE friend_relation SET status = 2 " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int blacklistFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 解除拉黑
     */
    @Update("UPDATE friend_relation SET status = 1 " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 2")
    int unblacklistFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
