package com.library.ledger.dto;

import com.library.ledger.enums.BookStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookForm {

    private Long id;

    @NotBlank(message = "ISBN không được để trống")
    @Size(max = 20, message = "ISBN tối đa 20 ký tự")
    private String isbn;

    @NotBlank(message = "Tên sách không được để trống")
    @Size(max = 255, message = "Tên sách tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Tác giả không được để trống")
    @Size(max = 150, message = "Tác giả tối đa 150 ký tự")
    private String author;

    @NotNull(message = "Vui lòng chọn thể loại")
    private Long categoryId;

    @NotNull(message = "Tổng số lượng không được để trống")
    @Min(value = 1, message = "Tổng số lượng tối thiểu là 1")
    private Integer totalQuantity;

    private Integer availableQuantity;

    private BookStatus status = BookStatus.ACTIVE;
}
