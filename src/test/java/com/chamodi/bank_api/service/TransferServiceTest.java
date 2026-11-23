package com.chamodi.bank_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chamodi.bank_api.domain.Account;
import com.chamodi.bank_api.domain.Transaction;
import com.chamodi.bank_api.domain.TransactionType;
import com.chamodi.bank_api.exception.AccountNotFoundException;
import com.chamodi.bank_api.exception.InsufficientFundsException;
import com.chamodi.bank_api.repository.AccountRepository;
import com.chamodi.bank_api.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferService transferService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private Account from;
    private Account to;

    @BeforeEach
    void setUp() {
        from = new Account("ACC-001", "Chamodi", new BigDecimal("100.00"));
        to = new Account("ACC-002", "Nimal", new BigDecimal("50.00"));
    }

    // Rules 5 and 6: money moves from one account to the other, and both sides are recorded
    @Test
    void transfer_whenValid_movesMoneyAndRecordsBothSides() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));

        transferService.transfer(1L, 2L, new BigDecimal("30.00"));

        assertThat(from.getBalance()).isEqualByComparingTo("70.00");
        assertThat(to.getBalance()).isEqualByComparingTo("80.00");

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues())
                .extracting(Transaction::getType)
                .containsExactly(TransactionType.TRANSFER_OUT, TransactionType.TRANSFER_IN);
    }

    // Rule 5: both sides must be different accounts; rejected before any database call
    @Test
    void transfer_whenSameAccount_throwsIllegalArgument() {
        assertThatThrownBy(() -> transferService.transfer(1L, 1L, new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same account");

        verifyNoInteractions(accountRepository, transactionRepository);
    }

    // Rule 3: insufficient funds leaves both balances unchanged and saves nothing
    @Test
    void transfer_whenInsufficientFunds_leavesBalancesUnchangedAndSavesNothing() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> transferService.transfer(1L, 2L, new BigDecimal("100.01")))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(from.getBalance()).isEqualByComparingTo("100.00");
        assertThat(to.getBalance()).isEqualByComparingTo("50.00");
        verify(transactionRepository, never()).save(any());
    }

    // Rule 7: an unknown destination account stops the transfer before any money moves
    @Test
    void transfer_whenDestinationAccountIsUnknown_throwsAccountNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(1L, 99L, new BigDecimal("10.00")))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("99");

        assertThat(from.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }
}