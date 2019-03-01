--Add relations between osiris files

CREATE TYPE osiris_files_relation_type AS ENUM ('VISUALIZATION_OF');

CREATE TABLE osiris_files_relations (
  id                  bigserial           primary key,
  source_file         bigint              NOT NULL REFERENCES osiris_files (id) ON DELETE CASCADE,
  target_file         bigint              NOT NULL REFERENCES osiris_files (id) ON DELETE CASCADE,
  type                osiris_files_relation_type NOT NULL
);

CREATE INDEX osiris_files_relations_source_idx
  ON osiris_files_relations (source_file);
CREATE INDEX osiris_files_relations_target_idx
  ON osiris_files_relations (target_file);
CREATE UNIQUE INDEX osiris_files_source_target_type_idx
  ON osiris_files_relations (source_file,target_file,type);

