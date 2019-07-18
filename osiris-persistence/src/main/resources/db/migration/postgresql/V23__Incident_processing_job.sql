 --Incident processing job
 
 ALTER TABLE osiris_incident_processings ADD COLUMN job BIGINT REFERENCES osiris_jobs(id);
  