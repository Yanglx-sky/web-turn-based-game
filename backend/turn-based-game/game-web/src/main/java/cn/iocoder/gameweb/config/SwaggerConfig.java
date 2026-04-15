package cn.iocoder.gameweb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("回合制游戏 API 文档")
                        .version("1.0")
                        .description("回合制游戏后端接口文档")
                );
    }

    /**
     * 全局添加 Authorization 请求头参数
     */
    @Bean
    public OperationCustomizer globalHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            Parameter authHeader = new Parameter()
                    .name("Authorization")
                    .description("认证Token，直接填入token字符串")
                    .in("header")
                    .required(false)
                    .schema(new StringSchema());
            operation.addParametersItem(authHeader);
            return operation;
        };
    }

    /**
     * 全局添加 Content-Type 请求头参数
     */
    @Bean
    public OperationCustomizer contentTypeHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            Parameter contentTypeHeader = new Parameter()
                    .name("Content-Type")
                    .description("请求内容类型，建议：application/json")
                    .in("header")
                    .required(false)
                    .schema(new StringSchema()._default("application/json"));
            operation.addParametersItem(contentTypeHeader);
            return operation;
        };
    }

}