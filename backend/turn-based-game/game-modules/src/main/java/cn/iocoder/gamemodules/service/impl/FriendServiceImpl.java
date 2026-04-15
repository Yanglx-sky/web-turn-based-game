package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamemodules.entity.FriendRelation;
import cn.iocoder.gamemodules.mapper.FriendRelationMapper;
import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.service.FriendService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 好友服务实现类
 */
@Service
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendRelationMapper friendRelationMapper;

    @Override
    @Transactional
    public Result<Void> sendFriendRequest(Long userId, Long friendId, String remark) {
        // 参数校验
        if (userId == null || friendId == null) {
            return Result.error("参数错误");
        }

        // 不能添加自己为好友
        if (userId.equals(friendId)) {
            return Result.error("不能添加自己为好友");
        }

        // 检查是否已存在关系
        QueryWrapper<FriendRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("friend_id", friendId);
        FriendRelation existing = friendRelationMapper.selectOne(wrapper);
        if (existing != null) {
            if (existing.getStatus() == 0) {
                return Result.error("好友申请已发送，等待对方确认");
            } else if (existing.getStatus() == 1) {
                return Result.error("对方已是您的好友");
            } else if (existing.getStatus() == 2) {
                return Result.error("您已拉黑对方，请先解除拉黑");
            }
        }

        // 创建好友申请（申请人视角）
        FriendRelation request = new FriendRelation();
        request.setUserId(userId);
        request.setFriendId(friendId);
        request.setRemark(remark);
        request.setStatus(0); // 待确认
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        friendRelationMapper.insert(request);

        // 创建好友申请记录（被申请人视角）
        FriendRelation receive = new FriendRelation();
        receive.setUserId(friendId);
        receive.setFriendId(userId);
        receive.setRemark(null);
        receive.setStatus(0); // 待确认
        receive.setCreateTime(LocalDateTime.now());
        receive.setUpdateTime(LocalDateTime.now());
        friendRelationMapper.insert(receive);

        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> acceptFriendRequest(Long userId, Long friendId) {
        // 参数校验
        if (userId == null || friendId == null) {
            return Result.error("参数错误");
        }

        // 更新被申请人视角的记录
        int result1 = friendRelationMapper.acceptFriendRequest(userId, friendId);
        if (result1 == 0) {
            return Result.error("好友申请不存在或已处理");
        }

        // 更新申请人视角的记录
        QueryWrapper<FriendRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", friendId).eq("friend_id", userId);
        FriendRelation requesterRelation = friendRelationMapper.selectOne(wrapper);
        if (requesterRelation != null) {
            requesterRelation.setStatus(1);
            requesterRelation.setUpdateTime(LocalDateTime.now());
            friendRelationMapper.updateById(requesterRelation);
        }

        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> rejectFriendRequest(Long userId, Long friendId) {
        // 参数校验
        if (userId == null || friendId == null) {
            return Result.error("参数错误");
        }

        // 删除被申请人视角的记录
        QueryWrapper<FriendRelation> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("user_id", userId).eq("friend_id", friendId).eq("status", 0);
        friendRelationMapper.delete(wrapper1);

        // 删除申请人视角的记录
        QueryWrapper<FriendRelation> wrapper2 = new QueryWrapper<>();
        wrapper2.eq("user_id", friendId).eq("friend_id", userId).eq("status", 0);
        friendRelationMapper.delete(wrapper2);

        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> deleteFriend(Long userId, Long friendId) {
        // 参数校验
        if (userId == null || friendId == null) {
            return Result.error("参数错误");
        }

        // 删除双向关系
        QueryWrapper<FriendRelation> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("user_id", userId).eq("friend_id", friendId);
        friendRelationMapper.delete(wrapper1);

        QueryWrapper<FriendRelation> wrapper2 = new QueryWrapper<>();
        wrapper2.eq("user_id", friendId).eq("friend_id", userId);
        friendRelationMapper.delete(wrapper2);

        return Result.success();
    }

    @Override
    public Result<List<FriendRelation>> getFriendList(Long userId) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        List<FriendRelation> friends = friendRelationMapper.selectUserFriends(userId);
        return Result.success(friends);
    }

    @Override
    public Result<List<FriendRelation>> getPendingRequests(Long userId) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        List<FriendRelation> requests = friendRelationMapper.selectPendingRequests(userId);
        return Result.success(requests);
    }

    @Override
    @Transactional
    public Result<Void> blacklistFriend(Long userId, Long friendId) {
        // 参数校验
        if (userId == null || friendId == null) {
            return Result.error("参数错误");
        }

        // 检查是否为好友
        if (!isFriend(userId, friendId)) {
            return Result.error("对方不是您的好友");
        }

        // 拉黑
        int result = friendRelationMapper.blacklistFriend(userId, friendId);
        if (result == 0) {
            return Result.error("操作失败");
        }

        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void> unblacklistFriend(Long userId, Long friendId) {
        // 参数校验
        if (userId == null || friendId == null) {
            return Result.error("参数错误");
        }

        // 解除拉黑
        int result = friendRelationMapper.unblacklistFriend(userId, friendId);
        if (result == 0) {
            return Result.error("操作失败或对方不在黑名单中");
        }

        return Result.success();
    }

    @Override
    public Result<List<FriendRelation>> getBlacklist(Long userId) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        List<FriendRelation> blacklist = friendRelationMapper.selectBlackList(userId);
        return Result.success(blacklist);
    }

    @Override
    @Transactional
    public Result<Void> updateRemark(Long userId, Long friendId, String remark) {
        // 参数校验
        if (userId == null || friendId == null) {
            return Result.error("参数错误");
        }

        int result = friendRelationMapper.updateRemark(userId, friendId, remark);
        if (result == 0) {
            return Result.error("更新失败，好友关系不存在");
        }

        return Result.success();
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return false;
        }
        // 检查双向好友关系
        int count1 = friendRelationMapper.checkFriendship(userId, friendId);
        int count2 = friendRelationMapper.checkFriendship(friendId, userId);
        return count1 > 0 && count2 > 0;
    }

    @Override
    public boolean isBlacklisted(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return false;
        }
        // 检查是否被对方拉黑
        int count = friendRelationMapper.checkBlacklisted(friendId, userId);
        return count > 0;
    }
}
