 --Service Templates
 
 CREATE TABLE osiris_service_templates (
  id             BIGSERIAL PRIMARY KEY,
  description    CHARACTER VARYING(255),
  name           CHARACTER VARYING(255) NOT NULL,
  wps_descriptor TEXT,
  required_resources TEXT,
  type           osiris_services_type NOT NULL,
  owner          BIGINT                 NOT NULL REFERENCES osiris_users (uid)
);

CREATE UNIQUE INDEX osiris_service_templates_name_idx
  ON osiris_service_templates (name);
CREATE INDEX osiris_service_templates_owner_idx
  ON osiris_service_templates (owner);

 
CREATE TABLE osiris_service_template_files (
  id         BIGSERIAL PRIMARY KEY,
  service_template    BIGINT  NOT NULL REFERENCES osiris_service_templates (id),
  filename   CHARACTER VARYING(255),
  executable BOOLEAN DEFAULT FALSE NOT NULL,
  content    TEXT
);
CREATE UNIQUE INDEX osiris_service_template_files_filename_service_idx
  ON osiris_service_template_files (filename, service_template);
CREATE INDEX osiris_service_template_files_filename_idx
  ON osiris_service_template_files (filename);
CREATE INDEX osiris_service_template_files_service_template_idx
  ON osiris_service_template_files (service_template);  
  
CREATE TABLE osiris_default_service_templates (
  id         BIGSERIAL PRIMARY KEY,
  service_template    BIGINT  NOT NULL REFERENCES osiris_service_templates (id),
  type           osiris_services_type NOT NULL  
);  

CREATE UNIQUE INDEX osiris_default_service_templates_type_idx
  ON osiris_default_service_templates (type);
