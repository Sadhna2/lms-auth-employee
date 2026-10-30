package com.nexturn.lms.aws;

import com.amazonaws.services.lambda.runtime.*;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/** Publishes committed outbox rows to SNS; SQS subscribers receive at-least-once events. */
public class EmployeeEventRelayHandler implements RequestHandler<Map<String,Object>,Map<String,Object>> {
    private final javax.sql.DataSource source;
    private final SnsClient sns;
    private final String topicArn;
    private final ObjectMapper jsonMapper=new ObjectMapper();
    public EmployeeEventRelayHandler() {
        this(DatabaseConnection.dataSource(),SnsClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build(),
                DatabaseConnection.required("EMPLOYEE_EVENTS_TOPIC_ARN"));
    }
    /** Injected dependencies allow testing delivery/rollback without contacting AWS. */
    public EmployeeEventRelayHandler(javax.sql.DataSource source,SnsClient sns,String topicArn) {
        this.source=source;this.sns=sns;this.topicArn=topicArn;
    }
    @Override public Map<String,Object> handleRequest(Map<String,Object> event,Context context) {
        var jdbc=new JdbcTemplate(source);
        var transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        Integer sent=transaction.execute(status -> {
            var rows=jdbc.queryForList("SELECT event_id,employee_id,event_type,created_at FROM employee_event WHERE published=false ORDER BY created_at LIMIT 20 FOR UPDATE SKIP LOCKED");
            for(var row:rows) {
                // Values originate only from server-generated UUIDs, integer IDs and fixed operation names.
                String message=switch(row.get("event_type").toString()) {
                    case "LOGIN_SUCCEEDED" -> "Login successful.";
                    case "ACCOUNT_CREATED" -> "Registration successful. Login account created.";
                    case "CREATED" -> "Employee created successfully.";
                    case "UPDATED" -> "Employee updated successfully.";
                    case "DELETED" -> "Employee deleted successfully.";
                    default -> "Employee account event recorded.";
                };
                String json=String.format("{\"eventId\":\"%s\",\"employeeId\":%s,\"eventType\":\"%s\",\"message\":\"%s\"}",
                        row.get("event_id"),row.get("employee_id"),row.get("event_type"),message);
                String emailBody="Hello,\n\n"+message+"\n\nEmployee ID: "+
                        (row.get("employee_id")==null?"No linked employee profile":row.get("employee_id"))+
                        "\nEvent: "+row.get("event_type")+"\nEvent ID: "+row.get("event_id")+
                        "\n\nLeaveHub - Login and Employee Management";
                // SQS gets structured JSON; confirmed email subscribers get a readable project message.
                String envelope=jsonMapper.valueToTree(Map.of("default",json,"sqs",json,"email",emailBody)).toString();
                sns.publish(PublishRequest.builder().topicArn(topicArn).subject("LeaveHub - "+message)
                        .messageStructure("json").message(envelope).build());
                jdbc.update("UPDATE employee_event SET published=true WHERE event_id=?",row.get("event_id"));
            }
            return rows.size();
        });
        // Failures propagate to Lambda/EventBridge and leave rows pending; consumers deduplicate by eventId.
        return Map.of("published",sent);
    }
}
