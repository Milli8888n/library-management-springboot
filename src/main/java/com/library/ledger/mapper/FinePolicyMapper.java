package com.library.ledger.mapper;

import com.library.ledger.dto.FinePolicyForm;
import com.library.ledger.entity.FinePolicy;
import org.mapstruct.*;

@Mapper
public interface FinePolicyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "graceDays",
             expression = "java(form.getGraceDays() != null ? form.getGraceDays() : 0)")
    @Mapping(target = "effectiveFrom",
             expression = "java(form.getEffectiveFrom() != null ? form.getEffectiveFrom() : java.time.LocalDate.now())")
    FinePolicy toEntity(FinePolicyForm form);

    FinePolicyForm toForm(FinePolicy policy);
}
