package com.library.ledger.dto;

import com.library.ledger.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberStatDto {

    private Long memberId;
    private String fullName;
    private String email;
    private MemberStatus status;
    private Long totalBorrowings;
    private Long activeBorrowings;
    private BigDecimal totalFine;
}
