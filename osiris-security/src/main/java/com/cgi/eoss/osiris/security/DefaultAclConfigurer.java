package com.cgi.eoss.osiris.security;

import com.cgi.eoss.osiris.model.OsirisEntity;
import com.cgi.eoss.osiris.model.OsirisEntityWithOwner;
import com.cgi.eoss.osiris.model.Project;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.ProjectDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;

/**
 * <p>A bean to provide one-time initialisation (and restoration) of any required access control entries.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class DefaultAclConfigurer {

    private final ProjectDataService projectDataService;
    private final MutableAclService aclService;

    // TODO Investigate why transactions must be managed manually
    private final PlatformTransactionManager txManager;

    @PostConstruct
    public void initAcls() throws Exception {
        Project defaultProject = projectDataService.refresh(Project.DEFAULT);
        ensureAcl(Project.class, defaultProject, OsirisPermission.PUBLIC, OsirisPermission.READ);
    }

    /**
     * <p>Check each associated entry in the Access Control List for the given entity, and ensure that it is added to
     * the list if it's not present.</p>
     * <p>This method <em>does not</em> remove existing ACEs.</p>
     */
    private void ensureAcl(Class<? extends OsirisEntity> entityClass, OsirisEntity entity, GrantedAuthority authority, OsirisPermission permission) {
        PrincipalSid ownerSid = OsirisEntityWithOwner.class.isAssignableFrom(entityClass)
                ? new PrincipalSid(User.DEFAULT.getName())
                : new PrincipalSid(((OsirisEntityWithOwner) entity).getOwner().getName());

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(OsirisSecurityService.PUBLIC_AUTHENTICATION);
        }

        GrantedAuthoritySid sid = new GrantedAuthoritySid(authority);
        ObjectIdentity objectIdentity = new ObjectIdentityImpl(entityClass, entity.getId());
        MutableAcl acl = getAcl(objectIdentity);
        acl.setOwner(ownerSid);

        permission.getAclPermissions().stream()
                .filter(p -> acl.getEntries().stream().noneMatch(ace -> ace.getSid().equals(sid) && ace.getPermission().equals(p)))
                .forEach(p -> acl.insertAce(acl.getEntries().size(), p, sid, true));
        new TransactionTemplate(txManager).execute(s -> aclService.updateAcl(acl));
    }

    private MutableAcl getAcl(ObjectIdentity objectIdentity) {
        try {
            return (MutableAcl) aclService.readAclById(objectIdentity);
        } catch (NotFoundException nfe) {
            return new TransactionTemplate(txManager).execute(s -> aclService.createAcl(objectIdentity));
        }
    }

}
