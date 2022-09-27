package com.softkit.dto;


import com.softkit.model.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteResponseDto {
    private Integer id;
    private String email;
    private Date dateSentInvite;
    private List<Status> status;
}