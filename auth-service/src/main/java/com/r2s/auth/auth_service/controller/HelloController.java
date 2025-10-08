package com.r2s.auth.auth_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.r2s.core.CommonConstants; // <-- thêm import

@RestController
@RequestMapping("/api/auth")
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello from Auth Service - v" + CommonConstants.APP_VERSION;
    }
}

