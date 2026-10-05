package com.library.ledger.controller;

import com.library.ledger.dto.BookForm;
import com.library.ledger.entity.Book;
import com.library.ledger.enums.BookStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.mapper.BookMapper;
import com.library.ledger.service.BookService;
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
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final BookMapper bookMapper;

    @GetMapping
    public String index(Model model,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) BookStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
        Page<Book> bookPage = bookService.search(keyword, categoryId, status,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("bookPage", bookPage);
        model.addAttribute("categories", categoryService.findActiveCategories());
        model.addAttribute("statuses", BookStatus.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStatus", status);
        return "books/index";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("bookForm", new BookForm());
        model.addAttribute("categories", categoryService.findActiveCategories());
        model.addAttribute("statuses", BookStatus.values());
        return "books/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("bookForm") BookForm form,
                         BindingResult bindingResult,
                         RedirectAttributes flash,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findActiveCategories());
            model.addAttribute("statuses", BookStatus.values());
            return "books/create";
        }
        try {
            Book saved = bookService.create(form);
            flash.addFlashAttribute("successMsg", "Đã thêm sách '" + saved.getTitle() + "' thành công!");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/books/create";
        }
        return "redirect:/books";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Book book = bookService.findById(id);
        BookForm form = bookMapper.toForm(book);
        model.addAttribute("bookForm", form);
        model.addAttribute("bookId", id);
        model.addAttribute("book", book);
        model.addAttribute("categories", categoryService.findActiveCategories());
        model.addAttribute("statuses", BookStatus.values());
        return "books/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("bookForm") BookForm form,
                         BindingResult bindingResult,
                         RedirectAttributes flash,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("bookId", id);
            try {
                model.addAttribute("book", bookService.findById(id));
            } catch (Exception ignored) {}
            model.addAttribute("categories", categoryService.findActiveCategories());
            model.addAttribute("statuses", BookStatus.values());
            return "books/edit";
        }
        try {
            Book updated = bookService.update(id, form);
            flash.addFlashAttribute("successMsg", "Đã cập nhật sách '" + updated.getTitle() + "'!");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/books";
    }

    @PostMapping("/{id}/delete")
    public String softDelete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            bookService.softDelete(id);
            flash.addFlashAttribute("successMsg", "Đã xóa sách khỏi hệ thống.");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/books";
    }
}
