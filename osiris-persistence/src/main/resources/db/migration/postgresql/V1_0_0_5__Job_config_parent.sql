 --Reference to parent in job config
ALTER TABLE osiris_job_configs ADD COLUMN parent BIGINT REFERENCES osiris_jobs(id);
  