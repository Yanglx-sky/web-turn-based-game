package cn.iocoder.gamemodules.service;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamemodules.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {
    User login(String phone, String password);
    Result<User> register(String password, String nickname, String phone, String email);
    Result<User> getUserInfo(Long userId);
    Result<User> updatePassword(Long userId, String oldPassword, String newPassword);
    Result<User> updateUserInfo(Long userId, String nickname, String phone, String email, String avatar);
    Result<Integer> getElfCount(Long userId);
    Result<User> updateUserLevel(Long userId, Integer levelId);
    Result<Long> addGold(Long userId, Long amount);
    Result<Long> getUserAsset(Long userId);
    Result<List<Map<String, Object>>> getUserLevelStars(Long userId);
    Result<Map<String, Object>> searchUserByPhone(String phone);
}