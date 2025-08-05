package com.moneyflow.moneyflow.repository;

import com.moneyflow.moneyflow.entity.Account;
import com.moneyflow.moneyflow.entity.Category;
import com.moneyflow.moneyflow.entity.Transaction;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByTransactionDateDesc(User user);
    List<Transaction> findByUserOrderByTransactionDateDesc(User user, Pageable pageable);
    List<Transaction> findByUserAndTransactionDateBetween(User user, LocalDateTime startDate, LocalDateTime endDate);
    List<Transaction> findByUserAndAccount(User user, Account account);
    List<Transaction> findByUserAndCategory(User user, Category category);
    List<Transaction> findByUserAndType(User user, TransactionType type);
    List<Transaction> findByUserAndTypeAndTransactionDateBetween(User user, TransactionType type, LocalDateTime startDate, LocalDateTime endDate);
}