package com.samsepiol.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.samsepiol")
@EnableScheduling
@EnableConfigurationProperties(com.samsepiol.portfolio.configuration.GitHubRefreshProperties.class)
public class PortfolioApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioApiApplication.class, args);
    }
}
