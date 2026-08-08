package com.finadvise.crm.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Schema<?> problemDetailSchema = ModelConverters.getInstance()
                .readAllAsResolvedSchema(ProblemDetail.class).schema;

        Content errorContent = new Content()
                .addMediaType("application/json", new MediaType()
                        .schema(problemDetailSchema));

        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("FinAdvise-Core API")
                        .version("1.0.0")
                        .description("Modern Wealth Management Backend"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addResponses("Unauthorized", new ApiResponse()
                                .description("Full authentication is required to access this resource")
                                .content(errorContent))
                        .addResponses("Forbidden", new ApiResponse()
                                .description("You do not have the necessary permissions (roles) for this operation")
                                .content(errorContent)));
    }

    @Bean
    public OpenApiCustomizer globalResponseCustomizer() {
        return openApi -> {
            Schema<?> problemDetailSchema = ModelConverters.getInstance()
                    .readAllAsResolvedSchema(ProblemDetail.class).schema;

            MediaType problemDetailMediaType = new MediaType().schema(problemDetailSchema);
            Content problemDetailContent = new Content()
                    .addMediaType(
                            org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            problemDetailMediaType
                    );

            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                // Adds ProblemDetail content to all 4xx/5xx responses that don't have any content defined yet
                operation.getResponses().forEach((statusCode, apiResponse) -> {
                    if (statusCode.matches("^[45]\\d{2}$") && apiResponse.getContent() == null) {
                        apiResponse.setContent(problemDetailContent);
                    }
                });

                // Omits public endpoints
                if (operation.getTags() != null && operation.getTags().contains("Authentication")) {
                    return;
                }

                // Appends global 401/403 for secured endpoints
                operation.getResponses().addApiResponse("401", new ApiResponse()
                        .description("Unauthorized - Missing or invalid JWT")
                        .content(problemDetailContent));
                operation.getResponses().addApiResponse("403", new ApiResponse()
                        .description("Forbidden - Insufficient access rights")
                        .content(problemDetailContent));
            }));
        };
    }
}
