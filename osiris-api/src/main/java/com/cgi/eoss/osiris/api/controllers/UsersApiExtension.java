package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>A {@link RepositoryRestController} for interacting with {@link User}s. Offers additional functionality over
 * the standard CRUD-style {@link UsersApi}.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/users")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class UsersApiExtension {

    private final OsirisSecurityService osirisSecurityService;

    @GetMapping("/current")
    public ResponseEntity currentUser() {
        return ResponseEntity.ok(new Resource<>(osirisSecurityService.getCurrentUser()));
    }

    @GetMapping("/current/groups")
    public ResponseEntity currentGroups() {
        return ResponseEntity.ok(Resources.wrap(osirisSecurityService.getCurrentGroups()));
    }

    @GetMapping("/current/grantedAuthorities")
    public List<String> grantedAuthorities() {
        return osirisSecurityService.getCurrentAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

}
