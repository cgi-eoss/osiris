package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.Databasket;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.Group;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.Project;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.UserEndpoint;
import com.cgi.eoss.osiris.model.UserMount;
import com.cgi.eoss.osiris.persistence.service.GroupDataService;
import com.cgi.eoss.osiris.security.OsirisPermission;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jooq.lambda.Seq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * <p>A {@link RestController} for updating and modifying ACLs. There is no direct model mapping, so this is not a
 * {@link org.springframework.data.rest.core.annotation.RepositoryRestResource}.</p>
 */
@RestController
@RequestMapping("/acls")
@Log4j2
public class AclsApi {

    /**
     * <p>Collate a collection of {@link AccessControlEntry}s into a set of permissions, and transform that set into its
     * corresponding {@link OsirisPermission}.</p>
     */
    private static final Collector<AccessControlEntry, ?, OsirisPermission> SPRING_OSIRIS_ACL_SET_COLLECTOR =
            Collectors.collectingAndThen(Collectors.mapping(AccessControlEntry::getPermission, Collectors.toSet()), OsirisPermission::getOsirisPermission);

    private final OsirisSecurityService osirisSecurityService;
    private final GroupDataService groupDataService;

    @Autowired
    public AclsApi(OsirisSecurityService osirisSecurityService, GroupDataService groupDataService) {
        this.osirisSecurityService = osirisSecurityService;
        this.groupDataService = groupDataService;
    }

    @PostMapping("/databasket/{databasketId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#databasket, 'administration')")
    public void setDatabasketAcl(@ModelAttribute("databasketId") Databasket databasket, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(databasket.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", databasket.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Databasket.class, databasket.getId()), databasket.getOwner(), acl.getPermissions());
    }

    @GetMapping("/databasket/{databasketId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#databasket, 'administration')")
    public OsirisAccessControlList getDatabasketAcls(@ModelAttribute("databasketId") Databasket databasket) {
        return OsirisAccessControlList.builder()
                .entityId(databasket.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(Databasket.class, databasket.getId())))
                .build();
    }

    @PostMapping("/osirisFile/{osirisFileId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#osirisFile, 'administration')")
    public void setOsirisFileAcl(@ModelAttribute("osirisFileId") OsirisFile osirisFile, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(osirisFile.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", osirisFile.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(OsirisFile.class, osirisFile.getId()), osirisFile.getOwner(), acl.getPermissions());
    }

    @GetMapping("/osirisFile/{osirisFileId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#osirisFile, 'administration')")
    public OsirisAccessControlList getOsirisFileAcls(@ModelAttribute("osirisFileId") OsirisFile osirisFile) {
        return OsirisAccessControlList.builder()
                .entityId(osirisFile.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(OsirisFile.class, osirisFile.getId())))
                .build();
    }
    
    @PostMapping("/collection/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#collection, 'administration')")
    public void setOsirisFileAcl(@ModelAttribute("collectionId") Collection collection, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(collection.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", collection.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Collection.class, collection.getId()), collection.getOwner(), acl.getPermissions());
    }
    
    @GetMapping("/collection/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#collection, 'administration')")
    public OsirisAccessControlList getCollectionAcls(@ModelAttribute("collectionId") Collection collection) {
        return OsirisAccessControlList.builder()
                .entityId(collection.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(Collection.class, collection.getId())))
                .build();
    }
    
    @PostMapping("/userMount/{userMountId}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasPermission(#userMount, 'administration')")
    public void setUserMountAcl(@ModelAttribute("userMountId") UserMount userMount, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(userMount.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", userMount.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(UserMount.class, userMount.getId()), userMount.getOwner(), acl.getPermissions());
    }
    
    @GetMapping("/userMount/{userMountId}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasPermission(#userMount, 'administration')")
    public OsirisAccessControlList getUserMountAcls(@ModelAttribute("userMountId") UserMount userMount) {
        return OsirisAccessControlList.builder()
                .entityId(userMount.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(UserMount.class, userMount.getId())))
                .build();
    }
    
    @PostMapping("/userEndpoint/{userEndpointId}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasPermission(#userEndpoint, 'administration')")
    public void setUserEndpointAcl(@ModelAttribute("userEndpointId") UserEndpoint userEndpoint, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(userEndpoint.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", userEndpoint.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(UserEndpoint.class, userEndpoint.getId()), userEndpoint.getOwner(), acl.getPermissions());
    }
    
    @GetMapping("/userEndpoint/{userEndpointId}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasPermission(#userEndpoint, 'administration')")
    public OsirisAccessControlList getUserEndpointAcls(@ModelAttribute("userEndpointId") UserEndpoint userEndpoint) {
        return OsirisAccessControlList.builder()
                .entityId(userEndpoint.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(UserEndpoint.class, userEndpoint.getId())))
                .build();
    }

    @PostMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#group, 'administration')")
    public void setGroupAcl(@ModelAttribute("groupId") Group group, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(group.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", group.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Group.class, group.getId()), group.getOwner(), acl.getPermissions());
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#group, 'administration')")
    public OsirisAccessControlList getGroupAcls(@ModelAttribute("groupId") Group group) {
        return OsirisAccessControlList.builder()
                .entityId(group.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(Group.class, group.getId())))
                .build();
    }

    @PostMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'administration')")
    public void setJobAcl(@ModelAttribute("jobId") Job job, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(job.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", job.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Job.class, job.getId()), job.getOwner(), acl.getPermissions());
        for (OsirisFile outputFile: job.getOutputFiles()) {
            setAcl(new ObjectIdentityImpl(OsirisFile.class, outputFile.getId()), outputFile.getOwner(), acl.getPermissions());
        }
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'administration')")
    public OsirisAccessControlList getJobAcls(@ModelAttribute("jobId") Job job) {
        return OsirisAccessControlList.builder()
                .entityId(job.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(Job.class, job.getId())))
                .build();
    }

    @PostMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'administration')")
    public void setJobConfigAcl(@ModelAttribute("jobConfigId") JobConfig jobConfig, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(jobConfig.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", jobConfig.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(JobConfig.class, jobConfig.getId()), jobConfig.getOwner(), acl.getPermissions());
    }

    @GetMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'administration')")
    public OsirisAccessControlList getJobConfigAcls(@ModelAttribute("jobConfigId") JobConfig jobConfig) {
        return OsirisAccessControlList.builder()
                .entityId(jobConfig.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(JobConfig.class, jobConfig.getId())))
                .build();
    }

    @PostMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#project, 'administration')")
    public void setProjectAcl(@ModelAttribute("projectId") Project project, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(project.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL {} vs BODY {}", project.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Project.class, project.getId()), project.getOwner(), acl.getPermissions());
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#project, 'administration')")
    public OsirisAccessControlList getProjectAcls(@ModelAttribute("projectId") Project project) {
        return OsirisAccessControlList.builder()
                .entityId(project.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(Project.class, project.getId())))
                .build();
    }

    @PostMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public void setServiceAcl(@ModelAttribute("serviceId") OsirisService service, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(service.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", service.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(OsirisService.class, service.getId()), service.getOwner(), acl.getPermissions());
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public OsirisAccessControlList getServiceAcls(@ModelAttribute("serviceId") OsirisService service) {
        return OsirisAccessControlList.builder()
                .entityId(service.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(OsirisService.class, service.getId())))
                .build();
    }
    
    @GetMapping("/serviceTemplate/{serviceTemplateId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceTemplate, 'administration')")
    public OsirisAccessControlList getServiceTemplateAcls(@ModelAttribute("serviceTemplateId") OsirisServiceTemplate serviceTemplate) {
        return OsirisAccessControlList.builder()
                .entityId(serviceTemplate.getId())
                .permissions(getOsirisPermissions(new ObjectIdentityImpl(OsirisServiceTemplate.class, serviceTemplate.getId())))
                .build();
    }
    
    @PostMapping("/serviceTemplate/{serviceTemplateId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceTemplate, 'administration')")
    public void setServiceTemplateAcl(@ModelAttribute("serviceTemplateId") OsirisServiceTemplate serviceTemplate, @RequestBody OsirisAccessControlList acl) {
        Preconditions.checkArgument(serviceTemplate.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", serviceTemplate.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(OsirisServiceTemplate.class, serviceTemplate.getId()), serviceTemplate.getOwner(), acl.getPermissions());
    }

    private List<OsirisAccessControlEntry> getOsirisPermissions(ObjectIdentity objectIdentity) {
        try {
            Acl acl = osirisSecurityService.getAcl(objectIdentity);

            return acl.getEntries().stream()
                    .filter(ace -> ace.getSid() instanceof GrantedAuthoritySid && ((GrantedAuthoritySid) ace.getSid()).getGrantedAuthority().startsWith("GROUP_"))
                    .collect(Collectors.groupingBy(this::getGroup, SPRING_OSIRIS_ACL_SET_COLLECTOR))
                    .entrySet().stream()
                    .map(e -> OsirisAccessControlEntry.builder().group(new SGroup(e.getKey())).permission(e.getValue()).build())
                    .collect(Collectors.toList());
        } catch (NotFoundException e) {
            LOG.debug("No ACLs present for object {}", objectIdentity);
            return ImmutableList.of();
        }
    }

    private Group getGroup(AccessControlEntry ace) {
        return groupDataService.getById(Long.parseLong(((GrantedAuthoritySid) ace.getSid()).getGrantedAuthority().replaceFirst("^GROUP_", "")));
    }

    private Group hydrateSGroup(SGroup sGroup) {
        return groupDataService.getById(sGroup.getId());
    }

    private void setAcl(ObjectIdentity objectIdentity, User owner, List<OsirisAccessControlEntry> newAces) {
        LOG.debug("Creating ACL on object {}: {}", objectIdentity, newAces);

        MutableAcl acl = osirisSecurityService.getAcl(objectIdentity);
        boolean published = osirisSecurityService.isPublic(objectIdentity);

        // JdbcMutableAclService#saveAcl deletes the entire list before saving
        // So we have to reset the entire desired permission list for the group

        // First delete all existing ACEs in reverse order...
        int aceCount = acl.getEntries().size();
        Seq.range(0, aceCount).reverse().forEach(acl::deleteAce);

        // ... then ensure the owner ACE is present (always ADMIN)
        if (owner != null) {
            Sid ownerSid = new PrincipalSid(owner.getName());
            OsirisPermission.ADMIN.getAclPermissions()
                    .forEach(p -> acl.insertAce(acl.getEntries().size(), p, ownerSid, true));
        }

        // ...then insert the new ACEs
        newAces.forEach((ace) -> {
            Sid sid = new GrantedAuthoritySid(hydrateSGroup(ace.getGroup()));
            ace.getPermission().getAclPermissions()
                    .forEach(p -> acl.insertAce(acl.getEntries().size(), p, sid, true));
        });

        osirisSecurityService.saveAcl(acl);

        // ... and finally re-publish if necessary
        if (published) {
            osirisSecurityService.publish(objectIdentity);
        }
    }

    @Data
    @NoArgsConstructor
    private static final class OsirisAccessControlList {
        private Long entityId;
        private List<OsirisAccessControlEntry> permissions;

        @Builder
        public OsirisAccessControlList(Long entityId, List<OsirisAccessControlEntry> permissions) {
            this.entityId = entityId;
            this.permissions = permissions;
        }
    }

    @Data
    @NoArgsConstructor
    private static final class OsirisAccessControlEntry {
        private SGroup group;
        private OsirisPermission permission;

        @Builder
        public OsirisAccessControlEntry(SGroup group, OsirisPermission permission) {
            this.group = group;
            this.permission = permission;
        }
    }

    @Data
    @NoArgsConstructor
    private static final class SGroup {
        private Long id;
        private String name;

        private SGroup(Group group) {
            this.id = group.getId();
            this.name = group.getName();
        }
    }

}
