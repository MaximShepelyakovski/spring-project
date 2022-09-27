package com.softkit;

import com.softkit.dto.UserDataDTO;
import com.softkit.dto.UserResponseDTO;
import com.softkit.mapper.UserMapper;
import com.softkit.model.Role;
import com.softkit.model.User;
import com.softkit.repository.UserRepository;
import com.softkit.service.UserService;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;

import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class CacheLogicTest  extends AbstractControllerTest{


    @Autowired
    //@Mock
    private UserRepository userRepository;

    @Autowired
    //@Mock
    UserMapper userMapper;

    //@Spy
   // @InjectMocks
    @Autowired
    private UserService userService;

    private final String signupUrl = "/users/signup";
    private final String signinUrl = "/users/signin";
    private final String verifyUrl = "/users/verify";
    private final String searchUrl = "/users/search";
    private final String updateBaseUrl = "/users/update";


    @Test
    public void cacheTestSearch(){

        User userForSignup = userRepository.save(userMapper.mapUserDataToUser(getValidUserForSignup()));
        for (int i = 0; i < 10; i++) {
            User user = userService.search(userForSignup.getUsername());
        }
       // verify(userService).search(userForSignup.getUsername());


   /*     User userForSignup = new User();
        when(userService.search("username")).thenReturn(userForSignup);
        Mockito.verify(userService, Mockito.times(1)).search("username");

*/
    }

    @Test
    public void cacheEvictUpdateDataTest(){
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

        System.out.println("BEGIN__________________________________");

        int count=0;
        for (int i = 0; i < 10; i++) {
            User user = userService.search(userAdmin.getUsername());
            count++;
        }
        System.out.println("END_______________________________________");


        String firstName = "SOFT";
        String lastName = "SOFT";
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

        System.out.println("BEGIN____________________________________");

        count=0;
        for (int i = 0; i < 10; i++) {
            User user = userService.search(userAdmin.getUsername());
            count++;
        }

        System.out.println("END_______________________________________");


    }
    private UserDataDTO getValidUserForSignup() {
        UUID randomUUID = UUID.randomUUID();
        return new UserDataDTO(
                 "softkit",
                 "HeisenbuG1!",
                 "softkit@yahoo.com",
                 "softkit",
                 "softkit",
                Date.valueOf("2003-01-11"),
                Date.valueOf(LocalDate.now()),
                "photo.image",
                Lists.newArrayList(Role.ROLE_ADMIN));
    }
}
