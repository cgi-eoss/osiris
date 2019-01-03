--Add incident and incident types

CREATE TABLE osiris_incident_types (
  id            BIGSERIAL                   PRIMARY KEY,
  owner         BIGINT                      NOT NULL REFERENCES osiris_users (uid),
  title         CHARACTER VARYING(255)      NOT NULL,
  description   CHARACTER VARYING(4096),
  icon_id       CHARACTER VARYING(255)
);

CREATE INDEX osiris_incident_types_owner_idx
  ON osiris_incident_types (owner);
  
CREATE TABLE osiris_incidents (
  id            BIGSERIAL                   PRIMARY KEY,
  owner         BIGINT                      NOT NULL REFERENCES osiris_users (uid),
  type          BIGSERIAL                   NOT NULL REFERENCES osiris_incident_types (id),
  title         CHARACTER VARYING(255)      NOT NULL,
  description   CHARACTER VARYING(4096),
  aoi           CHARACTER VARYING(8192)     NOT NULL,
  start_date    TIMESTAMP                   NOT NULL,
  end_date      TIMESTAMP                   NOT NULL
);

CREATE INDEX osiris_incidents_owner_idx
  ON osiris_incidents (owner);
