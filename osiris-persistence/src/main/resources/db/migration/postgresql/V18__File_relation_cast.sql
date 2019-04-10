-- Adds the cast for the osiris_files_relation_type

CREATE OR REPLACE FUNCTION osiris_files_relation_type_cast(VARCHAR)
  RETURNS osiris_files_relation_type AS $$ SELECT ('' || $1) :: osiris_files_relation_type $$ LANGUAGE SQL IMMUTABLE;
CREATE CAST ( VARCHAR AS osiris_files_relation_type )
WITH FUNCTION osiris_files_relation_type_cast(VARCHAR) AS IMPLICIT;