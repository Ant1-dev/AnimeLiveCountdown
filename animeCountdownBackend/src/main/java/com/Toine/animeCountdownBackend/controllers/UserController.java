package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("api/auth/user")
public class UserController {

    @GetMapping("/{id}")
    @PreAuthorize("#user.id == authentication.principal.attributes['id']")
    public UserEntity getUser(@PathVariable("id") UserEntity user){
        return user;
    }

    @GetMapping("/test")
    public String testAuth() {
        return "<h1>Are you logged in? </h1>";
    }
}
