package cn.iocoder.gameai.service.impl;

import cn.iocoder.gameai.advisor.ChatMemoryAdvisor;
import cn.iocoder.gameai.advisor.LoggingAdvisor;
import cn.iocoder.gameai.entity.ChatMessage;
import cn.iocoder.gameai.entity.SensitiveWord;
import cn.iocoder.gameai.mapper.SensitiveWordMapper;
import cn.iocoder.gameai.service.AIService;
import cn.iocoder.gameai.service.SessionService;
import cn.iocoder.gamecommon.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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
    private ChatClient chatClient;
    
    @Autowired
    private LoggingAdvisor loggingAdvisor;
    
    @Autowired
    private ChatMemoryAdvisor chatMemoryAdvisor;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private RedisUtil redisUtil;
    
    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;
    
    @Value("${spring.ai.dashscope.chat.temperature:0.7}")
    private double temperature;
    
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
        try {
            // 解析精灵和怪物信息
            String[] monsterParts = monsterInfo.split(",");
            
            if (monsterParts.length < 2) {
                return "请提供完整的怪物信息。";
            }
            
            String monsterName = monsterParts[0];
            String monsterElementType = monsterParts[1];
            String monsterSkills = monsterParts.length > 2 ? monsterParts[2] : "未知";
            
            // 解析精灵信息，处理多个精灵的情况
            String[] elfLines = elfInfo.split(";\n");
            if (elfLines.length == 0) {
                return "请提供完整的精灵信息。";
            }
            
            // 构建精灵详细信息
            StringBuilder elfDetails = new StringBuilder();
            for (int i = 0; i < elfLines.length; i++) {
                if (elfLines[i].trim().isEmpty()) {
                    continue;
                }
                
                String[] elfParts = elfLines[i].split(",");
                if (elfParts.length < 2) {
                    continue;
                }
                
                String elfName = elfParts[0];
                String elfElementType = elfParts[1];
                String elfSkills = elfParts.length > 2 ? elfParts[2] : "未知";
                
                elfDetails.append(String.format("精灵%d：%s（%s），技能：%s\n", 
                    i + 1, elfName, elfElementType, elfSkills));
            }
            
            if (elfDetails.length() == 0) {
                return "请提供完整的精灵信息。";
            }
            
            // 构建AI提示词
            String prompt = String.format(
                "你是一个专业的精灵训练师AI助手，擅长分析战斗局势并制定最优策略。\n\n" +
                "【对手信息】\n" +
                "怪物名称：%s\n" +
                "怪物系别：%s\n" +
                "怪物技能：%s\n\n" +
                "【我的精灵】\n" +
                "%s\n" +
                "【元素克制关系】（重要！请严格按照以下规则分析，不要自行推断）\n" +
                "克制关系（攻击方→被克制方，伤害×2）：\n" +
                "- 火系 → 草系（火克制草）\n" +
                "- 水系 → 火系（水克制火）\n" +
                "- 草系 → 水系（草克制水）\n" +
                "- 草系 → 光系（草克制光）\n\n" +
                "被克制关系（攻击方→克制方，伤害×0.5）：\n" +
                "- 火系 → 水系（火被水克制）\n" +
                "- 水系 → 草系（水被草克制）\n" +
                "- 草系 → 火系（草被火克制）\n" +
                "- 光系 → 草系（光被草克制）\n\n" +
                "【重要：没有克制关系的情况】（伤害×1，正常伤害）：\n" +
                "- 光系与火系：无克制关系（光不克制火，火也不克制光）\n" +
                "- 光系与水系：无克制关系（光不克制水，水也不克制光）\n" +
                "- 火系与火系、水系与水系、草系与草系、光系与光系：同系无克制\n" +
                "- 其他未列出的组合：均无克制关系\n\n" +
                "【三种情况分析】\n" +
                "1. 克制（有优势）：我的精灵系别 → 怪物系别（例如：我的火系 vs 怪物草系）\n" +
                "   → 伤害×2，推荐出战，优先使用克制属性技能\n\n" +
                "2. 被克制（有劣势）：怪物系别 → 我的精灵系别（例如：我的草系 vs 怪物火系）\n" +
                "   → 受到伤害×2，不推荐出战，如果必须出战需注意防御\n\n" +
                "3. 无克制关系（正常）：两者之间没有克制关系（例如火系vs光系、同系等）\n" +
                "   → 伤害正常（×1），可以正常出战\n\n" +
                "【分析步骤】\n" +
                "第一步：判断我的每个精灵与怪物的关系\n" +
                "  - 克制：我的精灵 → 怪物（优势，查看上方的克制关系列表）\n" +
                "  - 被克制：怪物 → 我的精灵（劣势，查看上方的克制关系列表）\n" +
                "  - 无克制：两者之间没有克制关系（正常，包括同系和未列出的组合）\n" +
                "  - 注意：不要自行推断不存在的克制关系！只使用上方列出的克制关系\n\n" +
                "第二步：根据关系推荐策略\n" +
                "  - 优先推荐克制怪物的精灵（最优选择）\n" +
                "  - 其次推荐无克制关系的精灵（正常选择）\n" +
                "  - 避免推荐被怪物克制的精灵（劣势选择，除非别无选择）\n" +
                "  - 明确告知用户是克制、被克制还是无克制关系\n\n" +
                "【输出要求】\n" +
                "1. 推荐最适合出战的精灵（必须基于克制关系分析）\n" +
                "2. 明确说明关系类型（克制/被克制/无克制）\n" +
                "   正确示例：\n" +
                "   - \"照美冥（水系）与光系怪物无克制关系，伤害正常\"\n" +
                "   - \"宇智波佐助（火系）与光系怪物无克制关系，伤害正常，可以出战\"\n" +
                "   - \"宇智波佐助（火系）克制草系怪物，伤害×2，强烈推荐\"\n" +
                "   - \"千手柱间（草系）被火系怪物克制，受到伤害×2，不推荐\"\n" +
                "   - \"千手柱间（草系）克制水系怪物，伤害×2，强烈推荐\"\n" +
                "   错误示例（不要这样写）：\n" +
                "   - \"宇智波佐助（火系）被光系怪物克制\"（错误！火系和光系无克制关系）\n" +
                "   - \"千手柱间（草系）克制火系怪物\"（错误！草系被火系克制，不是克制火系）\n" +
                "3. 给出技能使用建议\n" +
                "4. 提供战斗注意事项（如果处于劣势，提醒注意防御）\n" +
                "5. 语言风格生动有趣，符合游戏氛围\n" +
                "6. 长度控制在100-200字\n" +
                "7. 直接返回策略内容，不要添加任何解释或前缀\n\n" +
                "请生成策略：",
                monsterName,
                monsterElementType,
                monsterSkills,
                elfDetails.toString()
            );
            
            // 调用AI大模型
            String aiResponse = callAI(prompt);
            
            // 如果AI调用失败，使用默认策略
            if (aiResponse.contains("AI处理失败")) {
                return generateDefaultStrategy(elfInfo, monsterInfo);
            }
            
            return aiResponse.trim();
            
        } catch (Exception e) {
            log.error("生成AI策略推荐失败", e);
            return generateDefaultStrategy(elfInfo, monsterInfo);
        }
    }
    
    /**
     * 生成默认策略（AI失败时的备选方案）
     */
    private String generateDefaultStrategy(String elfInfo, String monsterInfo) {
        try {
            String[] monsterParts = monsterInfo.split(",");
            String monsterName = monsterParts[0];
            String monsterElementType = monsterParts[1];
            
            String[] elfLines = elfInfo.split(";\n");
            String bestElfName = "";
            String bestElfElementType = "";
            boolean foundCounter = false;
            
            for (String elfLine : elfLines) {
                if (elfLine.trim().isEmpty()) continue;
                
                String[] elfParts = elfLine.split(",");
                if (elfParts.length < 2) continue;
                
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
                    break;
                }
                
                if (bestElfName.isEmpty()) {
                    bestElfName = elfName;
                    bestElfElementType = elfElementType;
                }
            }
            
            if (bestElfName.isEmpty()) {
                return "请提供完整的精灵信息。";
            }
            
            // 使用默认逻辑生成战斗策略推荐
            if (foundCounter) {
                return "战斗策略推荐：" + bestElfName + "克制" + monsterName + "，用" + bestElfElementType + "技能攻击！";
            } else if (bestElfElementType.equals(monsterElementType)) {
                return "战斗策略推荐：" + bestElfName + "与" + monsterName + "同系，用高伤技能！";
            } else {
                return "战斗策略推荐：用" + bestElfName + "的强力技能攻击" + monsterName + "！";
            }
        } catch (Exception e) {
            return "战斗策略推荐：合理运用精灵技能，注意元素克制关系，祝你战斗顺利！";
        }
    }
    
    // 调用Spring AI的方法
    private String callAI(String prompt) {
        try {
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .advisors(loggingAdvisor)
                    .call()
                    .chatResponse();
            
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getText();
            }
            
            return "AI处理失败，请重试。";
        } catch (Exception e) {
            log.error("调用AI失败", e);
            return "AI处理失败，使用默认回复。";
        }
    }
    
    /**
     * 真正的SSE流式调用Spring AI（边接收边推送）
     * @param prompt 提示词
     * @param writer PrintWriter用于输出SSE数据到前端
     */
    private void callAIStream(String prompt, PrintWriter writer) {
        try {
            Flux<String> flux = chatClient.prompt()
                    .user(prompt)
                    .advisors(loggingAdvisor)
                    .stream()
                    .content();
            
            flux.subscribe(
                content -> {
                    if (content != null && !content.isEmpty()) {
                        writer.write("data: " + content + "\n\n");
                        writer.flush();
                    }
                },
                error -> {
                    log.error("流式调用AI失败", error);
                    writer.write("data: AI处理失败，请重试。\n\n");
                    writer.flush();
                },
                () -> {
                    log.info("[SSE流式] 调用完成");
                }
            );

        } catch (Exception e) {
            log.error("流式调用AI失败", e);
            writer.write("data: AI处理失败，请重试。\n\n");
            writer.flush();
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
            "请严格按照以下JSON格式返回，不要返回其他内容，不要添加任何解释或代码标记：\n" +
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
            return "输入内容过长，请控制500字以内。";
        }
            
        // 记录用户输入到数据库
        sessionService.addMessage(sessionId, "user", content, "text");
            
        // 构建系统提示词
        String systemPrompt = "你是一个精灵训练师游戏的AI助手。请根据对话历史，直接回答训练师的最后一个问题。\n" +
                "要求：\n" +
                "1. 回答要简洁明确，直接解决问题\n" +
                "2. 如果问题与精灵训练、战斗策略相关，给出具体建议\n" +
                "3. 如果问题超出游戏范围，礼貌地引导回游戏话题\n" +
                "4. 语言风格要友好、鼓励，符合游戏氛围\n" +
                "5. 回答长度控制在100字以内\n" +
                "6. 不要重复对话历史中已经说过的内容\n" +
                "7. 禁止使用任何Emoji表情符号，只使用纯文本";
            
        // 调用AI，使用ChatMemoryAdvisor加载历史消息
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(content)
                .advisors(loggingAdvisor, chatMemoryAdvisor)
                .advisors(advisor -> advisor.param("sessionId", sessionId))
                .call()
                .content();
            
        // 记录AI回复到数据库
        sessionService.addMessage(sessionId, "assistant", response, "text");
            
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
    public void streamChat(String sessionIdStr, String content, HttpServletResponse response, Long userId) {
        // 先设置SSE响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no"); // 禁用nginx缓冲
            
        try {
            // 转换sessionId为Long类型
            Long sessionId = Long.parseLong(sessionIdStr);
                
            // 从数据库获取会话信息（带Redis缓存）
            cn.iocoder.gameai.entity.ChatSession dbSession = sessionService.getSessionById(sessionId, userId);
            if (dbSession == null) {
                try {
                    PrintWriter writer = response.getWriter();
                    writer.write("data: 会话不存在，请先创建会话。\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (Exception e) {
                    log.error("写入响应失败", e);
                }
                return;
            }
                
            // 使用从数据库获取的userId
            Long sessionUserId = dbSession.getUserId();
                
            // 检查用户AI调用次数（使用Redis）
            if (!checkAICallLimit(sessionUserId)) {
                try {
                    PrintWriter writer = response.getWriter();
                    writer.write("data: ⚠️ 今日AI调用次数已达上限，请明日再试。\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (Exception e) {
                    log.error("写入响应失败", e);
                }
                return;
            }
                
            // 内容过滤
            if (containsSensitiveWords(content)) {
                try {
                    PrintWriter writer = response.getWriter();
                    writer.write("data: ⚠️ 输入内容包含敏感词，请修改后重试。\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (Exception e) {
                    log.error("写入响应失败", e);
                }
                return;
            }
                
            // 长度限制
            if (content.length() > 500) {
                try {
                    PrintWriter writer = response.getWriter();
                    writer.write("data: ⚠️ 输入内容过长，请控制500字以内。\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (Exception e) {
                    log.error("写入响应失败", e);
                }
                return;
            }
                
            // 设置SSE响应头
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("X-Accel-Buffering", "no");
            
            try (PrintWriter writer = response.getWriter()) {
                // 记录用户输入到数据库
                sessionService.addMessage(sessionId, "user", content, "text");
                    
                log.info("[真正SSE] 用户 {} 开始流式聊天, sessionId: {}", userId, sessionId);
                    
                // 构建系统提示词
                String systemPrompt = "你是一个精灵训练师游戏的AI助手。请根据对话历史，直接回答训练师的最后一个问题。\n" +
                        "要求：\n" +
                        "1. 回答要简洁明确，直接解决问题\n" +
                        "2. 如果问题与精灵训练、战斗策略相关，给出具体建议\n" +
                        "3. 如果问题超出游戏范围，礼貌地引导回游戏话题\n" +
                        "4. 语言风格要友好、鼓励，符合游戏氛围\n" +
                        "5. 回答长度控制在100字以内\n" +
                        "6. 不要重复对话历史中已经说过的内容\n" +
                        "7. 禁止使用任何Emoji表情符号，只使用纯文本";
                    
                // 【真正流式】边生成边推送，使用Spring AI的Flux
                StringBuilder fullResponse = new StringBuilder();
                    
                Flux<String> flux = chatClient.prompt()
                        .system(systemPrompt)
                        .user(content)
                        .advisors(loggingAdvisor, chatMemoryAdvisor)
                        .advisors(advisor -> advisor.param("sessionId", sessionId))
                        .stream()
                        .content();
                    
                // 使用阻塞式遍历，确保在请求线程中写入响应
                flux.doOnNext(chunk -> {
                    if (chunk != null && !chunk.isEmpty()) {
                        fullResponse.append(chunk);
                        writer.write("data: " + chunk + "\n\n");
                        writer.flush();
                    }
                })
                .doOnError(error -> {
                    log.error("[真正SSE] 流式聊天失败", error);
                    writer.write("data: ❌ AI处理失败，请重试。\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                })
                .doOnComplete(() -> {
                    // 完成时保存完整响应到数据库
                    sessionService.addMessage(sessionId, "assistant", fullResponse.toString(), "text");
                        
                    // 发送结束信号
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                    log.info("[真正SSE] 流式输出完成");
                })
                .blockLast(); // 阻塞等待流完成，确保响应在请求线程中写入
                    
            } catch (Exception e) {
                log.error("[真正SSE] 流式聊天失败", e);
                try {
                    PrintWriter errorWriter = response.getWriter();
                    errorWriter.write("data: ❌ AI处理失败，请重试。\n\n");
                    errorWriter.write("data: [DONE]\n\n");
                    errorWriter.flush();
                } catch (Exception ex) {
                    log.error("写入错误响应失败", ex);
                }
            }
    
        } catch (NumberFormatException e) {
            try {
                PrintWriter writer = response.getWriter();
                writer.write("data: ❌ 无效的会话ID。\n\n");
                writer.write("data: [DONE]\n\n");
                writer.flush();
            } catch (Exception ex) {
                log.error("写入响应失败", ex);
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
