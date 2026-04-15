package cn.iocoder.gameai.mapper;

import cn.iocoder.gameai.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 对话消息Mapper接口
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    
    /**
     * 根据会话ID查询最近的消息列表
     * @param sessionId 会话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<ChatMessage> selectBySessionIdOrderByCreateTimeDesc(Long sessionId, Integer limit);
    
    /**
     * 根据会话ID删除所有消息
     * @param sessionId 会话ID
     * @return 影响行数
     */
    int deleteBySessionId(Long sessionId);
}