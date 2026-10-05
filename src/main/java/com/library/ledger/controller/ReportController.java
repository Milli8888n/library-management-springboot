package com.library.ledger.controller;

import com.library.ledger.dto.MemberStatDto;
import com.library.ledger.dto.TopBookDto;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.enums.MemberStatus;
import com.library.ledger.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overdue")
    public String overdue(Model model) {
        List<Borrowing> overdueBorrowings = reportService.getOverdueBorrowings();
        // Compute aggregates server-side — Java stream syntax is not valid in Thymeleaf
        int totalOverdueBooks = overdueBorrowings.stream()
                .flatMap(b -> b.getDetails().stream())
                .mapToInt(d -> d.getRemainingQuantity())
                .sum();
        long overdueMembers = overdueBorrowings.stream()
                .map(b -> b.getMember() != null ? b.getMember().getId() : null)
                .distinct()
                .count();
        model.addAttribute("overdueBorrowings", overdueBorrowings);
        model.addAttribute("totalOverdueBooks", totalOverdueBooks);
        model.addAttribute("overdueMembers", overdueMembers);
        return "reports/overdue";
    }

    @GetMapping("/top-borrowed")
    public String topBorrowed(Model model) {
        List<TopBookDto> topBooks = reportService.getTop5BorrowedBooks();
        long totalBorrowed = topBooks.stream().mapToLong(TopBookDto::getTotalBorrowed).sum();
        model.addAttribute("topBooks", topBooks);
        model.addAttribute("totalBorrowed", totalBorrowed);
        return "reports/top-borrowed";
    }

    @GetMapping("/members")
    public String memberStats(Model model) {
        List<MemberStatDto> memberStats = reportService.getMemberBorrowingStats();
        long activeMembers = memberStats.stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .count();
        long membersWithActiveBorrowings = memberStats.stream()
                .filter(m -> m.getActiveBorrowings() != null && m.getActiveBorrowings() > 0)
                .count();
        model.addAttribute("memberStats", memberStats);
        model.addAttribute("activeMembers", activeMembers);
        model.addAttribute("membersWithActiveBorrowings", membersWithActiveBorrowings);
        return "reports/members";
    }
}
