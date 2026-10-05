package com.library.ledger.service.impl;

import com.library.ledger.dto.BorrowingForm;
import com.library.ledger.dto.BorrowingItemForm;
import com.library.ledger.dto.ReturnForm;
import com.library.ledger.dto.ReturnItemForm;
import com.library.ledger.entity.Book;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.entity.BorrowingDetail;
import com.library.ledger.entity.Member;
import com.library.ledger.enums.BorrowingStatus;
import com.library.ledger.enums.MemberStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.entity.FinePolicy;
import com.library.ledger.repository.BookRepository;
import com.library.ledger.repository.BorrowingDetailRepository;
import com.library.ledger.repository.BorrowingRepository;
import com.library.ledger.repository.FinePolicyRepository;
import com.library.ledger.repository.MemberRepository;
import com.library.ledger.service.BorrowingService;
import com.library.ledger.service.FineCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BorrowingServiceImpl implements BorrowingService {

    public static final BigDecimal FINE_RATE_PER_DAY = new BigDecimal("5000");

    private final BorrowingRepository borrowingRepository;
    private final BorrowingDetailRepository detailRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final FineCalculationService fineCalculationService;
    private final FinePolicyRepository finePolicyRepository;

    private FinePolicy getActivePolicy() {
        return finePolicyRepository.findByActiveTrue()
                .orElseGet(() -> FinePolicy.builder()
                        .dailyFineAmount(FINE_RATE_PER_DAY)
                        .graceDays(0)
                        .maxFineAmount(null)
                        .active(true)
                        .effectiveFrom(LocalDate.now())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Borrowing> search(BorrowingStatus status, Long memberId, Pageable pageable) {
        return borrowingRepository.searchBorrowings(status, memberId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Borrowing findById(Long id) {
        return borrowingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mượn #" + id));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Borrowing create(BorrowingForm form) {
        Member member = memberRepository.findById(form.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên đã chọn"));

        if (member.getStatus() == MemberStatus.BLOCKED) {
            throw new BusinessRuleException("Thành viên '" + member.getFullName() +
                    "' hiện đang bị KHÓA, không được phép lập phiếu mượn mới.");
        }

        LocalDate borrowDate = form.getBorrowDate() != null ? form.getBorrowDate() : LocalDate.now();
        LocalDate dueDate = form.getDueDate() != null ? form.getDueDate() : borrowDate.plusDays(14);

        if (dueDate.isBefore(borrowDate)) {
            throw new BusinessRuleException("Hạn trả sách không được trước ngày mượn");
        }

        if (form.getItems() == null || form.getItems().isEmpty()) {
            throw new BusinessRuleException("Phiếu mượn phải chứa ít nhất một cuốn sách");
        }

        // Combine duplicates if any
        Map<Long, Integer> bookQuantities = new HashMap<>();
        for (BorrowingItemForm item : form.getItems()) {
            if (item.getBookId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            bookQuantities.merge(item.getBookId(), item.getQuantity(), Integer::sum);
        }

        if (bookQuantities.isEmpty()) {
            throw new BusinessRuleException("Danh sách sách mượn không hợp lệ");
        }

        Borrowing borrowing = Borrowing.builder()
                .member(member)
                .borrowDate(borrowDate)
                .dueDate(dueDate)
                .status(BorrowingStatus.BORROWING)
                .totalFine(BigDecimal.ZERO)
                .build();

        for (Map.Entry<Long, Integer> entry : bookQuantities.entrySet()) {
            Long bookId = entry.getKey();
            Integer qty = entry.getValue();

            // Pessimistic Lock to avoid stock race condition
            Book book = bookRepository.findByIdWithLock(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách với ID: " + bookId));

            if (book.getAvailableQuantity() < qty) {
                throw new BusinessRuleException("Sách '" + book.getTitle() + "' không đủ số lượng tồn kho khả dụng (Yêu cầu: " +
                        qty + ", Hiện còn: " + book.getAvailableQuantity() + ")");
            }

            book.setAvailableQuantity(book.getAvailableQuantity() - qty);
            bookRepository.save(book);

            BorrowingDetail detail = BorrowingDetail.builder()
                    .book(book)
                    .quantity(qty)
                    .returnedQuantity(0)
                    .fineAmount(BigDecimal.ZERO)
                    .build();

            borrowing.addDetail(detail);
        }

        return borrowingRepository.save(borrowing);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Borrowing processReturn(ReturnForm form) {
        Borrowing borrowing = findById(form.getBorrowingId());

        if (borrowing.getStatus() == BorrowingStatus.RETURNED) {
            throw new BusinessRuleException("Phiếu mượn #" + borrowing.getId() + " đã được trả toàn bộ trước đó.");
        }

        LocalDate returnDate = form.getReturnDate() != null ? form.getReturnDate() : LocalDate.now();
        FinePolicy policy = getActivePolicy();

        int totalItemsReturnedInThisAction = 0;

        for (ReturnItemForm item : form.getItems()) {
            int returnQty = item.getReturnQuantity() != null ? item.getReturnQuantity() : 0;
            if (returnQty <= 0) {
                continue;
            }

            BorrowingDetail detail = borrowing.getDetails().stream()
                    .filter(d -> d.getId().equals(item.getDetailId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dòng sách ID: " + item.getDetailId()));

            int remaining = detail.getRemainingQuantity();
            if (returnQty > remaining) {
                throw new BusinessRuleException("Số lượng trả (" + returnQty +
                        ") vượt quá số lượng còn nợ (" + remaining + ") của sách '" + detail.getBook().getTitle() + "'");
            }

            // Restore book stock with lock
            Book book = bookRepository.findByIdWithLock(detail.getBook().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách ID: " + detail.getBook().getId()));
            book.setAvailableQuantity(book.getAvailableQuantity() + returnQty);
            bookRepository.save(book);

            // Calculate incremental fine using active policy
            BigDecimal itemFine = fineCalculationService.calculate(
                    borrowing.getDueDate(), returnDate, returnQty, policy);
            long itemLateDays = fineCalculationService.computeLateDays(
                    borrowing.getDueDate(), returnDate, policy.getGraceDays() != null ? policy.getGraceDays() : 0);

            detail.setReturnedQuantity(detail.getReturnedQuantity() + returnQty);
            detail.setFineAmount(detail.getFineAmount().add(itemFine));
            detail.setLateDays(itemLateDays); // Long field — compatible
            detail.setDailyFineSnapshot(policy.getDailyFineAmount());
            detail.setGraceDaysSnapshot(policy.getGraceDays());
            detailRepository.save(detail);

            totalItemsReturnedInThisAction += returnQty;
        }

        if (totalItemsReturnedInThisAction == 0) {
            throw new BusinessRuleException("Vui lòng nhập số lượng sách trả lớn hơn 0");
        }

        // Recalculate total fine & fine status
        BigDecimal newTotalFine = borrowing.getDetails().stream()
                .map(BorrowingDetail::getFineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        borrowing.setTotalFine(newTotalFine);
        borrowing.setTotalFineAmount(newTotalFine);
        borrowing.recalculateDebt();

        // Check if all books have been returned
        boolean allReturned = borrowing.getDetails().stream().allMatch(BorrowingDetail::isFullyReturned);
        if (allReturned) {
            borrowing.setStatus(BorrowingStatus.RETURNED);
            borrowing.setReturnedDate(returnDate);
        } else if (returnDate.isAfter(borrowing.getDueDate())) {
            borrowing.setStatus(BorrowingStatus.OVERDUE);
        }

        return borrowingRepository.save(borrowing);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Borrowing returnAll(Long borrowingId, LocalDate returnDate) {
        Borrowing borrowing = findById(borrowingId);
        ReturnForm form = new ReturnForm();
        form.setBorrowingId(borrowingId);
        form.setReturnDate(returnDate != null ? returnDate : LocalDate.now());

        for (BorrowingDetail detail : borrowing.getDetails()) {
            if (detail.getRemainingQuantity() > 0) {
                ReturnItemForm item = new ReturnItemForm();
                item.setDetailId(detail.getId());
                item.setBookId(detail.getBook().getId());
                item.setReturnQuantity(detail.getRemainingQuantity());
                form.getItems().add(item);
            }
        }

        return processReturn(form);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateEstimatedFine(Borrowing borrowing, LocalDate asOfDate) {
        BigDecimal currentFine = borrowing.getTotalFineAmount() != null
                ? borrowing.getTotalFineAmount()
                : (borrowing.getTotalFine() != null ? borrowing.getTotalFine() : BigDecimal.ZERO);

        if (borrowing.getStatus() == BorrowingStatus.RETURNED) {
            return currentFine;
        }

        LocalDate checkDate = asOfDate != null ? asOfDate : LocalDate.now();
        if (!checkDate.isAfter(borrowing.getDueDate())) {
            return currentFine;
        }

        FinePolicy policy = getActivePolicy();
        BigDecimal additionalFine = BigDecimal.ZERO;

        for (BorrowingDetail detail : borrowing.getDetails()) {
            int unreturned = detail.getRemainingQuantity();
            if (unreturned > 0) {
                BigDecimal itemFine = fineCalculationService.calculate(
                        borrowing.getDueDate(), checkDate, unreturned, policy);
                additionalFine = additionalFine.add(itemFine);
            }
        }

        return currentFine.add(additionalFine);
    }

    @Override
    public void checkAndUpdateOverdueBorrowings() {
        LocalDate today = LocalDate.now();
        List<Borrowing> pendingOverdue = borrowingRepository.findPendingOverdue(today);
        for (Borrowing b : pendingOverdue) {
            b.setStatus(BorrowingStatus.OVERDUE);
            borrowingRepository.save(b);
        }
    }
}
