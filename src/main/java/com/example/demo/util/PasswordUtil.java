package com.example.demo.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

  private final PasswordEncoder passwordEncoder;

  @Autowired
  public PasswordUtil(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  public String encodePassword(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }
}
