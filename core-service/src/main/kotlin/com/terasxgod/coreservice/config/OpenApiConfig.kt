package com.terasxgod.coreservice.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement as AnnotationSecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme as ModelSecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(security = [AnnotationSecurityRequirement(name = "bearerAuth")])
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
class OpenApiConfig {

    private fun bearerScheme(): ModelSecurityScheme = ModelSecurityScheme()
        .type(ModelSecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Crypto AI Predictor API")
                    .description("API для управления кошельком")
                    .version("1.0.0")
            )
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    bearerScheme()
                )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
    }

}



