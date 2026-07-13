package com.kawan.agendaaulas.service;

import com.kawan.agendaaulas.dto.AvailabilityRequest;
import com.kawan.agendaaulas.dto.AvailabilityResponse;
import com.kawan.agendaaulas.exception.BusinessRuleException;
import com.kawan.agendaaulas.exception.ResourceNotFoundException;
import com.kawan.agendaaulas.model.Availability;
import com.kawan.agendaaulas.model.SlotStatus;
import com.kawan.agendaaulas.model.User;
import com.kawan.agendaaulas.repository.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;

    public AvailabilityResponse create(User teacher, AvailabilityRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessRuleException("O horário de término precisa ser depois do início");
        }

        Availability slot = Availability.builder()
                .teacher(teacher)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(SlotStatus.AVAILABLE)
                .build();

        return AvailabilityResponse.from(availabilityRepository.save(slot));
    }

    public List<AvailabilityResponse> listAvailable() {
        return availabilityRepository.findByStatusOrderByStartTimeAsc(SlotStatus.AVAILABLE)
                .stream()
                .map(AvailabilityResponse::from)
                .toList();
    }

    public List<AvailabilityResponse> listByTeacher(Long teacherId) {
        return availabilityRepository.findByTeacherIdOrderByStartTimeAsc(teacherId)
                .stream()
                .map(AvailabilityResponse::from)
                .toList();
    }

    public void delete(User teacher, Long slotId) {
        Availability slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Horário não encontrado"));

        if (!slot.getTeacher().getId().equals(teacher.getId())) {
            throw new BusinessRuleException("Esse horário não pertence a você");
        }
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new BusinessRuleException("Não é possível remover um horário já reservado — cancele a reserva primeiro");
        }

        availabilityRepository.delete(slot);
    }
}
