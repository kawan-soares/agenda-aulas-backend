package com.kawan.agendaaulas.repository;

import com.kawan.agendaaulas.model.Availability;
import com.kawan.agendaaulas.model.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByStatusOrderByStartTimeAsc(SlotStatus status);
    List<Availability> findByTeacherIdOrderByStartTimeAsc(Long teacherId);
}
