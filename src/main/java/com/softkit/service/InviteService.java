package com.softkit.service;

import com.google.common.collect.Lists;
import com.softkit.dto.InviteResponseDto;
import com.softkit.exception.CustomException;
import com.softkit.mapper.InviteMapper;
import com.softkit.model.Invite;
import com.softkit.model.Status;
import com.softkit.model.User;
import com.softkit.repository.InviteRepository;
import com.softkit.repository.UserRepository;
import com.softkit.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InviteService {
    @Autowired
    private final InviteRepository inviteRepository;
    @Autowired
    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final InviteMapper inviteMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public void inviteUser(String email, HttpServletRequest request){

        if (userRepository.existsByEmailIgnoreCase(email.toLowerCase())) {
            throw new CustomException("Email is already in use", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        User user = userRepository.findByUsername(jwtTokenProvider.getUsername(jwtTokenProvider.resolveToken(request)));

        Invite inviteUser = new Invite();

        inviteUser.setEmail(email);
        inviteUser.setIdUser(user.getId());
        inviteUser.setDateSentInvite(Date.valueOf(LocalDate.now()));
        inviteUser.setStatus(Lists.newArrayList(Status.PENDING));

        inviteRepository.save(inviteUser);

        String url = "http://localhost:8080/users/signup";
        String subject = "Invite";
        String body = "You have been invited by " + user.getUsername() + " to register on our website. Follow the link and register:\n"+url;
        emailSenderService.sendSimpleEmail(email, body, subject);

    }

    public List<InviteResponseDto> listAllInviteUsers(HttpServletRequest request){

        User user = userRepository.findByUsername(jwtTokenProvider.getUsername(jwtTokenProvider.resolveToken(request)));

        List<InviteResponseDto> inviteUserResponse = new ArrayList<>();
        List<Invite> inviteUsers = new ArrayList<>();

        int page = 0,size = 3;

        int totalpages = -1;
        do {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<Invite> inviteUserPage = inviteRepository.findByIdUserOrderByDateSentInviteDescStatusDesc(user.getId(),pageRequest);
            inviteUsers.addAll(inviteUserPage.getContent());
            totalpages = inviteUserPage.getTotalPages();
            page++;
            inviteUserPage.nextPageable();
        }
        while(page < totalpages);

        userRepository.findById(user.getId());

        for(Invite invite : inviteUsers){
            inviteUserResponse.add(inviteMapper.inviteToInviteResponseDto(invite));
        }

        return inviteUserResponse;
    }

}
