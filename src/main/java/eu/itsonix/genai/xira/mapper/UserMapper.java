package eu.itsonix.genai.xira.mapper;

import java.util.UUID;

import eu.itsonix.genai.xira.jpa.entity.XiraUser;
import eu.itsonix.genai.xira.web.model.UserResponse;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toUserResponse(final XiraUser user) {
        return new UserResponse()
                .id(UUID.fromString(user.getId()))
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName());
    }
}
