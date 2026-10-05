package com.library.ledger.service;

import com.library.ledger.dto.FinePaymentForm;
import com.library.ledger.dto.FineWaiverForm;
import com.library.ledger.entity.FinePayment;
import com.library.ledger.entity.FineWaiver;

public interface FineService {
    /** UC-22: Ghi nhận thanh toán phí phạt */
    FinePayment recordPayment(Long borrowingId, FinePaymentForm form);

    /** UC-23: Miễn/giảm phí phạt */
    FineWaiver recordWaiver(Long borrowingId, FineWaiverForm form);
}
