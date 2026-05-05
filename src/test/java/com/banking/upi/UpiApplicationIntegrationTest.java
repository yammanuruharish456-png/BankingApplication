package com.banking.upi;

import com.banking.upi.account.dto.CreateAccountRequest;
import com.banking.upi.account.dto.AccountResponse;
import com.banking.upi.auth.dto.AuthResponse;
import com.banking.upi.auth.dto.LoginRequest;
import com.banking.upi.auth.dto.RegisterRequest;
import com.banking.upi.pin.dto.SetPinRequest;
import com.banking.upi.payment.transfer.dto.TransferRequest;
import com.banking.upi.payment.transfer.dto.TransferResponse;
import com.banking.upi.vpa.dto.CreateVpaRequest;
import com.banking.upi.vpa.dto.VpaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UpiApplicationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private RabbitTemplate rabbitTemplate;

    @Test
    void fullFlow_registerLoginCreateAccountSetPinTransfer() throws Exception {
        // 1. Register payer
        String payerMobile = "98" + System.currentTimeMillis() % 100000000L;
        AuthResponse payerAuth = registerUser(payerMobile, "payer@example.com");
        assertThat(payerAuth.getAccessToken()).isNotBlank();

        // 2. Register payee
        String payeeMobile = "97" + System.currentTimeMillis() % 100000000L;
        AuthResponse payeeAuth = registerUser(payeeMobile, "payee@example.com");
        assertThat(payeeAuth.getAccessToken()).isNotBlank();

        // 3. Create accounts
        AccountResponse payerAccount = createAccount(payerAuth.getAccessToken(), "ACC" + System.currentTimeMillis(), new BigDecimal("1000.00"));
        AccountResponse payeeAccount = createAccount(payeeAuth.getAccessToken(), "ACC" + (System.currentTimeMillis() + 1), new BigDecimal("100.00"));

        // 4. Create VPAs
        String payerVpaHandle = "payer" + System.currentTimeMillis() + "@test";
        String payeeVpaHandle = "payee" + System.currentTimeMillis() + "@test";
        VpaResponse payerVpa = createVpa(payerAuth.getAccessToken(), payerVpaHandle, payerAccount.getId());
        VpaResponse payeeVpa = createVpa(payeeAuth.getAccessToken(), payeeVpaHandle, payeeAccount.getId());

        // 5. Set PIN for payer
        setPin(payerAuth.getAccessToken(), "1234");

        // 6. Transfer
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setPayerVpa(payerVpaHandle);
        transferRequest.setPayeeVpa(payeeVpaHandle);
        transferRequest.setAmount(new BigDecimal("100.00"));
        transferRequest.setNote("Integration test transfer");
        transferRequest.setUpiPin("1234");

        MvcResult transferResult = mockMvc.perform(post("/v1/payments/transfer")
                        .header("Authorization", "Bearer " + payerAuth.getAccessToken())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andReturn();

        TransferResponse transferResponse = objectMapper.readValue(
                transferResult.getResponse().getContentAsString(), TransferResponse.class);
        assertThat(transferResponse.getStatus().name()).isEqualTo("SUCCESS");
        assertThat(transferResponse.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    private AuthResponse registerUser(String mobile, String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setMobile(mobile.length() > 10 ? mobile.substring(0, 10) : mobile);
        req.setEmail(email);
        req.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private AccountResponse createAccount(String token, String accountNumber, BigDecimal initialBalance) throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountNumber(accountNumber.substring(0, Math.min(accountNumber.length(), 20)));
        req.setIfsc("SBIN0001234");
        req.setBankName("State Bank of India");
        req.setInitialBalance(initialBalance);

        MvcResult result = mockMvc.perform(post("/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AccountResponse.class);
    }

    private VpaResponse createVpa(String token, String handle, Long accountId) throws Exception {
        CreateVpaRequest req = new CreateVpaRequest();
        req.setHandle(handle);
        req.setAccountId(accountId);

        MvcResult result = mockMvc.perform(post("/v1/vpa")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), VpaResponse.class);
    }

    private void setPin(String token, String pin) throws Exception {
        SetPinRequest req = new SetPinRequest();
        req.setPin(pin);

        mockMvc.perform(post("/v1/pin/set")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
