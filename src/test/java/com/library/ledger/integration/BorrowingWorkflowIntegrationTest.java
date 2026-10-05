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
class BorrowingWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("GET /borrowings → 200 OK và chứa danh sách borrowingPage")
    void getBorrowingsList_returns200() throws Exception {
        mockMvc.perform(get("/borrowings"))
                .andExpect(status().isOk())
                .andExpect(view().name("borrowings/index"))
                .andExpect(model().attributeExists("borrowingPage", "statuses", "members"));
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("GET /borrowings/create → 200 OK và chứa form tạo phiếu mượn")
    void getCreateBorrowingForm_returns200() throws Exception {
        mockMvc.perform(get("/borrowings/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("borrowings/create"))
                .andExpect(model().attributeExists("borrowingForm", "members", "books"));
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("POST /borrowings/create thiếu thông tin → Trả về form create kèm lỗi validation")
    void postCreateBorrowing_missingData_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/borrowings/create")
                        .with(csrf())
                        .param("notes", "Phiếu mượn thử nghiệm thiếu dữ liệu"))
                .andExpect(status().isOk())
                .andExpect(view().name("borrowings/create"))
                .andExpect(model().hasErrors());
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("POST /borrowings/create hợp lệ → Tạo phiếu thành công và chuyển hướng đến chi tiết")
    void postCreateBorrowing_valid_redirectsToDetail() throws Exception {
        mockMvc.perform(post("/borrowings/create")
                        .with(csrf())
                        .param("memberId", "1")
                        .param("items[0].bookId", "1")
                        .param("items[0].quantity", "1")
                        .param("notes", "Kiểm thử tạo phiếu mượn tự động"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMsg"))
                .andExpect(redirectedUrlPattern("/borrowings/*"));
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("GET /borrowings/1 → 200 OK hiển thị chi tiết phiếu mượn hạt giống")
    void getBorrowingDetail_returns200() throws Exception {
        mockMvc.perform(get("/borrowings/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("borrowings/detail"))
                .andExpect(model().attributeExists("borrowing"));
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("GET /borrowings/1/return → 200 OK hiển thị form trả sách")
    void getBorrowingReturnForm_returns200() throws Exception {
        mockMvc.perform(get("/borrowings/1/return"))
                .andExpect(status().isOk())
                .andExpect(view().name("borrowings/return"))
                .andExpect(model().attributeExists("borrowing", "returnForm"));
    }
}
