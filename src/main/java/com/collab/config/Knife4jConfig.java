package com.collab.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI(){
        return new OpenAPI()
                .info(new Info()
                                .title("Collab Flow 智能任务协作系统接口文档")
                                .description("Collab Flow API 文档")
                                .version("1.0")
                                .contact(new Contact()
                                                .name("猫树人")
                                                .email("admin@collab.com")
                                )
                )
                .externalDocs(
                        new ExternalDocumentation()
                                .description("项目接口文档")
                );
    }
}
