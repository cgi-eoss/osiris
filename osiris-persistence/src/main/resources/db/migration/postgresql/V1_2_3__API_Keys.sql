 --API keys
 
 CREATE TABLE osiris_api_keys (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES osiris_users (uid),
  api_key  CHARACTER VARYING(255)
  );  
  
CREATE UNIQUE INDEX osiris_api_keys_owner_idx 
ON osiris_api_keys (owner);
