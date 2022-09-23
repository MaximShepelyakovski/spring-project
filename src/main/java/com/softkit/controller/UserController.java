package com.softkit.controller;

import com.softkit.dto.UserDataDTO;
import com.softkit.dto.UserResponseDTO;
import com.softkit.mapper.UserMapper;
import com.softkit.service.UserService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Api(tags = "users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/signin")
    @ApiOperation(value = "${UserController.signin}")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 422, message = "Invalid username/password supplied")})
    public String login(
            @ApiParam("Username") @RequestParam String username,
            @ApiParam("Password") @RequestParam String password) {
        return userService.signin(username, password);
    }

    @PostMapping("/signup")
    @ApiOperation(value = "${UserController.signup}")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 422, message = "Username is already in use")})
    public ResponseEntity<String> signup(@ApiParam("Signup User") @Valid @RequestBody UserDataDTO user) {
        userService.signup(userMapper.mapUserDataToUser(user));
        return ResponseEntity.ok("An email confirmation link has been sent to you.");
    }

    @GetMapping("/verify")
    @ApiOperation(value = "${UserController.verify}")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong")})
    public void verify(@ApiParam("code") @RequestParam String code) {
        userService.verify(code);
    }



    @GetMapping(value = "/me")
    @PreAuthorize("hasRole('ROLE_CLIENT') or hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.me}", response = UserResponseDTO.class, authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public UserResponseDTO whoami(HttpServletRequest request) {
        return userMapper.mapUserToResponse(userService.whoami(request));
    }

    @DeleteMapping(value = "/delete")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.delete}",authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<Void> deleteUsername(
            @ApiParam("username") @RequestParam String username){

        userService.deleteUsername(username);

        return new ResponseEntity<>(HttpStatus.OK);
    }
    @GetMapping(value = "/search")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.search}",authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public UserResponseDTO searchByUsername(
            @ApiParam("username") @RequestParam String username){
        return userMapper.mapUserToResponse(userService.search(username));
    }

    @PostMapping(value = "/refresh")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_CLIENT')")
    @ApiOperation(value = "${UserController.search}",response = UserResponseDTO.class,authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public String refreshToken(HttpServletRequest request){
        return userService.refresh(request);
    }


    @PostMapping(value = "/images")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_CLIENT')")
    @ApiOperation(value = "${UserController.images}",response = UserResponseDTO.class,authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<String> postImage(@RequestParam("image") MultipartFile multipartFile,HttpServletRequest request) throws IOException {
        userService.saveImage(multipartFile,request);
        return ResponseEntity.ok("Success");
    }

    @GetMapping(value = "/images")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_CLIENT')")
    @ApiOperation(value = "${UserController.images}",response = UserResponseDTO.class,authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<InputStreamResource> getImage(HttpServletRequest request) throws IOException {

        File imageFile = userService.getImage(request);

        HttpHeaders headers = new HttpHeaders();
        headers.add("content-disposition", "inline;filename=" +imageFile.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(imageFile.length())
                .contentType(MediaType.parseMediaType("image/jpeg"))
                .body(new InputStreamResource(new FileInputStream(imageFile)));
    }

    @PutMapping(value = "/update")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_CLIENT')")
    @ApiOperation(value = "${UserController.update}",response = UserResponseDTO.class,authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<String> baseUpdateData(@ApiParam("firstName") @RequestParam String firstName,
                                                 @ApiParam("lastName") @RequestParam String lastName,
                                                 HttpServletRequest request)  {
        userService.updateData(firstName,lastName,request);
        return ResponseEntity.ok("Success");
    }

    @PutMapping(value = "/admin/update")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.update}",response = UserResponseDTO.class,authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<String> adminUpdateData(@ApiParam("username") @RequestParam String username,
                                                  @ApiParam("firstName") @RequestParam String firstName,
                                                  @ApiParam("lastName") @RequestParam String lastName)  {
        userService.adminUpdateData(username,firstName,lastName);
        return ResponseEntity.ok("Success");
    }

    @PostMapping(value = "/update/email")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_CLIENT')")
    @ApiOperation(value = "${UserController.updateEmail}",response = UserResponseDTO.class,authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<String> updateEmail(@ApiParam("email") @RequestParam String email,HttpServletRequest request) {
        userService.updateEmail(email,request);
        return ResponseEntity.ok("Success");
    }

    @PostMapping("/verify/email")
    @ApiOperation(value = "${UserController.verifyEmail}")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong")})
    public void verifyEmail(@ApiParam("code") @RequestParam String code) {
        userService.verifyEmail(code);
    }

    @GetMapping(value = "/exportCSV")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.exportCVS}", response = UserResponseDTO.class, authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<InputStreamResource> exportCSV(@ApiParam("page") @RequestParam Integer page,
                                                         @ApiParam("size") @RequestParam Integer size) throws FileNotFoundException {

        File csvFile = userService.exportCSV(page,size);

        HttpHeaders headers = new HttpHeaders();
        headers.add("content-disposition", "inline;filename=" +csvFile.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(csvFile.length())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(new FileInputStream(csvFile)));

    }


    @PostMapping(value = "/importCSV")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.importCSV}",response = UserResponseDTO.class,authorizations = {@Authorization(value = "apiKey")})
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public ResponseEntity<String> importCSV(@RequestParam("csv") MultipartFile multipartFile) throws IOException {
       String body = userService.importCSV(multipartFile);
       return ResponseEntity.ok(body);
    }




}
