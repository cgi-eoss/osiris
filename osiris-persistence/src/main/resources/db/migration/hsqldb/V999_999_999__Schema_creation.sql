-- OSIRIS does not support schema migration when using HSQLDB
-- This 'migration' script is primarily to track the current DB schema for use in tests and test environments

-- Tables & Indexes

CREATE TABLE osiris_users (
  uid  BIGINT IDENTITY PRIMARY KEY,
  mail CHARACTER VARYING(255),
  name CHARACTER VARYING(255)                 NOT NULL,
  role CHARACTER VARYING(255) DEFAULT 'GUEST' NOT NULL CHECK (role IN
                                                              ('GUEST', 'USER', 'EXPERT_USER', 'CONTENT_AUTHORITY', 'ADMIN'))
);
CREATE UNIQUE INDEX osiris_users_name_idx
  ON osiris_users (name);

CREATE TABLE osiris_wallets (
  id      BIGINT IDENTITY PRIMARY KEY,
  owner   BIGINT        NOT NULL FOREIGN KEY REFERENCES osiris_users (uid),
  balance INT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX osiris_wallets_owner_idx
  ON osiris_wallets (owner);

CREATE TABLE osiris_wallet_transactions (
  id               BIGINT IDENTITY PRIMARY KEY,
  wallet           BIGINT                      NOT NULL FOREIGN KEY REFERENCES osiris_wallets (id),
  balance_change   INT                         NOT NULL,
  transaction_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  type             CHARACTER VARYING(255)      NOT NULL CHECK (type IN ('CREDIT', 'JOB', 'DOWNLOAD')),
  associated_id    BIGINT
);
CREATE INDEX osiris_wallet_transactions_wallet_idx
  ON osiris_wallet_transactions (wallet);

CREATE TABLE osiris_services (
  id             BIGINT IDENTITY PRIMARY KEY,
  description    CHARACTER VARYING(255),
  docker_tag     CHARACTER VARYING(255),
  licence        CHARACTER VARYING(255) NOT NULL CHECK (licence IN ('OPEN', 'RESTRICTED')),
  name           CHARACTER VARYING(255) NOT NULL,
  wps_descriptor CLOB,
  docker_build_info CLOB,
  required_resources CLOB,
  strip_proxy_path BOOLEAN DEFAULT TRUE,
  external_uri   CHARACTER VARYING(255),
  status         CHARACTER VARYING(255) NOT NULL CHECK (status IN ('IN_DEVELOPMENT', 'AVAILABLE')),
  type           CHARACTER VARYING(255) NOT NULL CHECK (type IN ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION', 'PARALLEL_PROCESSOR', 'FTP_SERVICE', 'WPS_SERVICE')),
  owner          BIGINT                 NOT NULL FOREIGN KEY REFERENCES osiris_users (uid)
);
CREATE UNIQUE INDEX osiris_services_name_idx
  ON osiris_services (name);
CREATE INDEX osiris_services_owner_idx
  ON osiris_services (owner);
  
  
CREATE TABLE osiris_service_templates (
  id             BIGINT IDENTITY PRIMARY KEY,
  description    CHARACTER VARYING(255),
  name           CHARACTER VARYING(255) NOT NULL,
  wps_descriptor CLOB,
  required_resources CLOB,
  type           CHARACTER VARYING(255) NOT NULL CHECK (type IN ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION', 'PARALLEL_PROCESSOR')),
  owner          BIGINT                 NOT NULL FOREIGN KEY REFERENCES osiris_users (uid)
);

CREATE UNIQUE INDEX osiris_service_templates_name_idx
  ON osiris_service_templates (name);
CREATE INDEX osiris_service_templates_owner_idx
  ON osiris_service_templates (owner);

  CREATE TABLE osiris_default_service_templates (
  id         BIGINT IDENTITY PRIMARY KEY,
  service_template    BIGINT  NOT NULL FOREIGN KEY REFERENCES osiris_service_templates (id),
  type           CHARACTER VARYING(255) NOT NULL CHECK (type IN ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION', 'PARALLEL_PROCESSOR')) 
);  

CREATE UNIQUE INDEX osiris_default_service_templates_type_idx
  ON osiris_default_service_templates (type);
  
CREATE TABLE osiris_credentials (
  id               BIGINT IDENTITY PRIMARY KEY,
  certificate_path CHARACTER VARYING(255),
  host             CHARACTER VARYING(255) NOT NULL,
  password         CHARACTER VARYING(255),
  type             CHARACTER VARYING(255) NOT NULL CHECK (type IN ('BASIC', 'X509', 'PKCS8')),
  username         CHARACTER VARYING(255),
  data			   CLOB
);
CREATE UNIQUE INDEX osiris_credentials_host_idx
  ON osiris_credentials (host);

CREATE TABLE osiris_groups (
  gid         BIGINT IDENTITY PRIMARY KEY,
  description CHARACTER VARYING(255),
  name        CHARACTER VARYING(255) NOT NULL,
  owner       BIGINT                 NOT NULL FOREIGN KEY REFERENCES osiris_users (uid)
);
CREATE INDEX osiris_groups_name_idx
  ON osiris_groups (name);
CREATE INDEX osiris_groups_owner_idx
  ON osiris_groups (owner);
CREATE UNIQUE INDEX osiris_groups_name_owner_idx
  ON osiris_groups (name, owner);

CREATE TABLE osiris_group_member (
  group_id BIGINT FOREIGN KEY REFERENCES osiris_groups (gid),
  user_id  BIGINT FOREIGN KEY REFERENCES osiris_users (uid)
);
CREATE UNIQUE INDEX osiris_group_member_user_group_idx
  ON osiris_group_member (group_id, user_id);

CREATE TABLE osiris_job_configs (
  id      BIGINT IDENTITY PRIMARY KEY,
  inputs  CLOB,
  parent  BIGINT,
  owner   BIGINT NOT NULL FOREIGN KEY REFERENCES osiris_users (uid),
  service BIGINT NOT NULL FOREIGN KEY REFERENCES osiris_services (id),
  systematic_parameter CHARACTER VARYING(255), 
  label   CHARACTER VARYING(255)
  -- WARNING: No unique index on owner-service-inputs-parent-systematic_parameter as hsqldb 2.3.4 cannot index CLOB columns
  -- UNIQUE (owner, service, inputs, parent, systematic_parameter)
);
CREATE INDEX osiris_job_configs_service_idx
  ON osiris_job_configs (service);
CREATE INDEX osiris_job_configs_owner_idx
  ON osiris_job_configs (owner);
CREATE INDEX osiris_job_configs_label_idx
  ON osiris_job_configs (label);

CREATE TABLE osiris_jobs (
  id         BIGINT IDENTITY PRIMARY KEY,
  end_time   TIMESTAMP WITHOUT TIME ZONE,
  ext_id     CHARACTER VARYING(255) NOT NULL,
  gui_url    CHARACTER VARYING(255),
  gui_endpoint    CHARACTER VARYING(255),
  is_parent  BOOLEAN DEFAULT FALSE,
  outputs    CLOB,
  stage      CHARACTER VARYING(255),
  start_time TIMESTAMP WITHOUT TIME ZONE,
  status     CHARACTER VARYING(255) NOT NULL CHECK (status IN
                                                    ('CREATED', 'RUNNING', 'COMPLETED', 'ERROR', 'CANCELLED')),
  job_config BIGINT                 NOT NULL FOREIGN KEY REFERENCES osiris_job_configs (id),
  owner      BIGINT                 NOT NULL FOREIGN KEY REFERENCES osiris_users (uid),
  parent_job_id BIGINT REFERENCES osiris_jobs (id),
  worker_id CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX osiris_jobs_ext_id_idx
  ON osiris_jobs (ext_id);
CREATE INDEX osiris_jobs_job_config_idx
  ON osiris_jobs (job_config);
CREATE INDEX osiris_jobs_owner_idx
  ON osiris_jobs (owner);
  
--Reference to parent in job config
ALTER TABLE osiris_job_configs ADD FOREIGN KEY (parent) REFERENCES osiris_jobs(id);
  

--Incidents and incident types

CREATE TABLE osiris_incident_types (
  id            BIGINT IDENTITY             PRIMARY KEY,
  owner         BIGINT                      NOT NULL REFERENCES osiris_users (uid),
  title         CHARACTER VARYING(255)      NOT NULL,
  description   CHARACTER VARYING(4096),
  icon_id       CHARACTER VARYING(255)
);

CREATE INDEX osiris_incident_types_owner_idx
  ON osiris_incident_types (owner);

CREATE TABLE osiris_incidents (
  id            BIGINT IDENTITY             PRIMARY KEY,
  owner         BIGINT                      NOT NULL REFERENCES osiris_users (uid),
  type          BIGINT                      NOT NULL REFERENCES osiris_incident_types (id),
  title         CHARACTER VARYING(255)      NOT NULL,
  description   CHARACTER VARYING(4096),
  aoi           CHARACTER VARYING(8192)     NOT NULL,
  start_date    TIMESTAMP                   NOT NULL,
  end_date      TIMESTAMP                   NOT NULL
);

CREATE INDEX osiris_incidents_owner_idx
  ON osiris_incidents (owner);


-- Data sources

CREATE TABLE osiris_data_sources (
  id     BIGINT IDENTITY PRIMARY KEY,
  name   CHARACTER VARYING(255) NOT NULL,
  owner  BIGINT                 NOT NULL FOREIGN KEY REFERENCES osiris_users (uid),
  policy CHARACTER VARYING(255) DEFAULT 'CACHE' NOT NULL CHECK (policy IN ('CACHE', 'MIRROR', 'REMOTE_ONLY'))
);
CREATE UNIQUE INDEX osiris_data_sources_name_idx
  ON osiris_data_sources (name);
CREATE INDEX osiris_data_sources_owner_idx
  ON osiris_data_sources (owner);

 --User collections
 CREATE TABLE osiris_collections (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES osiris_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  identifier CHARACTER VARYING(255)	NOT NULL,
  description CLOB,
  file_type        CHARACTER VARYING(255) default 'OUTPUT_PRODUCT' CHECK (file_type IN ('REFERENCE_DATA', 'OUTPUT_PRODUCT', 'EXTERNAL_PRODUCT')),
  products_type    CHARACTER VARYING(255)				
);
  
CREATE UNIQUE INDEX osiris_collections_name_owner_idx
  ON osiris_collections (name, owner);
CREATE UNIQUE INDEX osiris_collections_identifier_idx
  ON osiris_collections (identifier);
CREATE INDEX osiris_collections_name_idx
  ON osiris_collections (name);
CREATE INDEX osiris_collections_owner_idx
  ON osiris_collections (owner); 

-- OsirisFile and Databasket tables

CREATE TABLE osiris_files (
  id         BIGINT IDENTITY PRIMARY KEY,
  uri        CHARACTER VARYING(255) NOT NULL,
  resto_id   BINARY(255)            NOT NULL,
  collection_id BIGINT REFERENCES osiris_collections (id),
  type       CHARACTER VARYING(255) CHECK (type IN ('REFERENCE_DATA', 'OUTPUT_PRODUCT', 'EXTERNAL_PRODUCT')),
  owner      BIGINT FOREIGN KEY REFERENCES osiris_users (uid),
  filename   CHARACTER VARYING(255),
  filesize   BIGINT,
  datasource BIGINT FOREIGN KEY REFERENCES osiris_data_sources (id)
);
CREATE UNIQUE INDEX osiris_files_uri_idx
  ON osiris_files (uri);
CREATE UNIQUE INDEX osiris_files_resto_id_idx
  ON osiris_files (resto_id);
CREATE INDEX osiris_files_owner_idx
  ON osiris_files (owner);


CREATE TABLE osiris_files_relations (
  id         BIGINT IDENTITY PRIMARY KEY,
  source_file      BIGINT FOREIGN KEY REFERENCES osiris_files (id) ON DELETE CASCADE,
  target_file      BIGINT FOREIGN KEY REFERENCES osiris_files (id) ON DELETE CASCADE,
  type       CHARACTER VARYING(255) CHECK (type IN ('VISUALIZATION_OF'))
);
CREATE INDEX osiris_files_relations_source_idx
  ON osiris_files_relations (source_file);
CREATE INDEX osiris_files_relations_target_idx
  ON osiris_files_relations (target_file);
CREATE UNIQUE INDEX osiris_files_source_target_type_idx
  ON osiris_files_relations (source_file,target_file,type);


CREATE TABLE osiris_databaskets (
  id          BIGINT IDENTITY PRIMARY KEY,
  name        CHARACTER VARYING(255) NOT NULL,
  description CHARACTER VARYING(255),
  owner       BIGINT FOREIGN KEY REFERENCES osiris_users (uid)
);
CREATE INDEX osiris_databaskets_name_idx
  ON osiris_databaskets (name);
CREATE INDEX osiris_databaskets_owner_idx
  ON osiris_databaskets (owner);
CREATE UNIQUE INDEX osiris_databaskets_name_owner_idx
  ON osiris_databaskets (name, owner);

CREATE TABLE osiris_databasket_files (
  databasket_id BIGINT FOREIGN KEY REFERENCES osiris_databaskets (id),
  file_id       BIGINT FOREIGN KEY REFERENCES osiris_files (id)
);
CREATE UNIQUE INDEX osiris_databasket_files_basket_file_idx
  ON osiris_databasket_files (databasket_id, file_id);

CREATE TABLE osiris_geoserver_layers (
  id          BIGINT IDENTITY PRIMARY KEY,
  workspace        CHARACTER VARYING(255) NOT NULL,
  layer CHARACTER VARYING(255) NOT NULL,
  store CHARACTER VARYING(255),
  store_type  CHARACTER VARYING(255) NOT NULL CHECK (store_type IN('MOSAIC', 'GEOTIFF', 'POSTGIS')),
  owner       BIGINT NOT NULL FOREIGN KEY REFERENCES osiris_users (uid)
);

CREATE INDEX osiris_geoserver_layers_owner_idx
  ON osiris_geoserver_layers (owner);
CREATE UNIQUE INDEX osiris_geoserver_layers_workspace_datastore_layer_idx
  ON osiris_geoserver_layers (workspace, layer);

CREATE TABLE osiris_geoserver_layer_files (
  geoserver_layer_id BIGINT FOREIGN KEY REFERENCES osiris_geoserver_layers (id),
  file_id       BIGINT FOREIGN KEY REFERENCES osiris_files (id)
);

CREATE UNIQUE INDEX osiris_geoserver_layer_files_layer_file_idx
  ON osiris_geoserver_layer_files (geoserver_layer_id, file_id);

CREATE TABLE osiris_projects (
  id          BIGINT IDENTITY PRIMARY KEY,
  name        CHARACTER VARYING(255) NOT NULL,
  description CHARACTER VARYING(255),
  owner       BIGINT FOREIGN KEY REFERENCES osiris_users (uid)
);
CREATE INDEX osiris_projects_name_idx
  ON osiris_projects (name);
CREATE INDEX osiris_projects_owner_idx
  ON osiris_projects (owner);
CREATE UNIQUE INDEX osiris_projects_name_owner_idx
  ON osiris_projects (name, owner);

CREATE TABLE osiris_project_databaskets (
  project_id    BIGINT FOREIGN KEY REFERENCES osiris_projects (id),
  databasket_id BIGINT FOREIGN KEY REFERENCES osiris_databaskets (id)
);
CREATE UNIQUE INDEX osiris_project_databaskets_ids_idx
  ON osiris_project_databaskets (project_id, databasket_id);

CREATE TABLE osiris_project_services (
  project_id BIGINT FOREIGN KEY REFERENCES osiris_projects (id),
  service_id BIGINT FOREIGN KEY REFERENCES osiris_services (id)
);
CREATE UNIQUE INDEX osiris_project_services_ids_idx
  ON osiris_project_services (project_id, service_id);

CREATE TABLE osiris_project_job_configs (
  project_id    BIGINT FOREIGN KEY REFERENCES osiris_projects (id),
  job_config_id BIGINT FOREIGN KEY REFERENCES osiris_job_configs (id)
);
CREATE UNIQUE INDEX osiris_project_job_configs_ids_idx
  ON osiris_project_job_configs (project_id, job_config_id);

-- OsirisServiceContextFile table

CREATE TABLE osiris_service_files (
  id         BIGINT IDENTITY PRIMARY KEY,
  service    BIGINT                NOT NULL FOREIGN KEY REFERENCES osiris_services (id),
  filename   CHARACTER VARYING(255),
  executable BOOLEAN DEFAULT FALSE NOT NULL,
  content    CLOB
);
CREATE UNIQUE INDEX osiris_service_files_filename_service_idx
  ON osiris_service_files (filename, service);
CREATE INDEX osiris_service_files_filename_idx
  ON osiris_service_files (filename);
CREATE INDEX osiris_service_files_service_idx
  ON osiris_service_files (service);

-- OsirisServiceContextFile table

CREATE TABLE osiris_service_template_files (
  id         BIGINT IDENTITY PRIMARY KEY,
  service_template    BIGINT  NOT NULL FOREIGN KEY REFERENCES osiris_service_templates (id),
  filename   CHARACTER VARYING(255),
  executable BOOLEAN DEFAULT FALSE NOT NULL,
  content    CLOB
);
CREATE UNIQUE INDEX osiris_service_template_files_filename_service_idx
  ON osiris_service_template_files (filename, service_template);
CREATE INDEX osiris_service_template_files_filename_idx
  ON osiris_service_template_files (filename);
CREATE INDEX osiris_service_template_files_service_template_idx
  ON osiris_service_template_files (service_template);  
  
-- Cost expressions

CREATE TABLE osiris_costing_expressions (
  id                        BIGINT IDENTITY PRIMARY KEY,
  type                      CHARACTER VARYING(255) NOT NULL CHECK (type IN ('SERVICE', 'DOWNLOAD')),
  associated_id             BIGINT                 NOT NULL,
  cost_expression           CHARACTER VARYING(255) NOT NULL,
  estimated_cost_expression CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX osiris_costing_expressions_type_associated_id_idx
  ON osiris_costing_expressions (type, associated_id);

-- Worker expressions

CREATE TABLE osiris_worker_locator_expressions (
  id         BIGINT IDENTITY PRIMARY KEY,
  service    BIGINT                 NOT NULL FOREIGN KEY REFERENCES osiris_services (id),
  expression CHARACTER VARYING(255) NOT NULL
);
CREATE UNIQUE INDEX osiris_worker_locator_expressions_service_idx
  ON osiris_worker_locator_expressions (service);

-- Publishing requests

CREATE TABLE osiris_publishing_requests (
  id            BIGINT IDENTITY PRIMARY KEY,
  owner         BIGINT                 NOT NULL FOREIGN KEY REFERENCES osiris_users (uid),
  request_time  TIMESTAMP WITHOUT TIME ZONE,
  updated_time  TIMESTAMP WITHOUT TIME ZONE,
  status        CHARACTER VARYING(255) NOT NULL CHECK (status IN
                                                       ('REQUESTED', 'GRANTED', 'NEEDS_INFO', 'REJECTED')),
  type          CHARACTER VARYING(255) NOT NULL CHECK (type IN
                                                       ('DATABASKET', 'DATASOURCE', 'FILE', 'SERVICE', 'SERVICE_TEMPLATE', 'GROUP', 'JOB_CONFIG', 'PROJECT', 'COLLECTION')),
  associated_id BIGINT                 NOT NULL
);
CREATE INDEX osiris_publishing_requests_owner_idx
  ON osiris_publishing_requests (owner);
CREATE UNIQUE INDEX osiris_publishing_requests_owner_object_idx
  ON osiris_publishing_requests (owner, type, associated_id);

-- Job-output file relationships

CREATE TABLE osiris_job_output_files (
  job_id  BIGINT NOT NULL FOREIGN KEY REFERENCES osiris_jobs (id),
  file_id BIGINT NOT NULL FOREIGN KEY REFERENCES osiris_files (id)
);
CREATE UNIQUE INDEX osiris_job_output_files_job_file_idx
  ON osiris_job_output_files (job_id, file_id);

-- ACL schema from spring-security-acl

CREATE TABLE acl_sid (
  id        BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )                  NOT NULL PRIMARY KEY,
  principal BOOLEAN                 NOT NULL,
  sid       VARCHAR_IGNORECASE(100) NOT NULL,
  UNIQUE (sid, principal)
);

CREATE TABLE acl_class (
  id    BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )              NOT NULL PRIMARY KEY,
  class VARCHAR_IGNORECASE(100) NOT NULL,
  UNIQUE (class)
);

CREATE TABLE acl_object_identity (
  id                 BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )           NOT NULL PRIMARY KEY,
  object_id_class    BIGINT  NOT NULL FOREIGN KEY REFERENCES acl_class (id),
  object_id_identity BIGINT  NOT NULL,
  parent_object      BIGINT FOREIGN KEY REFERENCES acl_object_identity (id),
  owner_sid          BIGINT FOREIGN KEY REFERENCES acl_sid (id),
  entries_inheriting BOOLEAN NOT NULL,
  UNIQUE (object_id_class, object_id_identity)
);

CREATE TABLE acl_entry (
  id                  BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )            NOT NULL PRIMARY KEY,
  acl_object_identity BIGINT  NOT NULL FOREIGN KEY REFERENCES acl_object_identity (id),
  ace_order           INT     NOT NULL,
  sid                 BIGINT  NOT NULL FOREIGN KEY REFERENCES acl_sid (id),
  mask                INTEGER NOT NULL,
  granting            BOOLEAN NOT NULL,
  audit_success       BOOLEAN NOT NULL,
  audit_failure       BOOLEAN NOT NULL,
  UNIQUE (acl_object_identity, ace_order)
);

--User preferences KV store
 CREATE TABLE osiris_user_preferences (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES osiris_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  type    CHARACTER VARYING(255)				,
  preference CLOB   CHECK (length(preference) <= 51200)  
);
  
CREATE UNIQUE INDEX osiris_user_preferences_name_owner_idx
  ON osiris_user_preferences (name, owner);
CREATE INDEX osiris_user_preferences_name_idx
  ON osiris_user_preferences (name);
CREATE INDEX osiris_user_preferences_owner_idx
  ON osiris_user_preferences (owner);

 --User mounts
 CREATE TABLE osiris_user_mounts (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES osiris_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  type 	  CHARACTER VARYING(255)	    NOT NULL,
  mount_path    CHARACTER VARYING(255)	NOT NULL	 
);

CREATE UNIQUE INDEX osiris_user_mount_name_owner_idx
  ON osiris_user_mounts (name, owner);
CREATE INDEX osiris_user_mount_name_idx
  ON osiris_user_mounts (name);
CREATE INDEX osiris_user_mount_owner_idx
  ON osiris_user_mounts (owner);

 --Mounts in services

 CREATE TABLE osiris_services_mounts (
  osiris_service_id BIGINT NOT NULL FOREIGN KEY REFERENCES osiris_services (id),
  user_mount_id BIGINT NOT NULL FOREIGN KEY REFERENCES osiris_user_mounts (id),
  target_mount_path    CHARACTER VARYING(255)	NOT NULL	 
);

CREATE UNIQUE INDEX osiris_services_mounts_service_mount_idx
  ON osiris_services_mounts (osiris_service_id, user_mount_id);
    
 --User endpoints
 CREATE TABLE osiris_user_endpoints (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES osiris_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  url    CHARACTER VARYING(255)	NOT NULL	 
);
  
CREATE UNIQUE INDEX osiris_user_endpoint_name_owner_idx
  ON osiris_user_endpoints (name, owner);
CREATE INDEX osiris_user_endpoint_name_idx
  ON osiris_user_endpoints (name);
CREATE INDEX osiris_user_endpoint_owner_idx
  ON osiris_user_endpoints (owner);

 --Systematic processings
 
 CREATE TABLE osiris_systematic_processings (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES osiris_users (uid),
  status  CHARACTER VARYING(255) NOT NULL CHECK (status IN
                                                    ('ACTIVE', 'BLOCKED', 'COMPLETED')),
  cron_expression CHARACTER VARYING(255),                                                   
  parent_job BIGINT NOT NULL REFERENCES osiris_jobs (id),
  last_updated TIMESTAMP WITHOUT TIME ZONE,
  search_parameters CLOB
  );  


  --Add incident processings and templates

create table osiris_incident_processing_templates (
    id                  bigint generated by default as identity     primary key,
    owner               bigint                                      not null references osiris_users (uid),
    title               varchar(255)                                not null,
    description         varchar(4096),
    incident_type       bigint                                      not null references osiris_incident_types (id),
    service             bigint                                      not null references osiris_services (id),
    systematic_input    varchar(255)                                not null,
    fixed_inputs        clob,
    cron_expression CHARACTER VARYING(255),                                                   
    search_parameters   clob
);


CREATE INDEX osiris_incident_processing_templates_owner_idx
  ON osiris_incident_processing_templates (owner);

create table osiris_incident_processings (
    id                      bigint generated by default as identity     primary key,
    owner                   bigint                                      not null references osiris_users (uid),
    template                bigint                                      not null references osiris_incident_processing_templates (id),
    job                     bigint                                      references osiris_jobs (id),
    incident                bigint                                      not null references osiris_incidents (id),
    systematic_processing   bigint                                      references osiris_systematic_processings (id),
    inputs                  clob,
    search_parameters       clob,
    collection              bigint                                      references osiris_collections (id)
);

CREATE INDEX osiris_incident_processings_owner_idx
  ON osiris_incident_processings (owner);

 -- Incidents to collections
CREATE TABLE osiris_incidents_collections (
  incident_id BIGINT FOREIGN KEY REFERENCES osiris_incidents (id),
  collection_id       BIGINT FOREIGN KEY REFERENCES osiris_collections (id)
);

CREATE UNIQUE INDEX osiris_incidents_collections_idx
  ON osiris_incidents_collections (incident_id, collection_id);

 --API keys
 
 CREATE TABLE osiris_api_keys (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES osiris_users (uid),
  api_key  CHARACTER VARYING(255)
  );  
  
CREATE UNIQUE INDEX osiris_api_keys_owner_idx
  ON osiris_api_keys (owner);
  
-- Initial data

-- Fallback internal user
INSERT INTO osiris_users (name, mail) VALUES ('osiris', 'foodsecurity-tep@esa.int');

-- Default project
INSERT INTO osiris_projects (name, owner) VALUES ('Default Project', (SELECT uid
                                                                    FROM osiris_users
                                                                    WHERE name = 'osiris'));

-- OSIRIS datasource
INSERT INTO osiris_data_sources (name, owner) VALUES ('OSIRIS', (SELECT uid
                                                              FROM osiris_users
                                                              WHERE name = 'osiris'));
-- Quartz schema creation

DROP TABLE qrtz_locks IF EXISTS;
DROP TABLE qrtz_scheduler_state IF EXISTS;
DROP TABLE qrtz_fired_triggers IF EXISTS;
DROP TABLE qrtz_paused_trigger_grps IF EXISTS;
DROP TABLE qrtz_calendars IF EXISTS;
DROP TABLE qrtz_blob_triggers IF EXISTS;
DROP TABLE qrtz_cron_triggers IF EXISTS;
DROP TABLE qrtz_simple_triggers IF EXISTS;
DROP TABLE qrtz_simprop_triggers IF EXISTS;
DROP TABLE qrtz_triggers IF EXISTS;
DROP TABLE qrtz_job_details IF EXISTS;

CREATE TABLE qrtz_job_details
(
SCHED_NAME VARCHAR(120) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
JOB_CLASS_NAME VARCHAR(250) NOT NULL,
IS_DURABLE BOOLEAN NOT NULL,
IS_NONCONCURRENT BOOLEAN NOT NULL,
IS_UPDATE_DATA BOOLEAN NOT NULL,
REQUESTS_RECOVERY BOOLEAN NOT NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
NEXT_FIRE_TIME NUMERIC(13) NULL,
PREV_FIRE_TIME NUMERIC(13) NULL,
PRIORITY INTEGER NULL,
TRIGGER_STATE VARCHAR(16) NOT NULL,
TRIGGER_TYPE VARCHAR(8) NOT NULL,
START_TIME NUMERIC(13) NOT NULL,
END_TIME NUMERIC(13) NULL,
CALENDAR_NAME VARCHAR(200) NULL,
MISFIRE_INSTR NUMERIC(2) NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_simple_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
REPEAT_COUNT NUMERIC(7) NOT NULL,
REPEAT_INTERVAL NUMERIC(12) NOT NULL,
TIMES_TRIGGERED NUMERIC(10) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_cron_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
CRON_EXPRESSION VARCHAR(120) NOT NULL,
TIME_ZONE_ID VARCHAR(80),
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_simprop_triggers
  (          
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 NUMERIC(9) NULL,
    INT_PROP_2 NUMERIC(9) NULL,
    LONG_PROP_1 NUMERIC(13) NULL,
    LONG_PROP_2 NUMERIC(13) NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 BOOLEAN NULL,
    BOOL_PROP_2 BOOLEAN NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) 
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_blob_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
BLOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_calendars
(
SCHED_NAME VARCHAR(120) NOT NULL,
CALENDAR_NAME VARCHAR(200) NOT NULL,
CALENDAR BLOB NOT NULL,
PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);

CREATE TABLE qrtz_paused_trigger_grps
(
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_fired_triggers
(
SCHED_NAME VARCHAR(120) NOT NULL,
ENTRY_ID VARCHAR(95) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
FIRED_TIME NUMERIC(13) NOT NULL,
SCHED_TIME NUMERIC(13) NOT NULL,
PRIORITY INTEGER NOT NULL,
STATE VARCHAR(16) NOT NULL,
JOB_NAME VARCHAR(200) NULL,
JOB_GROUP VARCHAR(200) NULL,
IS_NONCONCURRENT BOOLEAN NULL,
REQUESTS_RECOVERY BOOLEAN NULL,
PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);

CREATE TABLE qrtz_scheduler_state
(
SCHED_NAME VARCHAR(120) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
LAST_CHECKIN_TIME NUMERIC(13) NOT NULL,
CHECKIN_INTERVAL NUMERIC(13) NOT NULL,
PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);

CREATE TABLE qrtz_locks
(
SCHED_NAME VARCHAR(120) NOT NULL,
LOCK_NAME VARCHAR(40) NOT NULL,
PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);