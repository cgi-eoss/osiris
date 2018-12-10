package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.model.Group;
import com.cgi.eoss.osiris.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/currentUser")
public class CurrentUserApi {

    private final OsirisSecurityService osirisSecurityService;

    @Autowired
    public CurrentUserApi(OsirisSecurityService osirisSecurityService) {
        this.osirisSecurityService = osirisSecurityService;
    }

    @GetMapping
    public User currentUser() {
        return osirisSecurityService.getCurrentUser();
    }

    @GetMapping("/grantedAuthorities")
    public List<String> grantedAuthorities() {
        return osirisSecurityService.getCurrentAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    @GetMapping("/groups")
    public Set<Group> groups() {
        return osirisSecurityService.getCurrentGroups();
    }

}
