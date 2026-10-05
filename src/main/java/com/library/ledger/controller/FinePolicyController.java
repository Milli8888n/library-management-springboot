package com.library.ledger.controller;

import com.library.ledger.dto.FinePolicyForm;
import com.library.ledger.entity.FinePolicy;
import com.library.ledger.service.FinePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/fine-policy")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FinePolicyController {

    private final FinePolicyService finePolicyService;

    @GetMapping
    public String index(Model model) {
        FinePolicy activePolicy = finePolicyService.getActivePolicy();
        List<FinePolicy> policies = finePolicyService.getAllPolicies();

        FinePolicyForm form = new FinePolicyForm();
        if (activePolicy != null) {
            form.setDailyFineAmount(activePolicy.getDailyFineAmount());
            form.setMaxFineAmount(activePolicy.getMaxFineAmount());
            form.setGraceDays(activePolicy.getGraceDays());
            form.setEffectiveFrom(activePolicy.getEffectiveFrom());
        }

        model.addAttribute("activePolicy", activePolicy);
        model.addAttribute("policies", policies);
        model.addAttribute("finePolicyForm", form);
        return "admin/fine-policy";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("finePolicyForm") FinePolicyForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePolicy", finePolicyService.getActivePolicy());
            model.addAttribute("policies", finePolicyService.getAllPolicies());
            model.addAttribute("showEditModal", true);
            return "admin/fine-policy";
        }

        try {
            finePolicyService.updatePolicy(form);
            redirectAttributes.addFlashAttribute("successMsg", "Đã cập nhật chính sách phạt thành công.");
        } catch (Exception e) {
            log.error("Error updating fine policy", e);
            redirectAttributes.addFlashAttribute("errorMsg", "Không thể cập nhật chính sách phạt: " + e.getMessage());
        }

        return "redirect:/admin/fine-policy";
    }
}
