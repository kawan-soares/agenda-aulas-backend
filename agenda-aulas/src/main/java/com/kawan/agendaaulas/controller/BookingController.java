package com.kawan.agendaaulas.controller;

import com.kawan.agendaaulas.dto.BookingRequest;
import com.kawan.agendaaulas.dto.BookingResponse;
import com.kawan.agendaaulas.model.Role;
import com.kawan.agendaaulas.model.User;
import com.kawan.agendaaulas.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<BookingResponse> create(
            @AuthenticationPrincipal User student,
            @Valid @RequestBody BookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.create(student, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> listMine(@AuthenticationPrincipal User user) {
        if (user.getRole() == Role.TEACHER) {
            return ResponseEntity.ok(bookingService.listForTeacher(user.getId()));
        }
        return ResponseEntity.ok(bookingService.listMine(user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal User user, @PathVariable Long id) {
        bookingService.cancel(user, id);
        return ResponseEntity.noContent().build();
    }
}
