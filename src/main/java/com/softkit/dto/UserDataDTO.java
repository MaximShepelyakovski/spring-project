package com.softkit.dto;

import com.softkit.annotation.ValidPassword;
import com.softkit.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataDTO {

    private String username;
    @ValidPassword
    private String password;
    @Email(message = "Email not valid")
    private String email;
    private String firstName;
    private String lastName;
    private Date birthday;
    private Date registrationDate;
    private String photos;
    private List<Role> roles;

}
