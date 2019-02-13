--Fix erroneous bigserial references

alter table osiris_incidents
    alter column type drop default;

alter table osiris_incident_processing_templates
    alter column incident_type drop default;

alter table osiris_incident_processings
    alter column template drop default,
    alter column incident drop default,
    alter column systematic_processing drop not null,
    alter column systematic_processing drop default;
