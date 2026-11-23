package com.chamodi.bank_api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.chamodi.bank_api.domain.Account;
import com.chamodi.bank_api.exception.AccountNotFoundException;
import com.chamodi.bank_api.exception.InsufficientFundsException;
import com.chamodi.bank_api.service.AccountService;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    // Builds an account with an id, as if it had been saved to the database
    private Account savedAccount(Long id, String number, String balance) {
        Account account = new Account(number, "Chamodi", new BigDecimal(balance));
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    // Valid request: 201 Created, a Location header and the account in the body
    @Test
    void create_whenRequestIsValid_returns201() throws Exception {
        when(accountService.createAccount(any(), any(), any()))
                .thenReturn(savedAccount(1L, "ACC-001", "100.00"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountNumber":"ACC-001","ownerName":"Chamodi","openingBalance":100.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/accounts/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("ACC-001"));
    }

    // Bean Validation: a bad body never reaches the service and returns the standard error shape
    @Test
    void create_whenBodyIsInvalid_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountNumber":"","ownerName":"Chamodi","openingBalance":-5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("accountNumber")))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(accountService);
    }

    // AccountNotFoundException becomes 404 with no internal details
    @Test
    void get_whenAccountIsUnknown_returns404() throws Exception {
        when(accountService.getAccount(99L)).thenThrow(new AccountNotFoundException(99L));

        mockMvc.perform(get("/api/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account 99 not found"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // InsufficientFundsException becomes 422
    @Test
    void withdraw_whenFundsAreInsufficient_returns422() throws Exception {
        when(accountService.withdraw(eq(1L), any(BigDecimal.class)))
                .thenThrow(new InsufficientFundsException(1L));

        mockMvc.perform(post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1000}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422));
    }

    // Valid deposit: 200 OK with the updated account
    @Test
    void deposit_whenAmountIsValid_returns200() throws Exception {
        when(accountService.deposit(eq(1L), any(BigDecimal.class)))
                .thenReturn(savedAccount(1L, "ACC-001", "125.50"));

        mockMvc.perform(post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":25.50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC-001"));
    }
}