package com.library.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FineWaiverForm {

    @NotNull(message = "Số tiền miễn/giảm không được để trống")
    @DecimalMin(value = "1", message = "Số tiền miễn/giảm phải lớn hơn 0")
    private BigDecimal amount;

    @NotBlank(message = "Lý do miễn/giảm không được để trống")
    private String reason;

    @NotBlank(message = "Người duyệt không được để trống")
    private String approvedBy;
}
