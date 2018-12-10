 --Collections Publication Request
  
ALTER TABLE osiris_jobs ADD COLUMN gui_endpoint CHARACTER VARYING(255);

ALTER TABLE osiris_services ADD COLUMN strip_proxy_path BOOLEAN DEFAULT TRUE;

