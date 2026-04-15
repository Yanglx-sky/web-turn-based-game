package cn.iocoder.gameai.service.impl;

import cn.iocoder.gameai.config.AIConfig;
import cn.iocoder.gameai.entity.ChatMessage;
import cn.iocoder.gameai.service.AIService;
import cn.iocoder.gameai.service.SessionService;
import cn.iocoder.gamecommon.util.RedisUtil;
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

@Service
public class AIServiceImpl implements AIService {
    
    private final Random random = new Random();
    
    @Autowired
    private AIConfig aiConfig;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private RedisUtil redisUtil;
    
    // 会话管理（内存缓存，提高性能）
    private final Map<Long, SessionInfo> sessions = new ConcurrentHashMap<>();
    
    // 敏感词列表
    private final Set<String> sensitiveWords = new HashSet<>(Arrays.asList(
        "系统信息", "泄露", "漏洞", "攻击", "破解", "管理员", "密码", "token"
    ));
    
    // 会话信息类
    private static class SessionInfo {
        Long userId;
        Long sessionId;
        LocalDateTime createTime;
        List<Conversation> conversations;
        
        SessionInfo(Long userId, Long sessionId) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.createTime = LocalDateTime.now();
            this.conversations = new ArrayList<>();
        }
    }
    
    // 对话信息类
    private static class Conversation {
        String role; // user 或 assistant
        String content;
        LocalDateTime timestamp;
        
        Conversation(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    // 每日AI调用记录
    private static class DailyAICallRecord {
        LocalDate date;
        int callCount;
        
        DailyAICallRecord() {
            this.date = LocalDate.now();
            this.callCount = 0;
        }
    }

    @Override
    public String getAIAnalysis(String content) {
        return "AI分析结果: " + content;
    }

    @Override
    public String getBattleStrategy(String elfInfo, String monsterInfo) {
        return "战斗策略: 攻击怪物弱点";
    }

    @Override
    public Integer getAIAction(int playerHp, int playerMaxHp, int enemyHp, int enemyMaxHp, String skills) {
        try {
            // 解析技能列表
            String[] skillArray = skills.split(";\n");
            if (skillArray.length == 0) {
                return 0;
            }

            // 敌人血量低于30%时，优先使用回血技能
            double enemyHpPercentage = (double) enemyHp / enemyMaxHp;
            if (enemyHpPercentage < 0.3) {
                for (int i = 0; i < skillArray.length; i++) {
                    String skill = skillArray[i];
                    String[] skillParts = skill.split(",");
                    if (skillParts.length >= 3 && "回血".equals(skillParts[2])) {
                        return i;
                    }
                }
            }

            // 玩家血量较低时，优先选择高伤害攻击技能
            double playerHpPercentage = (double) playerHp / playerMaxHp;
            if (playerHpPercentage < 0.5) {
                int bestSkillIndex = 0;
                int maxDamage = 0;
                for (int i = 0; i < skillArray.length; i++) {
                    String skill = skillArray[i];
                    String[] skillParts = skill.split(",");
                    if (skillParts.length >= 3 && "攻击".equals(skillParts[2])) {
                        try {
                            int damage = Integer.parseInt(skillParts[1]);
                            if (damage > maxDamage) {
                                maxDamage = damage;
                                bestSkillIndex = i;
                            }
                        } catch (NumberFormatException e) {
                            // 忽略格式错误的技能
                        }
                    }
                }
                return bestSkillIndex;
            }

            // 无特殊局势时，随机使用普通攻击或技能
            int randomIndex = random.nextInt(skillArray.length);
            return randomIndex;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public String getMonsterTaunt(String monsterName, int playerHp, int playerMaxHp, int monsterHp, int monsterMaxHp, String actionType) {
        // 计算血量百分比
        double playerHpPercentage = (double) playerHp / playerMaxHp;
        double monsterHpPercentage = (double) monsterHp / monsterMaxHp;
        
        // 攻击时的嘲讽
        if ("attack".equals(actionType)) {
            String[] attackTaunts = {
                monsterName + " 狞笑着说：你的精灵不堪一击！",
                monsterName + " 嘲讽道：就这点能耐？",
                monsterName + " 冷笑道：这就是你的全部实力吗？",
                monsterName + " 大笑：太弱了，太弱了！",
                monsterName + " 不屑地说：浪费我的时间！"
            };
            return attackTaunts[random.nextInt(attackTaunts.length)];
        }
        // 被攻击时的嘲讽
        else if ("attacked".equals(actionType)) {
            if (monsterHpPercentage > 0.7) {
                String[] highHpTaunts = {
                    monsterName + " 轻蔑地说：挠痒痒吗？",
                    monsterName + " 大笑：这点伤害对我来说不算什么！",
                    monsterName + " 嘲讽道：就这点攻击力？",
                    monsterName + " 不屑地说：继续努力吧，蝼蚁！"
                };
                return highHpTaunts[random.nextInt(highHpTaunts.length)];
            } else if (monsterHpPercentage > 0.3) {
                String[] midHpTaunts = {
                    monsterName + " 怒道：有点意思！",
                    monsterName + " 咬牙切齿：你会为此付出代价的！",
                    monsterName + " 狞笑着说：看来我要认真了！",
                    monsterName + " 冷声道：不错的攻击，但还不够！"
                };
                return midHpTaunts[random.nextInt(midHpTaunts.length)];
            } else {
                String[] lowHpTaunts = {
                    monsterName + " 怒吼：不可能！我怎么会输？",
                    monsterName + " 惊慌失措：不，这不可能！",
                    monsterName + " 垂死挣扎：我还能战斗！",
                    monsterName + " 哀求：放过我吧！"
                };
                return lowHpTaunts[random.nextInt(lowHpTaunts.length)];
            }
        }
        return monsterName + " 发出了一声怪叫！";
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
            
            // 构建请求体，确保正确转义JSON特殊字符
            String escapedPrompt = prompt
                .replace("\\", "\\\\")  // 先转义反斜杠
                .replace("\"", "\\\"")  // 转义双引号
                .replace("\n", "\\n")    // 转义换行符
                .replace("\r", "\\r")    // 转义回车符
                .replace("\t", "\\t");   // 转义制表符
            
            String requestBody = "{" +
                "\"model\":\"" + aiConfig.getModel() + "\"," +
                "\"messages\":[" +
                "{" +
                "\"role\":\"user\"," +
                "\"content\":\"" + escapedPrompt + "\"" +
                "}]" +
                ",\"temperature\":0.7" +
                "}";
            
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
            
            // 解析响应
            String responseStr = response.toString();
            // 简单解析，实际项目中可以使用JSON解析库
            int contentStart = responseStr.indexOf("\"content\":\"");
            if (contentStart != -1) {
                contentStart += 11;
                int contentEnd = responseStr.indexOf("\"", contentStart);
                if (contentEnd != -1) {
                    return responseStr.substring(contentStart, contentEnd);
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
    public Long createSession(Long userId, String title, String scene) {
        // 创建数据库会话
        Long sessionId = sessionService.createSession(userId, title, scene);
        
        // 创建内存会话信息
        SessionInfo sessionInfo = new SessionInfo(userId, sessionId);
        sessions.put(sessionId, sessionInfo);
        
        return sessionId;
    }
    
    @Override
    public String addConversation(Long sessionId, String content) {
        // 获取或加载会话信息
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            // 从数据库加载会话信息
            cn.iocoder.gameai.entity.ChatSession dbSession = sessionService.getSessionById(sessionId, null);
            if (dbSession == null) {
                return "会话不存在，请先创建会话。";
            }
            session = new SessionInfo(dbSession.getUserId(), sessionId);
            sessions.put(sessionId, session);
        }
        
        // 检查用户AI调用次数
        if (!checkAICallLimit(session.userId)) {
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
        
        // 从数据库加载最近的消息作为上下文
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
        
        // 增加AI调用次数
        incrementAICallCount(session.userId);
        
        return response;
    }
    
    @Override
    public boolean closeSession(Long sessionId, Long userId) {
        // 从数据库删除会话
        boolean success = sessionService.deleteSession(sessionId, userId);
        if (success) {
            // 从内存缓存中移除
            sessions.remove(sessionId);
        }
        return success;
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
        
        // 增加AI调用次数
        incrementAICallCount(userId);
        
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
    public void streamAIAnalysis(String sessionIdStr, String content, HttpServletResponse response) {
        // 先设置SSE响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        
        try {
            // 转换sessionId为Long类型
            Long sessionId = Long.parseLong(sessionIdStr);
            
            // 获取或加载会话信息
            SessionInfo session = sessions.get(sessionId);
            if (session == null) {
                // 从数据库加载会话信息
                cn.iocoder.gameai.entity.ChatSession dbSession = sessionService.getSessionById(sessionId, null);
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
                session = new SessionInfo(dbSession.getUserId(), sessionId);
                sessions.put(sessionId, session);
            }
            
            // 检查用户AI调用次数
            if (!checkAICallLimit(session.userId)) {
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
                
                // 从数据库加载最近的消息作为上下文
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
                String responseContent = callAI(context.toString());
                
                // 记录AI回复到数据库
                sessionService.addMessage(sessionId, "assistant", responseContent, "text");
                
                // 增加AI调用次数
                incrementAICallCount(session.userId);
                
                // 流式输出
                for (char c : responseContent.toCharArray()) {
                    writer.write("data: " + c + "\n\n");
                    writer.flush();
                    Thread.sleep(50); // 控制输出速度
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
    
    // 增加AI调用次数
    private void incrementAICallCount(Long userId) {
        // 单独增加AI调用次数，用于统计
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "ai:limit:" + userId + ":" + date;
        redisUtil.increment(key);
        // 设置过期时间为当天结束
        long secondsUntilMidnight = LocalDate.now().plusDays(1).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) - System.currentTimeMillis() / 1000;
        redisUtil.expire(key, secondsUntilMidnight);
    }
    
    // 检查敏感词
    private boolean containsSensitiveWords(String content) {
        for (String word : sensitiveWords) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }
}