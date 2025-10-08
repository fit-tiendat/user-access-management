package com.r2s.user.user_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.r2s.core.CommonConstants; // <-- thêm import

@RestController
@RequestMapping("/api/users")
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello from User Service - v" + CommonConstants.APP_VERSION;
    }
}
