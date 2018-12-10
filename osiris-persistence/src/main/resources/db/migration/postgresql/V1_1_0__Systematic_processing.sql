 --Systematic processings
 
 CREATE TYPE osiris_systematic_processing_status AS ENUM ('ACTIVE', 'BLOCKED' ,'COMPLETED');
 
 CREATE TABLE osiris_systematic_processings (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES osiris_users (uid),
  status     osiris_systematic_processing_status,
  parent_job BIGINT NOT NULL REFERENCES osiris_jobs (id) ON DELETE CASCADE,
  last_updated TIMESTAMP WITHOUT TIME ZONE,
  search_parameters TEXT
  );
  
CREATE INDEX osiris_systematic_processing_owner_idx
  ON osiris_systematic_processings (owner);
  
 ALTER TABLE osiris_job_configs add column systematic_parameter character varying(255);
 ALTER TABLE osiris_job_configs drop constraint osiris_job_configs_owner_service_inputs_key;
 ALTER TABLE osiris_job_configs add constraint osiris_job_configs_unique_key UNIQUE (owner, service, inputs, parent, systematic_parameter);
 
