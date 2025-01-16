package net.sanyal.fshandler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void fsHandlerOpenAPI_ShouldReturnValidConfiguration() {
        OpenAPI openAPI = openApiConfig.fsHandlerOpenAPI();
        
        // Test Info object
        Info info = openAPI.getInfo();
        assertNotNull(info, "Info object should not be null");
        assertEquals("File System Handler API", info.getTitle(), "API title should match");
        assertTrue(info.getDescription().contains("versatile API"), "Description should contain expected text");
        assertEquals("1.0.0", info.getVersion(), "Version should match");

        // Test Contact information
        Contact contact = info.getContact();
        assertNotNull(contact, "Contact information should not be null");
        assertEquals("File System Handler Team", contact.getName(), "Contact name should match");
        assertEquals("support@fshandler.com", contact.getEmail(), "Contact email should match");
        assertTrue(contact.getUrl().contains("github.com"), "Contact URL should be a GitHub URL");

        // Test License information
        License license = info.getLicense();
        assertNotNull(license, "License information should not be null");
        assertEquals("Apache 2.0", license.getName(), "License name should match");
        assertTrue(license.getUrl().contains("apache.org"), "License URL should be from apache.org");

        // Test Server configuration
        assertFalse(openAPI.getServers().isEmpty(), "Server list should not be empty");
        Server server = openAPI.getServers().get(0);
        assertEquals("/", server.getUrl(), "Default server URL should match");
        assertNotNull(server.getDescription(), "Server description should not be null");
    }
}
