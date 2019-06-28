 -- Incidents to collections
CREATE TABLE osiris_incidents_collections (
  incident_id BIGINT NOT NULL REFERENCES osiris_incidents (id),
  collection_id       BIGINT NOT NULL REFERENCES osiris_collections (id),
  PRIMARY KEY (incident_id, collection_id)
);

CREATE UNIQUE INDEX osiris_incidents_collections_idx
  ON osiris_incidents_collections (incident_id, collection_id);
  