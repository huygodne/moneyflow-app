package com.moneyflow.moneyflow.service;

import com.moneyflow.moneyflow.entity.Transaction;
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionService {
    List<Transaction> getTransactionsByUser(User user);
    Page<Transaction> getTransactionsByUser(User user, Pageable pageable);
    List<Transaction> getRecentTransactions(User user, int limit);
    Optional<Transaction> getTransactionByIdAndUser(Long transactionId, User user);
    Transaction createTransaction(Transaction transaction, User user);
    Transaction updateTransaction(Long transactionId, Transaction transactionDetails, User user);
    void deleteTransaction(Long transactionId, User user);
    BigDecimal getTotalIncome(User user);
    BigDecimal getTotalExpense(User user);
    Map<LocalDate, BigDecimal> getDailyExpensesLast7Days(User user);
    List<Map<String, Object>> getExpensesByCategory();
}