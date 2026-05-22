package com.liangalong.ces.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.liangalong.ces")
public class CESDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CESDemoApplication.class, args);
        System.out.println("""
                
                ╔══════════════════════════════════════════════╗
                ║    WES 仓储执行系统 - 任务调度模拟器已启动    ║
                ║                                             ║
                ║    控制台: http://localhost:8080/             ║
                ║    API状态: http://localhost:8080/api/wes/status ║
                ╚══════════════════════════════════════════════╝
                """);
    }
}
