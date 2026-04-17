package cn.iocoder.gamemodules.service.impl;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.User;
import cn.iocoder.gamemodules.entity.UserAsset;
import cn.iocoder.gamemodules.entity.UserElf;
import cn.iocoder.gamemodules.entity.UserLevelStar;
import cn.iocoder.gamemodules.mapper.UserAssetMapper;
import cn.iocoder.gamemodules.mapper.UserLevelStarMapper;
import cn.iocoder.gamemodules.mapper.UserMapper;
import cn.iocoder.gamemodules.mapper.UserElfMapper;
import cn.iocoder.gamemodules.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserElfMapper userElfMapper;
    
    @Autowired
    private UserAssetMapper userAssetMapper;
    
    @Autowired
    private UserLevelStarMapper userLevelStarMapper;

    @Override
    public User login(String phone, String password) {
        User user = lambdaQuery()
                .eq(User::getPhone, phone)
                .one();

        if (user == null) return null;
        if (!passwordEncoder.matches(password, user.getPassword())) return null;
        return user;
    }

    @Override
    @Transactional
    public Result<User> register(String password, String nickname, String phone, String email) {
        // 检查手机号是否已存在
        if (lambdaQuery().eq(User::getPhone, phone).one() != null) {
            return Result.error("手机号已被注册");
        }
        // 检查邮箱是否已存在
        if (lambdaQuery().eq(User::getEmail, email).one() != null) {
            return Result.error("邮箱已被注册");
        }

        // 创建新用户
        User user = new User();
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        user.setCurrentLevel(1);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIsDeleted(0);

        boolean success = save(user);
        if (!success) {
            return Result.error("注册失败");
        }
        
        // 创建用户资产记录，初始金币为0
        UserAsset userAsset = new UserAsset();
        userAsset.setUserId(user.getId());
        userAsset.setGold(0L);
        userAsset.setUpdateTime(LocalDateTime.now());
        userAssetMapper.insert(userAsset);
        
        return Result.success(user);
    }

    @Override
    public Result<User> getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 清除密码信息
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<User> updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        boolean success = updateById(user);
        if (!success) {
            return Result.error("密码修改失败");
        }
        return Result.success(user);
    }

    @Override
    public Result<User> updateUserInfo(Long userId, String nickname, String phone, String email, String avatar) {
        User user = getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查手机号是否被其他用户使用
        if (lambdaQuery().eq(User::getPhone, phone).ne(User::getId, userId).one() != null) {
            return Result.error("手机号已被其他用户使用");
        }
        // 检查邮箱是否被其他用户使用
        if (lambdaQuery().eq(User::getEmail, email).ne(User::getId, userId).one() != null) {
            return Result.error("邮箱已被其他用户使用");
        }

        // 更新用户信息
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        user.setUpdateTime(LocalDateTime.now());
        boolean success = updateById(user);
        if (!success) {
            return Result.error("资料修改失败");
        }
        return Result.success(user);
    }

    @Override
    public Result<Integer> getElfCount(Long userId) {
        long count = userElfMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserElf>()
                        .eq("user_id", userId)
        );
        return Result.success((int) count);
    }

    @Override
    public Result<User> updateUserLevel(Long userId, Integer levelId) {
        User user = getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        user.setCurrentLevel(levelId);
        user.setUpdateTime(LocalDateTime.now());
        boolean success = updateById(user);
        if (!success) {
            return Result.error("更新关卡失败");
        }
        return Result.success(user);
    }

    @Override
    public Result<Long> addGold(Long userId, Long amount) {
        // 查找用户的资产记录
        QueryWrapper<UserAsset> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        UserAsset userAsset = userAssetMapper.selectOne(wrapper);

        if (userAsset == null) {
            // 创建新的资产记录
            userAsset = new UserAsset();
            userAsset.setUserId(userId);
            userAsset.setGold(amount);
            userAsset.setUpdateTime(LocalDateTime.now());
            userAssetMapper.insert(userAsset);
        } else {
            // 更新金币数量
            userAsset.setGold(userAsset.getGold() + amount);
            userAsset.setUpdateTime(LocalDateTime.now());
            userAssetMapper.updateById(userAsset);
        }

        return Result.success(userAsset.getGold());
    }

    @Override
    public Result<Long> getUserAsset(Long userId) {
        // 查找用户的资产记录
        QueryWrapper<UserAsset> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        UserAsset userAsset = userAssetMapper.selectOne(wrapper);

        if (userAsset == null) {
            // 如果用户资产记录不存在，返回0金币
            return Result.success(0L);
        } else {
            // 返回用户的金币数量
            return Result.success(userAsset.getGold());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getUserLevelStars(Long userId) {
        // 查找用户的所有关卡星级记录
        List<UserLevelStar> records = userLevelStarMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserLevelStar>()
                        .eq("user_id", userId)
        );

        // 将记录转换为Map列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserLevelStar record : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("levelId", record.getLevelId());
            map.put("star", record.getStar());
            map.put("maxScore", record.getMaxScore());
            result.add(map);
        }

        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> searchUserByPhone(String phone) {
        // 根据手机号查找用户
        User user = lambdaQuery()
                .eq(User::getPhone, phone)
                .one();

        if (user == null) {
            return Result.error("未找到该用户");
        }

        // 只返回用户的id和nickname，保护隐私
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());

        return Result.success(result);
    }
}