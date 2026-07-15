package com.kawan.agendaaulas.service;

import com.kawan.agendaaulas.dto.AvailabilityRequest;
import com.kawan.agendaaulas.dto.AvailabilityResponse;
import com.kawan.agendaaulas.exception.BusinessRuleException;
import com.kawan.agendaaulas.exception.ResourceNotFoundException;
import com.kawan.agendaaulas.model.Availability;
import com.kawan.agendaaulas.model.Role;
import com.kawan.agendaaulas.model.SlotStatus;
import com.kawan.agendaaulas.model.User;
import com.kawan.agendaaulas.repository.AvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private AvailabilityRepository availabilityRepository;

    private AvailabilityService availabilityService;

    private User teacher;
    private User anotherTeacher;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(availabilityRepository);

        teacher = User.builder().id(1L).name("Professor Teste").email("prof@teste.com").role(Role.TEACHER).build();
        anotherTeacher = User.builder().id(2L).name("Outro Professor").email("outro@teste.com").role(Role.TEACHER).build();
    }

    @Test
    @DisplayName("Deve criar horário disponível quando o término é depois do início")
    void deveCriarHorarioQuandoTerminoDepoisDoInicio() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        AvailabilityRequest request = new AvailabilityRequest(start, end);

        when(availabilityRepository.save(any(Availability.class))).thenAnswer(invocation -> {
            Availability a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });

        AvailabilityResponse response = availabilityService.create(teacher, request);

        assertThat(response.status()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(response.teacherName()).isEqualTo("Professor Teste");
        assertThat(response.startTime()).isEqualTo(start);
        assertThat(response.endTime()).isEqualTo(end);

        ArgumentCaptor<Availability> captor = ArgumentCaptor.forClass(Availability.class);
        verify(availabilityRepository).save(captor.capture());
        assertThat(captor.getValue().getTeacher()).isEqualTo(teacher);
        assertThat(captor.getValue().getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    @DisplayName("NÃO deve permitir criar horário quando o término não é depois do início")
    void naoDevePermitirHorarioComTerminoAntesOuIgualAoInicio() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.minusMinutes(30);
        AvailabilityRequest request = new AvailabilityRequest(start, end);

        assertThatThrownBy(() -> availabilityService.create(teacher, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("término precisa ser depois do início");

        verify(availabilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar apenas horários com status AVAILABLE, ordenados por início")
    void deveListarApenasHorariosDisponiveis() {
        Availability slot1 = Availability.builder()
                .id(1L).teacher(teacher)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(SlotStatus.AVAILABLE)
                .build();
        Availability slot2 = Availability.builder()
                .id(2L).teacher(teacher)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .status(SlotStatus.AVAILABLE)
                .build();

        when(availabilityRepository.findByStatusOrderByStartTimeAsc(SlotStatus.AVAILABLE))
                .thenReturn(List.of(slot1, slot2));

        List<AvailabilityResponse> result = availabilityService.listAvailable();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AvailabilityResponse::id).containsExactly(1L, 2L);
        verify(availabilityRepository).findByStatusOrderByStartTimeAsc(SlotStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Deve listar horários de um professor específico")
    void deveListarHorariosPorProfessor() {
        Availability slot = Availability.builder()
                .id(5L).teacher(teacher)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(SlotStatus.BOOKED)
                .build();

        when(availabilityRepository.findByTeacherIdOrderByStartTimeAsc(1L))
                .thenReturn(List.of(slot));

        List<AvailabilityResponse> result = availabilityService.listByTeacher(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(SlotStatus.BOOKED);
    }

    @Test
    @DisplayName("Professor dono do horário consegue removê-lo quando ainda está disponível")
    void professorConsegueRemoverProprioHorarioDisponivel() {
        Availability slot = Availability.builder()
                .id(10L).teacher(teacher)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(SlotStatus.AVAILABLE)
                .build();

        when(availabilityRepository.findById(10L)).thenReturn(Optional.of(slot));

        availabilityService.delete(teacher, 10L);

        verify(availabilityRepository).delete(slot);
    }

    @Test
    @DisplayName("NÃO deve permitir que outro professor remova um horário que não é seu")
    void naoDevePermitirRemocaoPorProfessorSemPermissao() {
        Availability slot = Availability.builder()
                .id(10L).teacher(teacher)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(SlotStatus.AVAILABLE)
                .build();

        when(availabilityRepository.findById(10L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> availabilityService.delete(anotherTeacher, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não pertence a você");

        verify(availabilityRepository, never()).delete(any());
    }

    @Test
    @DisplayName("NÃO deve permitir remover um horário que já foi reservado")
    void naoDevePermitirRemocaoDeHorarioJaReservado() {
        Availability slot = Availability.builder()
                .id(10L).teacher(teacher)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(SlotStatus.BOOKED)
                .build();

        when(availabilityRepository.findById(10L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> availabilityService.delete(teacher, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancele a reserva primeiro");

        verify(availabilityRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar erro claro ao tentar remover horário que não existe")
    void deveLancarErroAoRemoverHorarioInexistente() {
        when(availabilityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.delete(teacher, 999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(availabilityRepository, never()).delete(any());
    }
}