package com.kawan.agendaaulas.dto;

import com.kawan.agendaaulas.model.Availability;
import com.kawan.agendaaulas.model.SlotStatus;

import java.time.LocalDateTime;

public record AvailabilityResponse(
        Long id,
        String teacherName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        SlotStatus status
) {
    public static AvailabilityResponse from(Availability a) {
        return new AvailabilityResponse(
                a.getId(),
                a.getTeacher().getName(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus()
        );
    }
}
