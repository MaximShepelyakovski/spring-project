package com.softkit;


import com.google.common.collect.Lists;
import com.softkit.dto.UserDataDTO;
import com.softkit.exception.CustomException;
import com.softkit.model.Role;
import com.softkit.model.User;
import com.softkit.repository.InviteRepository;
import com.softkit.repository.UserRepository;
import com.softkit.service.EmailSenderService;
import com.softkit.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Date;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@SpringBootTest(classes = {StarterApplication.class})
public class UserServiceWithMockTests {

    @Autowired
    private UserService userService;

    //    mocking all repository calls
    @MockBean
    private UserRepository userRepository;




    /**
     * this method is mocking call to database, to simulate that user already exists
     * even if database is not set (so we don't have it up and running)
     * so you can change return value to false and then it will try to signup
     */
    @Test
    public void checkThatWithMockingWeCantDoEvenAFirstSignup() {

        String username = "username";
//        always exists
        Mockito.doReturn(true).when(this.userRepository).existsByUsername(Mockito.eq(username));

        try {
//        username must be the same, because of our above rule for mocking
            userService.signup(new User(null,
                    username,
                    "HeisenbuG1!",
                    "maxim.shepelyakovski@gmail.com",
                    null,
                    "maxim",
                    "maxim",
                    Date.valueOf("2003-01-11"),
                    Date.valueOf("2000-01-11"),
                    "photo",
                    Lists.newArrayList(Role.ROLE_CLIENT),
                    "asdasdas",
                    false));        } catch (CustomException e) {
            assertThat(e.getMessage()).isEqualTo("Username is already in use");
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }

    }


    @Test
    public void checkCallMethodEmailSenderServiceTest(){

        EmailSenderService emailSenderService = Mockito.mock(EmailSenderService.class);

        User user = new User(null,
                "SOFTkit",
                "HeisenbuG1!",
                "maxim.shepelyakovski@gmail.com",
                null,
                "maxim",
                "maxim",
                Date.valueOf("2003-01-11"),
                Date.valueOf("2000-01-11"),
                "photo",
                Lists.newArrayList(Role.ROLE_CLIENT),
                "asdasdas",
                false);

        userService.signup(user);

        String url = "http://localhost:8080/users/verify?code=" + userRepository.findByUsername(user.getUsername()).getVerificationCode();
        String subject = "Thank you for registering";
        String body = "To successfully complete the registration, confirm the email.\n"+url;

        Mockito.verify(emailSenderService).sendSimpleEmail(user.getEmail(), body, subject);

    }

}
