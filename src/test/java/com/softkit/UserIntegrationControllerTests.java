package com.softkit;

import com.softkit.dto.UserDataDTO;
import com.softkit.dto.UserResponseDTO;
import com.softkit.model.Role;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
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

    @Test
    public void simpleSignupSuccessTest() {
        String token = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                getValidUserForSignup(),
                String.class);

//        checking that token is ok
        assertThat(token).isNotBlank();
    }

    @Test
    public void signupAgainErrorTest() {
        UserDataDTO userForSignup = getValidUserForSignup();
        String token = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userForSignup,
                String.class);

//        checking that token is ok
        assertThat(token).isNotBlank();

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
    public void noSuchUserForLogin() {
        ResponseEntity<HashMap<String, Object>> response = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("username", "fakeusername")
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
    public void sucessSignupAndSignin() {
        UserDataDTO user = getValidUserForSignup();

        String signupToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                user,
                String.class);

        //System.out.println("1 =  "+signupToken);

//        checking that signup token is ok
        assertThat(signupToken).isNotBlank();

        String token = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("username", user.getUsername())
                        .queryParam("password", user.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);

        //System.out.println("2 =  "+signupToken);

//        checking that signin token is ok
        assertThat(token).isNotBlank();

//        set auth headers based on login response
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + token));

//        call /me endpoint to check that user is really authorized  
        ResponseEntity<UserResponseDTO> whoAmIResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

        //System.out.println(whoAmIResponse);

//        check status code 
        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails = whoAmIResponse.getBody();

//        check that all fields match and id has been set properly
        assertThat(userDetails.getUsername()).isEqualTo(user.getUsername());
        assertThat(userDetails.getEmail()).isEqualTo(user.getEmail());
        assertThat(userDetails.getRoles()).isEqualTo(user.getRoles());
        assertThat(userDetails.getId()).isNotNull();

    }

    @Test
    public void whoAmIWithIncorrectAuthToken() {

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
    public void deleteSigUpUsername(){
        UserDataDTO userAdmin = getValidUserForSignup();
        UserDataDTO userClient = getValidUserForDelete();

        String signupToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userAdmin,
                String.class);

        String clientToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userClient,
                String.class);

        assertThat(signupToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + signupToken));

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
    public void deleteNOUser(){
        UserDataDTO userAdmin = getValidUserForSignup();
        String signupToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userAdmin,
                String.class);
        assertThat(signupToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + signupToken));

        ResponseEntity<UserDataDTO> deleteUsernameResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + deleteUrl)
                        .queryParam("username", "maxim")
                        .build().encode().toUri(),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                UserDataDTO.class);

        assertThat(deleteUsernameResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    }


    @Test
    public void searchByUsername() {
        UserDataDTO userAdmin = getValidUserForSignup();
        UserDataDTO userClient = getValidUserForDelete();
        String signupToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userAdmin,
                String.class);

        String clientToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userClient,
                String.class);

        assertThat(signupToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + signupToken));

        ResponseEntity<UserResponseDTO> searchResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + searchUrl)
                        .queryParam("username", userClient.getUsername())
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

        //System.out.println(searchResponse);

//        check status code
        assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails = searchResponse.getBody();

//        check that all fields match and id has been set properly
        assertThat(userDetails.getUsername()).isEqualTo(userClient.getUsername());
        assertThat(userDetails.getEmail()).isEqualTo(userClient.getEmail());
        assertThat(userDetails.getRoles()).isEqualTo(userClient.getRoles());
        assertThat(userDetails.getId()).isNotNull();
    }

    @Test
    public void searchNOUser(){
        UserDataDTO userAdmin = getValidUserForSignup();
        String signupToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userAdmin,
                String.class);
        assertThat(signupToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + signupToken));

        ResponseEntity<UserDataDTO> searchUsernameResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + searchUrl)
                        .queryParam("username", "maxim")
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserDataDTO.class);

        assertThat(searchUsernameResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    }

    @Test
    public void  refreshToken(){
        UserDataDTO userClient = getValidUserForDelete();
        String signupToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userClient,
                String.class);
        assertThat(signupToken).isNotBlank();

        String sigintoken = this.restTemplate.postForObject(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + signinUrl)
                        .queryParam("username", userClient.getUsername())
                        .queryParam("password", userClient.getPassword())
                        .build().encode().toUri(),
                HttpEntity.EMPTY,
                String.class);
        assertThat(sigintoken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + sigintoken));

        ResponseEntity<UserResponseDTO> whoAmIResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails = whoAmIResponse.getBody();

        assertThat(userDetails.getUsername()).isEqualTo(userClient.getUsername());
        assertThat(userDetails.getEmail()).isEqualTo(userClient.getEmail());
        assertThat(userDetails.getRoles()).isEqualTo(userClient.getRoles());
        assertThat(userDetails.getId()).isNotNull();

        String refreshUrlToken = this.restTemplate.postForObject(
                getBaseUrl() + refreshUrl,
                userClient,
                String.class);
        assertThat(refreshUrlToken).isNotBlank();

        HttpHeaders headers1 = new HttpHeaders();
        headers1.put("Authorization", Collections.singletonList("Bearer " + refreshUrlToken));

        ResponseEntity<UserResponseDTO> refreshResponse = this.restTemplate.exchange(
                UriComponentsBuilder.fromHttpUrl(getBaseUrl() + whoamiUrl)
                        .build().encode().toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class);

        assertThat(whoAmIResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponseDTO userDetails1 = refreshResponse.getBody();

        assertThat(userDetails1.getUsername()).isEqualTo(userClient.getUsername());
        assertThat(userDetails1.getEmail()).isEqualTo(userClient.getEmail());
        assertThat(userDetails1.getRoles()).isEqualTo(userClient.getRoles());
        assertThat(userDetails1.getId()).isNotNull();


    }
    @Test
    public void refreshNoUserToken(){
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
    public void similarEmail(){
        UserDataDTO userOne = getValidUserForEmail1();
        UserDataDTO userTwo = getValidUserForEmail2();

        String signupToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userOne,
                String.class);

        assertThat(signupToken).isNotBlank();

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
    public void similarUsername(){
        UserDataDTO userOne = getValidUserForUsername1();
        UserDataDTO userTwo = getValidUserForUsername2();

        String signupToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                userOne,
                String.class);

        assertThat(signupToken).isNotBlank();


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
    public void updateDataBase(){
        UserDataDTO user = new UserDataDTO();
        String firstName = "SOFT";
        String lastName = "SOFT";

        String signupToken = this.restTemplate.postForObject(
                getBaseUrl() + signupUrl,
                user,
                String.class);
        assertThat(signupToken).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Collections.singletonList("Bearer " + signupToken));

        ResponseEntity<UserResponseDTO> responce = this.restTemplate.exchange(
            getBaseUrl() + updateBaseUrl,
                HttpMethod.PUT,
                new HttpEntity<>(headers),
                UserResponseDTO.class
        );



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

    private UserDataDTO getValidUserForEmail1() {
        return new UserDataDTO(
                 "softkit",
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
                "maxim",
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
                "softkit",
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
                "SOFTkit",
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
