package com.chamodi.bank_api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chamodi.bank_api.domain.Account;
import com.chamodi.bank_api.dto.AccountResponse;
import com.chamodi.bank_api.dto.AmountRequest;
import com.chamodi.bank_api.dto.CreateAccountRequest;
import com.chamodi.bank_api.dto.TransactionResponse;
import com.chamodi.bank_api.service.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(
                request.accountNumber(), request.ownerName(), request.openingBalance());
        return ResponseEntity
                .created(URI.create("/api/accounts/" + account.getId()))
                .body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return AccountResponse.from(accountService.getAccount(id));
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(accountService.deposit(id, request.amount()));
    }

    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(accountService.withdraw(id, request.amount()));
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionResponse> transactions(@PathVariable Long id) {
        return accountService.getTransactions(id).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}