-- Adds service external uri

ALTER TABLE osiris_services
  ADD COLUMN external_uri CHARACTER VARYING(255);
