package com.library.ledger.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("requestedUri", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public String handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorMessage", "Không tìm thấy trang yêu cầu: " + request.getRequestURI());
        model.addAttribute("requestedUri", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRuleException(BusinessRuleException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, HttpServletRequest request, Model model) {
        String errorId = java.util.UUID.randomUUID().toString().substring(0, 8);
        log.error("Internal server error [errorId={}]: URI={}", errorId, request.getRequestURI(), ex);
        model.addAttribute("errorId", errorId);
        model.addAttribute("errorMessage", "Đã xảy ra lỗi hệ thống. Vui lòng liên hệ quản trị viên.");
        model.addAttribute("requestedUri", request.getRequestURI());
        return "error/500";
    }
}
