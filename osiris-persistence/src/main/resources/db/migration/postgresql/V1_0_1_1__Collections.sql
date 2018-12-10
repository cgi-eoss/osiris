 --User collections
 CREATE TABLE osiris_collections (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES osiris_users (uid),
  name 	  CHARACTER VARYING(255)	    NOT NULL,
  identifier CHARACTER VARYING(255) UNIQUE NOT NULL,
  description TEXT, 
  products_type    CHARACTER VARYING(255)				
);

-- Insert the relation
ALTER TABLE osiris_files
  ADD COLUMN collection_id BIGINT REFERENCES osiris_collections (id);
  
CREATE UNIQUE INDEX osiris_collections_name_owner_idx
  ON osiris_collections (name, owner);
  CREATE UNIQUE INDEX osiris_collections_identifier_idx
  ON osiris_collections (identifier);
CREATE INDEX osiris_collections_name_idx
  ON osiris_collections (name);
CREATE INDEX osiris_collections_owner_idx
  ON osiris_collections (owner);
