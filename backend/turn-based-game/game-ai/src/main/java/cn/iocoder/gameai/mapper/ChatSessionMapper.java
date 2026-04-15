package cn.iocoder.gameai.mapper;

import cn.iocoder.gameai.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 对话会话Mapper接口
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    
    /**
     * 根据用户ID查询未删除的会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> selectByUserId(Long userId);
    
    /**
     * 根据会话ID和用户ID查询会话（用于权限验证）
     * @param id 会话ID
     * @param userId 用户ID
     * @return 会话对象
     */
    ChatSession selectByIdAndUserId(Long id, Long userId);
}