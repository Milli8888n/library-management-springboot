package com.library.ledger.mapper;

import com.library.ledger.dto.MemberForm;
import com.library.ledger.entity.Member;
import org.mapstruct.*;

@Mapper
public interface MemberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status",
             expression = "java(form.getStatus() != null ? form.getStatus() : com.library.ledger.enums.MemberStatus.ACTIVE)")
    Member toEntity(MemberForm form);

    MemberForm toForm(Member member);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromForm(MemberForm form, @MappingTarget Member member);

    @BeforeMapping
    default void trimMemberForm(MemberForm form) {
        if (form == null) return;
        if (form.getFullName() != null) form.setFullName(form.getFullName().trim());
        if (form.getPhone() != null) {
            String p = form.getPhone().trim();
            form.setPhone(p.isEmpty() ? null : p);
        }
    }
}
