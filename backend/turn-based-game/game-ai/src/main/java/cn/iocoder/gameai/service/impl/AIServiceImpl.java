package cn.iocoder.gameai.service.impl;

import cn.iocoder.gameai.config.AIConfig;
import cn.iocoder.gameai.entity.ChatMessage;
import cn.iocoder.gameai.entity.SensitiveWord;
import cn.iocoder.gameai.mapper.SensitiveWordMapper;
import cn.iocoder.gameai.service.AIService;
import cn.iocoder.gameai.service.SessionService;
import cn.iocoder.gamecommon.util.RedisUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AIServiceImpl implements AIService {
    
    private final Random random = new Random();
    
    @Autowired
    private AIConfig aiConfig;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private RedisUtil redisUtil;
    
    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;
    
    // Jackson JSON处理
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String SENSITIVE_WORDS_CACHE_KEY = "ai:sensitive:words";

    @Override
    public String getAIAnalysis(String content) {
        return "AI分析结果: " + content;
    }

    @Override
    public String getMonsterTaunt(String monsterName, int playerHp, int playerMaxHp, int monsterHp, int monsterMaxHp, String actionType) {
        try {
            // 计算血量百分比
            double playerHpPercent = (double) playerHp / playerMaxHp;
            double monsterHpPercent = (double) monsterHp / monsterMaxHp;
            
            // 构建AI提示词
            String prompt = String.format(
                "你是一个游戏中的怪物角色，名叫'%s'。\n" +
                "\n当前战斗状态：\n" +
                "- 玩家血量：%d/%d (%.0f%%)\n" +
                "- 你的血量：%d/%d (%.0f%%)\n" +
                "\n动作类型：%s\n" +
                "\n请生成一句符合以下要求的嘲讽/台词：\n" +
                "1. 必须以'%s '开头（注意有空格）\n" +
                "2. 如果你的血量较低（<30%%），表现出愤怒、恐惧或垂死挣扎，使用'怒吼'、'垂死挣扎'、'惊慌'等词\n" +
                "3. 如果玩家血量较低（<30%%），表现出得意、嘲讽或轻蔑，使用'狞笑着说'、'嘲讽道'、'大笑'等词\n" +
                "4. 如果是你攻击玩家，说出攻击性的台词，使用'冷笑道'、'不屑地说'、'嘲讽道'等词\n" +
                "5. 如果是你被玩家攻击，说出反击或受伤的台词，使用'怒道'、'咬牙切齿'、'冷声道'等词\n" +
                "6. 语言风格要符合游戏氛围，生动有趣\n" +
                "7. 长度控制在20-40字\n" +
                "8. 直接返回台词内容，不要添加任何解释或前缀\n" +
                "\n示例格式：\n" +
                "%s 冷笑道：这就是你的全部实力吗？\n" +
                "%s 怒吼：不可能！我怎么会输？\n" +
                "\n请生成台词：",
                monsterName,
                playerHp, playerMaxHp, playerHpPercent * 100,
                monsterHp, monsterMaxHp, monsterHpPercent * 100,
                "attack".equals(actionType) ? "你正在攻击玩家" : "你被玩家攻击",
                monsterName,
                monsterName,
                monsterName
            );
            
            // 调用AI
            String aiResponse = callAI(prompt);
            
            // 如果AI调用失败，使用默认台词
            if (aiResponse.contains("AI处理失败")) {
                return generateDefaultTaunt(monsterName, playerHpPercent, monsterHpPercent, actionType);
            }
            
            // 清理AI返回内容，确保格式正确
            return aiResponse.trim();
            
        } catch (Exception e) {
            log.error("生成怪物嘲讽失败", e);
            return generateDefaultTaunt(monsterName, 
                (double) playerHp / playerMaxHp, 
                (double) monsterHp / monsterMaxHp, 
                actionType);
        }
    }
    
    /**
     * 生成默认嘲讽（AI失败时的备选方案）
     */
    private String generateDefaultTaunt(String monsterName, double playerHpPercent, double monsterHpPercent, String actionType) {
        Random random = new Random();
        
        if ("attack".equals(actionType)) {
            // 怪物攻击玩家
            if (playerHpPercent < 0.3) {
                String[] taunts = {
                    monsterName + " 狞笑着说：你的精灵不堪一击！",
                    monsterName + " 嘲讽道：就这点能耐？马上就能结束战斗了！",
                    monsterName + " 大笑：太弱了，马上就能击败你了！"
                };
                return taunts[random.nextInt(taunts.length)];
            } else if (monsterHpPercent < 0.3) {
                String[] taunts = {
                    monsterName + " 怒吼：就算我受伤了，也能击败你！",
                    monsterName + " 咬牙切齿：我要把你的精灵打败！",
                    monsterName + " 狞笑：垂死挣扎也要拉你一起！"
                };
                return taunts[random.nextInt(taunts.length)];
            } else {
                String[] taunts = {
                    monsterName + " 冷笑道：这就是你的全部实力吗？",
                    monsterName + " 不屑地说：浪费我的时间！",
                    monsterName + " 嘲讽道：你的精灵还不够强！"
                };
                return taunts[random.nextInt(taunts.length)];
            }
        } else {
            // 怪物被玩家攻击
            if (monsterHpPercent < 0.3) {
                String[] taunts = {
                    monsterName + " 怒吼：不可能！我怎么会输？",
                    monsterName + " 垂死挣扎：我还能战斗！",
                    monsterName + " 惊慌：不，这不可能！"
                };
                return taunts[random.nextInt(taunts.length)];
            } else if (monsterHpPercent > 0.7) {
                String[] taunts = {
                    monsterName + " 轻蔑地说：这点伤害对我来说不算什么！",
                    monsterName + " 大笑：挠痒痒吗？继续努力吧！",
                    monsterName + " 不屑地说：就这点攻击力？"
                };
                return taunts[random.nextInt(taunts.length)];
            } else {
                String[] taunts = {
                    monsterName + " 怒道：有点意思！",
                    monsterName + " 咬牙切齿：你会为此付出代价的！",
                    monsterName + " 冷声道：不错的攻击，但还不够！"
                };
                return taunts[random.nextInt(taunts.length)];
            }
        }
    }


    @Override
    public String getStrategyRecommendation(String elfInfo, String monsterInfo) {
        // 解析精灵和怪物信息
        String[] monsterParts = monsterInfo.split(",");
        
        if (monsterParts.length < 2) {
            return "请提供完整的怪物信息。";
        }
        
        String monsterName = monsterParts[0];
        String monsterElementType = monsterParts[1];
        
        // 解析精灵信息，处理多个精灵的情况
        String[] elfLines = elfInfo.split(";\n");
        if (elfLines.length == 0) {
            return "请提供完整的精灵信息。";
        }
        
        // 找出最适合的精灵
        String bestElfName = "";
        String bestElfElementType = "";
        boolean foundCounter = false;
        
        for (String elfLine : elfLines) {
            if (elfLine.trim().isEmpty()) {
                continue;
            }
            
            String[] elfParts = elfLine.split(",");
            if (elfParts.length < 2) {
                continue;
            }
            
            String elfName = elfParts[0];
            String elfElementType = elfParts[1];
            
            // 检查是否克制怪物
            if (("火系".equals(elfElementType) && "草系".equals(monsterElementType)) ||
                ("草系".equals(elfElementType) && "水系".equals(monsterElementType)) ||
                ("水系".equals(elfElementType) && "火系".equals(monsterElementType)) ||
                ("草系".equals(elfElementType) && "光系".equals(monsterElementType))) {
                bestElfName = elfName;
                bestElfElementType = elfElementType;
                foundCounter = true;
                break; // 找到克制的精灵就停止
            }
            
            // 如果还没有找到克制的精灵，记录第一个精灵
            if (bestElfName.isEmpty()) {
                bestElfName = elfName;
                bestElfElementType = elfElementType;
            }
        }
        
        if (bestElfName.isEmpty()) {
            return "请提供完整的精灵信息。";
        }
        
        // 使用默认逻辑生成战斗策略推荐，确保系别克制关系正确
        if (foundCounter) {
            return "战斗策略推荐：" + bestElfName + "克制" + monsterName + "，用" + bestElfElementType + "技能攻击！";
        } else if (bestElfElementType.equals(monsterElementType)) {
            return "战斗策略推荐：" + bestElfName + "与" + monsterName + "同系，用高伤技能！";
        } else {
            return "战斗策略推荐：用" + bestElfName + "的强力技能攻击" + monsterName + "！";
        }
    }
    
    // 调用大模型API的方法
    private String callAI(String prompt) {
        try {
            // 使用配置中的API信息
            URL url = new URL(aiConfig.getApiUrl() + "/v1/chat/completions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
            connection.setDoOutput(true);
            
            // 使用Jackson构建请求体
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("model", aiConfig.getModel());
            rootNode.put("temperature", 0.7);
            
            ArrayNode messagesArray = rootNode.putArray("messages");
            ObjectNode messageNode = messagesArray.addObject();
            messageNode.put("role", "user");
            messageNode.put("content", prompt);
            
            String requestBody = objectMapper.writeValueAsString(rootNode);
            
            // 发送请求，使用UTF-8编码
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.getBytes("UTF-8"));
            os.flush();
            os.close();
            
            // 读取响应
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            
            // 使用Jackson解析响应
            String responseStr = response.toString();
            JsonNode rootNode_response = objectMapper.readTree(responseStr);
            
            // 解析 choices[0].message.content
            JsonNode choices = rootNode_response.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                if (message.has("content")) {
                    return message.get("content").asText();
                }
            }
            
            return "AI处理失败，请重试。";
        } catch (Exception e) {
            e.printStackTrace();
            // 如果API调用失败，返回一个默认的响应
            return "AI处理失败，使用默认回复。";
        }
    }
    
    // 修改getBattleSummary方法，使用大模型API生成战后总结
    @Override
    public String getBattleSummary(String battleLog, String battleResult) {
        // 构建提示词
        String prompt = "请根据以下战斗日志和战斗结果，生成一个详细的总结，包含两个并列部分：\n\n" +
            "战斗结果：" + battleResult + "\n\n" +
            "战斗日志：\n" + battleLog + "\n\n" +
            "第一部分：战后总结\n" +
            "1. 分析战斗过程中的关键转折点\n" +
            "2. 分析胜负原因\n" +
            "3. 给出改进建议\n\n" +
            "第二部分：近期成长状况\n" +
            "1. 精灵等级提升情况\n" +
            "2. 技能解锁情况\n" +
            "3. 战斗胜率分析\n" +
            "4. 推荐的训练方向\n\n" +
            "总结要求：\n" +
            "1. 两个部分要并列显示，清晰分明\n" +
            "2. 语言风格要符合游戏氛围，生动有趣\n" +
            "3. 总结长度控制在300-400字左右";
        
        // 调用大模型API
        String aiResponse = callAI(prompt);
        
        // 如果API调用失败，返回默认总结
        if (aiResponse.equals("AI处理失败，使用默认回复。")) {
            if ("胜利".equals(battleResult)) {
                return "战后总结：你成功击败了对手！在战斗中，你展现了出色的战术素养，合理运用精灵技能，最终取得了胜利。建议你继续保持这种良好的战斗状态，不断提升精灵的战斗能力。\n\n近期成长状况：最近你的精灵成长迅速，等级和技能都有显著提升，战斗胜率也在稳步提高。继续保持这种势头，你将成为更强大的训练师！";
            } else {
                return "战后总结：很遗憾，你在本次战斗中失败了。建议你提升精灵等级，优化战斗策略，下次一定能够取得胜利！\n\n近期成长状况：最近你的精灵也在不断成长，虽然暂时遇到了挫折，但这正是成长的必经之路。继续努力，你会变得更强！";
            }
        }
        
        return aiResponse;
    }
    
    @Override
    public String getBattleScoreAndStar(String battleLog, String battleResult) {
        // 构建提示词，要求返回JSON格式
        String prompt = "你是一个专业的游戏战斗评分系统。请根据以下战斗日志和战斗结果，客观地给出评分（0-100分）和评星（1-3星）。\n\n" +
            "战斗结果：" + battleResult + "\n\n" +
            "战斗日志：\n" + battleLog + "\n\n" +
            "评分标准：\n" +
            "- 90-100分：3星，完美通关，回合数少（≤5回合），无损失或极少损失\n" +
            "- 75-89分：2星，顺利通关，回合数适中（6-10回合），有一定损失\n" +
            "- 60-74分：1星，勉强通关，回合数较多（11-15回合），损失较大\n" +
            "- 0-59分：0星，非常艰难，回合数很多（>15回合），损失严重\n\n" +
            "评分要素：\n" +
            "1. 回合数：回合越少分数越高\n" +
            "2. 战斗效率：是否快速击败敌人\n" +
            "3. 策略运用：是否合理使用技能和属性克制\n\n" +
            "请严格按照以下JSON格式返回，不要返回其他内容，不要添加任何解释或markdown标记：\n" +
            "{\"score\": 分数, \"star\": 星级}";
        
        // 调用大模型API
        String aiResponse = callAI(prompt);
        
        // 如果API调用失败，返回默认JSON
        if (aiResponse.equals("AI处理失败，使用默认回复。")) {
            return "{\"score\": 75, \"star\": 2}";
        }
        
        return aiResponse;
    }
    
    @Override
    public Long createSession(Long userId, String title, String scene) {
        // 创建数据库会话（Redis缓存由SessionServiceImpl处理）
        return sessionService.createSession(userId, title, scene);
    }
    
    @Override
    public String addConversation(Long sessionId, String content) {
        // 从数据库获取会话信息（带Redis缓存）
        cn.iocoder.gameai.entity.ChatSession dbSession = sessionService.getSessionById(sessionId, null);
        if (dbSession == null) {
            return "会话不存在，请先创建会话。";
        }
                    
        // 使用从数据库获取的userId
        Long sessionUserId = dbSession.getUserId();
                    
        // 检查用户AI调用次数（使用Redis）
        if (!checkAICallLimit(sessionUserId)) {
            return "今日AI调用次数已达上限，请明日再试。";
        }
        
        // 内容过滤
        if (containsSensitiveWords(content)) {
            return "输入内容包含敏感词，请修改后重试。";
        }
        
        // 长度限制
        if (content.length() > 500) {
            return "输入内容过长，请控制在500字以内。";
        }
        
        // 记录用户输入到数据库
        sessionService.addMessage(sessionId, "user", content, "text");
        
        // 从数据库加载最近的消息作为上下文（带Redis缓存）
        List<ChatMessage> recentMessages = sessionService.getRecentMessages(sessionId, 10);
        
        // 构建上下文
        StringBuilder context = new StringBuilder();
        context.append("你是一个精灵训练师的AI助手，负责帮助训练师分析精灵成长和战斗策略。\n\n");
        
        // 倒序遍历消息，构建上下文
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessage message = recentMessages.get(i);
            if (message.getRole().equals("user")) {
                context.append("训练师：").append(message.getContent()).append("\n");
            } else if (message.getRole().equals("assistant")) {
                context.append("AI：").append(message.getContent()).append("\n");
            }
        }
        
        // 调用AI
        String response = callAI(context.toString());
        
        // 记录AI回复到数据库
        sessionService.addMessage(sessionId, "assistant", response, "text");
        
        // 增加AI调用次数（Redis计数已在checkAICallLimit中完成）
        // incrementAICallCount(userId); // 不需要重复增加
        
        return response;
    }
    
    @Override
    public boolean closeSession(Long sessionId, Long userId) {
        // 删除数据库会话（Redis缓存由SessionServiceImpl处理）
        return sessionService.deleteSession(sessionId, userId);
    }
    
    @Override
    public String getTrainingSummary(Long userId) {
        // 检查用户AI调用次数
        if (!checkAICallLimit(userId)) {
            return "今日AI调用次数已达上限，请明日再试。";
        }
        
        // 构建训练总结的提示词
        String prompt = "请作为精灵训练师的AI助手，总结玩家最近的成长状况，包括：\n" +
            "1. 精灵等级提升情况\n" +
            "2. 技能解锁情况\n" +
            "3. 战斗胜率分析\n" +
            "4. 推荐的训练方向\n" +
            "5. 鼓励和建议\n\n" +
            "语言风格要温暖、鼓励，符合游戏氛围。";
        
        String response = callAI(prompt);
        
        // Redis计数已在checkAICallLimit中完成，不需要重复增加
        
        return response;
    }
    
    @Override
    public List<Map<String, Object>> getSessionList(Long userId) {
        // 从数据库获取会话列表
        List<cn.iocoder.gameai.entity.ChatSession> sessions = sessionService.getSessionList(userId);
        
        // 转换为Map列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (cn.iocoder.gameai.entity.ChatSession session : sessions) {
            Map<String, Object> sessionMap = new HashMap<>();
            sessionMap.put("id", session.getId());
            sessionMap.put("title", session.getTitle());
            sessionMap.put("scene", session.getScene());
            sessionMap.put("createTime", session.getCreateTime());
            sessionMap.put("updateTime", session.getUpdateTime());
            result.add(sessionMap);
        }
        
        return result;
    }
    
    @Override
    public boolean updateSessionTitle(Long sessionId, Long userId, String title) {
        return sessionService.updateSessionTitle(sessionId, userId, title);
    }
    
    @Override
    public void streamAIAnalysis(String sessionIdStr, String content, HttpServletResponse response, Long userId) {
        // 先设置SSE响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        
        try {
            // 转换sessionId为Long类型
            Long sessionId = Long.parseLong(sessionIdStr);
            
            // 从数据库获取会话信息（带Redis缓存）
            cn.iocoder.gameai.entity.ChatSession dbSession = sessionService.getSessionById(sessionId, userId);
            if (dbSession == null) {
                try (PrintWriter writer = response.getWriter()) {
                    writer.write("data: 会话不存在，请先创建会话。\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            
            // 使用从数据库获取的userId
            Long sessionUserId = dbSession.getUserId();
            
            // 检查用户AI调用次数（使用Redis）
            if (!checkAICallLimit(sessionUserId)) {
                try (PrintWriter writer = response.getWriter()) {
                    writer.write("data: 今日AI调用次数已达上限，请明日再试。\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            
            // 内容过滤
            if (containsSensitiveWords(content)) {
                try (PrintWriter writer = response.getWriter()) {
                    writer.write("data: 输入内容包含敏感词，请修改后重试。\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            
            // 长度限制
            if (content.length() > 500) {
                try (PrintWriter writer = response.getWriter()) {
                    writer.write("data: 输入内容过长，请控制在500字以内。\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            
            try (PrintWriter writer = response.getWriter()) {
                // 记录用户输入到数据库
                sessionService.addMessage(sessionId, "user", content, "text");
                            
                // 从数据库加载最近的消息作为上下文（带Redis缓存）
                List<ChatMessage> recentMessages = sessionService.getRecentMessages(sessionId, 10);
                
                log.info("加载到 {} 条历史消息", recentMessages.size());
                
                // 构建上下文
                StringBuilder context = new StringBuilder();
                context.append("你是一个精灵训练师的AI助手，负责帮助训练师分析精灵成长和战斗策略。\n\n");
                context.append("请根据对话历史，回答用户的最后一个问题。\n\n");
                
                // 倒序遍历消息（因为查询结果是降序），构建正序上下文（从旧到新）
                for (int i = recentMessages.size() - 1; i >= 0; i--) {
                    ChatMessage message = recentMessages.get(i);
                    if (message.getRole().equals("user")) {
                        context.append("训练师：").append(message.getContent()).append("\n");
                    } else if (message.getRole().equals("assistant")) {
                        context.append("AI：").append(message.getContent()).append("\n");
                    }
                }
                
                log.info("构建的上下文：\n{}", context.toString());
                
                // 调用AI
                String responseContent = callAI(context.toString());
                
                // 记录AI回复到数据库
                sessionService.addMessage(sessionId, "assistant", responseContent, "text");
                
                // Redis计数已在checkAICallLimit中完成，不需要重复增加
                
                // 流式输出（优化性能：按词组输出而非单字符，减少flush次数）
                // 使用更简单的分割策略：按标点、空格分割，中文按2-3字一组
                String[] words = responseContent.split("(?<=[，。！？、；：\"\'\u2018\u2019\u201c\u201d（）【】《》])|(?<=\\s)");
                for (String word : words) {
                    if (word != null && !word.isEmpty()) {
                        // 如果是纯中文且长度>3，进一步拆分
                        if (word.length() > 3 && word.matches("^[\\u4e00-\\u9fa5]+$")) {
                            for (int i = 0; i < word.length(); i += 3) {
                                int end = Math.min(i + 3, word.length());
                                writer.write("data: " + word.substring(i, end) + "\n\n");
                                writer.flush();
                                Thread.sleep(10);
                            }
                        } else {
                            writer.write("data: " + word + "\n\n");
                            writer.flush();
                            Thread.sleep(10);
                        }
                    }
                }
                
                // 结束信号
                writer.write("data: [DONE]\n\n");
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (NumberFormatException e) {
            try (PrintWriter writer = response.getWriter()) {
                writer.write("data: 无效的会话ID。\n\n");
                writer.write("data: [DONE]\n\n");
                writer.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    @Override
    public boolean checkAICallLimit(Long userId) {
        return redisUtil.checkAILimit(userId, 20);
    }
    
    @Override
    public int getAITodayCallCount(Long userId) {
        return redisUtil.getAITodayCount(userId);
    }
    
    // 检查敏感词（从Redis缓存加载）
    private boolean containsSensitiveWords(String content) {
        Set<String> sensitiveWords = loadSensitiveWords();
        
        if (sensitiveWords == null || sensitiveWords.isEmpty()) {
            return false;
        }
        
        for (String word : sensitiveWords) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    // 从Redis加载敏感词（未命中则从数据库读取）
    @SuppressWarnings("unchecked")
    private Set<String> loadSensitiveWords() {
        try {
            // 先从 Redis 缓存读取
            Object cachedWords = redisUtil.get(SENSITIVE_WORDS_CACHE_KEY);
            
            if (cachedWords != null) {
                // Redis 缓存命中，解析为 Set
                if (cachedWords instanceof Set) {
                    return (Set<String>) cachedWords;
                } else if (cachedWords instanceof List) {
                    return new HashSet<>((List<String>) cachedWords);
                }
            }
            
            // Redis 缓存未命中，从数据库读取
            List<SensitiveWord> wordList = sensitiveWordMapper.selectList(null);
            Set<String> words = new HashSet<>();
            
            if (wordList != null && !wordList.isEmpty()) {
                for (SensitiveWord word : wordList) {
                    words.add(word.getWord());
                }
            }
            
            // 写入 Redis 缓存（24小时过期）
            redisUtil.set(SENSITIVE_WORDS_CACHE_KEY, new ArrayList<>(words), 24 * 60 * 60);
            
            return words;
        } catch (Exception e) {
            log.error("加载敏感词失败", e);
            return new HashSet<>();
        }
    }
    
    /**
     * 清理敏感词缓存（供管理接口调用）
     */
    public void clearSensitiveWordCache() {
        redisUtil.delete(SENSITIVE_WORDS_CACHE_KEY);
        log.info("敏感词Redis缓存已清理");
    }
}
