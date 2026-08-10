package com.bank.chiikawa.service;

import com.bank.chiikawa.entity.Customer;
import com.bank.chiikawa.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 機能-01 ログイン/ログアウト
 * SCR001 → SCR002 遷移 조건: 아이디/비밀번호 인증 성공
 */
@Service
public class AuthService {

    private final CustomerRepository customerRepository;

    public AuthService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> authenticate(String userId, String rawPassword) {
        return customerRepository.findByUserId(userId)
                .filter(c -> c.getPassword().equals(rawPassword));
    }
}
