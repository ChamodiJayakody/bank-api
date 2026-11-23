package com.chamodi.bank_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.chamodi.bank_api.service.TransferResult;
import com.chamodi.bank_api.service.TransferService;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    private Account savedAccount(Long id, String number, String balance) {
        Account account = new Account(number, "Owner", new BigDecimal(balance));
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    // Valid transfer: 201 Created with both account ids in the body
    @Test
    void transfer_whenRequestIsValid_returns201() throws Exception {
        TransferResult result = new TransferResult(
                savedAccount(1L, "ACC-001", "70.00"),
                savedAccount(2L, "ACC-002", "80.00"),
                new BigDecimal("30.00"));
        when(transferService.transfer(eq(1L), eq(2L), any(BigDecimal.class))).thenReturn(result);

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountId":1,"toAccountId":2,"amount":30.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromAccountId").value(1))
                .andExpect(jsonPath("$.toAccountId").value(2));
    }

    // The service's IllegalArgumentException (same account) becomes 400
    @Test
    void transfer_whenSameAccount_returns400() throws Exception {
        when(transferService.transfer(eq(1L), eq(1L), any(BigDecimal.class)))
                .thenThrow(new IllegalArgumentException("Cannot transfer to the same account"));

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountId":1,"toAccountId":1,"amount":10.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot transfer to the same account"));
    }
}