package com.kawan.agendaaulas.service;

import com.kawan.agendaaulas.model.Availability;
import com.kawan.agendaaulas.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@agendaaulas.com}")
    private String from;

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    public void sendWelcomeEmail(User user) {
        send(
                user.getEmail(),
                "Bem-vindo(a) ao Agenda Aulas",
                "Olá, " + user.getName() + "!\n\nSua conta foi criada com sucesso. " +
                        (user.getRole().name().equals("TEACHER")
                                ? "Agora você já pode cadastrar seus horários disponíveis."
                                : "Agora você já pode ver os horários disponíveis e reservar sua aula.")
        );
    }

    public void sendBookingConfirmation(User student, Availability slot) {
        String when = slot.getStartTime().format(FORMAT);
        send(
                student.getEmail(),
                "Aula confirmada — " + when,
                "Olá, " + student.getName() + "!\n\nSua aula com " + slot.getTeacher().getName() +
                        " está confirmada para " + when + ".\n\nQualquer imprevisto, você pode cancelar direto pelo sistema."
        );
        send(
                slot.getTeacher().getEmail(),
                "Nova reserva — " + when,
                "Olá, " + slot.getTeacher().getName() + "!\n\n" + student.getName() +
                        " reservou sua aula de " + when + "."
        );
    }

    public void sendCancellationNotice(User recipient, Availability slot) {
        String when = slot.getStartTime().format(FORMAT);
        send(
                recipient.getEmail(),
                "Reserva cancelada — " + when,
                "Olá, " + recipient.getName() + "!\n\nA aula de " + when + " foi cancelada."
        );
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("[EMAIL SIMULADO] Para: {} | Assunto: {} | {}", to, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail pra {}: {}", to, e.getMessage());
        }
    }
}