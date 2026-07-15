package com.kawan.agendaaulas.repository;

import com.kawan.agendaaulas.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    // busca reservas cujo horário pertence a um professor específico
    List<Booking> findByAvailability_Teacher_IdOrderByAvailability_StartTimeAsc(Long teacherId);
}
