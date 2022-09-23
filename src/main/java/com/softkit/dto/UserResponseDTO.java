package com.softkit.dto;

import com.softkit.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data

@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Integer id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Date birthday;
    private Date registrationDate;
    private String photos;
    private List<Role> roles;

}
