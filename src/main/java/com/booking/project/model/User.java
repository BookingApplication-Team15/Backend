package com.booking.project.model;

import com.booking.project.dto.UserCredentialsDTO;
import com.booking.project.dto.UserDTO;
import com.booking.project.dto.UserRegisterDTO;
import com.booking.project.model.enums.UserStatus;
import com.booking.project.model.enums.UserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(nullable = false)
    private boolean isReported;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Transient
    private String jwt;

    public User(UserDTO userDTO){
        this.id = userDTO.getId();
        this.email = userDTO.getEmail();
        this.password = userDTO.getPassword();
        this.userType = userDTO.getUserType();
        this.isReported = userDTO.isReported();
        this.status = userDTO.getStatus();
    }

    public User(UserCredentialsDTO userCredentialsDTO){
        this.id = userCredentialsDTO.getId();
        this.email = userCredentialsDTO.getEmail();
        this.password = userCredentialsDTO.getPassword();
        this.userType = userCredentialsDTO.getUserType();
        this.status = UserStatus.ACTIVE;
    }

    public void copyValues(UserCredentialsDTO userCredentialsDTO){
        this.email = userCredentialsDTO.getEmail();
        this.password = userCredentialsDTO.getPassword();
    }
    public User(UserRegisterDTO userRegisterDTO){
        this.email = userRegisterDTO.getEmail();
        this.password = userRegisterDTO.getPassword();
        this.userType = userRegisterDTO.getUserType();
        this.isReported = false;
        this.setStatus(UserStatus.PENDING);
    }
}
