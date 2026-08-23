package com.samsepiol.portfolio;

import com.samsepiol.library.mongo.config.RepositoryConfiguration;
import com.samsepiol.library.http.client.impl.DefaultHttpClient;
import com.samsepiol.library.http.config.HttpConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Import({RepositoryConfiguration.class, HttpConfig.class, DefaultHttpClient.class})
public class PortfolioApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioApiApplication.class, args);
    }
}
