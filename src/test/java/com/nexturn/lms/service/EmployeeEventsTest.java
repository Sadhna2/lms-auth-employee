package com.nexturn.lms.service;

import com.nexturn.lms.entity.EmployeeEvent;
import com.nexturn.lms.repository.EmployeeEventRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifies event metadata is persisted without copying employee personal information. */
class EmployeeEventsTest {
    @Test void recordsUniquePendingEvent() {
        var repository=mock(EmployeeEventRepository.class);new EmployeeEvents(repository).record(17,"UPDATED");
        var capture=org.mockito.ArgumentCaptor.forClass(EmployeeEvent.class);verify(repository).save(capture.capture());
        assertEquals(17,capture.getValue().getEmployeeId());assertEquals("UPDATED",capture.getValue().getEventType());
        assertNotNull(capture.getValue().getEventId());assertNotNull(capture.getValue().getCreatedAt());assertFalse(capture.getValue().isPublished());
    }
}
