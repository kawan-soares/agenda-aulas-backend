package com.kawan.agendaaulas.controller;

import com.kawan.agendaaulas.dto.AvailabilityRequest;
import com.kawan.agendaaulas.dto.AvailabilityResponse;
import com.kawan.agendaaulas.model.User;
import com.kawan.agendaaulas.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AvailabilityResponse> create(
            @AuthenticationPrincipal User teacher,
            @Valid @RequestBody AvailabilityRequest request
    ) {
        return ResponseEntity.ok(availabilityService.create(teacher, request));
    }

    @GetMapping
    public ResponseEntity<List<AvailabilityResponse>> listAvailable() {
        return ResponseEntity.ok(availabilityService.listAvailable());
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<AvailabilityResponse>> listByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(availabilityService.listByTeacher(teacherId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User teacher, @PathVariable Long id) {
        availabilityService.delete(teacher, id);
        return ResponseEntity.noContent().build();
    }
}
