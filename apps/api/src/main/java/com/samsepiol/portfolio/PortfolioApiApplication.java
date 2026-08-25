package com.samsepiol.portfolio;

import com.samsepiol.library.http.client.impl.DefaultHttpClient;
import com.samsepiol.library.http.config.HttpConfig;
import com.samsepiol.library.http.config.ThreadPoolConfig;
import com.samsepiol.library.mongo.config.RepositoryConfiguration;
import com.samsepiol.library.mongo.impl.DefaultRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Import({RepositoryConfiguration.class, HttpConfig.class, ThreadPoolConfig.class, DefaultRepository.class, DefaultHttpClient.class})
@EnableScheduling
@EnableConfigurationProperties({
        com.samsepiol.portfolio.configuration.GitHubRefreshProperties.class,
        com.samsepiol.portfolio.configuration.BeszelProperties.class
})
public class PortfolioApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioApiApplication.class, args);
    }
}
