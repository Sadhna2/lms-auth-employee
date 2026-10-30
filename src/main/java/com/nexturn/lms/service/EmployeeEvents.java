package com.nexturn.lms.service;

import com.nexturn.lms.entity.EmployeeEvent;
import com.nexturn.lms.repository.EmployeeEventRepository;
import org.springframework.stereotype.Service;

/** Records durable change events; the scheduled relay publishes committed records to SNS and SQS. */
@Service
public class EmployeeEvents {
    private final EmployeeEventRepository events;
    public EmployeeEvents(EmployeeEventRepository events) { this.events=events; }
    public void record(Integer employeeId,String operation) { events.save(new EmployeeEvent(employeeId,operation)); }
}
