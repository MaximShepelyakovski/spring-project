package com.softkit;

import com.google.common.collect.Lists;
import com.softkit.exception.CustomException;
import com.softkit.model.Role;
import com.softkit.model.User;
import com.softkit.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.sql.Date;


/**
 * This class is testing the service itself
 * and as you can see on the service level we don't have such strict rules (email, password validation, etc... )
 * so all interactions with the application must be through HTTP layer, that's very important to keep data clean
 */

@SpringBootTest(classes = {StarterApplication.class})
public class UserServiceTests {

    @Autowired
    private UserService userService;

    @Test
    public void successUserSignupTest() {
        userService.signup(new User(null,
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
                false));

        try {
            userService.signup(new User(null,
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
                    false));
        } catch (CustomException e) {
            assertThat(e.getMessage()).isEqualTo("Username is already in use");
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }

    }

}
