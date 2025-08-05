package com.moneyflow.moneyflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size; // Thêm import này
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {

    @NotBlank(message = "Tên người dùng không được để trống")
    // Giả định username không đổi qua form update profile này, nhưng vẫn có validation
    @Size(min = 3, max = 50, message = "Tên người dùng phải từ 3 đến 50 ký tự") // Thêm validation
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự") // Thêm validation
    private String email;

    // --- THÊM DÒNG NÀY: Trường cho tên đầy đủ (tên hiển thị) ---
    // Không cần @NotBlank nếu fullName có thể để trống
    @Size(max = 100, message = "Tên đầy đủ không được vượt quá 100 ký tự") // Validation kích thước
    private String fullName;
    // -----------------------------------------------------------
}