package com.library.ledger.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryAndBookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("POST /categories/create → Thêm thể loại mới với CSRF hợp lệ → Thành công")
    void postCreateCategory_success() throws Exception {
        mockMvc.perform(post("/categories/create")
                        .with(csrf())
                        .param("name", "Khoa học vũ trụ " + System.currentTimeMillis())
                        .param("description", "Sách về thiên văn và thám hiểm không gian"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"))
                .andExpect(flash().attributeExists("successMsg"));
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("POST /categories/create trùng tên → Trả về thông báo lỗi")
    void postCreateCategory_duplicateName_fails() throws Exception {
        mockMvc.perform(post("/categories/create")
                        .with(csrf())
                        .param("name", "Công nghệ thông tin") // Seed category
                        .param("description", "Trùng lặp"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"))
                .andExpect(flash().attributeExists("errorMsg"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /admin/fine-policy → Ban hành chính sách phạt mới thành công")
    void postNewFinePolicy_success() throws Exception {
        mockMvc.perform(post("/admin/fine-policy")
                        .with(csrf())
                        .param("dailyFineAmount", "6000")
                        .param("graceDays", "2")
                        .param("maxFineAmount", "600000")
                        .param("effectiveFrom", "2026-10-15")
                        .param("changeReason", "Điều chỉnh trượt giá năm 2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/fine-policy"))
                .andExpect(flash().attributeExists("successMsg"));
    }
}
