package com.bank.chiikawa.entity;

import jakarta.persistence.*;

/**
 * 고객 (로그인 사용자)
 * 관련요건: 機能-01 (로그인/로그아웃)
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 20)
    private String userId;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    protected Customer() {
    }

    public Customer(String userId, String password, String name) {
        this.userId = userId;
        this.password = password;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getPassword() { return password; }
    public String getName() { return name; }
}
