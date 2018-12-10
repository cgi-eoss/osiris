 --User endpoints
 CREATE TABLE osiris_user_endpoints (
  id      BIGSERIAL PRIMARY KEY,
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
