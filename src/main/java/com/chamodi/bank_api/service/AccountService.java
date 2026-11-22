package com.chamodi.bank_api.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chamodi.bank_api.domain.Account;
import com.chamodi.bank_api.domain.Transaction;
import com.chamodi.bank_api.domain.TransactionType;
import com.chamodi.bank_api.exception.AccountNotFoundException;
import com.chamodi.bank_api.exception.DuplicateAccountException;
import com.chamodi.bank_api.exception.InsufficientFundsException;
import com.chamodi.bank_api.repository.AccountRepository;
import com.chamodi.bank_api.repository.TransactionRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Account createAccount(String accountNumber, String ownerName, BigDecimal openingBalance) {
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative");
        }
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new DuplicateAccountException(accountNumber);
        }
        return accountRepository.save(new Account(accountNumber, ownerName, openingBalance));
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public Account deposit(Long id, BigDecimal amount) {
        requirePositive(amount);
        Account account = getAccount(id);
        account.credit(amount);
        transactionRepository.save(new Transaction(account, TransactionType.DEPOSIT, amount));
        return account;
    }

    @Transactional
    public Account withdraw(Long id, BigDecimal amount) {
        requirePositive(amount);
        Account account = getAccount(id);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(id);
        }
        account.debit(amount);
        transactionRepository.save(new Transaction(account, TransactionType.WITHDRAWAL, amount));
        return account;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(Long accountId) {
        getAccount(accountId); // throws 404 if the account doesn't exist
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}