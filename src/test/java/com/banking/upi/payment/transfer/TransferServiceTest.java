package com.banking.upi.payment.transfer;

import com.banking.upi.domain.*;
import com.banking.upi.domain.Transaction.TxnStatus;
import com.banking.upi.exception.ApiException;
import com.banking.upi.payment.transfer.dto.TransferRequest;
import com.banking.upi.payment.transfer.dto.TransferResponse;
import com.banking.upi.pin.PinService;
import com.banking.upi.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private VpaRepository vpaRepository;
    @Mock private BalanceRepository balanceRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private PinService pinService;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private TransferService transferService;

    private Vpa payerVpa;
    private Vpa payeeVpa;
    private Balance payerBalance;
    private Balance payeeBalance;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        payerVpa = Vpa.builder().id(1L).handle("alice@bank").userId(1L).accountId(10L).active(true).build();
        payeeVpa = Vpa.builder().id(2L).handle("bob@bank").userId(2L).accountId(20L).active(true).build();

        payerBalance = Balance.builder().id(1L).accountId(10L).amount(new BigDecimal("1000.00")).version(0L).build();
        payeeBalance = Balance.builder().id(2L).accountId(20L).amount(new BigDecimal("500.00")).version(0L).build();

        request = new TransferRequest();
        request.setPayerVpa("alice@bank");
        request.setPayeeVpa("bob@bank");
        request.setAmount(new BigDecimal("100.00"));
        request.setNote("Test transfer");
        request.setUpiPin("1234");
    }

    @Test
    void executeTransfer_success() throws Exception {
        when(idempotencyKeyRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(vpaRepository.findByHandle("alice@bank")).thenReturn(Optional.of(payerVpa));
        when(vpaRepository.findByHandle("bob@bank")).thenReturn(Optional.of(payeeVpa));
        doNothing().when(pinService).verifyPin(anyLong(), anyString());
        when(balanceRepository.findByAccountIdWithLock(10L)).thenReturn(Optional.of(payerBalance));
        when(balanceRepository.findByAccountIdWithLock(20L)).thenReturn(Optional.of(payeeBalance));

        Transaction savedTxn = Transaction.builder()
                .id(1L)
                .txnRef("TXN123")
                .payerVpa("alice@bank")
                .payeeVpa("bob@bank")
                .amount(new BigDecimal("100.00"))
                .status(TxnStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
        when(transactionRepository.save(any())).thenReturn(savedTxn);
        when(ledgerEntryRepository.save(any())).thenReturn(LedgerEntry.builder().build());
        when(balanceRepository.save(any())).thenReturn(payerBalance);
        when(outboxEventRepository.save(any())).thenReturn(OutboxEvent.builder().build());
        when(auditLogRepository.save(any())).thenReturn(AuditLog.builder().build());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(idempotencyKeyRepository.save(any())).thenReturn(IdempotencyKey.builder().build());

        TransferResponse response = transferService.executeTransfer("key-123", request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TxnStatus.SUCCESS);
        verify(pinService).verifyPin(1L, "1234");
        verify(ledgerEntryRepository, times(2)).save(any());
        verify(balanceRepository, times(2)).save(any());
    }

    @Test
    void executeTransfer_insufficientBalance_throwsPaymentRequired() {
        payerBalance.setAmount(new BigDecimal("50.00"));

        when(idempotencyKeyRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(vpaRepository.findByHandle("alice@bank")).thenReturn(Optional.of(payerVpa));
        when(vpaRepository.findByHandle("bob@bank")).thenReturn(Optional.of(payeeVpa));
        doNothing().when(pinService).verifyPin(anyLong(), anyString());
        when(balanceRepository.findByAccountIdWithLock(10L)).thenReturn(Optional.of(payerBalance));
        when(balanceRepository.findByAccountIdWithLock(20L)).thenReturn(Optional.of(payeeBalance));

        assertThatThrownBy(() -> transferService.executeTransfer("key-456", request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));
    }

    @Test
    void executeTransfer_payerVpaNotFound_throwsNotFound() {
        when(idempotencyKeyRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(vpaRepository.findByHandle("alice@bank")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.executeTransfer("key-789", request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void executeTransfer_idempotencyKeyExists_returnsCached() throws Exception {
        TransferResponse cachedResponse = TransferResponse.builder()
                .txnId(1L).txnRef("TXN123").status(TxnStatus.SUCCESS)
                .amount(new BigDecimal("100.00")).build();

        IdempotencyKey existingKey = IdempotencyKey.builder()
                .idempotencyKey("key-dup")
                .responseBody("{}")
                .responseStatus(200)
                .build();

        when(idempotencyKeyRepository.findByIdempotencyKey("key-dup")).thenReturn(Optional.of(existingKey));
        when(objectMapper.readValue(anyString(), eq(TransferResponse.class))).thenReturn(cachedResponse);

        TransferResponse response = transferService.executeTransfer("key-dup", request);

        assertThat(response.getTxnRef()).isEqualTo("TXN123");
        verify(vpaRepository, never()).findByHandle(any());
    }
}
