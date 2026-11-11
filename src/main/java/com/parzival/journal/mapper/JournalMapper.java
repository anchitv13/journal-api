package com.parzival.journal.mapper;

import com.parzival.journal.entity.JournalEntity;
import com.parzival.journal.entity.UserEntity;
import com.parzival.journal.resource.JournalRequest;
import com.parzival.journal.resource.JournalResponse;
import org.mapstruct.*;

import java.time.Instant;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        imports = Instant.class
)
public interface JournalMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    JournalEntity toEntity(JournalRequest journalRequest, UserEntity user);

    JournalResponse toResponse(JournalEntity journalEntity);

    @BeanMapping(nullValuePropertyMappingStrategy =
            NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    void updateEntity(
            JournalRequest request,
            @MappingTarget JournalEntity entity
    );
}