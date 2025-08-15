package com.moneyflow.moneyflow.repository;

import org.springframework.data.domain.Page;
import com.moneyflow.moneyflow.entity.Transaction;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByTransactionDateDesc(User user);
    List<Transaction> findByUserOrderByTransactionDateDesc(User user, Pageable pageable);
    Page<Transaction> findByUser(User user, Pageable pageable);
    List<Transaction> findByUserAndType(User user, TransactionType type);
    List<Transaction> findByUserAndTypeAndTransactionDateBetween(User user, TransactionType type, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT t.category.name AS categoryName, SUM(t.amount) AS totalAmount " +
            "FROM Transaction t " +
            "WHERE t.user = :user AND t.type = 'EXPENSE' AND t.transactionDate >= :startDate " +
            "GROUP BY t.category.name")
    List<Map<String, Object>> findExpensesByCategory(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
}