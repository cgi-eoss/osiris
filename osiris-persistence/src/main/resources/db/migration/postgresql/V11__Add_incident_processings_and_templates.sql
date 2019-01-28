--Add incident processings and templates

create table osiris_incident_processing_templates (
    id                  bigserial           primary key,
    owner               bigint              not null references osiris_users (uid),
    title               varchar(255)        not null,
    description         varchar(4096),
    incident_type       bigserial           not null references osiris_incident_types,
    service             bigint              not null references osiris_services (id),
    systematic_input    varchar(255)        not null default 'parallelInput',
    fixed_inputs        text,
    search_parameters   text
);


CREATE INDEX osiris_incident_processing_templates_owner_idx
  ON osiris_incident_processing_templates (owner);
  
create table osiris_incident_processings (
    id                      bigserial           primary key,
    owner                   bigint              not null references osiris_users (uid),
    template                bigserial           not null references osiris_incident_processing_templates (id),
    incident                bigserial           not null references osiris_incidents (id),
    inputs                  text,
    search_parameters       text,
    systematic_processing   bigserial           references osiris_systematic_processings (id),
    collection              bigint              references osiris_collections (id)
);

CREATE INDEX osiris_incident_processings_owner_idx
  ON osiris_incident_processings (owner);
