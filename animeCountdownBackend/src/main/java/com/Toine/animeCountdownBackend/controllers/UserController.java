package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/auth/user")
public class UserController {

    @GetMapping("/{id}")
    public UserEntity getUser(@PathVariable("id") UserEntity user){
        return user;
    }
}
