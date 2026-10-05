package com.library.ledger.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Chưa đăng nhập truy cập /books → Chuyển hướng đến /login")
    void anonymousAccessBooks_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /borrowings → Chuyển hướng đến /login")
    void anonymousAccessBorrowings_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/borrowings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /admin/fine-policy → Chuyển hướng đến /login")
    void anonymousAccessAdmin_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/fine-policy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Đăng nhập hợp lệ với tài khoản admin/admin123 → Xác thực thành công")
    void loginWithValidAdmin_authenticatesSuccessfully() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN", "LIBRARIAN"));
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu → Thất bại và chuyển hướng /login?error=true")
    void loginWithInvalidPassword_fails() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("Thủ thư truy cập các màn hình nghiệp vụ (/books, /borrowings, /reports/overdue) → 200 OK")
    void librarianAccessOperationalPages_returns200() throws Exception {
        mockMvc.perform(get("/books")).andExpect(status().isOk());
        mockMvc.perform(get("/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/members")).andExpect(status().isOk());
        mockMvc.perform(get("/borrowings")).andExpect(status().isOk());
        mockMvc.perform(get("/reports/overdue")).andExpect(status().isOk());
        mockMvc.perform(get("/reports/top-borrowed")).andExpect(status().isOk());
        mockMvc.perform(get("/reports/members")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("Thủ thư truy cập trang cấu hình chính sách phạt admin → Chuyển hướng 403 Forbidden")
    void librarianAccessAdminFinePolicy_forbidden() throws Exception {
        mockMvc.perform(get("/admin/fine-policy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/error/403"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin truy cập trang cấu hình chính sách phạt /admin/fine-policy → 200 OK")
    void adminAccessFinePolicy_returns200() throws Exception {
        mockMvc.perform(get("/admin/fine-policy"))
                .andExpect(status().isOk());
    }
}
