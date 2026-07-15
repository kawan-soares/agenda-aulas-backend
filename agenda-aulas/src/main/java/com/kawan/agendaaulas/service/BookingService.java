package com.kawan.agendaaulas.service;

import com.kawan.agendaaulas.dto.BookingRequest;
import com.kawan.agendaaulas.dto.BookingResponse;
import com.kawan.agendaaulas.exception.BusinessRuleException;
import com.kawan.agendaaulas.exception.ResourceNotFoundException;
import com.kawan.agendaaulas.model.Availability;
import com.kawan.agendaaulas.model.Booking;
import com.kawan.agendaaulas.model.BookingStatus;
import com.kawan.agendaaulas.model.SlotStatus;
import com.kawan.agendaaulas.model.User;
import com.kawan.agendaaulas.repository.AvailabilityRepository;
import com.kawan.agendaaulas.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;
    private final EmailService emailService;

    @Transactional
    public BookingResponse create(User student, BookingRequest request) {
        Availability slot = availabilityRepository.findById(request.availabilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Horário não encontrado"));

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new BusinessRuleException("Esse horário já foi reservado por outra pessoa");
        }

        slot.setStatus(SlotStatus.BOOKED);
        availabilityRepository.save(slot);

        Booking booking = Booking.builder()
                .availability(slot)
                .student(student)
                .status(BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        Booking saved = bookingRepository.save(booking);

        emailService.sendBookingConfirmation(student, slot);

        return BookingResponse.from(saved);
    }

    public List<BookingResponse> listMine(Long studentId) {
        return bookingRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    public List<BookingResponse> listForTeacher(Long teacherId) {
        return bookingRepository.findByAvailability_Teacher_IdOrderByAvailability_StartTimeAsc(teacherId)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional
    public void cancel(User requester, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

        boolean isOwner = booking.getStudent().getId().equals(requester.getId());
        boolean isTeacherOfSlot = booking.getAvailability().getTeacher().getId().equals(requester.getId());

        if (!isOwner && !isTeacherOfSlot) {
            throw new BusinessRuleException("Você não tem permissão pra cancelar essa reserva");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        Availability slot = booking.getAvailability();
        slot.setStatus(SlotStatus.AVAILABLE);
        availabilityRepository.save(slot);

        emailService.sendCancellationNotice(booking.getStudent(), slot);
    }
}