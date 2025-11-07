package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.user.UserProfileUpdateDto;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.repos.UserRepository;
import com.hszadkowski.iwa_backend.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    
    @Override
    public AppUser findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
    
    @Override
    @Transactional
    public AppUser updateUserProfile(String email, UserProfileUpdateDto updateDto) {
        AppUser user = findByEmail(email);
        
        user.setName(updateDto.getName());
        user.setSurname(updateDto.getSurname());
        user.setPhoneNum(updateDto.getPhoneNum());
        
        return userRepository.save(user);
    }
}
