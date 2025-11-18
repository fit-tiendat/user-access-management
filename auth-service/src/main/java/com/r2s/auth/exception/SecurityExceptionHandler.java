// auth-service/src/main/java/com/r2s/auth/exception/SecurityExceptionHandler.java
package com.r2s.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<String> handleAuthorizationDenied(AuthorizationDeniedException ex) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        //  Không có token / auth null / anonymous -> 401
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized");
        }

        // Có token, đã authenticate nhưng role không đủ -> 403
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Forbidden");
    }
}
