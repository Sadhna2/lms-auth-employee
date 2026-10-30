package com.nexturn.lms.repository;

import com.nexturn.lms.entity.EmployeeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores outbox events in the employee transaction so rolled-back changes cannot produce events. */
public interface EmployeeEventRepository extends JpaRepository<EmployeeEvent,String> { }
