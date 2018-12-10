package com.cgi.eoss.osiris.security;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

public enum OsirisPermission {

    READ,
    WRITE,
    ADMIN,
    /**
     * <p>A marker permission for API access information; not used to map Spring ACLs.</p>
     */
    SUPERUSER;

    private static final BiMap<OsirisPermission, Set<Permission>> SPRING_OSIRIS_PERMISSION_MAP = ImmutableBiMap.<OsirisPermission, Set<Permission>>builder()
            .put(OsirisPermission.READ, ImmutableSet.of(BasePermission.READ))
            .put(OsirisPermission.WRITE, ImmutableSet.of(BasePermission.WRITE, BasePermission.READ))
            .put(OsirisPermission.ADMIN, ImmutableSet.of(BasePermission.ADMINISTRATION, BasePermission.WRITE, BasePermission.READ))
            .build();

    /**
     * <p>A Spring Security GrantedAuthority for PUBLIC visibility. Not technically an OsirisPermission enum value, but
     * may be treated similarly.</p>
     */
    public static final GrantedAuthority PUBLIC = new SimpleGrantedAuthority("PUBLIC");

    public Set<Permission> getAclPermissions() {
        return SPRING_OSIRIS_PERMISSION_MAP.get(this);
    }

    public static OsirisPermission getOsirisPermission(Set<Permission> aclPermissions) {
        return SPRING_OSIRIS_PERMISSION_MAP.inverse().get(aclPermissions);
    }

}
