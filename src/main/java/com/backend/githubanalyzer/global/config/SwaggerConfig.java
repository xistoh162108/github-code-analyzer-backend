package com.backend.githubanalyzer.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

        @org.springframework.beans.factory.annotation.Value("${app.backend-url:http://localhost:8080}")
        private String backendUrl;

        @Bean
        public OpenAPI openAPI() {
                String securitySchemeName = "bearerAuth";
                String loginUrl = backendUrl + "/oauth2/authorization/github";

                return new OpenAPI()
                                .info(new Info()
                                                .title("GitHub Analyzer API")
                                                .description("### GitHub Login URL\n" +
                                                                "GitHub 로그인을 위해 아래 URL을 브라우저에 입력하세요:\n" +
                                                                "[" + loginUrl + "](" + loginUrl + ")\n\n"
                                                                +
                                                                "로그인 성공 후 화면에 표시되는 `accessToken`을 복사하여 상단의 'Authorize' 버튼에 입력해 주세요.")
                                                .version("v1.0.0"))
                                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                                .components(new Components()
                                                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                                                .name(securitySchemeName)
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")));
        }
}
