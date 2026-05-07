package com.banking.application.payments;

import com.banking.application.auth.dto.RegisterRequest;
import com.banking.application.auth.service.AuthService;
import com.banking.application.common.exception.ApiException;
import com.banking.application.payments.dto.TransferRequest;
import com.banking.application.payments.dto.TransferResponse;
import com.banking.application.payments.repository.BalanceRepository;
import com.banking.application.payments.service.TransferService;
import com.banking.application.users.entity.User;
import com.banking.application.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransferIntegrationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BalanceRepository balanceRepository;
    @Autowired
    private TransferService transferService;

    private User payer;
    private String payerVpa;
    private String payeeVpa;
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @BeforeEach
    void setUp() {
        int n = COUNTER.incrementAndGet();
        payerVpa = "alice" + n + "@bank";
        payeeVpa = "bob" + n + "@bank";
        String payerEmail = "alice" + n + "@test.com";
        String payeeEmail = "bob" + n + "@test.com";

        authService.register(new RegisterRequest("alice" + n, payerEmail, "password123", "1234", payerVpa));
        authService.register(new RegisterRequest("bob" + n, payeeEmail, "password123", "1234", payeeVpa));
        payer = userRepository.findByEmail(payerEmail).orElseThrow();
        var payerBalance = balanceRepository.findByAccountId(payer.getId()).orElseThrow();
        payerBalance.setAmount(new BigDecimal("100.00"));
        balanceRepository.save(payerBalance);
    }

    @Test
    void transferShouldBeIdempotentForSameKey() {
        TransferRequest request = new TransferRequest(payerVpa, payeeVpa, new BigDecimal("10.00"), "note", "1234", "client-1");
        TransferResponse first = transferService.transfer(payer, "idem-1", request);
        TransferResponse second = transferService.transfer(payer, "idem-1", request);
        assertEquals(first.transactionId(), second.transactionId());
        assertEquals("SUCCESS", second.status());
    }

    @Test
    void concurrentTransfersShouldNotOverdrawWhenLocked() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        TransferRequest request1 = new TransferRequest(payerVpa, payeeVpa, new BigDecimal("80.00"), "note", "1234", "client-2");
        TransferRequest request2 = new TransferRequest(payerVpa, payeeVpa, new BigDecimal("80.00"), "note", "1234", "client-3");

        Callable<Boolean> task1 = () -> attempt(request1, "idem-2");
        Callable<Boolean> task2 = () -> attempt(request2, "idem-3");

        List<Future<Boolean>> futures = executorService.invokeAll(List.of(task1, task2));
        executorService.shutdown();

        List<Boolean> results = new ArrayList<>();
        for (Future<Boolean> future : futures) {
            results.add(future.get());
        }

        long success = results.stream().filter(Boolean::booleanValue).count();
        assertEquals(1, success);
        var payerBalance = balanceRepository.findByAccountId(payer.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("20.00").compareTo(payerBalance.getAmount()));
    }

    private boolean attempt(TransferRequest request, String idempotencyKey) {
        try {
            transferService.transfer(payer, idempotencyKey, request);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }
}
