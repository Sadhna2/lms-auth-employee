package com.nexturn.lms;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifies a real API Gateway REST proxy event traverses the Lambda/Spring adapter. */
class StreamLambdaHandlerTest {
    @Test void apiGatewayCsrfRequestReturns200() throws Exception {
        System.setProperty("spring.profiles.active", "test");
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:lambda-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        StreamLambdaHandler handler = new StreamLambdaHandler();
        Context context = mock(Context.class);
        when(context.getLogger()).thenReturn(mock(LambdaLogger.class));
        when(context.getRemainingTimeInMillis()).thenReturn(30000);
        String event = """
            {"resource":"/{proxy+}","path":"/api/csrf","httpMethod":"GET",
             "headers":{"Accept":"application/json","Host":"example.execute-api.ap-south-1.amazonaws.com","X-Forwarded-Proto":"https"},
             "multiValueHeaders":{},"queryStringParameters":null,"pathParameters":{"proxy":"api/csrf"},
             "stageVariables":null,"requestContext":{"requestId":"unit-test","stage":"Prod",
               "resourcePath":"/{proxy+}","httpMethod":"GET","path":"/Prod/api/csrf",
               "identity":{"sourceIp":"127.0.0.1","userAgent":"test"}},"body":null,"isBase64Encoded":false}
            """;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(new ByteArrayInputStream(event.getBytes(StandardCharsets.UTF_8)),output,context);
        var response = new ObjectMapper().readTree(output.toByteArray());
        assertEquals(200,response.get("statusCode").asInt(),response.toPrettyString());
        var body = new ObjectMapper().readTree(response.get("body").asText());
        assertFalse(body.get("token").asText().isBlank());
        assertEquals("X-XSRF-TOKEN",body.get("headerName").asText());
    }
    @AfterEach void clearProperties() {
        System.clearProperty("spring.profiles.active"); System.clearProperty("spring.datasource.url");
    }
}
