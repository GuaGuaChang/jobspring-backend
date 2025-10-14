package com.jobspring.jobspringbackend.listener;

import com.jobspring.jobspringbackend.events.ApplicationSubmittedEvent;
import com.jobspring.jobspringbackend.repository.CompanyMemberRepository;
import com.jobspring.jobspringbackend.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationSubmittedListener {

    private final CompanyMemberRepository companyMembers;
    private final MailService mail;
    @Value("${app.web.base-url:http://localhost:5173}")
    private String webBaseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ApplicationSubmittedEvent e) {
        List<String> hrEmails = companyMembers.findHrEmailsByCompanyId(e.companyId());
        if (hrEmails.isEmpty()) {
            log.warn("No HR emails for company {}", e.companyId());
            return;
        }

        String subject = "[JobSpring] New application for " + e.jobTitle();
        String body = """
                A new application has been submitted.
                
                Job: %s
                Applicant: %s (%s)
                Application ID: %d
                
                Please log in to review:
                %s
                """.formatted(
                e.jobTitle(),
                e.applicantName(),
                e.applicantEmail(),
                e.applicationId(),
                webBaseUrl
        );
        for (String to : hrEmails) {
            try {
                mail.sendPlainText(to, subject, body);
            } catch (Exception ex) {
                log.error("Mail send failed to {} for application {}", to, e.applicationId(), ex);
            }
        }
    }
}