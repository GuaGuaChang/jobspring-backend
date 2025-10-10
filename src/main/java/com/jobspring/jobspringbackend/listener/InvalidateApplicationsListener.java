package com.jobspring.jobspringbackend.listener;

import com.jobspring.jobspringbackend.events.JobDeactivatedEvent;
import com.jobspring.jobspringbackend.repository.ApplicationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class InvalidateApplicationsListener {

    private final ApplicationRepository appRepo;

    public InvalidateApplicationsListener(ApplicationRepository appRepo) {
        this.appRepo = appRepo;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(JobDeactivatedEvent e) {
        appRepo.updateStatusByJobId(e.jobId(), 4);
    }
}

