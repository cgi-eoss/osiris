 --User mounts
 CREATE TABLE osiris_user_mounts (
  id      BIGSERIAL PRIMARY KEY,
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
  
 CREATE TABLE osiris_services_mounts (
  osiris_service_id BIGINT NOT NULL REFERENCES osiris_services (id),
  user_mount_id BIGINT NOT NULL REFERENCES osiris_user_mounts (id),
  target_mount_path    CHARACTER VARYING(255)	NOT NULL	 
);