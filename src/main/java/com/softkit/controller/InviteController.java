package com.softkit.controller;


import com.softkit.dto.InviteResponseDto;
import com.softkit.dto.UserResponseDTO;
import com.softkit.service.InviteService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/invite")
@Api(tags = "invite")
public class InviteController {

    @Autowired
    private final InviteService inviteService;

    @PostMapping(value = "/user")
    @PreAuthorize("hasRole('ROLE_CLIENT') or hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.inviteUser}", response = UserResponseDTO.class, authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<String> inviteUser(@ApiParam("email") @RequestParam String email,
                                             HttpServletRequest request) {
        inviteService.inviteUser(email, request);

        return ResponseEntity.ok("Success");
    }

    @GetMapping(value = "/users")
    @PreAuthorize("hasRole('ROLE_CLIENT') or hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.inviteUsers}", response = UserResponseDTO.class, authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<List<InviteResponseDto>> inviteUserAll(HttpServletRequest request) {
        return new ResponseEntity<>(inviteService.listAllInviteUsers(request), HttpStatus.OK);
    }



}
