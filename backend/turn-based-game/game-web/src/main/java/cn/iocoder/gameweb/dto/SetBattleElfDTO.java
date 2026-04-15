package cn.iocoder.gameweb.dto;

import lombok.Data;

@Data
public class SetBattleElfDTO {
    private Long userId;
    private Long elfId;
    private Integer fightOrder;
}