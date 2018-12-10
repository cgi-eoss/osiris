package com.cgi.eoss.osiris.security;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.OsirisEntityWithOwner;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;
import com.cgi.eoss.osiris.model.Group;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.Wallet;
import com.cgi.eoss.osiris.model.WalletTransaction;
import com.google.common.collect.ImmutableSet;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import lombok.extern.log4j.Log4j2;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@Log4j2
// TODO Replace this with an AOP aspect
public class AddOwnerAclListener implements PostInsertEventListener {

    /**
     * <p>Classes which implement {@link OsirisEntityWithOwner} but should not be configured with an access control
     * list.</p>
     */
    private static final Set<Class> NON_ACL_CLASSES = ImmutableSet.of(
            OsirisServiceContextFile.class,
            Wallet.class,
            WalletTransaction.class
    );

    private final EntityManagerFactory entityManagerFactory;
    private final OsirisSecurityService osirisSecurityService;

    public AddOwnerAclListener(EntityManagerFactory entityManagerFactory, OsirisSecurityService osirisSecurityService) {
        this.entityManagerFactory = entityManagerFactory;
        this.osirisSecurityService = osirisSecurityService;
    }

    @PostConstruct
    protected void registerSelf() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        Class<?> entityClass = event.getEntity().getClass();
        if (OsirisEntityWithOwner.class.isAssignableFrom(entityClass) && !NON_ACL_CLASSES.contains(entityClass)) {
            OsirisEntityWithOwner entity = (OsirisEntityWithOwner) event.getEntity();

            // The owner should be User.DEFAULT for EXTERNAL_PRODUCT OsirisFiles, otherwise the actual owner may be used
            PrincipalSid ownerSid =
                    OsirisFile.class.equals(entityClass) && ((OsirisFile) entity).getType() == OsirisFile.Type.EXTERNAL_PRODUCT
                            ? new PrincipalSid(User.DEFAULT.getName())
                            : new PrincipalSid(entity.getOwner().getName());

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(OsirisSecurityService.PUBLIC_AUTHENTICATION);
            }

            ObjectIdentity objectIdentity = new ObjectIdentityImpl(entityClass, entity.getId());
            MutableAcl acl = osirisSecurityService.getAcl(objectIdentity);
            acl.setOwner(ownerSid);

            if (acl.getEntries().size() > 0) {
                LOG.warn("Existing access control entries found for 'new' object: {} {}", entityClass.getSimpleName(), entity.getId());
            }

            if (Group.class.equals(entityClass)) {
                // Group members should be able to READ their groups
                LOG.debug("Adding self-READ ACL for new Group with ID {}", entity.getId());
                OsirisPermission.READ.getAclPermissions()
                        .forEach(p -> acl.insertAce(acl.getEntries().size(), p, new GrantedAuthoritySid((Group) entity), true));
            }
            
            

            if (OsirisFile.class.equals(entityClass) && ((OsirisFile) entity).getType() == OsirisFile.Type.EXTERNAL_PRODUCT) {
                // No one should have ADMIN permission for EXTERNAL_PRODUCT OsirisFiles, but they should be PUBLIC to read ...
                LOG.debug("Adding PUBLIC READ-level ACL for new EXTERNAL_PRODUCT OsirisFile with ID {}", entity.getId());
                OsirisPermission.READ.getAclPermissions()
                        .forEach(p -> acl.insertAce(acl.getEntries().size(), p, new GrantedAuthoritySid(OsirisPermission.PUBLIC), true));
            } else {
                // ... otherwise, the owner should have ADMIN permission for the entity
                LOG.debug("Adding owner-level ACL for new {} with ID {} (owner: {})", entityClass.getSimpleName(), entity.getId(), entity.getOwner().getName());
                OsirisPermission.ADMIN.getAclPermissions()
                        .forEach(p -> acl.insertAce(acl.getEntries().size(), p, ownerSid, true));
            }
            
            if (OsirisFile.class.equals(entityClass) && ((OsirisFile) entity).getType() == OsirisFile.Type.OUTPUT_PRODUCT) {
                // Osiris output products should have the collection as parent ACL
                LOG.debug("Adding PARENT ACL for new OUTPUT_PRODUCT OsirisFile with ID {}", entity.getId());
                OsirisFile osirisFile = (OsirisFile) entity;
                if (osirisFile.getCollection() != null) {
                    acl.setParent(osirisSecurityService.getAcl(new ObjectIdentityImpl(Collection.class, osirisFile.getCollection().getId())));
                }
            }

            osirisSecurityService.saveAcl(acl);
        }
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return true;
    }

}
