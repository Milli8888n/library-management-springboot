package com.library.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class FinePolicyForm {

    @NotNull(message = "Phí phạt mỗi ngày không được để trống")
    @DecimalMin(value = "0", message = "Phí phạt mỗi ngày phải lớn hơn hoặc bằng 0")
    private BigDecimal dailyFineAmount;

    @DecimalMin(value = "0", message = "Mức trần tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal maxFineAmount;

    @NotNull(message = "Số ngày ân hạn không được để trống")
    @Min(value = 0, message = "Số ngày ân hạn phải lớn hơn hoặc bằng 0")
    private Integer graceDays = 0;

    @NotNull(message = "Ngày áp dụng không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveFrom = LocalDate.now();
}
