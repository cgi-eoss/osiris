-- Adds the cast for the geoserver_store_type

CREATE OR REPLACE FUNCTION geoserver_store_type_cast(VARCHAR)
  RETURNS geoserver_store_type AS $$ SELECT ('' || $1) :: geoserver_store_type $$ LANGUAGE SQL IMMUTABLE;
CREATE CAST ( VARCHAR AS geoserver_store_type )
WITH FUNCTION geoserver_store_type_cast(VARCHAR) AS IMPLICIT;