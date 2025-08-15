package com.moneyflow.moneyflow.service;

import com.moneyflow.moneyflow.entity.Account;
import com.moneyflow.moneyflow.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountService {
    List<Account> findAllAccountsForCurrentUser();
    Optional<Account> getAccountByIdAndUser(Long id, User user);
    Optional<Account> getAccountByNameAndUser(String name, User user);
    List<Account> getAccountsByUser(User user);
    Account saveAccount(Account account);
    void deleteAccount(Long id);
    void updateAccountBalance(Account account, BigDecimal amount, boolean isIncome);
    BigDecimal getTotalBalance(User user);
}