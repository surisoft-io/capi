package io.surisoft.capi.lb.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${capi.manager.security.enabled}")
    private boolean capiManagerSecurityEnabled;

    @Autowired
    private BuildProperties buildProperties;

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("manager-endpoint")
                .packagesToScan("io.surisoft.capi.lb.controller")
                .pathsToMatch("/manager/**")
                .build();
    }

    @Bean
    public OpenAPI springShopOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        OpenAPI openAPI = new OpenAPI();
        if(capiManagerSecurityEnabled) {
            openAPI
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                    new Components()
                        .addSecuritySchemes(securitySchemeName,
                            new SecurityScheme()
                                    .name(securitySchemeName)
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                        )
                );
        }
        openAPI.info(new Info().title(buildProperties.getName())
                .description("Management endpoint")
                .version(buildProperties.getVersion())
                .license(new License().name("Apache 2.0")));
        return openAPI;
    }
}