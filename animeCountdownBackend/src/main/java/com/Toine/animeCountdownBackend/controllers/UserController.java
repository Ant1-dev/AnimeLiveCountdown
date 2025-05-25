package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/auth/user")
public class UserController {
    private UserRepository userRepository;

    public UserController(UserRepository userRepository) { this.userRepository = userRepository;}

    @GetMapping("/{id}")
    @PreAuthorize("#user.id == authentication.principal.attributes['id']")
    public UserEntity getUser(@PathVariable("id") UserEntity user){
        return user;
    }

}
