package cn.iocoder.gameweb.controller;

import cn.iocoder.gamecommon.result.Result;
import cn.iocoder.gamecommon.annotation.Loggable;
import cn.iocoder.gamecommon.util.JwtUtil;
import cn.iocoder.gamemodules.entity.User;
import cn.iocoder.gamemodules.service.UserService;
import cn.iocoder.gameweb.config.OSSConfig;
import com.aliyun.oss.OSS;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;

@RestController
@RequestMapping("/users")
@Tag(name = "用户管理", description = "用户相关接口")
@Loggable
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private OSSConfig ossConfig;
    
    @Autowired
    private OSS ossClient;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public Object register(@RequestBody RegisterRequest request) {
        // 检查密码和确认密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Result.error("两次输入的密码不一致");
        }
        return userService.register(request.getPassword(), request.getNickname(), request.getPhone(), request.getEmail());
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录获取用户信息和token")
    public Object login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getPhone(), request.getPassword());
        if (user == null) {
            return Result.error("手机号或密码错误");
        }
        
        // 生成JWT令牌
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("nickname", user.getNickname());
        claims.put("phone", user.getPhone());
        String token = jwtUtil.generateToken(user.getId(), claims);
        
        // 构建返回结果
        HashMap<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("token", token);
        
        return Result.success(result);
    }

    @GetMapping("/me")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户信息")
    public Object getUserInfo(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userService.getUserInfo(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @PutMapping("/me/password")
    @Operation(summary = "修改密码", description = "修改用户密码")
    public Object updatePassword(HttpServletRequest request, @RequestBody UpdatePasswordRequest updateRequest) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userService.updatePassword(userId, updateRequest.getOldPassword(), updateRequest.getNewPassword());
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @PutMapping("/me")
    @Operation(summary = "修改资料", description = "修改用户资料")
    public Object updateUserInfo(HttpServletRequest request, @RequestBody UpdateUserInfoRequest updateRequest) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userService.updateUserInfo(userId, updateRequest.getNickname(), updateRequest.getPhone(), updateRequest.getEmail(), updateRequest.getAvatar());
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @GetMapping("/me/elves/count")
    @Operation(summary = "获取用户精灵数量", description = "获取用户拥有的精灵数量")
    public Object getElfCount(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userService.getElfCount(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @GetMapping("/me/assets")
    @Operation(summary = "获取用户资产", description = "获取用户的金币数量")
    public Object getUserAsset(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userService.getUserAsset(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @GetMapping("/me/level-stars")
    @Operation(summary = "获取用户关卡星级", description = "获取用户的关卡星级和评分")
    public Object getUserLevelStars(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return Result.error("未登录，无权限访问");
            }
            // 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            return userService.getUserLevelStars(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @PostMapping("/me/avatar")
    @Operation(summary = "上传头像", description = "上传用户头像")
    public Object uploadAvatar(
            @Parameter(name = "file", description = "头像文件", required = true) @RequestParam("file") MultipartFile file) {
        try {
            // 校验文件是否为空
            if (file == null || file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            
            // 校验文件大小（最大5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("文件大小不能超过5MB");
            }
            
            // 校验文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名不能为空");
            }
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!fileExtension.matches("jpg|jpeg|png|gif|webp")) {
                return Result.error("只支持 jpg、jpeg、png、gif、webp 格式的图片");
            }
            
            // 生成唯一文件名
            String fileName = "avatars/" + System.currentTimeMillis() + "_" + originalFilename;
            
            // 上传文件到阿里云OSS
            ossClient.putObject(ossConfig.getBucketName(), fileName, file.getInputStream());
            
            // 生成完整的访问URL
            String endpoint = ossConfig.getEndpoint().replace("https://", "");
            String fileUrl = "https://" + ossConfig.getBucketName() + "." + endpoint + "/" + fileName;
            
            return Result.success(fileUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败");
        }
    }

    // 请求参数类
    static class RegisterRequest {
        private String password;
        private String confirmPassword;
        private String nickname;
        private String phone;
        private String email;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    static class LoginRequest {
        private String phone;
        private String password;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class UpdatePasswordRequest {
        private Long userId;
        private String oldPassword;
        private String newPassword;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    static class UpdateUserInfoRequest {
        private Long userId;
        private String nickname;
        private String phone;
        private String email;
        private String avatar;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
    }
}