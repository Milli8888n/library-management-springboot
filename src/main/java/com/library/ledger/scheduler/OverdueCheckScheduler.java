package com.library.ledger.scheduler;

import com.library.ledger.service.BorrowingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueCheckScheduler {

    private final BorrowingService borrowingService;

    /**
     * UC-21: Runs every day at 00:00:00 to flag overdue borrowings automatically.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void runOverdueCheck() {
        log.info("[Scheduler] Starting overdue borrowing check...");
        try {
            borrowingService.checkAndUpdateOverdueBorrowings();
            log.info("[Scheduler] Overdue check complete.");
        } catch (Exception e) {
            log.error("[Scheduler] Error during overdue check: {}", e.getMessage(), e);
        }
    }
}
