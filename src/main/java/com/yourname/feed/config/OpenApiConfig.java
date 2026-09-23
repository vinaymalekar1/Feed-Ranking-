package com.yourname.feed.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI feedRankingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Feed Ranking API")
                        .description("Naive vs ranked social feed demo: fan-out-on-write / fan-out-on-read, "
                                + "recency-decayed engagement + affinity scoring, in-memory cache.")
                        .version("v1"));
    }
}
