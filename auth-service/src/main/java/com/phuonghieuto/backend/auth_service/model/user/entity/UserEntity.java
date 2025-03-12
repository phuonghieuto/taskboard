package com.phuonghieuto.backend.auth_service.model.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.phuonghieuto.backend.auth_service.model.common.entity.BaseEntity;
import com.phuonghieuto.backend.auth_service.model.user.enums.TokenClaims;
import com.phuonghieuto.backend.auth_service.model.user.enums.UserStatus;
import com.phuonghieuto.backend.auth_service.model.user.enums.UserType;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@ToString
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private String id;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(
            name = "PHONE_NUMBER",
            length = 20
    )
    private String phoneNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private UserType userType = UserType.USER;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Builder.Default
    @Column(name = "EMAIL_CONFIRMED")
    private boolean emailConfirmed = false;

    @Builder.Default
    @Column(name = "CONFIRMATION_TOKEN")
    private String confirmationToken = UUID.randomUUID().toString();

    @Builder.Default
    @Column(name = "CONFIRMATION_TOKEN_EXPIRY")
    private LocalDateTime confirmationTokenExpiry = LocalDateTime.now().plusHours(24);

    /**
     * Constructs a map of claims based on the user's attributes.
     * This map is typically used to create JWT claims for the user.
     * @return a map of claims containing user attributes
     */
    public Map<String, Object> getClaims() {

        final Map<String, Object> claims = new HashMap<>();

        claims.put(TokenClaims.USER_ID.getValue(), this.id);
        claims.put(TokenClaims.USER_USERNAME.getValue(), this.username);
        claims.put(TokenClaims.USER_TYPE.getValue(), this.userType);
        claims.put(TokenClaims.USER_STATUS.getValue(), this.userStatus);
        claims.put(TokenClaims.USER_FIRST_NAME.getValue(), this.firstName);
        claims.put(TokenClaims.USER_LAST_NAME.getValue(), this.lastName);
        claims.put(TokenClaims.USER_EMAIL.getValue(), this.email);
        claims.put(TokenClaims.USER_PHONE_NUMBER.getValue(), this.phoneNumber);

        return claims;

    }

}
