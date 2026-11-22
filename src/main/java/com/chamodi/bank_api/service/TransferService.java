package com.chamodi.bank_api.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chamodi.bank_api.domain.Account;
import com.chamodi.bank_api.domain.Transaction;
import com.chamodi.bank_api.domain.TransactionType;
import com.chamodi.bank_api.exception.AccountNotFoundException;
import com.chamodi.bank_api.exception.InsufficientFundsException;
import com.chamodi.bank_api.repository.AccountRepository;
import com.chamodi.bank_api.repository.TransactionRepository;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransferResult transfer(Long fromId, Long toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account from = accountRepository.findById(fromId)
                .orElseThrow(() -> new AccountNotFoundException(fromId));
        Account to = accountRepository.findById(toId)
                .orElseThrow(() -> new AccountNotFoundException(toId));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromId);
        }

        from.debit(amount);
        to.credit(amount);

        transactionRepository.save(new Transaction(from, TransactionType.TRANSFER_OUT, amount));
        transactionRepository.save(new Transaction(to, TransactionType.TRANSFER_IN, amount));

        return new TransferResult(from, to, amount);
    }
}