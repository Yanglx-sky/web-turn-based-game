package cn.iocoder.gamemodules.task;

import cn.iocoder.gamemodules.service.BattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTask {

    @Autowired
    private BattleService battleService;

    /**
     * 每5秒执行一次，处理超时未重连的战斗
     */
    @Scheduled(cron = "*/5 * * * * *")
    public void handleTimeoutBattles() {
        battleService.handleTimeoutBattles();
    }
}