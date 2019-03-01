--Osiris geoserver layers

CREATE TYPE geoserver_store_type AS ENUM ('MOSAIC', 'GEOTIFF', 'POSTGIS');

CREATE TABLE osiris_geoserver_layers (
  id          bigserial           primary key,
  owner         BIGINT                      NOT NULL REFERENCES osiris_users (uid),
  workspace        CHARACTER VARYING(255) NOT NULL,
  layer CHARACTER VARYING(255) NOT NULL,
  store CHARACTER VARYING(255),
  store_type  geoserver_store_type
);

CREATE INDEX osiris_geoserver_layers_owner_idx
  ON osiris_geoserver_layers (owner); 
  
CREATE UNIQUE INDEX osiris_geoserver_layers_workspace_datastore_layer_idx
  ON osiris_geoserver_layers (workspace, layer);

CREATE TABLE osiris_geoserver_layer_files (
  geoserver_layer_id bigint NOT NULL REFERENCES osiris_geoserver_layers (id),
  file_id       bigint NOT NULL REFERENCES osiris_files (id),
 PRIMARY KEY (geoserver_layer_id, file_id)
);

