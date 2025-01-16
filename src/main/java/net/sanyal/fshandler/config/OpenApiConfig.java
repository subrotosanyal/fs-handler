package net.sanyal.fshandler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fsHandlerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("File System Handler API")
                        .description("A versatile API for handling file system operations across different storage backends " +
                                "including local file system and S3. Supports operations like creating, reading, writing, " +
                                "moving, and deleting files and directories.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("File System Handler Team")
                                .email("support@fshandler.com")
                                .url("https://github.com/yourusername/fs-handler"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server()
                        .url("/")
                        .description("Default Server URL"));
    }
}
