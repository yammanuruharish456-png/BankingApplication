package com.banking.application.auth.security;

import com.banking.application.users.entity.User;

public record AuthenticatedUser(Long id, String email) {
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail());
    }
}
