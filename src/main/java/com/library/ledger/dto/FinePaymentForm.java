package com.library.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class FinePaymentForm {

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "1", message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal amount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime paymentDate = LocalDateTime.now();

    @NotBlank(message = "Hình thức thanh toán không được để trống")
    private String method = "CASH";

    private String note;
}
