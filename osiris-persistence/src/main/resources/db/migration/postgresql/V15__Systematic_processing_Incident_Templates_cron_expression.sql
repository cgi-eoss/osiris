--Cron expression for systematic processings and incident processing templates

ALTER TABLE osiris_systematic_processings ADD COLUMN cron_expression VARCHAR(255);

ALTER TABLE osiris_incident_processing_templates ADD COLUMN cron_expression VARCHAR(255);