package com.softkit.mapper;

import com.softkit.dto.UserDataDTO;
import com.softkit.dto.UserResponseDTO;
import com.softkit.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target ="username",source = "username" )
    @Mapping(target ="email",source = "email" )
    @Mapping(target ="password",source = "password" )
    @Mapping(target ="firstName",source = "firstName" )
    @Mapping(target ="lastName",source = "lastName" )
    @Mapping(target ="birthday",source = "birthday" )
    @Mapping(target ="roles",source = "roles" )
    User mapUserDataToUser(UserDataDTO e);

    UserResponseDTO mapUserToResponse(User e);
}
