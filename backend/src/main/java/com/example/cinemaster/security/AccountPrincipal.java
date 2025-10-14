package com.example.cinemaster.security;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountPrincipal implements UserDetails {

    Integer id;
    String email;
    String fullName;
    String role;
    Integer branchId;
    String branchName;
    Collection<? extends GrantedAuthority> authorities;

    public boolean hasRole(String r) {
        return ("ROLE_" + r).equalsIgnoreCase(authorities.iterator().next().getAuthority());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
