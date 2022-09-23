package com.softkit.service;

import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.softkit.exception.CustomException;
import com.softkit.fileConfig.FileUploadUtil;
import com.softkit.mapper.UserMapper;
import com.softkit.model.Role;
import com.softkit.model.User;
import com.softkit.repository.UserRepository;
import com.softkit.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    private final UserMapper userMapper;

    @Autowired
    private EmailSenderService emailSenderService;

    @Autowired
    private Environment env;

    public User userFromToken(HttpServletRequest request){
        return userRepository.findByUsername(jwtTokenProvider.getUsername(jwtTokenProvider.resolveToken(request)));
    }


    public String signin(String username, String password) {
        try {
            if (userRepository.findByUsername(username).isEnabled()){
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
                return jwtTokenProvider.createToken(username, userRepository.findByUsername(username).getRoles());
            }
            else {
                throw  new CustomException("Not verified",HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } catch (AuthenticationException e) {
            throw new CustomException("Invalid username/password supplied", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public void signup(User user) {
        if (userRepository.existsByUsernameIgnoreCase(user.getUsername().toLowerCase())) {
            throw new CustomException("Username is already in use", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (userRepository.existsByEmailIgnoreCase(user.getEmail().toLowerCase())) {
            throw new CustomException("Email is already in use", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRegistrationDate(Date.valueOf(LocalDate.now()));
        user.setVerificationCode(UUID.randomUUID() + "");
        user.setEnabled(false);
        userRepository.save(user);

        String url = "http://localhost:8080/users/verify?code=" + user.getVerificationCode();
        String subject = "Thank you for registering";
        String body = "To successfully complete the registration, confirm the email.\n"+url;
        emailSenderService.sendSimpleEmail(user.getEmail(), body, subject);
    }

    public User whoami(HttpServletRequest request) {
        return userFromToken(request);
    }

    //  method must delete user, by username, throw appropriate exception is user doesn't exists
    public void deleteUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            userRepository.deleteById(userRepository.findByUsername(username).getId());
        }
        else {
            throw new CustomException("No such username", HttpStatus.UNPROCESSABLE_ENTITY);
        }

    }

    //  method must search user, by username, throw appropriate exception is user doesn't exists
    @Cacheable(value = "searchUser",key = "#username")
    public User search(String username) {
        if (userRepository.existsByUsername(username)) {
            return userRepository.findByUsername(username);
        }
        else {
            throw new CustomException("No such username", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

//  method must create a new access token, similar to login
    public String refresh(HttpServletRequest request) {
        User user = userFromToken(request);
        return jwtTokenProvider.createToken(user.getUsername(), user.getRoles());
    }
    @CacheEvict("searchUser")
    public void saveImage(MultipartFile multipartFile,HttpServletRequest request) throws IOException {

        User user = userFromToken(request);

        String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        user.setPhotos(fileName);
        userRepository.save(user);
        String uploadDir = env.getProperty("upload.path.image") + user.getId();
        FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);
    }

    @CacheEvict("searchUser")
    public File getImage(HttpServletRequest request){

        User user = userFromToken(request);

        return new File(env.getProperty("upload.path.image")+ user.getId() +"/"+ user.getPhotos());

    }

    //@CacheEvict("searchUser")
    public void updateData(String firstName,String lastName, HttpServletRequest request ){
        User user = userFromToken(request);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);
    }
    @CacheEvict("searchUser")
    public void adminUpdateData(String username,String firstName,String lastName){
        User userUpdate = userRepository.findByUsername(username);
        userUpdate.setFirstName(firstName);
        userUpdate.setLastName(lastName);
        userRepository.save(userUpdate);

    }
    @CacheEvict("searchUser")
    public void updateEmail(String email,HttpServletRequest request){

        User user = userFromToken(request);

        user.setUpdateEmail(email);
        userRepository.save(user);

        String url = "http://localhost:8080/users/verify/email?code=" + user.getVerificationCode();
        String subject = "Verify email";
        String body = "You wanted to change your email, please confirm your email.\n"+url;
        emailSenderService.sendSimpleEmail(user.getUpdateEmail(), body, subject);

    }

    public void verifyEmail(String code){
        if (userRepository.existsByVerificationCode(code)){
            User user = userRepository.findByVerificationCode(code);

            user.setEmail(user.getUpdateEmail());
            user.setUpdateEmail(null);
            userRepository.save(user);
        }
        else {
            throw new CustomException("No such user", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public File exportCSV(int page, int size){

        File file = new File(env.getProperty("upload.path.csv") + "usersCSV.csv");

        try(FileWriter outputfile = new FileWriter(file);){

            CSVWriter writer = new CSVWriter(outputfile);

            String[] header = { "Username","Email","First Name","Last Name","Birthday","Role" };
            writer.writeNext(header);


            List<String[]> users = new ArrayList<>();
            List<User> usersResult = new ArrayList<>();


            int totalpages = -1;
            do {
                PageRequest pageRequest = PageRequest.of(page, size);
                Page<User> user = userRepository.findAll(pageRequest);
                usersResult.addAll(user.getContent());
                totalpages = user.getTotalPages();
                page++;
                user.nextPageable();
            }
            while(page < totalpages);

            for (User userOne : usersResult){
                users.add(new String[]{userOne.getUsername(),
                                       userOne.getEmail(),
                                       userOne.getFirstName(),
                                       userOne.getLastName(),
                                       String.valueOf(userOne.getBirthday()),
                                       userOne.getRoles().toString()
                });
            }
            writer.writeAll(users);
            writer.flush();
            writer.close();
            return file;
        }
        catch (IOException e) {
            throw new CustomException("Not found file.", HttpStatus.NOT_FOUND);
        }
    }

    public String importCSV(MultipartFile multipartFile) throws IOException {

        if (!Objects.equals(multipartFile.getContentType(), "text/csv")) {
            throw new CustomException("This is not a csv file.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        BufferedReader fileReader = new BufferedReader(new InputStreamReader(multipartFile.getInputStream()));
        Integer countUsers = 0;

        try(CSVReader csvReader = new CSVReader(fileReader)){


            List<String[]> list = new ArrayList<>();
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                list.add(line);
            }
            for (int i=1; i<list.size(); i++) {
                    if (userRepository.existsByUsernameIgnoreCase(list.get(i)[0].toLowerCase())) {
                        continue;
                    }

                    if (userRepository.existsByEmailIgnoreCase(list.get(i)[1].toLowerCase())) {
                        continue;
                    }

                    User user = new User();

                    user.setUsername(list.get(i)[0]);
                    user.setPassword("HeisenbuG1!");
                    user.setEmail(list.get(i)[1]);
                    user.setFirstName(list.get(i)[2]);
                    user.setLastName(list.get(i)[3]);
                    user.setBirthday(Date.valueOf(list.get(i)[4]));

                    if (Objects.equals(list.get(i)[5], "[ROLE_ADMIN]")){
                        user.setRoles(Lists.newArrayList(Role.ROLE_ADMIN));
                    }
                    if (Objects.equals(list.get(i)[5], "[ROLE_CLIENT]")){
                        user.setRoles(Lists.newArrayList(Role.ROLE_CLIENT));
                    }
                    signup(user);
                    countUsers++;

            }
        }
        catch (IOException e) {
            throw new CustomException("File read error", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        String response = "The number of users who managed to invite: "
                        + countUsers + "\n"
                        +"The number of users that are already in the system: "
                        +userRepository.count()
                        +"( "
                        + userRepository.countByEnabledIsTrue()
                        + " of them are verified)";
        return response;
    }

    public void verify(String code) {
        if (userRepository.existsByVerificationCode(code)){
            User user = userRepository.findByVerificationCode(code);
            user.setEnabled(true);
            userRepository.save(user);
        }
        else {
            throw new CustomException("No such user", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
