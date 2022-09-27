package com.softkit.mapper;

import com.softkit.dto.InviteResponseDto;
import com.softkit.model.Invite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InviteMapper {

    @Mapping(target ="id",source = "id" )
    @Mapping(target ="email",source = "email" )
    @Mapping(target ="dateSentInvite",source = "dateSentInvite" )
    @Mapping(target ="status",source = "status" )
    InviteResponseDto inviteToInviteResponseDto(Invite e);

}
