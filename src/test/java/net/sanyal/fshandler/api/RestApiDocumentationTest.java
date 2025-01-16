package net.sanyal.fshandler.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class RestApiDocumentationTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void verifyAllEndpointsHaveOperationAnnotation() {
        Map<RequestMappingInfo, HandlerMethod> handlers = handlerMapping.getHandlerMethods();
        
        handlers.forEach((mappingInfo, method) -> {
            if (method.getBeanType().equals(FileSystemController.class)) {
                Method javaMethod = method.getMethod();
                Operation operation = javaMethod.getAnnotation(Operation.class);
                
                assertNotNull(operation, 
                    String.format("Endpoint %s is missing @Operation annotation", 
                        mappingInfo.toString()));
                
                assertFalse(operation.summary().isEmpty(), 
                    String.format("Endpoint %s is missing operation summary", 
                        mappingInfo.toString()));
            }
        });
    }

    @Test
    void verifyAllEndpointsHaveApiResponses() {
        Map<RequestMappingInfo, HandlerMethod> handlers = handlerMapping.getHandlerMethods();
        
        handlers.forEach((mappingInfo, method) -> {
            if (method.getBeanType().equals(FileSystemController.class)) {
                Method javaMethod = method.getMethod();
                ApiResponses responses = javaMethod.getAnnotation(ApiResponses.class);
                
                assertNotNull(responses, 
                    String.format("Endpoint %s is missing @ApiResponses annotation", 
                        mappingInfo.toString()));
                
                assertTrue(responses.value().length > 0, 
                    String.format("Endpoint %s has no response documentation", 
                        mappingInfo.toString()));
                
                // Verify each response has a description
                for (ApiResponse response : responses.value()) {
                    assertFalse(response.description().isEmpty(), 
                        String.format("Response %s for endpoint %s is missing description", 
                            response.responseCode(), mappingInfo.toString()));
                }
            }
        });
    }

    @Test
    void verifyAllParametersAreDocumented() {
        Map<RequestMappingInfo, HandlerMethod> handlers = handlerMapping.getHandlerMethods();
        
        handlers.forEach((mappingInfo, method) -> {
            if (method.getBeanType().equals(FileSystemController.class)) {
                Method javaMethod = method.getMethod();
                java.lang.reflect.Parameter[] parameters = javaMethod.getParameters();
                
                for (java.lang.reflect.Parameter parameter : parameters) {
                    Parameter paramAnnotation = parameter.getAnnotation(Parameter.class);
                    
                    assertNotNull(paramAnnotation, 
                        String.format("Parameter %s in endpoint %s is missing @Parameter annotation", 
                            parameter.getName(), mappingInfo.toString()));
                    
                    assertFalse(paramAnnotation.description().isEmpty(), 
                        String.format("Parameter %s in endpoint %s is missing description", 
                            parameter.getName(), mappingInfo.toString()));
                }
            }
        });
    }

    @Test
    void verifyCommonResponseCodesAreDefined() {
        Map<RequestMappingInfo, HandlerMethod> handlers = handlerMapping.getHandlerMethods();
        
        handlers.forEach((mappingInfo, method) -> {
            if (method.getBeanType().equals(FileSystemController.class)) {
                Method javaMethod = method.getMethod();
                ApiResponses responses = javaMethod.getAnnotation(ApiResponses.class);
                
                if (responses != null) {
                    Set<String> definedCodes = Arrays.stream(responses.value())
                        .map(ApiResponse::responseCode)
                        .collect(Collectors.toSet());

                    // Special cases for different endpoints
                    String methodName = javaMethod.getName();
                    if (methodName.equals("delete")) {
                        assertTrue(definedCodes.containsAll(Set.of("204", "404", "500")),
                            String.format("Delete endpoint %s should have 204, 404, and 500 response codes. Found: %s",
                                mappingInfo.toString(), definedCodes));
                    } else if (methodName.equals("health")) {
                        assertTrue(definedCodes.containsAll(Set.of("200", "503")),
                            String.format("Health endpoint %s should have 200 and 503 response codes. Found: %s",
                                mappingInfo.toString(), definedCodes));
                    } else {
                        // For all other endpoints, verify they have 200, 400/404, and 500
                        assertTrue(
                            definedCodes.contains("200") &&
                            (definedCodes.contains("400") || definedCodes.contains("404")) &&
                            definedCodes.contains("500"),
                            String.format("Endpoint %s should have 200, 400/404, and 500 response codes. Found: %s",
                                mappingInfo.toString(), definedCodes)
                        );
                    }
                }
            }
        });
    }

    @Test
    void verifyConsistentErrorResponseDescriptions() {
        Map<RequestMappingInfo, HandlerMethod> handlers = handlerMapping.getHandlerMethods();
        
        handlers.forEach((mappingInfo, method) -> {
            if (method.getBeanType().equals(FileSystemController.class)) {
                Method javaMethod = method.getMethod();
                ApiResponses responses = javaMethod.getAnnotation(ApiResponses.class);
                
                if (responses != null) {
                    for (ApiResponse response : responses.value()) {
                        if (response.responseCode().equals("400")) {
                            assertTrue(response.description().toLowerCase().contains("invalid") 
                                || response.description().toLowerCase().contains("bad request"),
                                String.format("400 response for endpoint %s should mention 'invalid' or 'bad request'", 
                                    mappingInfo.toString()));
                        }
                        if (response.responseCode().equals("500")) {
                            assertTrue(response.description().toLowerCase().contains("internal") 
                                || response.description().toLowerCase().contains("error"),
                                String.format("500 response for endpoint %s should mention 'internal' or 'error'", 
                                    mappingInfo.toString()));
                        }
                    }
                }
            }
        });
    }
}
