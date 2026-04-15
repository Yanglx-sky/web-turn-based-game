package cn.iocoder.gamemodules.util;

import cn.iocoder.gamemodules.entity.Skill;
import cn.iocoder.gamemodules.entity.UserElf;
import cn.iocoder.gamemodules.mapper.SkillMapper;
import cn.iocoder.gamemodules.mapper.ElfMapper;
import cn.iocoder.gamemodules.entity.Elf;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BattleUtils {

    // 计算系别克制倍数
    public static double calculateElementMultiplier(int attackerElement, int defenderElement) {
        // 1: 火系, 2: 水系, 3: 草系, 4: 光系
        double[][] multiplierMatrix = {
            {1.0, 0.5, 2.0, 1.0}, // 火系攻击
            {2.0, 1.0, 0.5, 1.0}, // 水系攻击
            {0.5, 2.0, 1.0, 2.0}, // 草系攻击（草系克制光系）
            {1.0, 1.0, 0.5, 1.0}  // 光系攻击（光系被草系克制）
        };
        if (attackerElement >= 1 && attackerElement <= 4 && defenderElement >= 1 && defenderElement <= 4) {
            return multiplierMatrix[attackerElement - 1][defenderElement - 1];
        }
        return 1.0;
    }

    // 计算技能伤害
    public static int calculateSkillDamage(Skill skill, int attackerAttack, int defenderDefense, int attackerElement, int defenderElement) {
        int skillPower = skill.getSkillDamage();
        
        // 计算基础伤害
        int baseDamage = (attackerAttack * skillPower) / (attackerAttack + defenderDefense);
        baseDamage = Math.max(baseDamage, 1);
        
        // 计算系别克制
        double multiplier = calculateElementMultiplier(attackerElement, defenderElement);
        
        return (int) (baseDamage * multiplier);
    }

    // 计算普通攻击伤害
    public static int calculateNormalDamage(int attackerAttack, int defenderDefense) {
        int damage = attackerAttack - defenderDefense / 2;
        return Math.max(damage, 1);
    }

    // 获取精灵系别
    public static int getElfElementType(UserElf elf, ElfMapper elfMapper) {
        Elf elfTemplate = elfMapper.selectById(elf.getElfId());
        return elfTemplate != null ? elfTemplate.getElementType() : 0;
    }

    // 判定先后手
    public static boolean isPlayerFirst(int playerSpeed, int enemySpeed) {
        return playerSpeed >= enemySpeed;
    }

    // 战斗日志管理类
    public static class BattleLogManager {
        private int currentRound = 1;
        private List<Map<String, Object>> roundLogs = new ArrayList<>();
        private List<String> currentRoundLogs = new ArrayList<>();

        // 开始新回合
        public void startNewRound() {
            // 保存当前回合的日志
            if (!currentRoundLogs.isEmpty()) {
                Map<String, Object> roundLog = new HashMap<>();
                roundLog.put("round", currentRound);
                roundLog.put("logs", new ArrayList<>(currentRoundLogs));
                roundLogs.add(roundLog);
                currentRoundLogs.clear();
            }
            currentRound++;
        }

        // 添加日志
        public void addLog(String log) {
            currentRoundLogs.add(log);
        }

        // 获取所有回合日志
        public List<Map<String, Object>> getRoundLogs() {
            // 保存当前回合的日志（如果有）
            if (!currentRoundLogs.isEmpty()) {
                Map<String, Object> roundLog = new HashMap<>();
                roundLog.put("round", currentRound);
                roundLog.put("logs", new ArrayList<>(currentRoundLogs));
                roundLogs.add(roundLog);
            }
            return roundLogs;
        }

        // 获取当前回合数
        public int getCurrentRound() {
            return currentRound;
        }

        // 重置日志管理器
        public void reset() {
            currentRound = 1;
            roundLogs.clear();
            currentRoundLogs.clear();
        }
    }
}