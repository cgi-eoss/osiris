--Add worker id to job
ALTER TABLE osiris_jobs
ADD COLUMN worker_id CHARACTER VARYING(255);

-- Insert the relation
ALTER TABLE osiris_jobs
  ADD COLUMN parent_job_id BIGINT REFERENCES osiris_jobs (id);
 
