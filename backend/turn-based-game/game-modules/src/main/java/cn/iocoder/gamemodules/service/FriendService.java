package cn.iocoder.gamemodules.service;

import cn.iocoder.gamemodules.entity.FriendRelation;
import cn.iocoder.gamecommon.result.Result;

import java.util.List;

/**
 * 好友服务接口
 */
public interface FriendService {

    /**
     * 发送好友申请
     * @param userId 申请人ID
     * @param friendId 被申请人ID
     * @param remark 备注
     */
    Result<Void> sendFriendRequest(Long userId, Long friendId, String remark);

    /**
     * 同意好友申请
     * @param userId 当前用户ID
     * @param friendId 申请人ID
     */
    Result<Void> acceptFriendRequest(Long userId, Long friendId);

    /**
     * 拒绝好友申请
     * @param userId 当前用户ID
     * @param friendId 申请人ID
     */
    Result<Void> rejectFriendRequest(Long userId, Long friendId);

    /**
     * 删除好友
     * @param userId 当前用户ID
     * @param friendId 好友ID
     */
    Result<Void> deleteFriend(Long userId, Long friendId);

    /**
     * 获取好友列表
     * @param userId 当前用户ID
     */
    Result<List<FriendRelation>> getFriendList(Long userId);

    /**
     * 获取待确认的好友申请列表
     * @param userId 当前用户ID
     */
    Result<List<FriendRelation>> getPendingRequests(Long userId);

    /**
     * 拉黑好友
     * @param userId 当前用户ID
     * @param friendId 好友ID
     */
    Result<Void> blacklistFriend(Long userId, Long friendId);

    /**
     * 解除拉黑
     * @param userId 当前用户ID
     * @param friendId 好友ID
     */
    Result<Void> unblacklistFriend(Long userId, Long friendId);

    /**
     * 获取拉黑列表
     * @param userId 当前用户ID
     */
    Result<List<FriendRelation>> getBlacklist(Long userId);

    /**
     * 更新好友备注
     * @param userId 当前用户ID
     * @param friendId 好友ID
     * @param remark 备注
     */
    Result<Void> updateRemark(Long userId, Long friendId, String remark);

    /**
     * 检查是否为好友关系
     * @param userId 用户ID
     * @param friendId 好友ID
     */
    boolean isFriend(Long userId, Long friendId);

    /**
     * 检查是否被拉黑
     * @param userId 用户ID
     * @param friendId 好友ID
     */
    boolean isBlacklisted(Long userId, Long friendId);
}
