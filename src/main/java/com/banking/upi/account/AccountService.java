package com.banking.upi.account;

import com.banking.upi.account.dto.AccountResponse;
import com.banking.upi.account.dto.CreateAccountRequest;
import com.banking.upi.domain.Account;
import com.banking.upi.domain.Balance;
import com.banking.upi.domain.User;
import com.banking.upi.exception.ApiException;
import com.banking.upi.repository.AccountRepository;
import com.banking.upi.repository.BalanceRepository;
import com.banking.upi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;
    private final UserRepository userRepository;

    @Transactional
    public AccountResponse createAccount(String mobile, CreateAccountRequest request) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        accountRepository.findByAccountNumber(request.getAccountNumber())
                .ifPresent(a -> { throw new ApiException(HttpStatus.CONFLICT, "Account number already exists"); });

        Account account = Account.builder()
                .userId(user.getId())
                .accountNumber(request.getAccountNumber())
                .ifsc(request.getIfsc())
                .bankName(request.getBankName())
                .enabled(true)
                .build();

        account = accountRepository.save(account);

        Balance balance = Balance.builder()
                .accountId(account.getId())
                .amount(request.getInitialBalance())
                .version(0L)
                .build();

        balance = balanceRepository.save(balance);

        log.info("Account created: accountId={}, userId={}", account.getId(), user.getId());

        return buildResponse(account, balance);
    }

    public List<AccountResponse> getMyAccounts(String mobile) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        List<Account> accounts = accountRepository.findByUserId(user.getId());

        return accounts.stream().map(acc -> {
            Balance balance = balanceRepository.findByAccountId(acc.getId())
                    .orElse(Balance.builder().amount(java.math.BigDecimal.ZERO).build());
            return buildResponse(acc, balance);
        }).collect(Collectors.toList());
    }

    private AccountResponse buildResponse(Account account, Balance balance) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .accountNumber(account.getAccountNumber())
                .ifsc(account.getIfsc())
                .bankName(account.getBankName())
                .balance(balance.getAmount())
                .enabled(account.getEnabled())
                .build();
    }
}
