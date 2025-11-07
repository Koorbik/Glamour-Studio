package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.auth.FacebookUserDto;
import com.hszadkowski.iwa_backend.services.interfaces.FacebookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class FacebookServiceImpl implements FacebookService {

    @Value("${FACEBOOK_APP_ID:}")
    private String facebookAppId;

    @Value("${FACEBOOK_APP_SECRET:}")
    private String facebookAppSecret;

    private final RestTemplate restTemplate = new RestTemplate(); // maybe later change to WebClient

    @Override
    public boolean validateFacebookToken(String accessToken) {
        String url = "https://graph.facebook.com/debug_token?input_token=" + accessToken +
                "&access_token=" + facebookAppId + "|" + facebookAppSecret;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                return data != null && (boolean) data.get("is_valid");
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public FacebookUserDto getFacebookUserInfo(String accessToken) {
        String url = "https://graph.facebook.com/me?fields=id,name,email&access_token=" + accessToken;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> userData = response.getBody();

                FacebookUserDto facebookUser = new FacebookUserDto();
                facebookUser.setId((String) userData.get("id"));
                facebookUser.setName((String) userData.get("name"));
                facebookUser.setEmail((String) userData.get("email"));
                facebookUser.setAccessToken(accessToken);

                return facebookUser;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}