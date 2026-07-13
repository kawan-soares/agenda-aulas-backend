package com.kawan.agendaaulas.dto;

import com.kawan.agendaaulas.model.Booking;
import com.kawan.agendaaulas.model.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String studentName,
        String teacherName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getStudent().getName(),
                b.getAvailability().getTeacher().getName(),
                b.getAvailability().getStartTime(),
                b.getAvailability().getEndTime(),
                b.getStatus()
        );
    }
}
