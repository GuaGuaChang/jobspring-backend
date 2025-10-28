package com.jobspring.jobspringbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setup() {
        mailService = new MailService(mailSender);
        // 手动设置默认发件人（因为 @Value 不会被 Mockito 注入）
        ReflectionTestUtils.setField(mailService, "from", "1649182810@qq.com");
    }

    @Test
    void sendPlainText_shouldSendSuccessfully() {
        String to = "user@example.com";
        String subject = "Test Mail";
        String text = "Hello, this is a test.";

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        mailService.sendPlainText(to, subject, text);

        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sentMsg = captor.getValue();
        assertEquals(to, sentMsg.getTo()[0]);
        assertEquals(subject, sentMsg.getSubject());
        assertEquals(text, sentMsg.getText());
        assertEquals("1649182810@qq.com", sentMsg.getFrom()); // ✅ 现在不为 null
    }

    @Test
    void sendPlainText_shouldThrowException_whenMailSenderFails() {
        doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(SimpleMailMessage.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                mailService.sendPlainText("user@example.com", "Error Test", "fail")
        );

        assertTrue(ex.getMessage().contains("Failed to send mail"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
