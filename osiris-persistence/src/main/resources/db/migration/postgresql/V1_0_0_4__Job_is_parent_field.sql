--Add isParent boolean to job
ALTER TABLE osiris_jobs
ADD COLUMN is_parent BOOLEAN DEFAULT FALSE;

 
