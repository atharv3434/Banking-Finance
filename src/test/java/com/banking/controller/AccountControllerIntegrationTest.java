package com.banking.controller;

import com.banking.dto.AccountDto;
import com.banking.model.Account;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private static final String BASE = "/api/v1/accounts";

    @Test
    @DisplayName("POST /accounts — creates account")
    void createAccount() throws Exception {
        AccountDto.CreateRequest req = AccountDto.CreateRequest.builder()
                .ownerName("Integration Tester")
                .accountType(Account.AccountType.CHECKING)
                .initialDeposit(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerName").value("Integration Tester"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /accounts — 400 on blank owner name")
    void createAccount_validationFails() throws Exception {
        AccountDto.CreateRequest req = AccountDto.CreateRequest.builder()
                .ownerName("")
                .accountType(Account.AccountType.SAVINGS)
                .initialDeposit(BigDecimal.ZERO)
                .build();

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.ownerName").exists());
    }

    @Test
    @DisplayName("GET /accounts — returns list")
    void getAllAccounts() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /accounts/{id} — 404 for unknown id")
    void getAccount_notFound() throws Exception {
        mvc.perform(get(BASE + "/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /accounts/{id}/deposit — increases balance")
    void deposit() throws Exception {
        // Create account first
        AccountDto.CreateRequest createReq = AccountDto.CreateRequest.builder()
                .ownerName("Depositor")
                .accountType(Account.AccountType.SAVINGS)
                .initialDeposit(new BigDecimal("500.00"))
                .build();

        String created = mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(created).get("id").asLong();

        AccountDto.AmountRequest depReq = new AccountDto.AmountRequest(new BigDecimal("250.00"), "Test deposit");

        mvc.perform(post(BASE + "/" + id + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(depReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.balanceAfter").value(750.00));
    }

    @Test
    @DisplayName("POST /accounts/{id}/withdraw — 422 on insufficient funds")
    void withdraw_insufficientFunds() throws Exception {
        AccountDto.CreateRequest createReq = AccountDto.CreateRequest.builder()
                .ownerName("Low Balance User")
                .accountType(Account.AccountType.CHECKING)
                .initialDeposit(new BigDecimal("50.00"))
                .build();

        String created = mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(created).get("id").asLong();

        AccountDto.AmountRequest req = new AccountDto.AmountRequest(new BigDecimal("9999.00"), null);

        mvc.perform(post(BASE + "/" + id + "/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient")));
    }
}
