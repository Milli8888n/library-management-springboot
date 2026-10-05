package com.library.ledger.controller;

import com.library.ledger.dto.BorrowingForm;
import com.library.ledger.dto.BorrowingItemForm;
import com.library.ledger.dto.FinePaymentForm;
import com.library.ledger.dto.FineWaiverForm;
import com.library.ledger.dto.ReturnForm;
import com.library.ledger.dto.ReturnItemForm;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.entity.BorrowingDetail;
import com.library.ledger.enums.BorrowingStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.repository.FinePaymentRepository;
import com.library.ledger.repository.FineWaiverRepository;
import com.library.ledger.service.BookService;
import com.library.ledger.service.BorrowingService;
import com.library.ledger.service.FineService;
import com.library.ledger.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/borrowings")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingService borrowingService;
    private final MemberService memberService;
    private final BookService bookService;
    private final FineService fineService;
    private final FinePaymentRepository finePaymentRepository;
    private final FineWaiverRepository fineWaiverRepository;

    @GetMapping
    public String index(Model model,
                        @RequestParam(required = false) BorrowingStatus status,
                        @RequestParam(required = false) Long memberId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
        Page<Borrowing> borrowingPage = borrowingService.search(status, memberId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("borrowingPage", borrowingPage);
        model.addAttribute("statuses", BorrowingStatus.values());
        model.addAttribute("members", memberService.findActiveMembers());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedMemberId", memberId);
        return "borrowings/index";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        BorrowingForm form = new BorrowingForm();
        // Add one empty item row by default
        form.getItems().add(new BorrowingItemForm());
        model.addAttribute("borrowingForm", form);
        model.addAttribute("members", memberService.findActiveMembers());
        model.addAttribute("books", bookService.findAvailableBooks());
        return "borrowings/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("borrowingForm") BorrowingForm form,
                         BindingResult bindingResult,
                         RedirectAttributes flash,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("members", memberService.findActiveMembers());
            model.addAttribute("books", bookService.findAvailableBooks());
            return "borrowings/create";
        }
        try {
            Borrowing saved = borrowingService.create(form);
            flash.addFlashAttribute("successMsg", "Đã tạo phiếu mượn #" + saved.getId() + " thành công!");
            return "redirect:/borrowings/" + saved.getId();
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
            model.addAttribute("members", memberService.findActiveMembers());
            model.addAttribute("books", bookService.findAvailableBooks());
            return "borrowings/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Borrowing borrowing = borrowingService.findById(id);
        model.addAttribute("borrowing", borrowing);
        model.addAttribute("estimatedFine", borrowingService.calculateEstimatedFine(borrowing, LocalDate.now()));
        // Pre-build return form
        ReturnForm returnForm = new ReturnForm();
        returnForm.setBorrowingId(id);
        for (BorrowingDetail detail : borrowing.getDetails()) {
            if (detail.getRemainingQuantity() > 0) {
                ReturnItemForm item = new ReturnItemForm();
                item.setDetailId(detail.getId());
                item.setBookId(detail.getBook().getId());
                item.setBookTitle(detail.getBook().getTitle());
                item.setIsbn(detail.getBook().getIsbn());
                item.setQuantity(detail.getQuantity());
                item.setRemainingQuantity(detail.getRemainingQuantity());
                item.setReturnQuantity(0);
                returnForm.getItems().add(item);
            }
        }
        model.addAttribute("returnForm", returnForm);
        // §12.8 — Fine payment and waiver history
        model.addAttribute("finePayments", finePaymentRepository.findByBorrowingIdOrderByPaymentDateAsc(id));
        model.addAttribute("fineWaivers", fineWaiverRepository.findByBorrowingIdOrderByApprovedDateAsc(id));
        return "borrowings/detail";
    }

    @GetMapping("/{id}/return")
    public String returnForm(@PathVariable Long id, Model model) {
        Borrowing borrowing = borrowingService.findById(id);
        if (borrowing.getStatus() == BorrowingStatus.RETURNED) {
            return "redirect:/borrowings/" + id;
        }
        model.addAttribute("borrowing", borrowing);
        model.addAttribute("estimatedFine", borrowingService.calculateEstimatedFine(borrowing, LocalDate.now()));
        ReturnForm returnForm = new ReturnForm();
        returnForm.setBorrowingId(id);
        for (BorrowingDetail detail : borrowing.getDetails()) {
            if (detail.getRemainingQuantity() > 0) {
                ReturnItemForm item = new ReturnItemForm();
                item.setDetailId(detail.getId());
                item.setBookId(detail.getBook().getId());
                item.setBookTitle(detail.getBook().getTitle());
                item.setIsbn(detail.getBook().getIsbn());
                item.setQuantity(detail.getQuantity());
                item.setRemainingQuantity(detail.getRemainingQuantity());
                item.setReturnQuantity(detail.getRemainingQuantity());
                returnForm.getItems().add(item);
            }
        }
        model.addAttribute("returnForm", returnForm);
        return "borrowings/return";
    }

    @PostMapping("/{id}/return")
    public String processReturn(@PathVariable Long id,
                                @ModelAttribute("returnForm") ReturnForm form,
                                RedirectAttributes flash) {
        form.setBorrowingId(id);
        try {
            Borrowing updated = borrowingService.processReturn(form);
            if (updated.getStatus() == BorrowingStatus.RETURNED) {
                flash.addFlashAttribute("successMsg", "Phiếu mượn #" + id + " đã được TRẢ TOÀN BỘ!");
            } else {
                flash.addFlashAttribute("successMsg", "Đã ghi nhận trả sách cho phiếu mượn #" + id);
            }
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/borrowings/" + id;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC-22: Thanh toán phí phạt
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/fines/pay")
    public String payFineForm(@PathVariable Long id, Model model) {
        Borrowing borrowing = borrowingService.findById(id);
        if (borrowing.getUnpaidFineAmount() == null ||
                borrowing.getUnpaidFineAmount().signum() <= 0) {
            return "redirect:/borrowings/" + id;
        }
        model.addAttribute("borrowing", borrowing);
        model.addAttribute("finePaymentForm", new FinePaymentForm());
        return "borrowings/pay-fine";
    }

    @PostMapping("/{id}/fines/pay")
    public String payFine(@PathVariable Long id,
                          @Valid @ModelAttribute("finePaymentForm") FinePaymentForm form,
                          BindingResult bindingResult,
                          RedirectAttributes flash,
                          Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("borrowing", borrowingService.findById(id));
            return "borrowings/pay-fine";
        }
        try {
            fineService.recordPayment(id, form);
            flash.addFlashAttribute("successMsg", "Đã ghi nhận thanh toán phí phạt thành công!");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/borrowings/" + id;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC-23: Miễn/giảm phí phạt
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/fines/waive")
    public String waiveFineForm(@PathVariable Long id, Model model) {
        Borrowing borrowing = borrowingService.findById(id);
        if (borrowing.getUnpaidFineAmount() == null ||
                borrowing.getUnpaidFineAmount().signum() <= 0) {
            return "redirect:/borrowings/" + id;
        }
        model.addAttribute("borrowing", borrowing);
        model.addAttribute("fineWaiverForm", new FineWaiverForm());
        return "borrowings/waive-fine";
    }

    @PostMapping("/{id}/fines/waive")
    public String waiveFine(@PathVariable Long id,
                            @Valid @ModelAttribute("fineWaiverForm") FineWaiverForm form,
                            BindingResult bindingResult,
                            RedirectAttributes flash,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("borrowing", borrowingService.findById(id));
            return "borrowings/waive-fine";
        }
        try {
            fineService.recordWaiver(id, form);
            flash.addFlashAttribute("successMsg", "Đã ghi nhận miễn/giảm phí phạt thành công!");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/borrowings/" + id;
    }
}
