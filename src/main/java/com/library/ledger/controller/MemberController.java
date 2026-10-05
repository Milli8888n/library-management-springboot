package com.library.ledger.controller;

import com.library.ledger.dto.MemberForm;
import com.library.ledger.entity.Member;
import com.library.ledger.enums.MemberStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
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

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public String index(Model model,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) MemberStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
        Page<Member> memberPage = memberService.search(keyword, status,
                PageRequest.of(page, size, Sort.by("fullName").ascending()));
        model.addAttribute("memberPage", memberPage);
        model.addAttribute("statuses", MemberStatus.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("memberForm", new MemberForm());
        return "members/index";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("memberForm") MemberForm form,
                         BindingResult bindingResult,
                         RedirectAttributes flash,
                         Model model) {
        if (bindingResult.hasErrors()) {
            Page<Member> memberPage = memberService.search(null, null, PageRequest.of(0, 10, Sort.by("fullName")));
            model.addAttribute("memberPage", memberPage);
            model.addAttribute("statuses", MemberStatus.values());
            model.addAttribute("showCreateModal", true);
            return "members/index";
        }
        try {
            memberService.create(form);
            flash.addFlashAttribute("successMsg", "Đã thêm thành viên thành công!");
        } catch (BusinessRuleException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/members";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Member member = memberService.findById(id);
        MemberForm form = new MemberForm();
        form.setFullName(member.getFullName());
        form.setEmail(member.getEmail());
        form.setPhone(member.getPhone());
        form.setStatus(member.getStatus());
        model.addAttribute("memberForm", form);
        model.addAttribute("memberId", id);
        model.addAttribute("member", member);
        model.addAttribute("statuses", MemberStatus.values());
        return "members/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("memberForm") MemberForm form,
                         BindingResult bindingResult,
                         RedirectAttributes flash,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("memberId", id);
            try {
                model.addAttribute("member", memberService.findById(id));
            } catch (Exception ignored) {}
            model.addAttribute("statuses", MemberStatus.values());
            return "members/edit";
        }
        try {
            memberService.update(id, form);
            flash.addFlashAttribute("successMsg", "Đã cập nhật thông tin thành viên!");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Member member = memberService.findById(id);
            memberService.toggleStatus(id);
            String action = member.getStatus() == MemberStatus.ACTIVE ? "KHÓA" : "MỞ KHÓA";
            flash.addFlashAttribute("successMsg", "Đã " + action + " tài khoản thành viên.");
        } catch (ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/members";
    }
}
