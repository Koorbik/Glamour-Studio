package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.auth.GoogleUserDto;
import com.hszadkowski.iwa_backend.services.interfaces.GoogleAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    @Value("${google.auth.client.id}")
    private String googleClientId;

    @Value("${google.auth.frontend.client.id}")
    private String googleFrontendClientId;

    @Value("${GOOGLE_CALENDAR_CLIENT_ID}")
    private String googleCalendarClientId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean validateGoogleToken(String token) {
        // First try to validate as ID token (from Google Identity Services)
        if (token.contains(".")) {
           return validateIdToken(token);
            //return validateAccessToken(token);
        }
        // Otherwise validate as access token
        return validateAccessToken(token);
    }

    private boolean validateIdToken(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenInfo = response.getBody();
                String audience = (String) tokenInfo.get("aud");
                String issuer = (String) tokenInfo.get("iss");

                // Validate the token
                boolean validAudience = googleCalendarClientId.equals(audience);
                boolean validIssuer = "https://accounts.google.com".equals(issuer) ||
                                     "accounts.google.com".equals(issuer);

                return validAudience && validIssuer;
            }
            return false;
        } catch (Exception e) {
            log.error("Error validating Google ID token: ", e);
            return false;
        }
    }

    private boolean validateAccessToken(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenInfo = response.getBody();
                String audience = (String) tokenInfo.get("audience");

                return googleCalendarClientId.equals(audience);
            }
            return false;
        } catch (Exception e) {
            log.error("Error validating Google access token: ", e);
            return false;
        }
    }

    @Override
    public GoogleUserDto getGoogleUserInfo(String token) {
        // Check if it's an ID token or access token
        if (token.contains(".")) {
           return getUserInfoFromIdToken(token);
            //return getUserInfoFromAccessToken(token);
        }
        return getUserInfoFromAccessToken(token);
    }

    private GoogleUserDto getUserInfoFromIdToken(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenInfo = response.getBody();

                GoogleUserDto googleUser = new GoogleUserDto();
                googleUser.setId((String) tokenInfo.get("sub"));
                googleUser.setEmail((String) tokenInfo.get("email"));
                googleUser.setName((String) tokenInfo.get("name"));
                googleUser.setGivenName((String) tokenInfo.get("given_name"));
                googleUser.setFamilyName((String) tokenInfo.get("family_name"));
                googleUser.setAccessToken(idToken);

                return googleUser;
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting Google user info from ID token: ", e);
            return null;
        }
    }

    private GoogleUserDto getUserInfoFromAccessToken(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + accessToken;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                GoogleUserDto googleUser = getGoogleUserDto(accessToken, response);
                return googleUser;
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting Google user info from access token: ", e);
            return null;
        }
    }

    private static GoogleUserDto getGoogleUserDto(String accessToken, ResponseEntity<Map> response) {
        Map<String, Object> userInfo = response.getBody();

        GoogleUserDto googleUser = new GoogleUserDto();
        googleUser.setId((String) userInfo.get("id"));
        googleUser.setEmail((String) userInfo.get("email"));
        googleUser.setName((String) userInfo.get("name"));
        googleUser.setGivenName((String) userInfo.get("given_name"));
        googleUser.setFamilyName((String) userInfo.get("family_name"));
        googleUser.setAccessToken(accessToken);
        return googleUser;
    }
}
