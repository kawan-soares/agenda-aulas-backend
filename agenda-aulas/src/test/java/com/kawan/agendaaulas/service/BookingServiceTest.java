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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testa a regra de negócio mais importante do sistema — impedir reserva duplicada —
 * na camada de serviço, isolada de banco de dados real (os repositórios são "dublês"/mocks).
 * <p>
 * Essa é uma camada de teste diferente e complementar à suíte E2E com Cypress
 * (github.com/kawan-soares/agenda-aulas-e2e-tests): aqui validamos a lógica pura,
 * em milissegundos, sem precisar subir aplicação nem banco de dados.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private AvailabilityRepository availabilityRepository;
    @Mock private EmailService emailService;

    private BookingService bookingService;

    private User teacher;
    private User student;
    private Availability availableSlot;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, availabilityRepository, emailService);

        teacher = User.builder().id(1L).name("Professor Teste").email("prof@teste.com").build();
        student = User.builder().id(2L).name("Aluno Teste").email("aluno@teste.com").build();

        availableSlot = Availability.builder()
                .id(10L)
                .teacher(teacher)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(SlotStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("Deve criar reserva quando o horário está disponível")
    void deveCriarReservaQuandoHorarioDisponivel() {
        when(availabilityRepository.findById(10L)).thenReturn(Optional.of(availableSlot));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        BookingResponse response = bookingService.create(student, new BookingRequest(10L));

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(availableSlot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        verify(emailService).sendBookingConfirmation(student, availableSlot);
    }

    @Test
    @DisplayName("NÃO deve permitir reservar um horário que já está reservado — regra central do sistema")
    void naoDevePermitirReservaDuplicada() {
        availableSlot.setStatus(SlotStatus.BOOKED);
        when(availabilityRepository.findById(10L)).thenReturn(Optional.of(availableSlot));

        assertThatThrownBy(() -> bookingService.create(student, new BookingRequest(10L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("já foi reservado");

        verify(bookingRepository, never()).save(any());
        verify(emailService, never()).sendBookingConfirmation(any(), any());
    }

    @Test
    @DisplayName("Deve lançar erro claro quando o horário não existe")
    void deveLancarErroQuandoHorarioNaoExiste() {
        when(availabilityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(student, new BookingRequest(999L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Aluno dono da reserva consegue cancelar, e o horário volta a ficar disponível")
    void alunoConsegueCancelarPropriaReserva() {
        Booking booking = Booking.builder()
                .id(100L)
                .availability(availableSlot)
                .student(student)
                .status(BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        availableSlot.setStatus(SlotStatus.BOOKED);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        bookingService.cancel(student, 100L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(availableSlot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    @DisplayName("NÃO deve permitir que outro aluno cancele a reserva de alguém")
    void naoDevePermitirCancelamentoPorPessoaSemPermissao() {
        User outroAluno = User.builder().id(3L).name("Outro Aluno").email("outro@teste.com").build();
        Booking booking = Booking.builder()
                .id(100L)
                .availability(availableSlot)
                .student(student)
                .status(BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(outroAluno, 100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permissão");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Professor dono do horário também consegue cancelar a reserva de um aluno")
    void professorConsegueCancelarReservaDoProprioHorario() {
        Booking booking = Booking.builder()
                .id(100L)
                .availability(availableSlot)
                .student(student)
                .status(BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        availableSlot.setStatus(SlotStatus.BOOKED);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        bookingService.cancel(teacher, 100L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(emailService).sendCancellationNotice(student, availableSlot);
    }
}