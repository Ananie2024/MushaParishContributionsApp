package org.ananie.parishApp.services;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
@Component
public interface SecurityUserService extends UserDetailsService {
void updateLastLogin(String xxx);

}
