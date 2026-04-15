package cn.iocoder.gameweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan("cn.iocoder")
@MapperScan({"cn.iocoder.gamemodules.mapper", "cn.iocoder.gameai.mapper"})
@EnableScheduling
public class GameWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameWebApplication.class, args);
    }
}