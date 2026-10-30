package com.nexturn.lms.aws;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.core.exception.SdkClientException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Checks that successful delivery marks rows and failures preserve pending outbox events. */
class EmployeeEventRelayTest {
    JdbcTemplate jdbc; SnsClient sns; EmployeeEventRelayHandler handler;
    @BeforeEach void setup() {
        var source=new DriverManagerDataSource("jdbc:h2:mem:relay-"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(source);sns=mock(SnsClient.class);handler=new EmployeeEventRelayHandler(source,sns,"arn:aws:sns:ap-south-1:123456789012:test");
        jdbc.execute("CREATE TABLE employee_event(event_id VARCHAR(36) PRIMARY KEY,employee_id INT,event_type VARCHAR(20),created_at TIMESTAMP,published BOOLEAN)");
        jdbc.update("INSERT INTO employee_event VALUES(?,?,?,CURRENT_TIMESTAMP,false)","54d3fddc-56cc-4e0f-bbc0-76daf2d83634",17,"CREATED");
    }
    @Test void successfulDeliveryMarksRowAndPublishesSafePayload() throws Exception {
        when(sns.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().messageId("accepted").build());
        assertEquals(1,handler.handleRequest(java.util.Map.of(),null).get("published"));
        assertTrue(jdbc.queryForObject("SELECT published FROM employee_event",Boolean.class));
        var capture=org.mockito.ArgumentCaptor.forClass(PublishRequest.class);verify(sns).publish(capture.capture());
        var protocols=new com.fasterxml.jackson.databind.ObjectMapper().readTree(capture.getValue().message());
        var payload=new com.fasterxml.jackson.databind.ObjectMapper().readTree(protocols.get("sqs").asText());
        assertEquals(17,payload.get("employeeId").asInt());
        assertEquals("CREATED",payload.get("eventType").asText());
        assertEquals("json",capture.getValue().messageStructure());
        assertTrue(protocols.get("email").asText().contains("Employee created successfully."));
        assertTrue(capture.getValue().subject().startsWith("LeaveHub"));
        assertFalse(capture.getValue().message().contains("password"));
    }
    @Test void failedDeliveryRollsBackAndRemainsRetryable() {
        when(sns.publish(any(PublishRequest.class))).thenThrow(SdkClientException.create("temporary SNS outage"));
        assertThrows(SdkClientException.class,()->handler.handleRequest(java.util.Map.of(),null));
        assertFalse(jdbc.queryForObject("SELECT published FROM employee_event",Boolean.class));
    }

    @Test void loginWithoutEmployeeProfilePublishesSafeSuccessMessage() throws Exception {
        jdbc.update("UPDATE employee_event SET employee_id=NULL,event_type='LOGIN_SUCCEEDED'");
        when(sns.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().messageId("accepted").build());
        handler.handleRequest(java.util.Map.of(),null);
        var capture=org.mockito.ArgumentCaptor.forClass(PublishRequest.class);verify(sns).publish(capture.capture());
        var protocols=new com.fasterxml.jackson.databind.ObjectMapper().readTree(capture.getValue().message());
        var payload=new com.fasterxml.jackson.databind.ObjectMapper().readTree(protocols.get("default").asText());
        assertTrue(payload.get("employeeId").isNull());
        assertTrue(protocols.get("email").asText().contains("Login successful."));
        assertTrue(capture.getValue().message().contains("Login successful."));
        assertFalse(capture.getValue().message().contains("password"));
    }
}
