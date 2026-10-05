package com.library.ledger.controller;

import com.library.ledger.dto.CategoryForm;
import com.library.ledger.entity.Category;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.service.CategoryService;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final com.library.ledger.mapper.CategoryMapper categoryMapper;

    @GetMapping
    public String index(Model model,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Boolean active,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
        Page<Category> categoryPage = categoryService.search(keyword, active,
                PageRequest.of(page, size, Sort.by("name").ascending()));
        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("categoryForm", new CategoryForm());
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);
        model.addAttribute("totalCount", categoryService.countTotal());
        model.addAttribute("activeCount", categoryService.countActive());
        model.addAttribute("inactiveCount", categoryService.countInactive());
        return "categories/index";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult bindingResult,
                         RedirectAttributes flash,
                         Model model) {
        if (bindingResult.hasErrors()) {
            Page<Category> categoryPage = categoryService.search(null, null, PageRequest.of(0, 10, Sort.by("name")));
            model.addAttribute("categoryPage", categoryPage);
            model.addAttribute("showCreateModal", true);
            model.addAttribute("totalCount", categoryService.countTotal());
            model.addAttribute("activeCount", categoryService.countActive());
            model.addAttribute("inactiveCount", categoryService.countInactive());
            return "categories/index";
        }
        try {
            categoryService.create(form);
            flash.addFlashAttribute("successMsg", "Thêm thể loại thành công!");
        } catch (BusinessRuleException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryService.findById(id);
        CategoryForm form = categoryMapper.toForm(category);
        model.addAttribute("categoryForm", form);
        model.addAttribute("categoryId", id);
        model.addAttribute("category", category);
        return "categories/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult bindingResult,
                         RedirectAttributes flash,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            try {
                model.addAttribute("category", categoryService.findById(id));
            } catch (Exception ignored) {}
            return "categories/edit";
        }
        try {
            categoryService.update(id, form);
            flash.addFlashAttribute("successMsg", "Cập nhật thể loại thành công!");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String softDelete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            categoryService.softDelete(id);
            flash.addFlashAttribute("successMsg", "Đã ngừng hoạt động thể loại.");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/categories";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes flash) {
        try {
            categoryService.restore(id);
            flash.addFlashAttribute("successMsg", "Đã khôi phục thể loại.");
        } catch (ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/categories";
    }
}
