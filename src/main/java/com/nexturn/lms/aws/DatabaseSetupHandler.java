package com.nexturn.lms.aws;

import com.amazonaws.services.lambda.runtime.*;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit, manually invoked first-deployment schema setup; not exposed through API Gateway. */
public class DatabaseSetupHandler implements RequestHandler<Map<String,Object>,Map<String,Object>> {
    @Override public Map<String,Object> handleRequest(Map<String,Object> request,Context context) {
        if(!Boolean.TRUE.equals(request.get("initialize"))) throw new IllegalArgumentException("Explicit initialize=true is required");
        var source=DatabaseConnection.dataSource();
        new ResourceDatabasePopulator(new ClassPathResource("database/schema.sql")).execute(source);
        var jdbc=new JdbcTemplate(source);
        String username=DatabaseConnection.required("BOOTSTRAP_ADMIN_USERNAME");
        String password=DatabaseConnection.required("BOOTSTRAP_ADMIN_PASSWORD");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM login",Long.class)==0L)
            jdbc.update("INSERT INTO login(username,password,role) VALUES (?,?,?)",username,
                    new BCryptPasswordEncoder(10).encode(password),"ADMIN");
        return Map.of("initialized",true,"database","lms_auth_employee");
    }
}
