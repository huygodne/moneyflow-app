package com.moneyflow.moneyflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter // Sử dụng @Getter thay cho @Data
@Setter // Sử dụng @Setter thay cho @Data
@NoArgsConstructor
@AllArgsConstructor
// Rất quan trọng: Loại trừ TẤT CẢ các mối quan hệ (Set hoặc ManyToOne)
// để tránh LazyInitializationException và ConcurrentModificationException trong toString()
@ToString(exclude = {"accounts", "categories", "transactions"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    // --- THÊM DÒNG NÀY: Trường cho tên đầy đủ (tên hiển thị) ---
    @Column(name = "full_name") // Tên cột trong cơ sở dữ liệu
    private String fullName; // Tên đầy đủ/tên hiển thị của người dùng
    // -----------------------------------------------------------

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Mối quan hệ Một-Nhiều: Một User có nhiều Accounts
    // Rất quan trọng: Thêm FetchType.LAZY để các collections này không được tải ngay lập tức
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Account> accounts = new HashSet<>();

    // Mối quan hệ Một-Nhiều: Một User có nhiều Categories
    // Rất quan trọng: Thêm FetchType.LAZY
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Category> categories = new HashSet<>();

    // Mối quan hệ Một-Nhiều: Một User có nhiều Transactions
    // Rất quan trọng: Thêm FetchType.LAZY
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Transaction> transactions = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Phương thức equals() và hashCode() an toàn cho Hibernate ---
    // Rất quan trọng: CHỈ so sánh/tính toán dựa trên ID để tránh các vấn đề với Hibernate proxies và Lazy Loading
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Kiểm tra đúng kiểu lớp, hoạt động với cả Hibernate proxies
        if (!(o instanceof User)) return false;
        User other = (User) o;
        // Chỉ so sánh dựa trên ID, và đảm bảo ID không null
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        // Chỉ tính toán hashCode dựa trên ID
        // Trả về một giá trị hằng số (ví dụ: 31) nếu ID là null để tránh lỗi khi đối tượng chưa được lưu
        return id != null ? Objects.hash(id) : 31;
    }
}