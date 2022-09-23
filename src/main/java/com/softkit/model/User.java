package com.softkit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity(name = "Users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Size(min = 4, max = 255, message = "Minimum username length: 4 characters")
    @Column(unique = true, nullable = false)
    private String username;

    @Size(min = 8, message = "Minimum password length: 8 characters")
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "UpdateEmail",nullable = true)
    private String updateEmail;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date birthday;

    @Temporal(TemporalType.DATE)
    @Column(nullable = true)
    private Date registrationDate;

    @Column(nullable = true, length = 64)
    private String photos;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Role> roles;

    @Column(name = "verification_code",nullable = true)
    private String verificationCode;

    @Column(name = "enabled",nullable = true)
    private boolean enabled;

}
