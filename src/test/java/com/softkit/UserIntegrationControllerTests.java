package com.softkit;

import com.softkit.dto.UserDataDTO;
import com.softkit.dto.UserResponseDTO;
import com.softkit.model.Role;
import com.softkit.repository.UserRepository;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;

import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UserIntegrationControllerTests extends AbstractControllerTest {

    private final String signupUrl = "/users/signup";
    private final String signinUrl = "/users/signin";
    private final String whoamiUrl = "/users/me";
    private final String deleteUrl = "/users/delete";
    private final String searchUrl = "/users/search";
    private final String refreshUrl = "/users/refresh";
    private final String updateBaseUrl = "/users/update";
    private final String updateAdminUrl = "users/admin/update";
    private final String verifyUrl = "/users/verify";
    private final String imageUrl = "/users/images";
    private final String inviteUrl = "/invite/user";
    private final String updateEmailUrl = "/users/update/email";
    private final String verifyEmailUrl = "users/verify/email";

    private final String exportEmailUrl = "/users/exportCSV";

    @Autowired
    private UserRepository userRepository;

    @Test
    public void simpleSignupSuccessTest() {
        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userForSignup.getUsername()).isEnabled());


    }

    @Test
    public void signupAgainErrorTest() {
        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

//        signup same user second time
        ResponseEntity<HashMap<String, Object>> response = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<HashMap<String, Object>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("error")).isEqualTo("Unprocessable Entity");

    }


    @Test
    public void noSuchUserForLoginTest() {
        ResponseEntity<HashMap<String, Object>> response = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", "fakeusername")
                        .queryParam("password", "fakepass")
                        .build().encode().toUri(),
                HttpMethod.POST,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<HashMap<String, Object>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("error")).isEqualTo("Unprocessable Entity");
    }


    @Test
    public void sucessSignupAndSigninTest() {
        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userForSignup.getUsername()).isEnabled());


        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userForSignup.getUsername())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);


        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

        ResponseEntity<UserResponseDTO> whoAmIResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails = whoAmIResponse.getBody();

        assertThat(userDetails.getUsername()).isEqualTo(userForSignup.getUsername());
        assertThat(userDetails.getEmail()).isEqualTo(userForSignup.getEmail());
        assertThat(userDetails.getRoles()).isEqualTo(userForSignup.getRoles());
        assertThat(userDetails.getId()).isNotNull();

    }

    @Test
    public void whoAmIWithIncorrectAuthTokenTest() {

//        set auth headers based on login response
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + "Secure (no) token"));

//        call /me endpoint to check that user is really authorized  
        ResponseEntity<UserResponseDTO> whoAmIResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

//        check status code 
        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(whoAmIResponse.getBody()).isNull();
    }

    @Test
    public void deleteSigUpUsernameTest(){
        UserDataDTO userClient = getValidUserForDelete();
        UserDataDTO userAdmin = getValidUserForSignup();

        ResponseEntity<String> responseSignUpAdmin = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userAdmin),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUpAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUpAdmin.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<String> responseSignUpClient = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userClient),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUpClient.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUpClient.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerifyAdmin = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userAdmin.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userAdmin.getUsername()).isEnabled());

        ResponseEntity<Object> responseVerifyClient = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userClient.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userClient.getUsername()).isEnabled());


        String tokenAdmin = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userAdmin.getUsername())
                        .queryParam("password", userAdmin.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);
        assertThat(tokenAdmin).isNotBlank();


        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + tokenAdmin));

        ResponseEntity<UserDataDTO> deleteUsernameResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + deleteUrl)
                        .queryParam("username", userClient.getUsername())
                        .build().encode().toUri(),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                UserDataDTO.class);

        assertThat(deleteUsernameResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    public void deleteNOUserTest(){
        UserDataDTO userAdmin = getValidUserForSignup();

        ResponseEntity<String> responseSignUpAdmin = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userAdmin),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUpAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUpAdmin.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerifyAdmin = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userAdmin.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userAdmin.getUsername()).isEnabled());

        String tokenAdmin = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userAdmin.getUsername())
                        .queryParam("password", userAdmin.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);
        assertThat(tokenAdmin).isNotBlank();


        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + tokenAdmin));

        ResponseEntity<UserDataDTO> deleteUsernameResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + deleteUrl)
                        .queryParam("username", "fakeUsername")
                        .build().encode().toUri(),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                UserDataDTO.class);

        assertThat(deleteUsernameResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    }


    @Test
    public void searchByUsernameTest() {
        UserDataDTO userAdmin = getValidUserForSignup();

        ResponseEntity<String> responseSignUpAdmin = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userAdmin),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUpAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUpAdmin.getBody()).isEqualTo("An email confirmation link has been sent to you.");


        ResponseEntity<Object> responseVerifyAdmin = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userAdmin.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userAdmin.getUsername()).isEnabled());


        String tokenAdmin = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userAdmin.getUsername())
                        .queryParam("password", userAdmin.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);
        assertThat(tokenAdmin).isNotBlank();


        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + tokenAdmin));

        ResponseEntity<UserResponseDTO> searchResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + searchUrl)
                        .queryParam("username", userAdmin.getUsername())
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

        //System.out.println(searchResponse);

//        check status code
        assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails = searchResponse.getBody();

//        check that all fields match and id has been set properly
        assertThat(userDetails.getUsername()).isEqualTo(userAdmin.getUsername());
        assertThat(userDetails.getEmail()).isEqualTo(userAdmin.getEmail());
        assertThat(userDetails.getRoles()).isEqualTo(userAdmin.getRoles());
        assertThat(userDetails.getId()).isNotNull();
    }

    @Test
    public void searchNoUserTest(){
        UserDataDTO userAdmin = getValidUserForSignup();

        ResponseEntity<String> responseSignUpAdmin = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userAdmin),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUpAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUpAdmin.getBody()).isEqualTo("An email confirmation link has been sent to you.");


        ResponseEntity<Object> responseVerifyAdmin = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userAdmin.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userAdmin.getUsername()).isEnabled());


        String tokenAdmin = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userAdmin.getUsername())
                        .queryParam("password", userAdmin.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);
        assertThat(tokenAdmin).isNotBlank();


        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + tokenAdmin));

        ResponseEntity<UserResponseDTO> searchResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + searchUrl)
                        .queryParam("username", "fakeUsername")
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);


        assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    }

    @Test
    public void  refreshTokenTest(){
        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userForSignup.getUsername()).isEnabled());


        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userForSignup.getUsername())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);


        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

        ResponseEntity<UserResponseDTO> whoAmIResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails = whoAmIResponse.getBody();

        assertThat(userDetails.getUsername()).isEqualTo(userForSignup.getUsername());
        assertThat(userDetails.getEmail()).isEqualTo(userForSignup.getEmail());
        assertThat(userDetails.getRoles()).isEqualTo(userForSignup.getRoles());
        assertThat(userDetails.getId()).isNotNull();

        String refreshToken = this.restTemplate.postForObject(
                (getBaseUrl() + refreshUrl),
                new HttpEntity<>(headers),
                String.class);

        assertThat(refreshToken).isNotBlank();

        //System.out.println(refreshToken);

        HttpHeaders headersNewToken = new HttpHeaders();
        headersNewToken.put("Authorization", Collections.singletonList("Bearer " + refreshToken));

        ResponseEntity<UserResponseDTO> refreshWhoToken = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headersNewToken),
                UserResponseDTO.class);

        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails1 = refreshWhoToken.getBody();

        assertThat(userDetails1.getUsername()).isEqualTo(userForSignup.getUsername());
        assertThat(userDetails1.getEmail()).isEqualTo(userForSignup.getEmail());
        assertThat(userDetails1.getRoles()).isEqualTo(userForSignup.getRoles());
        assertThat(userDetails1.getId()).isNotNull();


    }
    @Test
    public void refreshNoUserTokenTest(){
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + "Secure (no) token"));

//        call /me endpoint to check that user is really authorized
        ResponseEntity<UserResponseDTO> whoAmIResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + refreshUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

//        check status code
        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(whoAmIResponse.getBody()).isNull();
    }

    @Test
    public void similarEmailTest(){
        UserDataDTO userOne = getValidUserForEmail1();
        UserDataDTO userTwo = getValidUserForEmail2();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userOne),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<HashMap<String,Object>> response = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userTwo),
                new ParameterizedTypeReference<HashMap<String,Object>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("error")).isEqualTo("Unprocessable Entity");
    }

    @Test
    public void similarUsernameTest(){
        UserDataDTO userOne = getValidUserForUsername1();
        UserDataDTO userTwo = getValidUserForUsername2();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userOne),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");


        ResponseEntity<HashMap<String,Object>> response = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userTwo),
                new ParameterizedTypeReference<HashMap<String,Object>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("error")).isEqualTo("Unprocessable Entity");


    }

    @Test
    public void notVerifiedUserTest(){
        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<HashMap<String, Object>> response = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", "fakeusername")
                        .queryParam("password", "fakepass")
                        .build().encode().toUri(),
                HttpMethod.POST,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<HashMap<String, Object>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("error")).isEqualTo("Unprocessable Entity");


    }

    @Test
    public void updateDataBaseTest(){
        String firstName = "SOFT";
        String lastName = "SOFT";

        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userForSignup.getUsername()).isEnabled());


        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userForSignup.getUsername())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);


        assertThat(token).isNotBlank();


        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

        ResponseEntity<String> responce = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + updateBaseUrl)
                        .queryParam("firstName", firstName)
                        .queryParam("lastName", lastName)
                        .build().encode().toUri(),
                HttpMethod.PUT,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(responce.getBody()).isEqualTo("Success");

        ResponseEntity<UserResponseDTO> whoAmIResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails = whoAmIResponse.getBody();

        assertThat(userDetails.getFirstName()).isEqualTo(firstName);
        assertThat(userDetails.getLastName()).isEqualTo(lastName);
    }
    @Test
    public void updateDataAdminTest(){
        String firstName = "SOFT";
        String lastName = "SOFT";

        UserDataDTO userForSignup = getValidUserForSignup();
        UserDataDTO userClient = getValidUserForDelete();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<String> responseSignUpClient = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userClient),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUpClient.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUpClient.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userForSignup.getUsername()).isEnabled());

        ResponseEntity<Object> responseVerifyClient = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userClient.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userClient.getUsername()).isEnabled());

        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userForSignup.getUsername())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);


        assertThat(token).isNotBlank();


        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

        ResponseEntity<String> responce = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + updateAdminUrl)
                        .queryParam("username", userClient.getUsername())
                        .queryParam("firstName", firstName)
                        .queryParam("lastName", lastName)
                        .build().encode().toUri(),
                HttpMethod.PUT,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(responce.getBody()).isEqualTo("Success");

        String tokenClient = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userClient.getUsername())
                        .queryParam("password", userClient.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);


        assertThat(tokenClient).isNotBlank();


        HttpHeaders headersClient = new HttpHeaders();
        headersClient.put("Authorization", Collections.singletonList("Bearer " + tokenClient));

        ResponseEntity<UserResponseDTO> whoAmIResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headersClient),
                UserResponseDTO.class);

        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails = whoAmIResponse.getBody();

        assertThat(userDetails.getFirstName()).isEqualTo(firstName);
        assertThat(userDetails.getLastName()).isEqualTo(lastName);
    }

/*
    @Test
    public void saveImageTest() throws IOException {
        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userForSignup.getUsername()).isEnabled());


        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("username", userForSignup.getUsername())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);

        assertThat(token).isNotBlank();

        File imagefile = new File("/home/softkit/Documents/spring-boot-base-learning/src/test/java/com/softkit/images/1/download.jpeg");
        FileInputStream input = new FileInputStream(imagefile);
        MultipartFile multipartFile = new MockMultipartFile("download.jpeg",
                imagefile.getName(), "text/jpeg", Files.readAllBytes(Path.of("/home/softkit/Documents/spring-boot-base-learning/src/test/java/com/softkit/images/1/download.jpeg")));

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

        ResponseEntity<String> responseSaveImage = this.restTemplate.exchange(
                getBaseUrl() + imageUrl,
                HttpMethod.POST,
                new HttpEntity<>(multipartFile,headers),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSaveImage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSaveImage.getBody()).isEqualTo("Success");

    }

    @Test
    public void getImageTest(){}
*/

    @Test
    public void inviteAccessUserAndRegister(){
        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userForSignup.getUsername()).isEnabled());

        String email = "m@gmail.com";

        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userForSignup.getUsername())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);


        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

        ResponseEntity<String> inviteResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + updateBaseUrl)
                        .queryParam("email", email)
                        .build().encode().toUri(),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        assertThat(inviteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);


    }
    @Test
    public void inviteAgainUserRegister(){
        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userForSignup.getUsername()).isEnabled());

        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userForSignup.getUsername())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);


        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

        ResponseEntity<String> inviteResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + inviteUrl)
                        .queryParam("email", userForSignup.getEmail())
                        .build().encode().toUri(),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        assertThat(inviteResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }


    @Test
    public void updateEmailSignInTest(){
        UserDataDTO userForSignup = getValidUserForSignup();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userForSignup),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userForSignup.getUsername()).isEnabled());

        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userForSignup.getUsername())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

        String email = "m1@gmail.com";
        ResponseEntity<String> updateEmailResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + updateEmailUrl)
                        .queryParam("email", email)
                        .build().encode().toUri(),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);


        assertThat(updateEmailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String tokenForEmail = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userForSignup.getEmail())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);
        assertThat(token).isNotBlank();

        HttpHeaders headersEmail = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + tokenForEmail));

        ResponseEntity<UserResponseDTO> whoAmIResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails = whoAmIResponse.getBody();

        assertThat(userDetails.getUsername()).isEqualTo(userForSignup.getUsername());
        assertThat(userDetails.getEmail()).isEqualTo(userForSignup.getEmail());
        assertThat(userDetails.getRoles()).isEqualTo(userForSignup.getRoles());
        assertThat(userDetails.getId()).isNotNull();

        ResponseEntity<Object> responseVerifyEmail = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyEmailUrl)
                        .queryParam("code", userRepository.findByUsername(userForSignup.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);


        assertThat(userRepository.findByUsername(userForSignup.getUsername()).getEmail()).isEqualTo(email);
        assertThat(userRepository.findByUsername(userForSignup.getUsername()).getUpdateEmail()).isEqualTo(null);


        ResponseEntity<String> sigInOldEmail = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userForSignup.getEmail())
                        .queryParam("password", userForSignup.getPassword())
                        .build().encode().toUri(),
                HttpMethod.POST,
                HttpEntity.EMPTY,
                String.class);

        assertThat(sigInOldEmail.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    }

    @Test
    public void exportCSVAccessDeniedTest() {
        UserDataDTO userClient = getValidUserForDelete();

        ResponseEntity<String> responseSignUp = this.restTemplate.exchange(
                getBaseUrl() + signupUrl,
                HttpMethod.POST,
                new HttpEntity<>(userClient),
                new ParameterizedTypeReference<String>() {
                });

        assertThat(responseSignUp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseSignUp.getBody()).isEqualTo("An email confirmation link has been sent to you.");

        ResponseEntity<Object> responseVerify = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + verifyUrl)
                        .queryParam("code", userRepository.findByUsername(userClient.getUsername()).getVerificationCode())
                        .build().encode().toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Object.class);

        assertThat(true).isEqualTo(userRepository.findByUsername(userClient.getUsername()).isEnabled());


        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("usernameOrEmail", userClient.getUsername())
                        .queryParam("password", userClient.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);


        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

        ResponseEntity<String> exportCSVResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + exportEmailUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(exportCSVResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void cacheTestLoad(){


        }

    private UserDataDTO getValidUserForSignup() {
        UUID randomUUID = UUID.randomUUID();
        return new UserDataDTO(
                randomUUID + "softkit",
                randomUUID + "HeisenbuG1!",
                randomUUID + "softkit@yahoo.com",
                randomUUID + "softkit",
                randomUUID + "softkit",
                Date.valueOf("2003-01-11"),
                Date.valueOf(LocalDate.now()),
                randomUUID + "photo.image",
                Lists.newArrayList(Role.ROLE_ADMIN));
    }

    private UserDataDTO getValidUserForDelete() {
        UUID randomUUID = UUID.randomUUID();
        return new UserDataDTO(
                randomUUID + "Maxim",
                randomUUID + "HeisenbuG1!",
                randomUUID + "softkit1@yahoo.com",
                randomUUID + "softkit",
                randomUUID + "softkit",
                Date.valueOf("2003-01-11"),
                Date.valueOf(LocalDate.now()),
                randomUUID + "photo.image",
                Lists.newArrayList(Role.ROLE_CLIENT));
    }

    private UserDataDTO getValidUserForEmail1() {
        return new UserDataDTO(
                 "softkitEm",
                 "HeisenbuG1!",
                 "softkit@softkit.com",
                 "softkit",
                 "softkit",
                Date.valueOf("2003-01-11"),
                Date.valueOf(LocalDate.now()),
                 "photo.image",
                Lists.newArrayList(Role.ROLE_ADMIN));
    }

    private UserDataDTO getValidUserForEmail2() {
        return new UserDataDTO(
                "maximEm",
                "HeisenbuG1!",
                "SOFTkit@softkit.com",
                "maxim",
                "maxim",
                Date.valueOf("2003-01-11"),
                Date.valueOf(LocalDate.now()),
                "photo.image",
                Lists.newArrayList(Role.ROLE_CLIENT));
    }

    private UserDataDTO getValidUserForUsername1() {
        return new UserDataDTO(
                "turtle",
                "HeisenbuG1!",
                "softkit@softkit.com",
                "maxim",
                "maxim",
                Date.valueOf("2003-01-11"),
                Date.valueOf(LocalDate.now()),
                "photo.image",
                Lists.newArrayList(Role.ROLE_CLIENT));
    }

    private UserDataDTO getValidUserForUsername2() {
        return new UserDataDTO(
                "TURTLE",
                "HeisenbuG1!",
                "maxim.shepelyakovski@gmail.com",
                "maxim",
                "maxim",
                Date.valueOf("2003-01-11"),
                Date.valueOf(LocalDate.now()),
                "photo.image",
                Lists.newArrayList(Role.ROLE_CLIENT));
    }

}
