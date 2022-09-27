package com.softkit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity(name = "Invite")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invite implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private Integer idUser;

    @Temporal(TemporalType.DATE)
    @Column(nullable = true)
    private Date dateSentInvite;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Status> status;

}
