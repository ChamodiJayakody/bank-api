package com.chamodi.bank_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chamodi.bank_api.domain.Account;
import com.chamodi.bank_api.domain.Transaction;
import com.chamodi.bank_api.exception.AccountNotFoundException;
import com.chamodi.bank_api.exception.DuplicateAccountException;
import com.chamodi.bank_api.exception.InsufficientFundsException;
import com.chamodi.bank_api.repository.AccountRepository;
import com.chamodi.bank_api.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    // ---------- Pattern 1: happy path ----------
    @Test
    void createAccount_whenNumberIsNew_savesAccount() {
        // arrange
        when(accountRepository.existsByAccountNumber("ACC-001")).thenReturn(false);
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        Account result = accountService.createAccount("ACC-001", "Chamodi", new BigDecimal("100.00"));

        // assert
        assertThat(result.getAccountNumber()).isEqualTo("ACC-001");
        assertThat(result.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository).save(any(Account.class));
    }

    // ---------- Pattern 2: rule violation ----------
    @Test
    void createAccount_whenNumberExists_throwsDuplicateAccount() {
        // arrange
        when(accountRepository.existsByAccountNumber("ACC-001")).thenReturn(true);

        // act + assert
        assertThatThrownBy(() ->
                accountService.createAccount("ACC-001", "Chamodi", new BigDecimal("100.00")))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessageContaining("ACC-001");

        verify(accountRepository, never()).save(any());
    }

    // ---------- Pattern 3: one test, many inputs ----------
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-100.50"})
    void deposit_whenAmountIsNotPositive_throwsIllegalArgument(String amount) {
        assertThatThrownBy(() -> accountService.deposit(1L, new BigDecimal(amount)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(accountRepository, transactionRepository);
    }

    // ---------- Your tests go below ----------
        // Rule 4: a negative opening balance is rejected before the database is touched
    @Test
    void createAccount_whenOpeningBalanceIsNegative_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                accountService.createAccount("ACC-001", "Chamodi", new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");

        verifyNoInteractions(accountRepository);
    }

    // Rules 2 and 6: a valid deposit increases the balance and records a transaction
    @Test
    void deposit_whenAmountIsValid_increasesBalance() {
        Account account = new Account("ACC-001", "Chamodi", new BigDecimal("100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Account result = accountService.deposit(1L, new BigDecimal("25.50"));

        assertThat(result.getBalance()).isEqualByComparingTo("125.50");
        verify(transactionRepository).save(any(Transaction.class));
    }

    // Rule 6: a valid withdrawal reduces the balance and records a transaction
    @Test
    void withdraw_whenAmountIsValid_reducesBalance() {
        Account account = new Account("ACC-001", "Chamodi", new BigDecimal("100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Account result = accountService.withdraw(1L, new BigDecimal("40.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("60.00");
        verify(transactionRepository).save(any(Transaction.class));
    }

    // Rule 3: no overdraft; the balance is unchanged and nothing is recorded
    @Test
    void withdraw_whenAmountExceedsBalance_throwsInsufficientFunds() {
        Account account = new Account("ACC-001", "Chamodi", new BigDecimal("100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw(1L, new BigDecimal("100.01")))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    // Rule 3 edge case: withdrawing exactly the balance is allowed and leaves zero
    @Test
    void withdraw_whenAmountEqualsBalance_isAllowed() {
        Account account = new Account("ACC-001", "Chamodi", new BigDecimal("100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Account result = accountService.withdraw(1L, new BigDecimal("100.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("0.00");
    }

    // Rule 7: an unknown account gives a clear not-found error
    @Test
    void getAccount_whenIdIsUnknown_throwsAccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(99L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("99");
    }
}