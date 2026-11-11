package com.parzival.journal.mapper;

import com.parzival.journal.auth.resource.UserResponse;
import com.parzival.journal.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    UserResponse toResponse(UserEntity user);
}