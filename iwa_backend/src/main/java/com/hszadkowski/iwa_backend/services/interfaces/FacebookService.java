package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.dto.auth.FacebookUserDto;

public interface FacebookService {
    boolean validateFacebookToken(String accessToken);

    FacebookUserDto getFacebookUserInfo(String accessToken);
}