package com.ridehailing.ridematch.dto;

import com.ridehailing.ridematch.entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String phone;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .build();
    }
}